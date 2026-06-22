package ro.solomon.app.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ro.solomon.app.di.ServiceLocator
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
                text = "Buna! Sunt Solomon. Spune-mi ce vrei sa afli despre banii tai.\n\nPot sa te ajut cu buget, plan, scenarii sau sfaturi financiare.\nExemple:\n- cat am cheltuit luna asta\n- adauga o chirie de 1500 RON pe 5\n- cum stau cu banii?\n- ce fac azi?"
            )
        ),
        val draft: String = "",
        val thinking: Boolean = false,
        val voiceEnabled: Boolean = false
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    fun setDraft(v: String) { _state.value = _state.value.copy(draft = v) }

    fun toggleVoice() {
        val enabled = !_state.value.voiceEnabled
        _state.value = _state.value.copy(voiceEnabled = enabled)
        if (!enabled) ro.solomon.app.services.SolomonTTS.stop()
    }

    fun speakMessage(text: String) {
        ro.solomon.app.services.SolomonTTS.speak(text)
    }

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

    // Solomon Coach engine: LLMAgentTool.systemInstructions() teaches the model
    // to call tools via <TOOL>{...}</TOOL>. LLMToolExecutor runs each tool call
    // and feeds the result back. This loop is what lets Solomon give advice even
    // when it cannot see all expenses directly (the user's core requirement).
    private suspend fun handleUserText(text: String) {
        val system = SolomonTools.systemPrompt + "\n\n" +
            ro.solomon.llm.LLMAgentTool.systemInstructions()
        val executor = ro.solomon.app.ui.tools.LLMToolExecutor(ServiceLocator.appContext)
        var context = buildUserContext(text)
        var iterations = 0
        val maxIterations = 4

        while (iterations < maxIterations) {
            iterations++
            val raw = runLLMText(system, context)
            val call = ro.solomon.llm.AgentToolParser.parse(raw)
            if (call == null || !ro.solomon.llm.LLMAgentTool.isKnownTool(call.name)) {
                val reply = ro.solomon.llm.AgentToolParser.stripToolTags(raw).ifBlank { raw }
                appendAssistant(reply)
                return
            }
            // Show tool status to user while executing
            _state.value = _state.value.copy(
                messages = _state.value.messages + Message(role = Role.Tool, text = call.statusLabel)
            )
            val toolResult = try {
                executor.execute(call)
            } catch (t: Throwable) {
                "[Eroare tool ${call.name}] ${t.message ?: "necunoscuta"}"
            }
            context = context + "\n\n[Rezultat ${call.name}]:\n$toolResult\n\n" +
                "Pe baza rezultatului de mai sus, raspunde utilizatorului in romana, clar si scurt. " +
                "Nu mai chema alt tool daca nu mai e nevoie. " +
                "Daca mai ai nevoie de date, cheama urmatorul tool printr-un singur <TOOL>...</TOOL>."
        }
        // After max iterations, force a plain-text answer
        val finalText = runLLMText(SolomonTools.systemPrompt, context)
        appendAssistant(
            ro.solomon.llm.AgentToolParser.stripToolTags(finalText).ifBlank { finalText }
        )
    }

    private suspend fun runLLMText(system: String, context: String): String {
        return try {
            ServiceLocator.llm.generate(
                systemPrompt = system,
                userContext = context,
                maxWords = 220
            )
        } catch (_: Throwable) {
            "Imi pare rau, am o problema tehnica momentan. Incearca din nou in cateva secunde."
        }
    }

    private fun appendAssistant(text: String) {
        val cleaned = ro.solomon.core.util.AdvisorTextCleaner.clean(text)
        _state.value = _state.value.copy(
            messages = _state.value.messages + Message(role = Role.Assistant, text = cleaned)
        )
        if (_state.value.voiceEnabled) ro.solomon.app.services.SolomonTTS.speak(cleaned)
    }

    private suspend fun buildUserContext(userText: String): String {
        val profile = ServiceLocator.userRepo.fetchProfile()
        val txns = ServiceLocator.txnRepo.fetchAll()
        val obligs = ServiceLocator.obligationRepo.fetchAll()
        val subs = ServiceLocator.subRepo.fetchAll()
        val goals = ServiceLocator.goalRepo.fetchAll()
        val cashFlow = ServiceLocator.cashFlow.analyze(
            transactions = txns,
            referenceDate = System.currentTimeMillis()
        )
        val detectedMoment = ro.solomon.app.services.TodayContextBridge.detectedToday
        val momentLine = if (detectedMoment != null && detectedMoment.isFromToday) {
            "\nMoment detectat azi: ${detectedMoment.humanReadable}"
        } else ""
        return """
            Nume: ${profile?.demographics?.name ?: "necunoscut"}
            Addressing: ${profile?.demographics?.addressing?.name ?: "tu"}
            Venit mediu lunar: ${cashFlow.monthlyIncomeAvg.amount / 100} RON
            Cheltuieli medii lunare: ${cashFlow.monthlySpendingAvg.amount / 100} RON
            Obligatii lunare (${obligs.size}): ${obligs.sumOf { it.amount.amount } / 100} RON
            Abonamente active (${subs.size}): ${subs.sumOf { it.amountMonthly.amount } / 100} RON/luna
            Obiective (${goals.size}): ${goals.joinToString { it.destination ?: it.kind.displayNameRO }}
            Tranzactii istorice: ${txns.size}$momentLine

            Intrebare utilizator: $userText
        """.trimIndent()
    }
}
