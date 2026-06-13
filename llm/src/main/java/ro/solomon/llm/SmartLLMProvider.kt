package ro.solomon.llm

import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout

class SmartLLMProvider(
    private val primary: LLMProvider,
    private val fallback: LLMProvider
) : LLMProvider {

    override val isReady: Boolean
        get() = primary.isReady || fallback.isReady

    override suspend fun generate(
        systemPrompt: String,
        userContext: String,
        maxWords: Int
    ): String = tryGenerate(systemPrompt, userContext, maxWords, imageData = null)

    override suspend fun generate(
        systemPrompt: String,
        userContext: String,
        maxWords: Int,
        imageData: ByteArray?
    ): String = tryGenerate(systemPrompt, userContext, maxWords, imageData)

    private suspend fun tryGenerate(
        system: String,
        context: String,
        maxWords: Int,
        imageData: ByteArray?
    ): String {
        return try {
            withTimeout(45_000L) {
                primary.generate(system, context, maxWords, imageData)
            }
        } catch (_: TimeoutCancellationException) {
            fallback.generate(system, context, maxWords, imageData)
        } catch (e: LLMError) {
            if (imageData != null) {
                try {
                    fallback.generate(system, context, maxWords, imageData)
                } catch (_: Throwable) {
                    throw e
                }
            } else throw e
        } catch (e: Throwable) {
            try {
                fallback.generate(system, context, maxWords, imageData)
            } catch (_: Throwable) {
                throw LLMError.ProviderError(e.message ?: "unknown")
            }
        }
    }
}
