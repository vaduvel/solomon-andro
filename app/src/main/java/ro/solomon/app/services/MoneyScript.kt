package ro.solomon.app.services

/**
 * Money Scripts - convingerile de baza despre bani care conduc comportamentul financiar.
 *
 * Bazat pe cercetarea lui Brad Klontz (Klontz Money Script Inventory): cele 4 tipare
 * fundamentale care prezic comportamentul financiar mai bine decat venitul sau educatia.
 * Aici sunt localizate pentru context romanesc si folosite ca "stratul de DE CE" al coach-ului:
 * acelasi adevar din cifre se comunica diferit in functie de scriptul dominant al userului.
 */
enum class MoneyScript(val raw: String) {
    /** Banii sunt rai / nu merit bani / mai putin = mai bine. Tinde sa evite, sa nu se uite la cifre. */
    AVOIDANCE("avoidance"),

    /** Mai multi bani = mai fericit; solutia la orice e mai mult venit. Tinde sa cheltuie, sa se indatoreze. */
    WORSHIP("worship"),

    /** Valoarea de sine = valoarea neta; banii se arata. Tinde la cheltuieli de status si comparatie sociala. */
    STATUS("status"),

    /** Banii se tin secret, vigilenta, anxietate de cheltuiala. Tinde la economisire excesiva, frica. */
    VIGILANCE("vigilance");

    val labelRo: String
        get() = when (this) {
            AVOIDANCE -> "evitare"
            WORSHIP -> "venerare"
            STATUS -> "status"
            VIGILANCE -> "vigilenta"
        }

    /** Cum se manifesta, pe scurt - pentru afisare si pentru a explica userului tiparul. */
    val signatureRo: String
        get() = when (this) {
            AVOIDANCE -> "Eviti sa te uiti la bani; cifrele te streseaza, asa ca le amani."
            WORSHIP -> "Crezi ca mai multi bani rezolva orice; cheltui usor pentru confort imediat."
            STATUS -> "Banii sunt despre cum esti vazut; cheltui ca sa tii pasul sau sa arati bine."
            VIGILANCE -> "Esti atent si precaut; uneori prea anxios ca sa te bucuri de banii pe care ii ai."
        }

    /**
     * Tonul potrivit cu care coach-ul trebuie sa comunice ACELASI adevar din cifre.
     * Asta e miezul hibridului: motorul da faptul, scriptul da modul de a-l spune.
     */
    val coachingToneRo: String
        get() = when (this) {
            AVOIDANCE -> "Bland, fara cifre coplesitoare. Un singur numar, un singur pas. Fara rusine."
            WORSHIP -> "Leaga cheltuiala de obiectivul pe care il intarzie. Arata costul real, nu interzice."
            STATUS -> "Reincadreaza valoarea: ce vrei cu adevarat, nu ce vad altii. Fara judecata."
            VIGILANCE -> "Linisteste. Confirma ca e ok sa cheltui pe ce conteaza. Permisiune, nu alarma."
        }

    companion object {
        fun from(raw: String?): MoneyScript? {
            if (raw == null) return null
            val n = raw.lowercase().trim()
            return entries.firstOrNull { it.raw == n || n.contains(it.raw) }
        }
    }
}
