package ro.solomon.moments

import kotlinx.serialization.json.Json
import ro.solomon.core.moments.MomentType
import ro.solomon.core.moments.SubscriptionAuditContext

class SubscriptionAuditBuilder : MomentBuilder<SubscriptionAuditContext> {
    override val momentType: MomentType = MomentType.subscriptionAudit
    override val contextSerializer = SubscriptionAuditContext.serializer()

    override val systemPrompt: String = """
        Ești Solomon — ai găsit abonamente „fantomă" (nefolosite de peste 30 de zile) la user.
        Contextul complet e în JSON: lista de ghost subscriptions, sumele, recovery estimate.

        Sarcina: scrie 2-3 propoziții scurte, în română, adresate conform `user.addressing`,
        care cuantifică cât pierde userul lunar/anual pe chestii nefolosite.

        Folosește `totals.monthlyRecoverable` și `totals.annualRecoverable` LITERAL.
        Dacă vreun `GhostSubscriptionDetail` are `cancellationUrl` sau `alternativeSuggestion`,
        poți menționa una în paranteză la final.

        Tonul: empatic, fără judecată. Mulți oameni uită — nu e vina lor.
        Maxim ${'$'}{momentType.maxWords} cuvinte.
    """.trimIndent()

    override fun buildContextJSON(context: SubscriptionAuditContext, json: Json): String =
        json.encodeToString(SubscriptionAuditContext.serializer(), context)
}
