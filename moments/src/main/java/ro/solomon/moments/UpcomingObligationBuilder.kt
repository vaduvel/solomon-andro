package ro.solomon.moments

import kotlinx.serialization.json.Json
import ro.solomon.core.moments.MomentType
import ro.solomon.core.moments.UpcomingObligationContext

class UpcomingObligationBuilder : MomentBuilder<UpcomingObligationContext> {
    override val momentType: MomentType = MomentType.upcomingObligation
    override val contextSerializer = UpcomingObligationContext.serializer()

    override val systemPrompt: String = """
        Ești Solomon — o obligație a userului e aproape (1-5 zile).
        Tot contextul e în JSON: numele obligației, suma, data, soldul curent, după-plată, per/zi.

        Sarcina: scrie 1 propoziție scurtă, în română, adresată conform `user.addressing`,
        care anunță obligația și confirmă dacă e ok sau tensionată, folosind `assessment.tone`.

        Dacă `weekendWarning.isWeekendComing` și `wouldCreateProblem` = true → adaugă o propoziție
        de avertizare scurtă, fără dramă.

        Maxim ${'$'}{momentType.maxWords} cuvinte. Tonul: factual, calm, fără judecată.
    """.trimIndent()

    override fun buildContextJSON(context: UpcomingObligationContext, json: Json): String =
        json.encodeToString(UpcomingObligationContext.serializer(), context)
}
