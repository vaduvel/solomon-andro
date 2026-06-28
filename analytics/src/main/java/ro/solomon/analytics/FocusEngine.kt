package ro.solomon.analytics

import ro.solomon.core.domain.Focus
import ro.solomon.core.domain.FocusFeasibility
import ro.solomon.core.domain.FocusType
import ro.solomon.core.domain.Goal
import ro.solomon.core.domain.Money
import ro.solomon.core.domain.SolBucket
import ro.solomon.core.domain.TransactionCategory
import ro.solomon.core.format.RomanianDateFormatter
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.roundToInt

/** Monthly spending split into Solomon Focus buckets (bani via [Money]). */
data class BucketBreakdown(
    val necesar: Money,
    val moft: Money,
    val prioritate: Money
) {
    val total: Money get() = necesar + moft + prioritate
}

data class FocusPlan(
    val focus: Focus,
    val recommendedMonthlyContribution: Money,
    val monthsToTarget: Int?,
    val feasibility: FocusFeasibility,
    val coachingRO: String
)

data class FocusOverview(
    val monthlyIncome: Money,
    val buckets: BucketBreakdown,
    val necesarFloor: Money,
    val goalsMonthlyContribution: Money,
    val focusMonthlyContribution: Money,
    val disponibil: Money,
    val daysUntilPayday: Int,
    val dailyAllowance: Money,
    val plans: List<FocusPlan>,
    val coachingRO: String
)

/**
 * The brain of Solomon Focus. From real cash-flow data it computes how much room
 * the user has for one or more short-term Focuses *without* pausing long-term
 * [Goal]s. Goals always keep their monthly contribution; a Focus works with what
 * remains (mostly the Moft pool). All math is in bani (see [Money]).
 */
class FocusEngine(
    private val goalContribution: GoalBudgetContribution = GoalBudgetContribution()
) {

    /** Classify monthly spending-by-category into the three buckets. */
    fun bucketize(
        spendingByCategory: Map<TransactionCategory, Money>,
        bucketOverrides: Map<TransactionCategory, SolBucket> = emptyMap()
    ): BucketBreakdown {
        var necesar = 0
        var moft = 0
        var prioritate = 0
        for ((cat, amount) in spendingByCategory) {
            when (bucketOverrides[cat] ?: SolBucket.defaultFor(cat)) {
                SolBucket.necesar -> necesar += amount.amount
                SolBucket.moft -> moft += amount.amount
                SolBucket.prioritate -> prioritate += amount.amount
            }
        }
        return BucketBreakdown(Money(necesar), Money(moft), Money(prioritate))
    }

    fun calculate(
        cashFlow: CashFlowAnalysis,
        focuses: List<Focus>,
        goals: List<Goal>,
        daysUntilNextPayday: Int,
        referenceDate: Long = System.currentTimeMillis(),
        bucketOverrides: Map<TransactionCategory, SolBucket> = emptyMap()
    ): FocusOverview {
        val monthlyIncome = cashFlow.monthlyIncomeAvg
        val buckets = bucketize(cashFlow.spendingByCategory, bucketOverrides)
        val necesarFloor = buckets.necesar
        val moftPool = buckets.moft

        // Goals run in PARALLEL with any Focus and are never paused: their monthly
        // contribution is reserved up-front, before computing Focus headroom.
        val activeGoals = goals.filterNot { it.isReached }
        val goalsMonthly = Money(goalContribution.totalMonthlySaving(activeGoals, referenceDate))

        // Money not committed to the Necesar floor or to long-term goals.
        val availableForFocus = (monthlyIncome - necesarFloor - goalsMonthly).amount

        val activeFocuses = focuses.filter { it.isActive }

        data class Computed(val focus: Focus, val rec: Int, val months: Int?)
        val computed = activeFocuses.map { f ->
            when (f.type) {
                FocusType.event, FocusType.emergency_fund, FocusType.early_repayment -> {
                    val remaining = f.amountRemaining.amount
                    val planned = f.plannedMonthlyContribution.amount
                    val rec = when {
                        planned > 0 -> planned
                        f.deadline != null -> ceil(remaining.toDouble() / monthsUntil(f.deadline, referenceDate)).toInt()
                        else -> minOf(remaining, maxOf(0, (monthlyIncome.amount * 0.10).roundToInt()))
                    }
                    val months = if (rec > 0 && remaining > 0) ceil(remaining.toDouble() / rec).toInt() else null
                    Computed(f, rec, months)
                }
                FocusType.moft_detox -> {
                    val pct = (f.detoxPercent ?: 0).coerceIn(0, 100)
                    val saved = (moftPool.amount.toDouble() * pct / 100.0).roundToInt()
                    Computed(f, saved, null)
                }
                FocusType.runway -> Computed(f, 0, null)
            }
        }

        val focusContribTotal = computed
            .filter { it.focus.type.hasMonetaryTarget }
            .sumOf { it.rec }
        // A Moft detox frees money from the Moft pool back into what's available.
        val detoxFreed = computed
            .filter { it.focus.type == FocusType.moft_detox }
            .sumOf { it.rec }

        val disponibilBani = monthlyIncome.amount - necesarFloor.amount - goalsMonthly.amount - focusContribTotal + detoxFreed
        val disponibil = Money(disponibilBani)
        val safeDays = maxOf(1, daysUntilNextPayday)
        val dailyAllowance = Money(maxOf(0, disponibilBani) / safeDays)

        val plans = computed.map { c ->
            val feasibility = feasibilityFor(c.focus.type, c.rec, availableForFocus, disponibilBani)
            FocusPlan(
                focus = c.focus,
                recommendedMonthlyContribution = Money(c.rec),
                monthsToTarget = c.months,
                feasibility = feasibility,
                coachingRO = coachingFor(c.focus, Money(c.rec), c.months, feasibility)
            )
        }

        return FocusOverview(
            monthlyIncome = monthlyIncome,
            buckets = buckets,
            necesarFloor = necesarFloor,
            goalsMonthlyContribution = goalsMonthly,
            focusMonthlyContribution = Money(focusContribTotal),
            disponibil = disponibil,
            daysUntilPayday = daysUntilNextPayday,
            dailyAllowance = dailyAllowance,
            plans = plans,
            coachingRO = overviewCoaching(disponibil, dailyAllowance, plans)
        )
    }

    private fun feasibilityFor(
        type: FocusType,
        rec: Int,
        availableForFocus: Int,
        disponibil: Int
    ): FocusFeasibility = when (type) {
        FocusType.runway -> when {
            disponibil > 0 -> FocusFeasibility.realist
            disponibil == 0 -> FocusFeasibility.strans
            else -> FocusFeasibility.nerealist
        }
        FocusType.moft_detox -> FocusFeasibility.realist
        else -> when {
            rec <= 0 -> FocusFeasibility.realist
            availableForFocus <= 0 -> FocusFeasibility.nerealist
            rec.toDouble() <= availableForFocus * 0.6 -> FocusFeasibility.realist
            rec <= availableForFocus -> FocusFeasibility.strans
            else -> FocusFeasibility.nerealist
        }
    }

    private fun monthsUntil(deadline: Long, referenceDate: Long): Int {
        val cal = RomanianDateFormatter.gregorianROCalendar(); cal.timeInMillis = referenceDate
        val dl = RomanianDateFormatter.gregorianROCalendar(); dl.timeInMillis = deadline
        val diff = (dl.get(Calendar.YEAR) - cal.get(Calendar.YEAR)) * 12 +
            (dl.get(Calendar.MONTH) - cal.get(Calendar.MONTH))
        return maxOf(1, diff)
    }

    private fun coachingFor(focus: Focus, rec: Money, months: Int?, feasibility: FocusFeasibility): String =
        when (focus.type) {
            FocusType.runway -> when (feasibility) {
                FocusFeasibility.realist -> "Ai spațiu să stai pe plus până la salariu. Ține mofturile sub control."
                FocusFeasibility.strans -> "Ești exact pe muchie până la salariu. Orice moft tăiat te ajută."
                FocusFeasibility.nerealist -> "La ritmul actual nu ajungi pe plus la salariu. Redu din Necesar sau amână un obiectiv."
            }
            FocusType.moft_detox -> "Tai ${focus.detoxPercent ?: 0}% din mofturi: economisești ~${lei(rec)} pe lună."
            FocusType.event -> goalCoaching("eveniment", rec, months, feasibility)
            FocusType.emergency_fund -> goalCoaching("fondul de urgență", rec, months, feasibility)
            FocusType.early_repayment -> goalCoaching("rata anticipată", rec, months, feasibility)
        }

    private fun goalCoaching(label: String, rec: Money, months: Int?, feasibility: FocusFeasibility): String {
        val monthsTxt = if (months != null) " (~$months luni)" else ""
        return when (feasibility) {
            FocusFeasibility.realist -> "Pentru $label pune ${lei(rec)} pe lună$monthsTxt. E realist, fără să atingi obiectivele."
            FocusFeasibility.strans -> "Pentru $label ai nevoie de ${lei(rec)} pe lună$monthsTxt. E strâns — vine mai ales din mofturi."
            FocusFeasibility.nerealist -> "Pentru $label ar trebui ${lei(rec)} pe lună$monthsTxt, mai mult decât ai liber. Prelungește termenul sau redu ținta."
        }
    }

    private fun overviewCoaching(disponibil: Money, daily: Money, plans: List<FocusPlan>): String {
        val head = if (disponibil.isNegative)
            "Atenție: după Necesar și obiective ești pe minus cu ${lei(-disponibil)}. Niciun Focus nu încape fără ajustări."
        else
            "Ai ${lei(disponibil)} liberi după Necesar și obiective, adică ~${lei(daily)} pe zi până la salariu."
        val primary = plans.firstOrNull { it.focus.isPrimary } ?: plans.firstOrNull()
        val tail = primary?.let { " Focus principal: ${it.focus.title} — ${it.feasibility.displayNameRO}." } ?: ""
        return head + tail
    }

    /** Locale-safe RON formatting (RO uses comma as decimal separator). */
    private fun lei(m: Money): String {
        val whole = m.amount / 100
        val cents = abs(m.amount % 100)
        return if (cents == 0) "$whole lei" else "$whole,%02d lei".format(cents)
    }
}
