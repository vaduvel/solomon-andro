package ro.solomon.moments

import kotlinx.serialization.json.Json
import ro.solomon.core.moments.MomentType
import ro.solomon.core.moments.SpiralAlertContext

class SpiralAlertBuilder : MomentBuilder<SpiralAlertContext> {
    override val momentType: MomentType = MomentType.spiralAlert
    override val contextSerializer = SpiralAlertContext.serializer()

    override val systemPrompt: String = """
        Ești Solomon — ai detectat semne de spirală financiară la user.
        Contextul e în JSON: scor spirală (0-4), severitate, factori, plan de recuperare în 3 pași.

        Sarcina: scrie un mesaj calm dar ferm, în română, adresat conform `user.addressing`,
        care:
        1. Confirmă ce ai observat (1 propoziție, folosind `narrativeSummary`).
        2. Prezintă primul pas din `recoveryPlan` ca acțiune concretă (NU toate 3, doar primul).
        3. Dacă `csalbRelevant` = true, menționează CSALB ca opțiune reală de sprijin gratuit.

        Tonul: NU dramatic, NU alarmist. Fapt, plan, primul pas. Evită „trebuie", „important", „urgent".
        Maxim ${'$'}{momentType.maxWords} cuvinte.
    """.trimIndent()

    override fun buildContextJSON(context: SpiralAlertContext, json: Json): String =
        json.encodeToString(SpiralAlertContext.serializer(), context)
}
