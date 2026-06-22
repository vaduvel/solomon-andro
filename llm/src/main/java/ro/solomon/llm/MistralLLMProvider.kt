package ro.solomon.llm

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.net.HttpURLConnection
import java.net.URL

class MistralLLMProvider(
    private val apiKey: String,
    private val model: String = "mistral-small-latest",
    private val temperature: Double = 0.4,
    private val topP: Double = 0.9,
    private val maxTokens: Int = 600,
    private val timeoutSeconds: Long = 60L
) : LLMProvider, LLMToolAwareProvider {

    override val isReady: Boolean get() = apiKey.isNotBlank()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    override suspend fun generate(
        systemPrompt: String,
        userContext: String,
        maxWords: Int
    ): String = withContext(Dispatchers.IO) {
        val body = buildChatRequest(
            systemPrompt = systemPrompt,
            userContext = userContext,
            tools = null,
            toolChoice = null,
            maxTokens = maxOf(maxWords * 5, 200)
        )
        val response = postMistral(body)
        response.choices.firstOrNull()?.message?.content?.takeIf { it.isNotBlank() }
            ?: throw LLMError.EmptyResponse
    }

    override suspend fun generateWithToolsInternal(
        systemPrompt: String,
        userContext: String,
        tools: List<LLMTool>,
        maxWords: Int
    ): LLMResponse = withContext(Dispatchers.IO) {
        val body = buildChatRequest(
            systemPrompt = systemPrompt,
            userContext = userContext,
            tools = tools.takeIf { it.isNotEmpty() },
            toolChoice = "auto",
            maxTokens = maxOf(maxWords * 5, 300)
        )
        val response = postMistral(body)
        val msg = response.choices.firstOrNull()?.message
            ?: throw LLMError.EmptyResponse

        val calls = msg.toolCalls.orEmpty().map { tc ->
            LLMToolCall(
                id = tc.id,
                name = tc.function.name,
                arguments = parseArgs(tc.function.arguments)
            )
        }
        LLMResponse(text = msg.content.orEmpty(), toolCalls = calls)
    }

    @Serializable
    private data class ChatRequest(
        val model: String,
        val messages: List<ChatMessage>,
        val temperature: Double,
        @kotlinx.serialization.SerialName("top_p") val topP: Double,
        @kotlinx.serialization.SerialName("max_tokens") val maxTokens: Int,
        val tools: List<ToolDef>? = null,
        @kotlinx.serialization.SerialName("tool_choice") val toolChoice: String? = null
    )

    @Serializable
    private data class ChatMessage(
        val role: String,
        val content: String? = null,
        @kotlinx.serialization.SerialName("tool_calls") val toolCalls: List<ToolCallWire>? = null,
        @kotlinx.serialization.SerialName("tool_call_id") val toolCallId: String? = null
    )

    @Serializable
    private data class ToolDef(
        val type: String = "function",
        val function: ToolFunction
    )

    @Serializable
    private data class ToolFunction(
        val name: String,
        val description: String,
        val parameters: JsonElement
    )

    @Serializable
    private data class ToolCallWire(
        val id: String,
        val type: String = "function",
        val function: ToolCallFunction
    )

    @Serializable
    private data class ToolCallFunction(
        val name: String,
        val arguments: String
    )

    @Serializable
    private data class ChatResponse(
        val id: String,
        val model: String,
        val choices: List<Choice>,
        val usage: Usage? = null
    )

    @Serializable
    private data class Choice(
        val index: Int,
        val message: ResponseMessage,
        @kotlinx.serialization.SerialName("finish_reason") val finishReason: String
    )

    @Serializable
    private data class ResponseMessage(
        val role: String,
        val content: String? = null,
        @kotlinx.serialization.SerialName("tool_calls") val toolCalls: List<ToolCallWire>? = null
    )

    @Serializable
    private data class Usage(
        @kotlinx.serialization.SerialName("prompt_tokens") val promptTokens: Int,
        @kotlinx.serialization.SerialName("completion_tokens") val completionTokens: Int,
        @kotlinx.serialization.SerialName("total_tokens") val totalTokens: Int
    )

    private fun buildChatRequest(
        systemPrompt: String,
        userContext: String,
        tools: List<LLMTool>?,
        toolChoice: String?,
        maxTokens: Int
    ): String {
        // Strip personal identifiers before the payload leaves the device.
        val safeContext = PiiScrubber.scrub(userContext)
        val messages = listOf(
            ChatMessage(
                role = "system",
                content = "$systemPrompt\n\nContext utilizator:\n$safeContext"
            ),
            ChatMessage(role = "user", content = "Continuă.")
        )
        val toolDefs = tools?.map { t ->
            ToolDef(function = ToolFunction(t.name, t.description, t.parameters))
        }
        val req = ChatRequest(
            model = model,
            messages = messages,
            temperature = temperature,
            topP = topP,
            maxTokens = maxTokens,
            tools = toolDefs,
            toolChoice = toolChoice
        )
        return json.encodeToString(ChatRequest.serializer(), req)
    }

    private fun postMistral(jsonBody: String): ChatResponse {
        val url = URL("https://api.mistral.ai/v1/chat/completions")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = (timeoutSeconds * 1000).toInt()
            readTimeout = (timeoutSeconds * 1000).toInt()
            doOutput = true
            doInput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Authorization", "Bearer $apiKey")
            setRequestProperty("Accept", "application/json")
        }
        return try {
            conn.outputStream.use { os ->
                os.write(jsonBody.toByteArray(Charsets.UTF_8))
            }
            val status = conn.responseCode
            if (status !in 200..299) {
                val errBody = (conn.errorStream ?: conn.inputStream).bufferedReader().use { it.readText() }
                throw LLMError.ProviderError("Mistral HTTP $status: ${errBody.take(200)}")
            }
            val raw = conn.inputStream.bufferedReader().use { it.readText() }
            json.decodeFromString(ChatResponse.serializer(), raw)
        } catch (e: LLMError) {
            throw e
        } catch (e: Throwable) {
            throw LLMError.NetworkError(e.message ?: "unknown", e)
        } finally {
            conn.disconnect()
        }
    }

    private fun parseArgs(args: String): JsonElement {
        return try {
            json.parseToJsonElement(args)
        } catch (_: Throwable) {
            JsonPrimitive(args)
        }
    }
}
