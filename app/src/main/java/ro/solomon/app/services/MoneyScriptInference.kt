package ro.solomon.app.services

import ro.solomon.core.domain.Transaction
import ro.solomon.core.domain.TransactionCategory

/**
 * Inferenta money script-ului din comportamentul real de cheltuieli.
 *
 * NU e un diagnostic clinic (scriptul Klontz se masoara cu un chestionar). E un semnal
 * conservator, folosit doar ca fallback cand userul nu si-a declarat scriptul in onboarding.
 * Returneaza null cand semnalul e neclar - mai bine ton neutru decat o eticheta gresita.
 *
 * Heuristici (pe cheltuielile de iesire, dintr-un istoric suficient):
 *  - VIGILANCE: economiseste mult si se rasfata rar (savings sus, lifestyle jos).
 *  - STATUS:    pondere mare pe cumparaturi + calatorii (aparenta, lifestyle premium).
 *  - WORSHIP:   multe cheltuieli mici impulsive (livrari + distractie, ca frecventa).
 *  - AVOIDANCE: nu se infera din cheltuieli (tine de a evita sa te uiti) -> onboarding/engagement.
 */
object MoneyScriptInference {

    private const val MIN_OUTGOING = 15

    fun infer(transactions: List<Transaction>): MoneyScript? {
        val outgoing = transactions.filter { it.isOutgoing }
        if (outgoing.size < MIN_OUTGOING) return null

        val totalBani = outgoing.sumOf { it.amount.amount.toLong() }
        if (totalBani <= 0L) return null

        fun shareOf(predicate: (TransactionCategory) -> Boolean): Double =
            outgoing.filter { predicate(it.category) }
                .sumOf { it.amount.amount.toLong() }
                .toDouble() / totalBani

        val savingsShare = shareOf { it == TransactionCategory.savings }
        val lifestyleShare = shareOf { it.group == TransactionCategory.Group.lifestyle }
        val statusShare = shareOf {
            it == TransactionCategory.shopping_online ||
                it == TransactionCategory.shopping_offline ||
                it == TransactionCategory.travel
        }

        val impulsiveCount = outgoing.count {
            it.category == TransactionCategory.food_delivery ||
                it.category == TransactionCategory.entertainment
        }
        val impulsiveCountShare = impulsiveCount.toDouble() / outgoing.size

        return when {
            savingsShare >= 0.20 && lifestyleShare <= 0.15 -> MoneyScript.VIGILANCE
            statusShare >= 0.35 -> MoneyScript.STATUS
            impulsiveCountShare >= 0.30 -> MoneyScript.WORSHIP
            else -> null
        }
    }
}
