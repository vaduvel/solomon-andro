package ro.solomon.analytics

import ro.solomon.core.domain.*
import ro.solomon.core.format.RomanianDateFormatter
import java.util.Calendar

data class CategorySpending(
    val category: TransactionCategory,
    val totalAmount: Money,
    val transactionCount: Int,
    val dominantMerchant: String? = null,
    val fractionOfTotal: Double
)

data class WeekendSpikeSignal(
    val weekendAvgPerDay: Money,
    val weekdayAvgPerDay: Money,
    val ratio: Double,
    val isSignificant: Boolean
)

data class TemporalCluster(
    val category: TransactionCategory,
    val dominantWeekday: Int,
    val concentration: Double,
    val isStrong: Boolean,
    val description: String
)

data class OutlierTransaction(
    val transactionId: String,
    val amount: Money,
    val category: TransactionCategory,
    val merchant: String? = null,
    val date: Long,
    val ratioToDailyAvg: Double
)

data class FrequencySpike(
    val category: TransactionCategory,
    val merchantDominant: String? = null,
    val countLast7Days: Int,
    val amountLast7Days: Money,
    val monthlyProjection: Money,
    val description: String
)

data class PatternReport(
    val topCategories: List<CategorySpending>,
    val weekendSpike: WeekendSpikeSignal,
    val temporalClusters: List<TemporalCluster>,
    val outliers: List<OutlierTransaction>,
    val frequencySpikes: List<FrequencySpike>
)

class PatternDetector {

    fun detect(
        transactions: List<Transaction>,
        windowDays: Int = 90,
        referenceDate: Long = System.currentTimeMillis(),
        calendar: Calendar = RomanianDateFormatter.gregorianROCalendar()
    ): PatternReport {
        val cal = calendar.clone() as Calendar
        cal.timeInMillis = referenceDate
        cal.add(Calendar.DAY_OF_YEAR, -windowDays)
        val windowStart = cal.timeInMillis

        val inWindow = transactions.filter {
            it.direction == FlowDirection.outgoing && it.date >= windowStart && it.date <= referenceDate
        }

        return PatternReport(
            topCategories = computeTopCategories(inWindow),
            weekendSpike = computeWeekendSpike(inWindow, windowDays, calendar),
            temporalClusters = computeTemporalClusters(inWindow, calendar),
            outliers = computeOutliers(inWindow, windowDays),
            frequencySpikes = computeFrequencySpikes(inWindow, referenceDate, calendar)
        )
    }

    fun computeTopCategories(txs: List<Transaction>): List<CategorySpending> {
        val totalSpending = txs.sumOf { it.amount.amount }
        if (totalSpending <= 0) return emptyList()

        val byCategory = txs.groupBy { it.category }
        return byCategory.map { (cat, items) ->
            val total = items.sumOf { it.amount.amount }
            CategorySpending(
                category = cat,
                totalAmount = Money(total),
                transactionCount = items.size,
                dominantMerchant = mostFrequentMerchant(items),
                fractionOfTotal = total.toDouble() / totalSpending.toDouble()
            )
        }.sortedByDescending { it.totalAmount.amount }
    }

    private fun mostFrequentMerchant(txs: List<Transaction>): String? {
        val counts = txs.mapNotNull { it.merchant }.groupingBy { it }.eachCount()
        return counts.maxByOrNull { it.value }?.key
    }

    fun computeWeekendSpike(txs: List<Transaction>, windowDays: Int, calendar: Calendar): WeekendSpikeSignal {
        var weekendTotal = 0
        var weekdayTotal = 0
        for (tx in txs) {
            val cal = calendar.clone() as Calendar
            cal.timeInMillis = tx.date
            val weekday = cal.get(Calendar.DAY_OF_WEEK)
            if (weekday == Calendar.SUNDAY || weekday == Calendar.SATURDAY) {
                weekendTotal += tx.amount.amount
            } else {
                weekdayTotal += tx.amount.amount
            }
        }
        val weekendDays = windowDays.toDouble() * 2.0 / 7.0
        val weekdayDays = windowDays.toDouble() * 5.0 / 7.0
        val weekendAvg = if (weekendDays > 0) weekendTotal.toDouble() / weekendDays else 0.0
        val weekdayAvg = if (weekdayDays > 0) weekdayTotal.toDouble() / weekdayDays else 0.0
        val ratio = if (weekdayAvg > 0) weekendAvg / weekdayAvg else 0.0
        return WeekendSpikeSignal(
            weekendAvgPerDay = Money(weekendAvg.toInt()),
            weekdayAvgPerDay = Money(weekdayAvg.toInt()),
            ratio = ratio,
            isSignificant = ratio >= 1.8
        )
    }

    fun computeTemporalClusters(txs: List<Transaction>, calendar: Calendar): List<TemporalCluster> {
        val byCategory = txs.groupBy { it.category }
        val clusters = mutableListOf<TemporalCluster>()

        for ((cat, items) in byCategory) {
            if (items.size < 5) continue
            val counts = items.groupBy { tx ->
                val cal = calendar.clone() as Calendar
                cal.timeInMillis = tx.date
                cal.get(Calendar.DAY_OF_WEEK)
            }.mapValues { it.value.size }

            val dominant = counts.maxByOrNull { it.value } ?: continue
            val concentration = dominant.value.toDouble() / items.size.toDouble()
            if (concentration >= 0.40) {
                val dayName = RomanianDateFormatter.weekdayName(dominant.key)
                val pct = (concentration * 100).toInt()
                clusters.add(TemporalCluster(
                    category = cat,
                    dominantWeekday = dominant.key,
                    concentration = concentration,
                    isStrong = concentration >= 0.60,
                    description = "$pct% din tranzacțiile la ${cat.displayNameRO} sunt $dayName"
                ))
            }
        }
        return clusters.sortedByDescending { it.concentration }
    }

    fun computeOutliers(txs: List<Transaction>, windowDays: Int): List<OutlierTransaction> {
        if (txs.isEmpty()) return emptyList()
        val totalAmount = txs.sumOf { it.amount.amount }
        val dailyAvg = totalAmount.toDouble() / maxOf(windowDays, 1)
        if (dailyAvg <= 0) return emptyList()

        return txs.filter { it.amount.amount.toDouble() >= dailyAvg * 5.0 }
            .map {
                OutlierTransaction(
                    transactionId = it.id,
                    amount = it.amount,
                    category = it.category,
                    merchant = it.merchant,
                    date = it.date,
                    ratioToDailyAvg = it.amount.amount.toDouble() / dailyAvg
                )
            }
            .sortedByDescending { it.amount.amount }
            .take(5)
    }

    fun computeFrequencySpikes(txs: List<Transaction>, referenceDate: Long, calendar: Calendar): List<FrequencySpike> {
        val cal = calendar.clone() as Calendar
        cal.timeInMillis = referenceDate
        cal.add(Calendar.DAY_OF_YEAR, -7)
        val weekAgo = cal.timeInMillis

        val last7 = txs.filter { it.date >= weekAgo && it.date <= referenceDate }
        val byCategory = last7.groupBy { it.category }

        return byCategory.filter { it.value.size >= 4 }
            .map { (cat, items) ->
                val total = items.sumOf { it.amount.amount }
                val dominant = mostFrequentMerchant(items)
                FrequencySpike(
                    category = cat,
                    merchantDominant = dominant,
                    countLast7Days = items.size,
                    amountLast7Days = Money(total),
                    monthlyProjection = Money(total * 4),
                    description = "${items.size} tranzacții la ${cat.displayNameRO} în 7 zile"
                )
            }
            .sortedByDescending { it.countLast7Days }
    }

    companion object {
        val empty = PatternReport(
            topCategories = emptyList(),
            weekendSpike = WeekendSpikeSignal(Money(0), Money(0), 0.0, false),
            temporalClusters = emptyList(),
            outliers = emptyList(),
            frequencySpikes = emptyList()
        )
    }
}
