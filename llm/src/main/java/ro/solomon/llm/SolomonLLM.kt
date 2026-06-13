package ro.solomon.llm

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

interface LLMProvider {
    suspend fun generate(
        systemPrompt: String,
        userContext: String,
        maxWords: Int
    ): String

    suspend fun generate(
        systemPrompt: String,
        userContext: String,
        maxWords: Int,
        imageData: ByteArray?
    ): String = generate(systemPrompt, userContext, maxWords)

    val isReady: Boolean
}

suspend fun LLMProvider.generateWithTools(
    systemPrompt: String,
    userContext: String,
    tools: List<LLMTool>,
    maxWords: Int = 100
): LLMResponse {
    val aware = this as? LLMToolAwareProvider
    if (aware != null) {
        return aware.generateWithToolsInternal(systemPrompt, userContext, tools, maxWords)
    }
    val fallback = generate(systemPrompt, userContext, maxWords)
    return LLMResponse(text = fallback, toolCalls = emptyList())
}

interface LLMToolAwareProvider {
    suspend fun generateWithToolsInternal(
        systemPrompt: String,
        userContext: String,
        tools: List<LLMTool>,
        maxWords: Int
    ): LLMResponse
}

@Serializable
data class LLMTool(
    val name: String,
    val description: String,
    val parameters: JsonElement
)

@Serializable
data class LLMToolCall(
    val id: String,
    val name: String,
    val arguments: JsonElement
)

data class LLMResponse(
    val text: String,
    val toolCalls: List<LLMToolCall> = emptyList()
)

sealed class LLMError : Exception() {
    object ModelNotLoaded : LLMError()
    data class ContextTooLong(val charCount: Int) : LLMError()
    data class GenerationFailed(val reason: String) : LLMError()
    object Timeout : LLMError()
    object EmptyResponse : LLMError()
    data class ProviderError(override val message: String) : LLMError()
    data class NetworkError(override val message: String, val cause: Throwable? = null) : LLMError()
}

object SolomonLLM {
    const val VERSION = "1.0.0"
    const val DEFAULT_MODEL = "gemma4:e2b"
    const val DEFAULT_BASE_URL = "http://localhost:11434"
    const val MAX_CONTEXT_CHARS = 32_000
    const val REQUEST_TIMEOUT_SECONDS = 180L
}
