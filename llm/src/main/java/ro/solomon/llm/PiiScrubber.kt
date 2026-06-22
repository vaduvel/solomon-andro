package ro.solomon.llm

/**
 * Redacts personal identifiers from text before it leaves the device for a
 * cloud LLM (Mistral). We only strip identifiers that are never needed to give
 * financial advice: IBANs, card numbers, emails, phone numbers, and the
 * structured "Nume:" line emitted by the app's user-context builder.
 *
 * Conservative by design: merchant names, amounts and categories are preserved
 * because the advisor needs them to reason about spending.
 */
object PiiScrubber {
    private val nameLine = Regex("(?im)^(\\s*Nume:\\s*).+$")
    private val iban = Regex("\\b[A-Z]{2}\\d{2}[A-Z0-9]{10,30}\\b")
    private val email = Regex("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\\b")
    private val phone = Regex("(?:(?:\\+|00)40|0)\\s?7\\d{2}[\\s.-]?\\d{3}[\\s.-]?\\d{3}\\b")
    private val card = Regex("\\b(?:\\d[ -]?){13,19}\\b")

    fun scrub(input: String): String {
        if (input.isBlank()) return input
        var out = input
        out = nameLine.replace(out) { it.groupValues[1] + "[redactat]" }
        out = iban.replace(out, "[IBAN]")
        out = email.replace(out, "[email]")
        out = phone.replace(out, "[telefon]")
        out = card.replace(out) { m ->
            val digits = m.value.count { it.isDigit() }
            if (digits in 13..19) "[card]" else m.value
        }
        return out
    }
}
