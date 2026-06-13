package ro.solomon.core.domain

import kotlinx.serialization.Serializable

@Serializable
enum class TransactionCategory {
    food_grocery, food_delivery, food_dining, transport, utilities,
    rent_mortgage, subscriptions, shopping_online, shopping_offline,
    entertainment, health, loans_ifn, loans_bank, bnpl, travel, savings, unknown;

    val displayNameRO: String get() = when (this) {
        food_grocery -> "Cumpărături alimentare"
        food_delivery -> "Livrări mâncare"
        food_dining -> "Restaurante"
        transport -> "Transport"
        utilities -> "Utilități"
        rent_mortgage -> "Chirie / rată"
        subscriptions -> "Abonamente"
        shopping_online -> "Cumpărături online"
        shopping_offline -> "Cumpărături magazine"
        entertainment -> "Distracție"
        health -> "Sănătate"
        loans_ifn -> "Credite IFN"
        loans_bank -> "Credite bancare"
        bnpl -> "BNPL"
        travel -> "Călătorii"
        savings -> "Economii"
        unknown -> "Necategorizat"
    }

    enum class Group { essentials, lifestyle, debt, savings, other }

    val group: Group get() = when (this) {
        rent_mortgage, utilities, food_grocery, transport, health -> Group.essentials
        food_delivery, food_dining, subscriptions, shopping_online,
        shopping_offline, entertainment, travel -> Group.lifestyle
        loans_ifn, loans_bank, bnpl -> Group.debt
        savings -> Group.savings
        unknown -> Group.other
    }

    companion object {
        val debtCategories: Set<TransactionCategory> = setOf(loans_ifn, loans_bank, bnpl)
    }
}
