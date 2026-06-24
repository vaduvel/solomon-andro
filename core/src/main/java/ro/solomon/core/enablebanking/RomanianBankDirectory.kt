package ro.solomon.core.enablebanking

/**
 * Single source of truth for the well-known Romanian ASPSP (bank) brands.
 *
 * Used as a display fallback when the live Enable Banking ASPSP listing is
 * unavailable (no network, not yet configured, or an empty response). Keeping
 * the list here -- instead of hardcoded inside a Composable -- means it can be
 * maintained in one place and reused by any screen, test, or future
 * ASPSP-matching logic.
 */
object RomanianBankDirectory {

    /**
     * A well-known bank brand.
     *
     * @param displayName Human-friendly name shown in the UI.
     * @param aspspHint Substring that typically appears in the Enable Banking
     *                  ASPSP `name`, used to match a live ASPSP to this brand.
     */
    data class KnownBank(val displayName: String, val aspspHint: String)

    val knownBanks: List<KnownBank> = listOf(
        KnownBank("Banca Transilvania", "Banca Transilvania"),
        KnownBank("BCR", "BCR"),
        KnownBank("ING Bank", "ING"),
        KnownBank("BRD", "BRD"),
        KnownBank("Raiffeisen Bank", "Raiffeisen"),
        KnownBank("UniCredit Bank", "UniCredit"),
        KnownBank("CEC Bank", "CEC"),
        KnownBank("Alpha Bank", "Alpha"),
        KnownBank("Revolut", "Revolut"),
        KnownBank("Garanti Bank", "Garanti"),
        KnownBank("Libra Bank", "Libra"),
    )

    /** Plain display names, for simple list rendering. */
    val displayNames: List<String> get() = knownBanks.map { it.displayName }

    /** Best-effort match of a live ASPSP name to a known brand display name. */
    fun displayNameFor(aspspName: String): String =
        knownBanks.firstOrNull { aspspName.contains(it.aspspHint, ignoreCase = true) }?.displayName
            ?: aspspName
}
