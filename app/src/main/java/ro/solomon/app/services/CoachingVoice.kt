package ro.solomon.app.services

/**
 * "Stratul de VOCE" al coach-ului hibrid.
 *
 * Traduce un fapt sec din motorul determinist (ex: "ai depasit cu 200 RON la livrari")
 * in limbaj de coaching real, in stil Motivational Interviewing: intrebari deschise,
 * scalare, ascultare reflexiva, fara judecata. NU "nu mai sparge banii", ci "a meritat
 * pentru tine?". Tonul se adapteaza la money script.
 */
object CoachingVoice {

    /** Intrebare de scalare MI - il face pe user sa-si spuna singur motivatia (change talk). */
    fun scalingQuestion(topicRo: String): String =
        "Pe o scara de la 1 la 10, cat de important e pentru tine $topicRo chiar acum?"

    /** Reincadrare reflexiva, nu mustrare. */
    fun reflect(factRo: String): String =
        "Vad ca $factRo. Nu e o problema in sine - hai sa ne uitam impreuna la ce inseamna pentru tine."

    /** Intrebarea-cheie anti-"bib bib" (inspirata din abordarea Peek). */
    fun worthItQuestion(thingRo: String): String =
        "$thingRo - a meritat pentru tine? Nu te judec; vreau doar sa vedem daca merge spre ce-ti doresti."

    /**
     * Mesajul final adaptat la money script: ACELASI fapt, ton diferit.
     */
    fun frameForScript(factRo: String, script: MoneyScript?): String = when (script) {
        MoneyScript.AVOIDANCE ->
            "Un singur lucru azi: $factRo. Atat. Nu trebuie sa rezolvi tot acum."
        MoneyScript.WORSHIP ->
            "$factRo. Banii astia ar fi dus obiectivul tau mai aproape - vrei sa vedem cu cat?"
        MoneyScript.STATUS ->
            "$factRo. Intrebarea reala nu e ce vad altii, ci ce vrei tu sa construiesti."
        MoneyScript.VIGILANCE ->
            "$factRo. E sub control - si ai voie sa cheltui pe ce chiar conteaza pentru tine."
        null ->
            "$factRo. Hai sa vedem impreuna ce vrei sa faci cu asta."
    }

    /**
     * Inchidere tonata pe money script pentru un insight complet (ex: momentul "wow").
     * Nu reincadreaza textul (e deja o analiza), ci adauga un singur indemn final
     * in tonul scriptului dominant - ca sa fie consistent cu restul coach-ului.
     */
    fun closingNudge(script: MoneyScript?): String = when (script) {
        MoneyScript.AVOIDANCE ->
            "Nu trebuie sa rezolvi tot acum - alege un singur lucru din asta si opreste-te aici."
        MoneyScript.WORSHIP ->
            "Intrebarea buna: cat din ce vezi mai sus te apropie de ce-ti doresti cu adevarat?"
        MoneyScript.STATUS ->
            "Conteaza ce construiesti tu pe termen lung, nu ce se vede din afara."
        MoneyScript.VIGILANCE ->
            "E sub control - ai voie sa te bucuri si sa cheltui pe ce chiar conteaza pentru tine."
        null ->
            "Hai sa vedem impreuna ce vrei sa faci concret cu asta."
    }

    /**
     * Consuma feedback loop-ul: cat de tare impingem pasul concret depinde de cat
     * de mult a actionat userul la nudge-urile trecute.
     *  - istoric insuficient sau engagement ridicat -> direct si scurt;
     *  - engagement mediu -> oferim pasul, mai bland;
     *  - engagement scazut (ne ignora) -> NU impingem mai tare, dam inapoi la un ton
     *    care sustine autonomia (Motivational Interviewing).
     */
    fun adaptPlanToEngagement(
        planSentenceRo: String,
        engagementRatio: Double,
        hasEnoughHistory: Boolean
    ): String = when {
        !hasEnoughHistory -> "Plan concret: $planSentenceRo"
        engagementRatio >= 0.6 -> "Plan concret: $planSentenceRo"
        engagementRatio < 0.3 -> "Fara presiune - tu decizi daca merita. Daca vrei un pas mic: $planSentenceRo"
        else -> "O idee, daca ti se pare utila: $planSentenceRo"
    }
}
