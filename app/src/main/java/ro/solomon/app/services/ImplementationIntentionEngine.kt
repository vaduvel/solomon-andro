package ro.solomon.app.services

import ro.solomon.core.domain.TransactionCategory

/**
 * Generator de "implementation intentions" (planuri daca-atunci).
 *
 * Cercetarea (Gollwitzer; popularizat de James Clear) arata ca un plan concret
 * "daca [situatie], atunci [actiune]" creste rata de a trece la fapte de 2-3x fata de
 * o intentie vaga ("trebuie sa cheltui mai putin"). Asta e diferenta dintre "bib bib,
 * nu mai sparge banii" si un coach care chiar schimba comportamentul.
 */
data class ImplementationIntention(
    val cue: String,      // "daca..."
    val action: String,   // "atunci..."
) {
    fun asSentenceRo(): String = "Daca $cue, atunci $action."
}

object ImplementationIntentionEngine {

    /**
     * Construieste un plan daca-atunci pentru o categorie unde userul tinde sa depaseasca.
     * Tonul se adapteaza la money script - acelasi plan, formulat diferit.
     */
    fun forCategory(
        category: TransactionCategory,
        script: MoneyScript? = null,
    ): ImplementationIntention {
        val base = when (category) {
            TransactionCategory.food_delivery -> ImplementationIntention(
                cue = "vrei sa comanzi mancare seara",
                action = "verifici intai ce ai in frigider si comanzi doar daca chiar nu e nimic",
            )
            TransactionCategory.food_dining -> ImplementationIntention(
                cue = "iesi in oras la masa a treia oara intr-o saptamana",
                action = "alegi un loc din lista ta de buget, nu pe cel mai scump din apropiere",
            )
            TransactionCategory.shopping_online -> ImplementationIntention(
                cue = "adaugi ceva in cos online peste 200 RON",
                action = "il lasi in cos 24 de ore inainte sa confirmi plata",
            )
            TransactionCategory.shopping_offline -> ImplementationIntention(
                cue = "esti in magazin si vrei ceva neplanificat",
                action = "te intrebi 'mi-am dorit asta si ieri?' inainte sa platesti",
            )
            TransactionCategory.entertainment -> ImplementationIntention(
                cue = "te plictisesti si deschizi aplicatia de cumparaturi sau abonamente",
                action = "deschizi in loc lista cu ce ai deja platit luna asta",
            )
            else -> ImplementationIntention(
                cue = "esti pe punctul unei cheltuieli neplanificate la ${category.displayNameRO}",
                action = "astepti 24 de ore si verifici cat iti mai ramane sigur de cheltuit",
            )
        }
        return adaptToScript(base, script)
    }

    private fun adaptToScript(
        intention: ImplementationIntention,
        script: MoneyScript?,
    ): ImplementationIntention = when (script) {
        // Pentru cei vigilenti, un plan prea restrictiv creste anxietatea; il inmoaie.
        MoneyScript.VIGILANCE -> intention.copy(
            action = intention.action + " - si daca e ceva ce chiar conteaza pentru tine, ai voie",
        )
        else -> intention
    }
}
