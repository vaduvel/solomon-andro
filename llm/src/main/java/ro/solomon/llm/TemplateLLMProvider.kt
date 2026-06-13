package ro.solomon.llm

import ro.solomon.core.moments.MomentType
import ro.solomon.core.moments.MomentUser

class TemplateLLMProvider : LLMProvider {

    override val isReady: Boolean = true

    override suspend fun generate(
        systemPrompt: String,
        userContext: String,
        maxWords: Int
    ): String {
        val momentHint = MomentType.entries.firstOrNull { systemPrompt.contains(it.name, ignoreCase = true) }
        val momentName = momentHint?.name ?: "azi"
        val user = extractUserName(userContext)
        val greet = if (user.isNotBlank()) "$user, " else ""

        val text = when {
            systemPrompt.contains("can_i_afford", ignoreCase = true) ||
            systemPrompt.contains("îmi permit", ignoreCase = true) ->
                "$greet pe baza datelor pe care le am, nu pot să-ți dau un răspuns exact fără să verific soldul și cheltuielile din această lună. Deschide Solomon pentru detalii."
            systemPrompt.contains("payday", ignoreCase = true) ||
            systemPrompt.contains("salariu", ignoreCase = true) ->
                "$greet salariul a intrat. Am rezervat automat obligațiile și abonamentele — verifică în tab-ul Azi cât ai rămas disponibil pe zi."
            systemPrompt.contains("spiral", ignoreCase = true) ->
                "$greet am observat un risc de spirală. Cel mai important pas acum e cel mai mic, nu cel mai mare. Deschide momentul Spiral Alert pentru planul în 3 pași."
            systemPrompt.contains("weekly", ignoreCase = true) ->
                "$greet rezumatul săptămânii e gata. Cea mai mare cheltuială și micile câștiguri sunt în tab-ul Azi."
            systemPrompt.contains("pattern", ignoreCase = true) ->
                "$greet dacă păstrezi ritmul curent, la finalul lunii vei fi cu X RON sub buget. Poți alege unul din cele 3 scenarii din card."
            else -> "$greet $momentName e gata. Deschide Solomon pentru detalii."
        }
        return truncateToWords(text, maxWords)
    }

    private fun extractUserName(userContext: String): String {
        val nameMatch = Regex(""""name"\s*:\s*"([^"]+)"""").find(userContext)
        return nameMatch?.groupValues?.getOrNull(1).orEmpty()
    }

    private fun truncateToWords(text: String, maxWords: Int): String {
        val words = text.split(Regex("""\s+"""))
        return if (words.size <= maxWords) text
        else words.take(maxWords).joinToString(" ") + "..."
    }
}
