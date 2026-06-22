package ro.solomon.analytics

import ro.solomon.core.domain.Money
import ro.solomon.core.domain.Transaction
import ro.solomon.core.domain.FlowDirection

data class ForecastResult(
    val projectedBalanceIn7Days: Money,
    val projectedBalanceIn30Days: Money,
    val riskLevel: RiskLevel,
    val tip: String
) {
    enum class RiskLevel { low, medium, high }
}

class ForecastEngine {

    /**
     * @param currentBalance when provided, used as the starting balance for the
     *   projection. When null, falls back to the net of all known transactions up
     *   to [nowEpoch] (a proxy, not the true account balance).
     */
    fun analyze(
        cashFlow: CashFlowAnalysis,
        transactions: List<Transaction>,
        nowEpoch: Long,
        currentBalance: Money? = null
    ): ForecastResult {
        // Compute the monthly net first, then divide once. Dividing income and
        // spending separately truncated twice and lost precision each day.
        val monthlyNet = cashFlow.monthlyIncomeAvg.amount - cashFlow.monthlySpendingAvg.amount
        val netDaily = monthlyNet / 30

        val balanceNow = currentBalance?.amount ?: transactions
            .filter { it.date <= nowEpoch }
            .fold(0) { acc, t -> acc + (if (t.direction == FlowDirection.incoming) t.amount.amount else -t.amount.amount) }

        val in7 = balanceNow + netDaily * 7
        val in30 = balanceNow + netDaily * 30

        // Thresholds in bani. 500 lei = 50_000 bani.
        val mediumThresholdBani = 500 * 100
        val hasIncomeSignal = cashFlow.monthlyIncomeAvg.isPositive

        val risk = when {
            in7 < 0 -> ForecastResult.RiskLevel.high
            in7 < mediumThresholdBani -> ForecastResult.RiskLevel.medium
            // Without any income signal a 'low risk' projection isn't trustworthy.
            !hasIncomeSignal && balanceNow < mediumThresholdBani -> ForecastResult.RiskLevel.medium
            else -> ForecastResult.RiskLevel.low
        }
        val tip = when (risk) {
            ForecastResult.RiskLevel.high -> "Atenție: în 7 zile estimăm fonduri negative. Redu cheltuielile neesențiale."
            ForecastResult.RiskLevel.medium -> "Ești pe muchie. Mică reducere la lifestyle acum te scapă mai târziu."
            ForecastResult.RiskLevel.low -> "Ritmul actual e sustenabil. Continuă așa."
        }
        return ForecastResult(
            projectedBalanceIn7Days = Money(in7),
            projectedBalanceIn30Days = Money(in30),
            riskLevel = risk,
            tip = tip
        )
    }
}
