package ro.solomon.analytics

import ro.solomon.core.domain.Transaction
import ro.solomon.core.domain.FlowDirection
import java.util.Calendar
import java.util.concurrent.TimeUnit

data class Suspicion(
    val transactionId: String,
    val trigger: Trigger,
    val severity: Severity,
    val evidenceText: String
) {
    enum class Trigger { large_amount_vs_average, burst_activity, unusual_night_merchant }
    enum class Severity { soft, medium, high }

    companion object {
        fun Severity.rank(s: Severity) = when (s) {
            Severity.soft -> 1; Severity.medium -> 2; Severity.high -> 3
        }
    }
}

class SuspiciousTransactionDetector(
    val dailyAverageMultiplier: Double = 5.0,
    val burstThreshold: Int = 5,
    val burstWindowMs: Long = TimeUnit.HOURS.toMillis(1),
    val nightStart: Int = 0,
    val nightEnd: Int = 5,
    val historyWindowDays: Int = 30
) {

    fun detect(
        transactions: List<Transaction>,
        referenceDate: Long = System.currentTimeMillis()
    ): List<Suspicion> {
        val outgoing = transactions
            .filter { it.direction == FlowDirection.outgoing }
            .sortedBy { it.date }
        if (outgoing.isEmpty()) return emptyList()

        val avgDaily = computeDailyAverage(outgoing, referenceDate)

        val triggersByTransaction = mutableMapOf<String, MutableSet<Suspicion.Trigger>>()
        val evidenceByTransaction = mutableMapOf<String, MutableList<String>>()

        if (avgDaily > 0) {
            val threshold = avgDaily.toDouble() * dailyAverageMultiplier
            for (tx in outgoing) {
                if (tx.amount.amount.toDouble() >= threshold) {
                    triggersByTransaction.getOrPut(tx.id) { mutableSetOf() }.add(Suspicion.Trigger.large_amount_vs_average)
                    val multiple = tx.amount.amount.toDouble() / avgDaily.toDouble()
                    evidenceByTransaction.getOrPut(tx.id) { mutableListOf() }.add(
                        String.format("%.1fx media zilnică (%d RON)", multiple, avgDaily)
                    )
                }
            }
        }

        val burstIds = detectBursts(outgoing)
        for (id in burstIds) {
            triggersByTransaction.getOrPut(id) { mutableSetOf() }.add(Suspicion.Trigger.burst_activity)
            evidenceByTransaction.getOrPut(id) { mutableListOf() }.add("$burstThreshold+ tranzacții în <1h")
        }

        val nightSuspectIds = detectNightNewMerchants(outgoing, referenceDate)
        for (id in nightSuspectIds) {
            triggersByTransaction.getOrPut(id) { mutableSetOf() }.add(Suspicion.Trigger.unusual_night_merchant)
            evidenceByTransaction.getOrPut(id) { mutableListOf() }.add("Merchant nou la noapte (00–05)")
        }

        val suspicions = triggersByTransaction.map { (txId, triggers) ->
            val primaryTrigger = when {
                triggers.contains(Suspicion.Trigger.large_amount_vs_average) -> Suspicion.Trigger.large_amount_vs_average
                triggers.contains(Suspicion.Trigger.unusual_night_merchant) -> Suspicion.Trigger.unusual_night_merchant
                else -> Suspicion.Trigger.burst_activity
            }
            val severity = when (triggers.size) {
                1 -> Suspicion.Severity.soft
                2 -> Suspicion.Severity.medium
                else -> Suspicion.Severity.high
            }
            val evidence = (evidenceByTransaction[txId] ?: emptyList()).joinToString(" · ")
            Suspicion(transactionId = txId, trigger = primaryTrigger, severity = severity, evidenceText = evidence)
        }

        return suspicions.sortedWith(compareByDescending<Suspicion> { it.severity.ordinal }
            .thenBy { it.trigger.name })
    }

    fun computeDailyAverage(outgoing: List<Transaction>, referenceDate: Long): Int {
        val cal = Calendar.getInstance()
        cal.timeInMillis = referenceDate; cal.add(Calendar.DAY_OF_YEAR, -historyWindowDays)
        val windowStart = cal.timeInMillis
        val recent = outgoing.filter { it.date >= windowStart }
        if (recent.isEmpty()) return 0
        val totalSpent = recent.sumOf { it.amount.amount }
        return totalSpent / maxOf(1, historyWindowDays)
    }

    fun detectBursts(outgoing: List<Transaction>): Set<String> {
        val burstIds = mutableSetOf<String>()
        if (outgoing.size < burstThreshold) return burstIds
        for (i in 0..(outgoing.size - burstThreshold)) {
            val window = outgoing.subList(i, i + burstThreshold)
            val first = window.first(); val last = window.last()
            if (last.date - first.date <= burstWindowMs) {
                for (tx in window) burstIds.add(tx.id)
            }
        }
        return burstIds
    }

    fun detectNightNewMerchants(outgoing: List<Transaction>, referenceDate: Long): Set<String> {
        val cal = Calendar.getInstance()
        cal.timeInMillis = referenceDate; cal.add(Calendar.DAY_OF_YEAR, -historyWindowDays)
        val cutoff = cal.timeInMillis

        val seenMerchants = mutableSetOf<String>()
        for (tx in outgoing) {
            if (tx.date < cutoff) { tx.merchant?.let { seenMerchants.add(it.lowercase()) } }
        }

        val suspect = mutableSetOf<String>()
        for (tx in outgoing.filter { it.date >= cutoff }) {
            val c = Calendar.getInstance(); c.timeInMillis = tx.date
            val hour = c.get(Calendar.HOUR_OF_DAY)
            val isNight = hour in nightStart until nightEnd
            if (!isNight) {
                tx.merchant?.let { seenMerchants.add(it.lowercase()) }
                continue
            }
            val merchantLow = tx.merchant?.lowercase()
            if (merchantLow != null) {
                if (!seenMerchants.contains(merchantLow)) suspect.add(tx.id)
                seenMerchants.add(merchantLow)
            } else {
                suspect.add(tx.id)
            }
        }
        return suspect
    }
}
