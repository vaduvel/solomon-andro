package ro.solomon.llm

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL

@Serializable
private data class OllamaGenerateRequest(
    val model: String,
    val prompt: String,
    val stream: Boolean = false,
    val think: Boolean = false,
    val options: OllamaOptions? = null
)

@Serializable
private data class OllamaOptions(
    val temperature: Double,
    @kotlinx.serialization.SerialName("top_p") val topP: Double,
    @kotlinx.serialization.SerialName("num_predict") val numPredict: Int
)

@Serializable
private data class OllamaResponse(
    val response: String = "",
    val thinking: String? = null,
    val done: Boolean = false,
    @kotlinx.serialization.SerialName("eval_count") val evalCount: Int? = null
)

sealed class OllamaError : Exception() {
    object InvalidURL : OllamaError()
    data class HttpError(val statusCode: Int) : OllamaError()
    object EmptyResponse : OllamaError()
    data class NetworkError(val causeValue: Throwable) : OllamaError() {
        override val cause: Throwable get() = causeValue
    }
}

class OllamaLLMProvider(
    private val baseURL: String = SolomonLLM.DEFAULT_BASE_URL,
    private val model: String = SolomonLLM.DEFAULT_MODEL,
    private val temperature: Double = 0.4,
    private val topP: Double = 0.9,
    private val timeoutSeconds: Long = SolomonLLM.REQUEST_TIMEOUT_SECONDS
) : LLMProvider {

    override val isReady: Boolean = true

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    override suspend fun generate(
        systemPrompt: String,
        userContext: String,
        maxWords: Int
    ): String = withContext(Dispatchers.IO) {
        val fullPrompt = buildPrompt(systemPrompt, userContext)
        val numPredict = maxOf(maxWords * 5, 200)

        val url = runCatching { URL("$baseURL/api/generate") }.getOrNull()
            ?: throw OllamaError.InvalidURL

        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = (timeoutSeconds * 1000).toInt()
            readTimeout = (timeoutSeconds * 1000).toInt()
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
        }

        val body = OllamaGenerateRequest(
            model = model,
            prompt = fullPrompt,
            stream = false,
            think = false,
            options = OllamaOptions(temperature, topP, numPredict)
        )

        try {
            conn.outputStream.use { os ->
                json.encodeToString(OllamaGenerateRequest.serializer(), body)
                    .toByteArray(Charsets.UTF_8)
                    .let { os.write(it) }
            }

            val status = conn.responseCode
            if (status !in 200..299) {
                throw OllamaError.HttpError(status)
            }

            val raw = conn.inputStream.bufferedReader().use { it.readText() }
            val parsed = json.decodeFromString(OllamaResponse.serializer(), raw)
            val result = parsed.response.trim()
            if (result.isEmpty()) throw OllamaError.EmptyResponse
            result
        } catch (e: OllamaError) {
            throw e
        } catch (e: Throwable) {
            throw OllamaError.NetworkError(e)
        } finally {
            conn.disconnect()
        }
    }

    private fun Throwable.causeValue(): Throwable = this

    private fun buildPrompt(system: String, context: String): String =
        """$system

Context JSON:
$context"""
}
