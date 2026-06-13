package ro.solomon.llm

class LLMOutputValidator {

    sealed class Result {
        object Valid : Result()
        data class Empty(val reason: String) : Result()
        data class TooLong(val wordCount: Int) : Result()
        data class WrongLanguage(val dominantLanguage: String) : Result()
    }

    fun validate(text: String, maxWords: Int, requireRomanian: Boolean = true): Result {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return Result.Empty("empty response")
        val words = trimmed.split(Regex("""\s+"""))
        if (words.size > maxWords * 1.2) return Result.TooLong(words.size)
        if (requireRomanian) {
            val roChars = trimmed.count { it in 'ă'..'ž' || it == 'â' || it == 'î' || it == 'ș' || it == 'ț' }
            val total = trimmed.count { it.isLetter() }
            if (total > 30 && roChars == 0) {
                val hasCommon = listOf(
                    "esti", "sunt", "este", "este", "nu", "da", "dar",
                    "salariu", "cheltuieli", "facturi", "buget"
                ).any { trimmed.lowercase().contains(it) }
                if (!hasCommon) return Result.WrongLanguage("en")
            }
        }
        return Result.Valid
    }

    fun truncate(text: String, maxWords: Int): String {
        val words = text.trim().split(Regex("""\s+"""))
        return if (words.size <= maxWords) text
        else words.take(maxWords).joinToString(" ") + "..."
    }
}
