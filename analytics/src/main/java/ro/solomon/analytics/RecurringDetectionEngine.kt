package ro.solomon.analytics

import ro.solomon.core.domain.FlowDirection
import ro.solomon.core.domain.Money
import ro.solomon.core.domain.Transaction
import ro.solomon.core.domain.TransactionCategory
import java.util.concurrent.TimeUnit

enum class RecurringFrequency {
    weekly, fortnightly, monthly
}

data class RecurringPattern(
    val merchant: String,
    val amountAvg: Money,
    val frequency: RecurringFrequency,
    val confidence: Double,
    val transactionCount: Int,
    val firstDate: Long,
    val lastDate: Long,
    val daysBetweenAvg: Double,
    val suggestedCategory: TransactionCategory? = null
)

data class RecurringDetectionReport(
    val patterns: List<RecurringPattern>,
    val totalOutgoingTransactions: Int,
    val merchantsScanned: Int,
    val recurringRatio: Double
)

class RecurringDetectionEngine {

    fun detect(
        transactions: List<Transaction>,
        windowDays: Int = 120,
        referenceDate: Long = System.currentTimeMillis()
    ): RecurringDetectionReport {
        val cutoff = referenceDate - TimeUnit.DAYS.toMillis(windowDays.toLong())
        val outgoing = transactions.filter {
            it.direction == FlowDirection.outgoing && it.date >= cutoff
        }

        val byMerchant = outgoing
            .filter { !it.merchant.isNullOrBlank() }
            .groupBy { it.merchant!!.trim().lowercase() }

        val patterns = mutableListOf<RecurringPattern>()

        for ((merchant, txns) in byMerchant) {
            if (txns.size < 2) continue
            val sorted = txns.sortedBy { it.date }
            val gaps = sorted.zipWithNext { a, b ->
                TimeUnit.MILLISECONDS.toDays(b.date - a.date).toInt()
            }

            val best = findBestFrequency(gaps)
            if (best == null || best.confidence < 0.4) continue

            val gapDays = gaps.filter { matchesFrequency(it, best.frequency) }
            val avgGap = if (gapDays.isNotEmpty()) gapDays.average() else best.expectedGap.toDouble()
            val matchingCount = gapDays.size

            val amountSum = sorted.filterIndexed { i, _ ->
                i == 0 || (i > 0 && matchesFrequency(
                    TimeUnit.MILLISECONDS.toDays(sorted[i].date - sorted[i - 1].date).toInt(),
                    best.frequency
                ))
            }.sumOf { it.amount.amount }

            val amountCount = matchingCount + 1
            val avgAmount = if (amountCount > 0) amountSum / amountCount else txns.first().amount.amount
            val amountVariance = computeAmountVariance(sorted.map { it.amount.amount })

            val confidence = best.confidence * (1.0 - amountVariance.coerceIn(0.0, 0.5))
            val finalConfidence = confidence.coerceIn(0.0, 1.0)

            patterns.add(RecurringPattern(
                merchant = txns.first().merchant ?: merchant,
                amountAvg = Money(avgAmount),
                frequency = best.frequency,
                confidence = finalConfidence,
                transactionCount = sorted.size,
                firstDate = sorted.first().date,
                lastDate = sorted.last().date,
                daysBetweenAvg = avgGap,
                suggestedCategory = dominantCategory(sorted)
            ))
        }

        val sortedPatterns = patterns.sortedByDescending { it.confidence }
        val scanned = byMerchant.filter { it.value.size >= 2 }.size
        val recurringSum = sortedPatterns.sumOf { it.amountAvg.amount }
        val totalSpend = outgoing.sumOf { it.amount.amount }
        val ratio = if (totalSpend > 0) recurringSum.toDouble() / totalSpend.toDouble() else 0.0

        return RecurringDetectionReport(
            patterns = sortedPatterns,
            totalOutgoingTransactions = outgoing.size,
            merchantsScanned = scanned,
            recurringRatio = ratio
        )
    }

    private fun findBestFrequency(gaps: List<Int>): FrequencyMatch? {
        var bestMatch: FrequencyMatch? = null

        for (freq in listOf(
            FrequencyParams(RecurringFrequency.monthly, 30, 25..35),
            FrequencyParams(RecurringFrequency.fortnightly, 14, 12..16),
            FrequencyParams(RecurringFrequency.weekly, 7, 5..9)
        )) {
            val matching = gaps.count { it in freq.range }
            if (matching == 0) continue
            val ratio = matching.toDouble() / gaps.size.toDouble()

            if (ratio > 0.4 && (bestMatch == null || ratio > bestMatch.confidence)) {
                bestMatch = FrequencyMatch(freq.frequency, ratio, freq.expectedGap)
            }
        }

        return bestMatch
    }

    private fun matchesFrequency(gapDays: Int, frequency: RecurringFrequency): Boolean {
        return when (frequency) {
            RecurringFrequency.monthly -> gapDays in 25..35
            RecurringFrequency.fortnightly -> gapDays in 12..16
            RecurringFrequency.weekly -> gapDays in 5..9
        }
    }

    private fun computeAmountVariance(amounts: List<Int>): Double {
        if (amounts.size < 2) return 0.0
        val avg = amounts.average()
        if (avg <= 0) return 1.0
        val maxDev = amounts.maxOf { kotlin.math.abs(it - avg) }
        return maxDev / avg
    }

    private fun dominantCategory(txns: List<Transaction>): TransactionCategory? {
        return txns.groupBy { it.category }.maxByOrNull { it.value.size }?.key
    }

    private data class FrequencyParams(
        val frequency: RecurringFrequency,
        val expectedGap: Int,
        val range: IntRange
    )

    private data class FrequencyMatch(
        val frequency: RecurringFrequency,
        val confidence: Double,
        val expectedGap: Int
    )
}
