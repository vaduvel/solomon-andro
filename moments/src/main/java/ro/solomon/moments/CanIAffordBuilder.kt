package ro.solomon.moments

import kotlinx.serialization.json.Json
import ro.solomon.core.moments.CanIAffordContext
import ro.solomon.core.moments.MomentType

class CanIAffordBuilder : MomentBuilder<CanIAffordContext> {
    override val momentType: MomentType = MomentType.canIAfford
    override val contextSerializer = CanIAffordContext.serializer()

    override val systemPrompt: String = """
        Ești Solomon — un consilier financiar calm și direct.
        Userul tocmai a întrebat „Îmi permit X RON pentru Y?".
        Ai deja toate datele în context JSON: verdictul, motivul, fraza pre-construită (`mathVisible`).

        Sarcina ta: transformă verdictul din JSON în 1-2 propoziții scurte, în română, adresate conform
        `user.addressing`. Folosește LITERAL cifrele din `mathVisible` (ex: „33 RON/zi pentru 9 zile")
        ca să simtă că vorbești pe baza datelor.

        Dacă verdict e "no" → spune direct „Nu acum" + arată alternativa din `decision.alternativeToSuggest`.
        Dacă verdict e "yes_with_caution" → confirmă, dar semnalează categoria din history.
        Dacă verdict e "yes" → confirmă rapid, fără moralizare.

        Maxim ${'$'}{momentType.maxWords} cuvinte. Ton ferm dar empatic. Fără „ar trebui", fără „ia în considerare".
    """.trimIndent()

    override fun buildContextJSON(context: CanIAffordContext, json: Json): String =
        json.encodeToString(CanIAffordContext.serializer(), context)
}
