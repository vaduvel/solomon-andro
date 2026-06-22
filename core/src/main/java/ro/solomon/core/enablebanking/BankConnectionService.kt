package ro.solomon.core.enablebanking

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ro.solomon.core.domain.FlowDirection
import ro.solomon.core.domain.Money
import ro.solomon.core.domain.Transaction
import ro.solomon.core.domain.TransactionCategory
import ro.solomon.core.domain.TransactionSource
import ro.solomon.core.domain.deterministicUUID
import ro.solomon.core.notifications.MerchantCategoryMatcher
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

data class BankConnection(
    val sessionID: String,
    val aspspName: String,
    val aspspCountry: String,
    val accounts: List<BankAccount>,
    val connectedAt: Instant,
    val validUntil: Instant,
    var lastSyncAt: Instant? = null,
) {
    val isExpired: Boolean get() = Instant.now() > validUntil
}

data class BankAccount(
    val uid: String,
    val iban: String? = null,
    val name: String? = null,
    val currency: String = "RON",
)

data class PendingAuth(
    val bank: ASPSP,
    val state: String,
    val startedAt: Instant = Instant.now(),
)

object BankConnectionService {
    var onTransactionIngested: ((Transaction) -> Unit)? = null
    var persistenceDir: File? = null

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val connections = mutableListOf<BankConnection>()
    private var _pendingAuth: PendingAuth? = null
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    val allConnections: List<BankConnection> get() = connections.toList()
    val pendingAuth: PendingAuth? get() = _pendingAuth

    var isSyncing: Boolean = false
        private set
    var lastSyncError: String? = null
        private set
    var lastIngestedCount: Int = 0
        private set

    fun initialize() { loadFromDisk() }

    fun startConnect(bank: ASPSP): String {
        val auth = EnableBankingClient.startAuth(bank)
        _pendingAuth = PendingAuth(bank = bank, state = auth.authorizationId)
        return auth.url
    }

    suspend fun handleCallback(url: String): Boolean {
        val scheme = "solomon://"
        if (!url.startsWith(scheme)) return false
        val hostAndQuery = url.removePrefix(scheme)
        if (!hostAndQuery.startsWith("bankcallback")) return false
        val pending = _pendingAuth ?: return false

        val code = extractQueryParam(url, "code")
        val receivedState = extractQueryParam(url, "state")

        if (receivedState != null && receivedState != pending.state) {
            lastSyncError = "Auth state mismatch"
            _pendingAuth = null
            return true
        }

        if (code == null) {
            val err = extractQueryParam(url, "error") ?: "unknown"
            lastSyncError = "Auth error: $err"
            _pendingAuth = null
            return true
        }

        _pendingAuth = null

        return try {
            val session = EnableBankingClient.createSession(code)
            val connection = BankConnection(
                sessionID = session.sessionId,
                aspspName = session.aspsp.name,
                aspspCountry = session.aspsp.country,
                accounts = session.accounts.map { acc ->
                    BankAccount(
                        uid = acc.uid,
                        iban = acc.accountId?.iban,
                        name = acc.name,
                        currency = acc.currency ?: "RON"
                    )
                },
                connectedAt = Instant.now(),
                validUntil = parseISO(session.access.validUntil) ?: Instant.now().plusSeconds(180L * 86400)
            )
            connections.add(connection)
            saveToDisk()
            scope.launch { syncConnection(connection) }
            true
        } catch (e: Exception) {
            lastSyncError = e.message
            false
        }
    }

    suspend fun disconnect(connection: BankConnection) {
        try { EnableBankingClient.revokeSession(connection.sessionID) } catch (_: Exception) {}
        connections.removeAll { it.sessionID == connection.sessionID }
        saveToDisk()
    }

    suspend fun syncAll(): Int {
        if (isSyncing) return 0
        isSyncing = true
        var total = 0
        for (c in connections.toList().filter { !it.isExpired }) {
            total += syncConnection(c)
        }
        lastIngestedCount = total
        isSyncing = false
        return total
    }

    suspend fun syncConnection(connection: BankConnection): Int {
        lastSyncError = null
        var ingested = 0
        val since = connection.lastSyncAt ?: Instant.now().minusSeconds(90L * 86400)
        val dateFrom = LocalDate.ofInstant(since, ZoneOffset.UTC).format(DateTimeFormatter.ISO_LOCAL_DATE)
        val dateTo = LocalDate.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_LOCAL_DATE)

        for (account in connection.accounts) {
            try {
                val response = EnableBankingClient.transactions(
                    sessionID = connection.sessionID,
                    accountID = account.uid,
                    dateFrom = dateFrom,
                    dateTo = dateTo
                )
                for (bt in response.transactions) {
                    val tx = mapToSolomon(bt, account.iban, connection.aspspName, account.currency)
                    if (tx != null) {
                        onTransactionIngested?.invoke(tx)
                        ingested++
                    }
                }
            } catch (e: Exception) {
                lastSyncError = e.message
            }
        }

        val idx = connections.indexOfFirst { it.sessionID == connection.sessionID }
        if (idx >= 0) {
            connections[idx] = connections[idx].copy(lastSyncAt = Instant.now())
            saveToDisk()
        }
        return ingested
    }

    fun mapToSolomon(
        bt: BankTransaction, accountIban: String?, bankName: String, accountCurrency: String
    ): Transaction? {
        val amountDecimal = bt.transactionAmount.amount.toBigDecimalOrNull() ?: return null
        val idSeed = "enablebanking|${bt.transactionId ?: bt.entryReference ?: ""}|" +
            "${bt.bookingDate ?: ""}|${bt.transactionAmount.amount}|${accountIban ?: ""}"
        val id = deterministicUUID(idSeed).toString()

        val amountCents = (amountDecimal.toDouble() * 100).toInt()
        val absAmount = kotlin.math.abs(amountCents)
        val direction = if (amountDecimal < java.math.BigDecimal.ZERO) FlowDirection.outgoing else FlowDirection.incoming

        val dateStr = bt.bookingDate ?: bt.valueDate ?: bt.transactionDate
        // Canonical Transaction.date is epoch MILLIS (matches manual/notification ingest and all date-window filters).
        val dateEpoch = try {
            LocalDate.parse(dateStr).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        } catch (_: Exception) {
            Instant.now().toEpochMilli()
        }

        val merchant = if (direction == FlowDirection.outgoing) bt.creditor?.name else bt.debtor?.name
        val category = if (!merchant.isNullOrBlank()) {
            MerchantCategoryMatcher.categoryFor(merchant)
        } else {
            TransactionCategory.unknown
        }

        val description = bt.remittanceInformation?.joinToString(" ")?.trim()?.takeIf { it.isNotEmpty() }

        return Transaction(
            id = id, date = dateEpoch, amount = Money(absAmount),
            direction = direction, category = category,
            merchant = merchant, description = description,
            source = TransactionSource.bank_connection,
        )
    }

    private fun extractQueryParam(url: String, param: String): String? {
        val qi = url.indexOf('?')
        if (qi < 0) return null
        val query = url.substring(qi + 1)
        return query.split('&').firstNotNullOfOrNull { kv ->
            val parts = kv.split('=', limit = 2)
            if (parts.size == 2 && parts[0] == param) parts[1] else null
        }
    }

    private fun parseISO(s: String): Instant? = try { Instant.parse(s) } catch (_: Exception) { null }

    private fun loadFromDisk() {
        val dir = persistenceDir ?: return
        val file = File(dir, "bank_connections.json")
        if (!file.exists()) return
        try {
            val data = file.readText()
            val list = json.decodeFromString<List<ConnectionDTO>>(data)
            connections.clear()
            connections.addAll(list.map { it.toDomain() })
        } catch (_: Exception) {}
    }

    private fun saveToDisk() {
        val dir = persistenceDir ?: return
        dir.mkdirs()
        val file = File(dir, "bank_connections.json")
        try {
            file.writeText(json.encodeToString(connections.map { ConnectionDTO.fromDomain(it) }))
        } catch (_: Exception) {}
    }

    @Serializable
    private data class ConnectionDTO(
        val sessionID: String, val aspspName: String, val aspspCountry: String,
        val accounts: List<AccountDTO>, val connectedAt: String, val validUntil: String,
        val lastSyncAt: String? = null,
    ) {
        fun toDomain() = BankConnection(
            sessionID = sessionID, aspspName = aspspName, aspspCountry = aspspCountry,
            accounts = accounts.map { it.toDomain() },
            connectedAt = Instant.parse(connectedAt),
            validUntil = Instant.parse(validUntil),
            lastSyncAt = lastSyncAt?.let { try { Instant.parse(it) } catch (_: Exception) { null } },
        )
        companion object {
            fun fromDomain(d: BankConnection) = ConnectionDTO(
                sessionID = d.sessionID, aspspName = d.aspspName, aspspCountry = d.aspspCountry,
                accounts = d.accounts.map { AccountDTO.fromDomain(it) },
                connectedAt = d.connectedAt.toString(), validUntil = d.validUntil.toString(),
                lastSyncAt = d.lastSyncAt?.toString(),
            )
        }
    }

    @Serializable
    private data class AccountDTO(
        val uid: String, val iban: String? = null, val name: String? = null, val currency: String = "RON",
    ) {
        fun toDomain() = BankAccount(uid = uid, iban = iban, name = name, currency = currency)
        companion object {
            fun fromDomain(d: BankAccount) = AccountDTO(uid = d.uid, iban = d.iban, name = d.name, currency = d.currency)
        }
    }
}
