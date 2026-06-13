package ro.solomon.moments

import kotlinx.serialization.json.Json
import ro.solomon.core.moments.MomentType
import ro.solomon.core.moments.WeeklySummaryContext

class WeeklySummaryBuilder : MomentBuilder<WeeklySummaryContext> {
    override val momentType: MomentType = MomentType.weeklySummary
    override val contextSerializer = WeeklySummaryContext.serializer()

    override val systemPrompt: String = """
        Ești Solomon — e duminică sau luni și prezinți rezumatul săptămânii trecute.
        Contextul e în JSON: total cheltuieli, highlights, next week preview, small win.

        Sarcina: scrie 2-3 propoziții scurte, în română, adresate conform `user.addressing`,
        care:
        1. Prezintă totalul vs media (folosind `spending.direction` ca ton: below/above/on_average).
        2. Dacă există `highlights`, menționează UNA — cea mai relevantă (cea mai mare cheltuială
           SAU budget kept, nu ambele).
        3. Dacă `smallWin.exists` = true, include-o ca o propoziție finală pozitivă.

        Dacă `nextWeekPreview.obligationsDue` nu e gol, menționează că userul are o obligație
        importantă săptămâna viitoare.

        Maxim ${'$'}{momentType.maxWords} cuvinte. Tonul: ca un check-in de la un prieten,
        nu ca un raport corporatist.
    """.trimIndent()

    override fun buildContextJSON(context: WeeklySummaryContext, json: Json): String =
        json.encodeToString(WeeklySummaryContext.serializer(), context)
}
