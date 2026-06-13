package ro.solomon.llm

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class AgentToolCall(
    val name: String,
    val arguments: Map<String, String> = emptyMap()
)

@Serializable
data class AgentToolResult(
    val name: String,
    val success: Boolean,
    val summary: String
)

abstract class LLMAgentTool {
    abstract val name: String
    abstract val descriptionRO: String
    abstract suspend fun execute(call: AgentToolCall): AgentToolResult

    protected fun parseArgs(json: String, expectedKeys: List<String>): Result<Map<String, String>> {
        return runCatching {
            val parsed = Json.parseToJsonElement(json).toString()
            val pairs = expectedKeys.mapNotNull { key ->
                Regex(""""$key"\s*:\s*"([^"]*)"""").find(parsed)?.groupValues?.get(1)?.let { key to it }
            }
            pairs.toMap()
        }
    }
}

class CalculatorTool : LLMAgentTool() {
    override val name = "calculator"
    override val descriptionRO = "Efectuează operații aritmetice simple (adunare, scădere, înmulțire, împărțire)."

    override suspend fun execute(call: AgentToolCall): AgentToolResult {
        val op = call.arguments["op"] ?: return AgentToolResult(name, false, "Lipsește operatorul")
        val a = call.arguments["a"]?.toDoubleOrNull() ?: return AgentToolResult(name, false, "Lipsește a")
        val b = call.arguments["b"]?.toDoubleOrNull() ?: return AgentToolResult(name, false, "Lipsește b")
        val result = when (op) {
            "add" -> a + b
            "sub" -> a - b
            "mul" -> a * b
            "div" -> if (b == 0.0) return AgentToolResult(name, false, "Împărțire la 0")
                     else a / b
            else -> return AgentToolResult(name, false, "Operator necunoscut: $op")
        }
        return AgentToolResult(name, true, "%.2f".format(result))
    }
}

class TodayBalanceTool : LLMAgentTool() {
    override val name = "today_balance"
    override val descriptionRO = "Returnează soldul curent estimat al userului."

    override suspend fun execute(call: AgentToolCall): AgentToolResult {
        val amount = call.arguments["amount"] ?: "0"
        return AgentToolResult(name, true, "Sold curent estimat: $amount RON")
    }
}
