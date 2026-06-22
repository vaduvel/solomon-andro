package ro.solomon.app.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import ro.solomon.app.di.ServiceLocator
import ro.solomon.core.domain.CancellationDifficulty
import ro.solomon.core.domain.FlowDirection
import ro.solomon.core.domain.Goal
import ro.solomon.core.domain.GoalKind
import ro.solomon.core.domain.Money
import ro.solomon.core.domain.Obligation
import ro.solomon.core.domain.ObligationConfidence
import ro.solomon.core.domain.ObligationKind
import ro.solomon.core.domain.Subscription
import ro.solomon.core.domain.Transaction
import ro.solomon.core.domain.TransactionCategory
import ro.solomon.core.domain.TransactionSource
import ro.solomon.llm.LLMToolCall
import ro.solomon.llm.generateWithTools
import java.util.UUID

class ChatViewModel : ViewModel() {

    enum class Role { User, Assistant, System, Tool }

    data class Message(
        val id: String = UUID.randomUUID().toString(),
        val role: Role,
        val text: String,
        val timestamp: Long = System.currentTimeMillis()
    )

    data class State(
        val messages: List<Message> = listOf(
            Message(
                role = Role.Assistant,
                text = "Bună! Sunt Solomon. Spune-mi ce vrei să afli despre banii tăi — pot să te ajut cu buget, plan sau scenarii.\n\nPoți să-mi spui și lucruri de făcut, de exemplu:\n• adaugă o chirie de 1500 RON pe 5\n• cât am cheltuit luna asta\n• setează buget 500 RON la mâncare"
            )
        ),
        val draft: String = "",
        val thinking: Boolean = false
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    fun setDraft(v: String) { _state.value = _state.value.copy(draft = v) }

    fun send() {
        val text = _state.value.draft.trim()
        if (text.isEmpty() || _state.value.thinking) return
        val userMsg = Message(role = Role.User, text = text)
        _state.value = _state.value.copy(
            messages = _state.value.messages + userMsg,
            draft = "",
            thinking = true
        )
        viewModelScope.launch {
            handleUserText(text)
            _state.value = _state.value.copy(thinking = false)
        }
    }

    private suspend fun handleUserText(text: String) {
        val system = SolomonTools.systemPrompt
        val context = buildUserContext(text)

        val response = try {
            ServiceLocator.llm.generateWithTools(
                systemPrompt = system,
                userContext = context,
                tools = SolomonTools.all,
                maxWords = 100
            )
        } catch (_: Throwable) {
            null
        }

        if (response == null) {
            val reply = runLLMText(system, context)
            appendAssistant(reply)
            return
        }

        if (response.toolCalls.isNotEmpty()) {
            executeAndRespond(response.toolCalls)
            return
        }

        if (response.text.isNotBlank()) {
            appendAssistant(response.text.trim())
            return
        }

        val fallback = runLLMText(system, context)
        appendAssistant(fallback)
    }

    private suspend fun executeAndRespond(calls: List<LLMToolCall>) {
        val sb = StringBuilder()
        for (call in calls) {
            val result = dispatchToolCall(call)
            sb.appendLine(result)
        }
        val combined = sb.toString().trim()
        if (combined.isNotEmpty()) {
            _state.value = _state.value.copy(
                messages = _state.value.messages + Message(role = Role.Tool, text = combined)
            )
        }
    }

    private suspend fun dispatchToolCall(call: LLMToolCall): String {
        val args = call.arguments as? JsonObject ?: return "Eroare: argumente invalide."
        return when (call.name) {
            "add_transaction" -> {
                val amount = args.intOrZero("amount_ron")
                val direction = when (args.stringOrNull("direction")) {
                    "incoming" -> FlowDirection.incoming
                    else -> FlowDirection.outgoing
                }
                val cat = args.enumOrElse(args.stringOrNull("category"), TransactionCategory.entries.toList()) { TransactionCategory.unknown }
                val merchant = args.stringOrNull("merchant")
                if (amount <= 0) return "Suma trebuie să fie > 0."
                persistTransaction(amount, direction, cat, merchant)
                if (direction == FlowDirection.incoming) "Am notat venit: +${amount} RON la ${merchant ?: cat.displayNameRO}."
                else "Am notat cheltuială: -${amount} RON la ${merchant ?: cat.displayNameRO}."
            }
            "add_obligation" -> {
                val name = args.stringOrNull("name") ?: return "Lipsește numele obligației."
                val amount = args.intOrZero("amount_ron")
                val day = args.intOrZero("day_of_month").coerceIn(1, 31)
                val kind = args.enumOrElse(args.stringOrNull("kind"), ObligationKind.entries.toList()) { ObligationKind.other }
                if (amount <= 0) return "Suma trebuie să fie > 0."
                persistObligation(name, amount, day, kind)
                "Am adăugat obligația «$name» de $amount RON pe $day."
            }
            "add_goal" -> {
                val dest = args.stringOrNull("destination") ?: return "Lipsește destinația obiectivului."
                val amount = args.intOrZero("amount_target_ron")
                val months = args.intOrZero("deadline_months").let { if (it <= 0) 6 else it }
                if (amount <= 0) return "Suma trebuie să fie > 0."
                persistGoal(dest, amount, months)
                "Am adăugat obiectivul «$dest» cu ținta de $amount RON în $months luni."
            }
            "add_subscription" -> {
                val name = args.stringOrNull("name") ?: return "Lipsește numele abonamentului."
                val amount = args.intOrZero("amount_monthly_ron")
                val lastUsed = args.intOrZero("last_used_days_ago")
                if (amount <= 0) return "Suma trebuie să fie > 0."
                persistSubscription(name, amount, lastUsed)
                "Am adăugat abonamentul «$name» de $amount RON/lună."
            }
            "delete_obligation" -> {
                val name = args.stringOrNull("name") ?: return "Lipsește numele."
                deleteObligationByName(name)
            }
            "delete_goal" -> {
                val dest = args.stringOrNull("destination") ?: return "Lipsește destinația."
                deleteGoalByDest(dest)
            }
            "delete_subscription" -> {
                val name = args.stringOrNull("name") ?: return "Lipsește numele."
                deleteSubscriptionByName(name)
            }
            "set_category_limit" -> {
                val cat = args.enumOrElse(args.stringOrNull("category"), TransactionCategory.entries.toList()) { TransactionCategory.unknown }
                val amount = args.intOrZero("amount_ron")
                if (amount <= 0) return "Suma trebuie să fie > 0."
                ro.solomon.app.services.CategoryLimitsStore.setLimit(cat, amount)
                "Am setat bugetul de $amount RON pentru ${cat.displayNameRO}."
            }
            else -> "Nu știu cum să execut «${call.name}»."
        }
    }

    private suspend fun persistTransaction(
        amount: Int,
        direction: FlowDirection,
        cat: TransactionCategory,
        merchant: String?
    ) {
        val txn = Transaction(
            id = "txn-${System.currentTimeMillis()}",
            date = System.currentTimeMillis() / 1000,
            amount = Money(amount),
            direction = direction,
            category = cat,
            merchant = merchant,
            source = TransactionSource.manual_entry,
            categorizationConfidence = 1.0
        )
        ServiceLocator.txnRepo.save(txn)
    }

    private suspend fun persistObligation(
        name: String,
        amount: Int,
        day: Int,
        kind: ObligationKind
    ) {
        val now = System.currentTimeMillis() / 1000
        ServiceLocator.obligationRepo.save(
            Obligation(
                id = "obl-${System.currentTimeMillis()}",
                name = name,
                amount = Money(amount),
                dayOfMonth = day,
                kind = kind,
                confidence = ObligationConfidence.declared,
                since = now,
                nextDueDate = now + day * 86400L
            )
        )
    }

    private suspend fun persistGoal(destination: String, amount: Int, months: Int) {
        ServiceLocator.goalRepo.save(
            Goal(
                id = "goal-${System.currentTimeMillis()}",
                kind = GoalKind.custom,
                destination = destination,
                amountTarget = Money(amount),
                amountSaved = Money(0),
                deadline = System.currentTimeMillis() / 1000 + months * 30L * 86400L
            )
        )
    }

    private suspend fun persistSubscription(name: String, amount: Int, lastUsed: Int) {
        ServiceLocator.subRepo.save(
            Subscription(
                id = "sub-${System.currentTimeMillis()}",
                name = name,
                amountMonthly = Money(amount),
                lastUsedDaysAgo = if (lastUsed > 0) lastUsed else null,
                cancellationDifficulty = CancellationDifficulty.medium
            )
        )
    }

    private suspend fun deleteObligationByName(name: String): String {
        val all = ServiceLocator.obligationRepo.fetchAll()
        val match = all.firstOrNull { it.name.contains(name, ignoreCase = true) }
            ?: return "Nu am găsit obligația «$name»."
        ServiceLocator.obligationRepo.delete(match.id)
        return "Am șters obligația «${match.name}»."
    }

    private suspend fun deleteGoalByDest(destination: String): String {
        val all = ServiceLocator.goalRepo.fetchAll()
        val match = all.firstOrNull { it.destination?.contains(destination, ignoreCase = true) == true }
            ?: return "Nu am găsit obiectivul «$destination»."
        ServiceLocator.goalRepo.delete(match.id)
        return "Am șers obiectivul «${match.destination}»."
    }

    private suspend fun deleteSubscriptionByName(name: String): String {
        val all = ServiceLocator.subRepo.fetchAll()
        val match = all.firstOrNull { it.name.contains(name, ignoreCase = true) }
            ?: return "Nu am găsit abonamentul «$name»."
        ServiceLocator.subRepo.delete(match.id)
        return "Am șters abonamentul «${match.name}»."
    }

    private suspend fun runLLMText(system: String, context: String): String {
        return try {
            ServiceLocator.llm.generate(
                systemPrompt = system,
                userContext = context,
                maxWords = 120
            )
        } catch (_: Throwable) {
            "Îmi pare rău, am o problemă tehnică momentan. Încearcă din nou în câteva secunde."
        }
    }

    private fun appendAssistant(text: String) {
        val cleaned = ro.solomon.core.util.AdvisorTextCleaner.clean(text)
        _state.value = _state.value.copy(
            messages = _state.value.messages + Message(role = Role.Assistant, text = cleaned)
        )
    }

    private suspend fun buildUserContext(userText: String): String {
        val profile = ServiceLocator.userRepo.fetchProfile()
        val txns = ServiceLocator.txnRepo.fetchAll()
        val obligs = ServiceLocator.obligationRepo.fetchAll()
        val subs = ServiceLocator.subRepo.fetchAll()
        val goals = ServiceLocator.goalRepo.fetchAll()
        val cashFlow = ServiceLocator.cashFlow.analyze(transactions = txns, referenceDate = System.currentTimeMillis())
        val detectedMoment = ro.solomon.app.services.TodayContextBridge.detectedToday
        val momentLine = if (detectedMoment != null && detectedMoment.isFromToday) {
            "\n            Moment detectat azi: ${detectedMoment.humanReadable}"
        } else ""
        return """
            Nume: ${profile?.demographics?.name ?: "necunoscut"}
            Addressing: ${profile?.demographics?.addressing?.name ?: "tu"}
            Venit mediu lunar: ${cashFlow.monthlyIncomeAvg.amount} RON
            Cheltuieli medii lunare: ${cashFlow.monthlySpendingAvg.amount} RON
            Obligații lunare (${obligs.size}): ${obligs.sumOf { it.amount.amount }} RON
            Abonamente active (${subs.size}): ${subs.sumOf { it.amountMonthly.amount }} RON/lună
            Obiective (${goals.size}): ${goals.joinToString { it.destination ?: it.kind.displayNameRO }}
            Tranzacții istorice: ${txns.size}$momentLine

            Întrebare utilizator: $userText
        """.trimIndent()
    }
}

private fun JsonObject.intOrZero(key: String): Int {
    val prim = this[key] as? JsonPrimitive ?: return 0
    return prim.intOrNull ?: 0
}

private fun JsonObject.stringOrNull(key: String): String? {
    val prim = this[key] as? JsonPrimitive ?: return null
    return prim.contentOrNull
}

private inline fun <reified T : Enum<T>> JsonObject.enumOrElse(
    raw: String?,
    values: List<T>,
    fallback: () -> T
): T {
    if (raw.isNullOrBlank()) return fallback()
    return values.firstOrNull { it.name.equals(raw, ignoreCase = true) } ?: fallback()
}
