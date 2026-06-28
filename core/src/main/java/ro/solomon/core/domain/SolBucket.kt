package ro.solomon.core.domain

import kotlinx.serialization.Serializable

/**
 * Prioritization bucket layered on top of [TransactionCategory].
 *
 * Solomon Focus uses buckets to decide what is protected ([necesar]), what gets
 * cut first when a Focus is active ([moft]), and what is growth/priority
 * spending ([prioritate]). The mapping below is the *default*; users can
 * override the bucket of any category.
 */
@Serializable
enum class SolBucket {
    necesar, moft, prioritate;

    val displayNameRO: String
        get() = when (this) {
            necesar -> "Necesar"
            moft -> "Moft"
            prioritate -> "Prioritate"
        }

    val descriptionRO: String
        get() = when (this) {
            necesar -> "Cheltuieli esențiale, protejate de un prag minim"
            moft -> "Cheltuieli flexibile, primele reduse când există un Focus"
            prioritate -> "Economii și obiective de creștere"
        }

    companion object {
        /**
         * Default bucket for a [TransactionCategory]. Mandatory loan payments are
         * [necesar] (the floor); early/extra repayment is handled separately as a
         * Focus. Users can override any of these.
         */
        fun defaultFor(category: TransactionCategory): SolBucket = when (category) {
            TransactionCategory.rent_mortgage,
            TransactionCategory.utilities,
            TransactionCategory.food_grocery,
            TransactionCategory.transport,
            TransactionCategory.health,
            TransactionCategory.loans_bank,
            TransactionCategory.loans_ifn,
            TransactionCategory.bnpl -> necesar

            TransactionCategory.food_delivery,
            TransactionCategory.food_dining,
            TransactionCategory.subscriptions,
            TransactionCategory.shopping_online,
            TransactionCategory.shopping_offline,
            TransactionCategory.entertainment,
            TransactionCategory.travel,
            TransactionCategory.unknown -> moft

            TransactionCategory.savings -> prioritate
        }
    }
}
