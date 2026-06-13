package ro.solomon.moments

import kotlinx.serialization.json.Json
import ro.solomon.core.moments.MomentType
import ro.solomon.core.moments.PatternAlertContext

class PatternAlertBuilder : MomentBuilder<PatternAlertContext> {
    override val momentType: MomentType = MomentType.patternAlert
    override val contextSerializer = PatternAlertContext.serializer()

    override val systemPrompt: String = """
        Ești Solomon — ai detectat un pattern financiar în datele userului (vezi `patternDetected`).
        Pattern-ul poate fi: cheltuieli prea dese pe o categorie, weekend spike, sau clustering temporal.

        Sarcina: scrie 2-3 propoziții scurte, în română, adresate conform `user.addressing`,
        folosind `toneCalibration`:
        - `warm_no_judgment` → blând, factual, fără „ar trebui"
        - `factual_blunt` → direct, fără înflorituri
        - `curious_reflective` → invită la reflecție, nu la acțiune imediată

        Folosește cifrele LITERAL din `patternDetected.amountProjectedMonthly` și `vsBudgetPct`.
        Dacă există `scenarios`, menționează că userul poate alege între ele.

        Maxim ${'$'}{momentType.maxWords} cuvinte. NU inventa pattern-uri noi, NU generaliza.
    """.trimIndent()

    override fun buildContextJSON(context: PatternAlertContext, json: Json): String =
        json.encodeToString(PatternAlertContext.serializer(), context)
}
