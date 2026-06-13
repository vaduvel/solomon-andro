package ro.solomon.core.registries

import kotlinx.serialization.Serializable

@Serializable
enum class ScamSeverity : Comparable<ScamSeverity> {
    suspicious, likely_scam, definite_scam
}

@Serializable
enum class ScamCategory {
    investment_return, crypto, forex, pyramid_scheme, phishing_financial,
    investment_impersonation, loan_fee, lottery, romance
}

@Serializable
data class ScamPattern(
    val code: String,
    val category: ScamCategory,
    val severity: ScamSeverity,
    val keywords: List<String>,
    val explanation: String,
    val recommendation: String,
    val officialReference: String? = null
) {
    val id: String get() = code
}

object ScamPatterns {

    val all: List<ScamPattern> = listOf(
        ScamPattern("high_yield_2pct_monthly", ScamCategory.investment_return, ScamSeverity.definite_scam,
            listOf("2% pe luna", "2% lunar", "2% pe lună", "5% pe luna", "10% pe luna",
                "garantat", "randament garantat", "profit garantat", "fara risc"),
            "Niciun produs de investiții reglementat nu garantează randament peste 1%/lună fără risc.",
            "Nu plăti nimic. Verifică pe asf.ro.", "https://asf.ro/avertismente"),
        ScamPattern("investment_500_to_5000_30days", ScamCategory.investment_return, ScamSeverity.definite_scam,
            listOf("investeste 500", "primesti 5000", "în 30 de zile", "in 30 zile", "x10 in 30"),
            "'Investește 500€, primești 5000€ în 30 zile' e formula clasică de scam crypto/forex.",
            "Ignoră. Nu trimite niciun ban.", "https://asf.ro/avertismente"),
        ScamPattern("crypto_guaranteed", ScamCategory.crypto, ScamSeverity.likely_scam,
            listOf("crypto garantat", "bitcoin garantat", "investitie cripto sigura", "robot trading crypto"),
            "Crypto-ul e volatil prin definiție; nimeni nu poate garanta randament.",
            "Verifică dacă platforma e listată pe asf.ro.", "https://asf.ro/avertismente"),
        ScamPattern("forex_unregulated_ro", ScamCategory.forex, ScamSeverity.likely_scam,
            listOf("forex automat", "trading semnale gratuit", "broker forex", "expert advisor garantat"),
            "Brokerii Forex în RO trebuie autorizați ASF.",
            "Caută broker-ul pe asf.ro/registru-public.", "https://asf.ro/registru"),
        ScamPattern("loan_fee_advance", ScamCategory.loan_fee, ScamSeverity.definite_scam,
            listOf("taxa procesare in avans", "depune o taxa", "100 lei procesare credit",
                "comision in avans pentru credit", "transfera 50 lei pentru aprobare"),
            "Niciun creditor legal nu cere taxă înainte de disbursement.",
            "Nu plăti nimic în avans. Raportează la ANPC.", "https://anpc.ro"),
        ScamPattern("phishing_bank_link", ScamCategory.phishing_financial, ScamSeverity.likely_scam,
            listOf("contul tau a fost suspendat", "click pentru a verifica contul",
                "actualizeaza datele bancare imediat", "verifica acum cardul"),
            "Băncile RO nu cer niciodată actualizare credențiale prin link.",
            "Nu da click. Loghează-te direct prin app-ul oficial.", "https://anpc.ro"),
        ScamPattern("impersonation_asf_bnr", ScamCategory.investment_impersonation, ScamSeverity.definite_scam,
            listOf("platforma autorizata asf", "investitie aprobata bnr", "garantat de stat", "fond de investitii bnr"),
            "ASF și BNR nu autorizează platforme private individuale.",
            "Verifică direct pe asf.ro și bnr.ro.", "https://asf.ro"),
        ScamPattern("pyramid_recruitment", ScamCategory.pyramid_scheme, ScamSeverity.likely_scam,
            listOf("aduci 3 prieteni", "comision daca recrutezi", "matrix marketing", "schema 1+2+4+8", "venit pasiv recrutare"),
            "Câștigurile bazate pe recrutare sunt schemă piramidală.",
            "Schemele piramidale sunt ilegale în RO. Raportează la ANPC.", "https://anpc.ro"),
        ScamPattern("lottery_unsolicited", ScamCategory.lottery, ScamSeverity.definite_scam,
            listOf("ai castigat la loterie", "premiu de 1.000.000", "trimite date pentru a primi premiul", "tax for prize"),
            "Nu ai jucat → nu ai cum să câștigi.",
            "Șterge mesajul. Nu răspunde.", null),
        ScamPattern("romance_money_request", ScamCategory.romance, ScamSeverity.likely_scam,
            listOf("trimite-mi bani urgent", "am o urgenta medicala", "blocat in alta tara",
                "trimite la western union", "ridica un colet pentru mine"),
            "Cererile de bani de la persoane cunoscute online sunt scam clasic.",
            "Nu trimite bani. Verifică prin alte canale.", null)
    )

    fun matchIn(text: String): ScamPattern? {
        val lowered = text.lowercase()
        var best: ScamPattern? = null
        for (pattern in all) {
            for (keyword in pattern.keywords) {
                if (lowered.contains(keyword)) {
                    if (best == null || pattern.severity > best.severity) {
                        best = pattern
                    }
                    break
                }
            }
        }
        return best
    }

    fun patternsIn(category: ScamCategory): List<ScamPattern> = all.filter { it.category == category }
}
