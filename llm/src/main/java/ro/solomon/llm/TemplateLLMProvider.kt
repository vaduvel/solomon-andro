package ro.solomon.llm

import ro.solomon.core.moments.MomentType

/**
 * Offline, deterministic fallback provider. It receives the same system prompt +
 * user context JSON as the real provider, so when the moment context already
 * carries a precomputed verdict and pre-formatted figures we surface them instead
 * of a generic non-answer. Only string fields are read (no unit assumptions), and
 * everything degrades gracefully to the previous copy when fields are absent.
 */
class TemplateLLMProvider : LLMProvider {

    override val isReady: Boolean = true

    override suspend fun generate(
        systemPrompt: String,
        userContext: String,
        maxWords: Int
    ): String {
        val momentHint = MomentType.entries.firstOrNull { systemPrompt.contains(it.name, ignoreCase = true) }
        val momentName = momentHint?.name ?: "azi"
        val user = extractString(userContext, "name").orEmpty()
        val greet = if (user.isNotBlank()) "$user, " else ""

        val text = when {
            systemPrompt.contains("can_i_afford", ignoreCase = true) ||
            systemPrompt.contains("îmi permit", ignoreCase = true) ->
                buildAffordAnswer(greet, userContext)
            systemPrompt.contains("payday", ignoreCase = true) ||
            systemPrompt.contains("salariu", ignoreCase = true) ->
                buildPaydayAnswer(greet, userContext)
            systemPrompt.contains("spiral", ignoreCase = true) ->
                "$greet am observat un risc de spirală. Cel mai important pas acum e cel mai mic, nu cel mai mare. Deschide momentul Spiral Alert pentru planul în 3 pași."
            systemPrompt.contains("weekly", ignoreCase = true) ->
                "$greet rezumatul săptămânii e gata. Cea mai mare cheltuială și micile câștiguri sunt în tab-ul Azi."
            systemPrompt.contains("pattern", ignoreCase = true) ->
                buildPatternAnswer(greet, userContext)
            else -> "$greet $momentName e gata. Deschide Solomon pentru detalii."
        }
        return truncateToWords(text, maxWords)
    }

    private fun buildAffordAnswer(greet: String, ctx: String): String {
        val verdict = extractString(ctx, "verdict")?.lowercase()
        val safeToSpend = extractString(ctx, "safe_to_spend") ?: extractString(ctx, "available_per_day")
        val available = extractString(ctx, "available_after_obligations") ?: extractString(ctx, "available")
        val head = when {
            verdict == null -> null
            verdict.contains("no") || verdict == "nu" -> "$greet pe scurt: nu acum — te-ar duce sub obligațiile lunii."
            verdict.contains("caution") -> "$greet da, dar cu atenție — ești pe muchie."
            verdict.contains("yes") || verdict == "da" -> "$greet da, ți-o poți permite."
            else -> null
        }
        if (head == null && safeToSpend == null && available == null) {
            return "$greet ca să-ți spun exact, am nevoie de soldul și cheltuielile lunii. Deschide Solomon pentru verdict."
        }
        val sb = StringBuilder(head ?: "${greet}uite cum stai.")
        if (available != null) sb.append(" Disponibil după obligații: $available.")
        if (safeToSpend != null) sb.append(" Sigur de cheltuit: $safeToSpend.")
        return sb.toString()
    }

    private fun buildPaydayAnswer(greet: String, ctx: String): String {
        val perDay = extractString(ctx, "safe_to_spend") ?: extractString(ctx, "available_per_day")
        val base = "$greet salariul a intrat. Am rezervat automat obligațiile și abonamentele"
        return if (perDay != null) "$base — îți rămân $perDay disponibili pe zi până la următorul salariu."
        else "$base — verifică în tab-ul Azi cât ai rămas disponibil pe zi."
    }

    private fun buildPatternAnswer(greet: String, ctx: String): String {
        val projected = extractString(ctx, "projected_end_of_month") ?: extractString(ctx, "projection")
        return if (projected != null)
            "$greet dacă păstrezi ritmul curent, la finalul lunii vei fi pe $projected. Poți alege unul din cele 3 scenarii din card."
        else
            "$greet dacă păstrezi ritmul curent, vezi în card proiecția pentru finalul lunii și cele 3 scenarii."
    }

    private fun extractString(ctx: String, key: String): String? {
        val m = Regex("\"" + Regex.escape(key) + "\"\\s*:\\s*\"([^\"]+)\"").find(ctx)
        return m?.groupValues?.getOrNull(1)?.takeIf { it.isNotBlank() }
    }

    private fun truncateToWords(text: String, maxWords: Int): String {
        val words = text.split(Regex("\\s+"))
        return if (words.size <= maxWords) text
        else words.take(maxWords).joinToString(" ") + "..."
    }
}
