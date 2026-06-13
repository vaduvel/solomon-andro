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

    fun analyze(
        cashFlow: CashFlowAnalysis,
        transactions: List<Transaction>,
        nowEpoch: Long
    ): ForecastResult {
        val dailySpend = if (cashFlow.monthlySpendingAvg.amount > 0) cashFlow.monthlySpendingAvg.amount / 30 else 0
        val netDaily = (cashFlow.monthlyIncomeAvg.amount / 30) - dailySpend

        val balanceNow = transactions
            .filter { it.date <= nowEpoch }
            .fold(0) { acc, t -> acc + (if (t.direction == FlowDirection.incoming) t.amount.amount else -t.amount.amount) }

        val in7 = balanceNow + netDaily * 7
        val in30 = balanceNow + netDaily * 30
        val risk = when {
            in7 < 0 -> ForecastResult.RiskLevel.high
            in7 < 500 -> ForecastResult.RiskLevel.medium
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
