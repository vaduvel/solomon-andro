package ro.solomon.core.enablebanking

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec
import java.time.Instant
import java.util.Base64

object JWTSigner {
    class SigningError(message: String) : Exception(message)

    fun generate(appID: String, pem: String, ttlSeconds: Long = 3600): String {
        val now = Instant.now().epochSecond
        val exp = now + ttlSeconds

        val header = """{"typ":"JWT","alg":"RS256","kid":"$appID"}"""
        val payload = """{"iss":"enablebanking.com","aud":"api.enablebanking.com","iat":$now,"exp":$exp}"""

        val headerB64 = base64URLEncode(header.toByteArray())
        val payloadB64 = base64URLEncode(payload.toByteArray())
        val signingInput = "$headerB64.$payloadB64"

        val privateKey = parsePrivateKey(pem)
        val signature = sign(signingInput.toByteArray(), privateKey)
        val signatureB64 = base64URLEncode(signature)

        return "$signingInput.$signatureB64"
    }

    private fun parsePrivateKey(pem: String): PrivateKey {
        val cleaned = pem.lines()
            .filter { !it.contains("BEGIN") && !it.contains("END") && it.isNotBlank() }
            .joinToString("")

        val derBytes = Base64.getDecoder().decode(cleaned)
        val pkcs8Bytes = if (isPKCS1(derBytes)) pkcs1ToPkcs8(derBytes) else derBytes

        val keySpec = PKCS8EncodedKeySpec(pkcs8Bytes)
        val keyFactory = KeyFactory.getInstance("RSA")
        return keyFactory.generatePrivate(keySpec)
    }

    private fun isPKCS1(der: ByteArray): Boolean {
        val text = try {
            String(der, 0, Math.min(der.size, 16), Charsets.ISO_8859_1)
        } catch (_: Exception) { "" }
        return der.isNotEmpty() && der[0].toInt() == 0x30 && !text.contains("BEGIN")
    }

    private fun pkcs1ToPkcs8(pkcs1: ByteArray): ByteArray {
        val totalLen = pkcs1.size + 26
        val bytes = mutableListOf<Byte>(
            0x30, (totalLen shr 8 and 0xFF).toByte(), (totalLen and 0xFF).toByte(),
            0x02, 0x01, 0x00,
            0x30, 0x0D, 0x06, 0x09, 0x2A.toByte(), 0x86.toByte(), 0x48, 0x86.toByte(),
            0xF7.toByte(), 0x0D, 0x01, 0x01, 0x01, 0x05, 0x00,
            0x04, (pkcs1.size shr 8 and 0xFF).toByte(), (pkcs1.size and 0xFF).toByte()
        )
        bytes.addAll(pkcs1.toList())
        return bytes.toByteArray()
    }

    private fun sign(data: ByteArray, key: PrivateKey): ByteArray {
        val sig = Signature.getInstance("SHA256withRSA")
        sig.initSign(key)
        sig.update(data)
        return sig.sign()
    }

    private fun base64URLEncode(data: ByteArray): String {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data)
    }
}

sealed class EnableBankingError(message: String) : Exception(message) {
    class NotConfigured : EnableBankingError("Enable Banking not configured")
    class HTTPError(val code: Int, val body: String) : EnableBankingError("HTTP $code")
    class DecodingError(reason: String) : EnableBankingError("Decoding: $reason")
    class NetworkError(cause: Throwable) : EnableBankingError("Network: ${cause.message}")
    class JWTError(cause: Throwable) : EnableBankingError("JWT: ${cause.message}")
}

object EnableBankingClient {
    private val json = Json { ignoreUnknownKeys = true }

    val isConfigured: Boolean get() = EnableBankingConfig.isConfigured

    fun listBanksRO(): List<ASPSP> {
        val url = URL("${EnableBankingConfig.baseURL}/aspsps?country=RO&psu_type=personal&service=AIS")
        val response: ASPSPList = request(url, "GET")
        return response.aspsps
    }

    fun startAuth(bank: ASPSP, validUntil: String? = null): AuthResponse {
        val vu = validUntil ?: Instant.now().plusSeconds(180L * 86400).toString()
        val body = AuthRequest(
            access = AccessScope(validUntil = vu),
            aspsp = ASPSPRef(name = bank.name, country = bank.country),
            state = java.util.UUID.randomUUID().toString(),
            redirectUrl = EnableBankingConfig.redirectURL,
            psuType = "personal",
            authMethod = bank.authMethods?.firstOrNull()?.name,
            language = "ro"
        )
        val url = URL("${EnableBankingConfig.baseURL}/auth")
        return request(url, "POST", body)
    }

    fun createSession(code: String): SessionResponse {
        val body = SessionCreateRequest(code = code)
        val url = URL("${EnableBankingConfig.baseURL}/sessions")
        return request(url, "POST", body)
    }

    fun transactions(
        sessionID: String,
        accountID: String,
        dateFrom: String? = null,
        dateTo: String? = null,
        continuationKey: String? = null,
    ): TransactionsResponse {
        val qs = mutableListOf<String>()
        dateFrom?.let { qs.add("date_from=$it") }
        dateTo?.let { qs.add("date_to=$it") }
        if (continuationKey == null) qs.add("strategy=longest")
        continuationKey?.let { qs.add("continuation_key=$it") }
        val q = if (qs.isNotEmpty()) "?${qs.joinToString("&")}" else ""
        val url = URL("${EnableBankingConfig.baseURL}/accounts/${accountID}/transactions$q")
        return request(url, "GET", extraHeaders = mapOf(
            "Psu-Id-Type" to "session",
            "Psu-Id" to sessionID
        ))
    }

    fun balances(sessionID: String, accountID: String): BalancesResponse {
        val url = URL("${EnableBankingConfig.baseURL}/accounts/${accountID}/balances")
        return request(url, "GET", extraHeaders = mapOf(
            "Psu-Id-Type" to "session",
            "Psu-Id" to sessionID
        ))
    }

    fun revokeSession(sessionID: String) {
        val url = URL("${EnableBankingConfig.baseURL}/sessions/$sessionID")
        rawRequest(url, "DELETE")
    }

    private inline fun <reified T> request(
        url: URL, method: String, body: Any? = null,
        extraHeaders: Map<String, String> = emptyMap()
    ): T {
        val data = rawRequest(url, method, body?.let { json.encodeToString(it) }, extraHeaders)
        return try {
            json.decodeFromString(data)
        } catch (e: Exception) {
            throw EnableBankingError.DecodingError(e.message ?: "unknown")
        }
    }

    private fun rawRequest(
        url: URL, method: String, body: String? = null,
        extraHeaders: Map<String, String> = emptyMap()
    ): String {
        if (!EnableBankingConfig.isConfigured) throw EnableBankingError.NotConfigured()

        val jwt: String = try {
            val appID = EnableBankingConfig.applicationID!!
            val pem = EnableBankingConfig.privateKeyPEM!!
            JWTSigner.generate(appID, pem)
        } catch (e: Exception) {
            throw EnableBankingError.JWTError(e)
        }

        val conn = url.openConnection() as HttpURLConnection
        conn.apply {
            requestMethod = method
            setRequestProperty("Authorization", "Bearer $jwt")
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
            extraHeaders.forEach { (k, v) -> setRequestProperty(k, v) }
            connectTimeout = 30_000
            readTimeout = 60_000
            doInput = true
            if (body != null) {
                doOutput = true
                outputStream.use { os ->
                    os.write(body.toByteArray())
                }
            }
        }

        val responseCode = conn.responseCode
        val responseBody = if (responseCode in 200..299) {
            conn.inputStream.bufferedReader().readText()
        } else {
            val errBody = try { conn.errorStream?.bufferedReader()?.readText() ?: "" } catch (_: Exception) { "" }
            throw EnableBankingError.HTTPError(responseCode, errBody)
        }
        conn.disconnect()
        return responseBody
    }
}
