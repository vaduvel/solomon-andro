package ro.solomon.core.domain

import kotlinx.serialization.Serializable

/**
 * The kind of short-term prioritization a [Focus] represents. Ordered by the
 * real prioritization sequence of Romanian households (survival -> safety net
 * -> debt -> big purchase/event), so UIs can suggest them in this order.
 */
@Serializable
enum class FocusType {
    runway,
    emergency_fund,
    early_repayment,
    event,
    moft_detox;

    val displayNameRO: String
        get() = when (this) {
            runway -> "Rămâi pe plus"
            emergency_fund -> "Fond de urgență"
            early_repayment -> "Rată anticipată"
            event -> "Eveniment"
            moft_detox -> "Detox de mofturi"
        }

    val descriptionRO: String
        get() = when (this) {
            runway -> "Stai pe plus până la următorul salariu"
            emergency_fund -> "Strânge 3-6 venituri nete, în sprinturi lunare"
            early_repayment -> "Pune o sumă extra pe credit ca să scazi dobânda totală"
            event -> "Strânge o sumă fixă până la o dată anume"
            moft_detox -> "Taie cheltuielile de moft cu un procent, o perioadă"
        }

    /** Whether this focus tracks progress toward a fixed [Focus.targetAmount]. */
    val hasMonetaryTarget: Boolean
        get() = when (this) {
            emergency_fund, early_repayment, event -> true
            runway, moft_detox -> false
        }
}
