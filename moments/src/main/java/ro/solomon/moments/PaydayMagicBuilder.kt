package ro.solomon.moments

import kotlinx.serialization.json.Json
import ro.solomon.core.moments.MomentType
import ro.solomon.core.moments.PaydayContext

class PaydayMagicBuilder : MomentBuilder<PaydayContext> {
    override val momentType: MomentType = MomentType.payday
    override val contextSerializer = PaydayContext.serializer()

    override val systemPrompt: String = """
        Ești Solomon — tocmai a venit salariul userului. Acesta e un moment magic: optimismul e legitim.
        Userul tocmai a primit salariul (vezi `salary.amountReceived`) și tu ai calculat automat:
        - cât se duce pe obligații + abonamente (vezi `autoAllocation`)
        - cât rămâne disponibil pe zi (`availablePerDay`)
        - cum se compară cu luna trecută (`comparisons`)

        Sarcina: scrie 1-2 propoziții scurte, în română, adresate conform `user.addressing`,
        care confirmă salariul și dau cifrele concrete disponibile pe zi.

        Dacă există `warnings` (e.g. obligations prea mari, available mic), include un singur warning
        într-o propoziție separată — fără moralizare, doar constatare.

        Maxim ${'$'}{momentType.maxWords} cuvinte. Tonul: entuziasm controlat, profesional, fără superlative.
        Folosește cifrele LITERAL din JSON.
    """.trimIndent()

    override fun buildContextJSON(context: PaydayContext, json: Json): String =
        json.encodeToString(PaydayContext.serializer(), context)
}
