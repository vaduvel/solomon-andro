package ro.solomon.moments

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import ro.solomon.core.moments.MomentType
import ro.solomon.llm.LLMProvider

data class MomentOutput(
    val momentType: MomentType,
    val contextJSON: String,
    val promptSent: String,
    val llmResponse: String,
    val generatedAtEpochSeconds: Long = System.currentTimeMillis() / 1000L
) {
    val wordCount: Int get() = llmResponse.split(Regex("""\s+""")).count { it.isNotEmpty() }
    val isWithinWordLimit: Boolean get() = wordCount <= momentType.maxWords
    val hasResponse: Boolean get() = llmResponse.trim().isNotEmpty()
}

interface MomentBuilder<C> {
    val momentType: MomentType
    val systemPrompt: String
    val contextSerializer: KSerializer<C>

    fun buildContextJSON(context: C, json: Json): String =
        json.encodeToString(contextSerializer, context)

    suspend fun build(context: C, llm: LLMProvider, json: Json): MomentOutput {
        val contextJSON = buildContextJSON(context, json)
        val response = llm.generate(
            systemPrompt = systemPrompt,
            userContext = contextJSON,
            maxWords = momentType.maxWords
        )
        return MomentOutput(
            momentType = momentType,
            contextJSON = contextJSON,
            promptSent = systemPrompt,
            llmResponse = response
        )
    }
}
