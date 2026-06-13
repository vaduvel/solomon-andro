package ro.solomon.moments

import kotlinx.serialization.json.Json
import ro.solomon.analytics.CashFlowAnalyzer
import ro.solomon.analytics.CashFlowAnalysis
import ro.solomon.analytics.PatternDetector
import ro.solomon.analytics.SpiralDetector
import ro.solomon.analytics.SubscriptionAuditor
import ro.solomon.core.domain.*
import ro.solomon.core.format.RomanianDateFormatter
import ro.solomon.core.moments.*
import ro.solomon.llm.LLMProvider
import ro.solomon.llm.LLMOutputValidator
import ro.solomon.llm.TemplateLLMProvider
import java.util.Calendar

class MomentEngine(
    private val llm: LLMProvider,
    private val json: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    }
) {

    data class Snapshot(
        val userProfile: UserProfile? = null,
        val transactions: List<Transaction> = emptyList(),
        val obligations: List<Obligation> = emptyList(),
        val subscriptions: List<Subscription> = emptyList(),
        val goals: List<Goal> = emptyList(),
        val referenceDateEpochSeconds: Long = System.currentTimeMillis() / 1000L
    )

    private val cashFlowAnalyzer = CashFlowAnalyzer()
    private val subscriptionAuditor = SubscriptionAuditor()
    private val spiralDetector = SpiralDetector()
    private val patternDetector = PatternDetector()
    private val validator = LLMOutputValidator()
    private val orchestrator = MomentOrchestrator(json)

    suspend fun generateBestMoment(snapshot: Snapshot): MomentOutput? {
        val candidates = buildCandidates(snapshot)
        if (!candidates.hasAnyCandidate) return null
        return try {
            val initial = orchestrator.generate(candidates, llm)
            val validation = validator.validate(
                text = initial.llmResponse,
                maxWords = initial.momentType.maxWords
            )
            if (validation is LLMOutputValidator.Result.Valid) {
                initial
            } else {
                val safeTemplate = TemplateLLMProvider()
                try {
                    orchestrator.generate(candidates, safeTemplate)
                } catch (_: Throwable) {
                    initial
                }
            }
        } catch (e: Throwable) {
            null
        }
    }

    fun selectedType(snapshot: Snapshot): MomentType? {
        return orchestrator.selectedType(buildCandidates(snapshot))
    }

    suspend fun generateWowMoment(snapshot: Snapshot): MomentOutput {
        val context = buildWowMomentContext(snapshot)
        return WowMomentBuilder().build(context, llm, json)
    }

    fun buildCandidates(snapshot: Snapshot): MomentCandidates {
        if (snapshot.transactions.isEmpty()) {
            return MomentCandidates(wowMoment = buildWowMomentContext(snapshot))
        }
        val cashFlow = cashFlowAnalyzer.analyze(
            transactions = snapshot.transactions,
            referenceDate = snapshot.referenceDateEpochSeconds * 1000L
        )
        val history = computeMonthlyBalanceHistory(snapshot, cashFlow)

        val spiralReport = spiralDetector.detect(
            transactions = snapshot.transactions,
            obligations = snapshot.obligations,
            monthlyIncomeAvg = cashFlow.monthlyIncomeAvg,
            monthlySpendingAvg = cashFlow.monthlySpendingAvg,
            monthlyBalanceHistory = history,
            referenceDate = snapshot.referenceDateEpochSeconds * 1000L
        )
        val spiralCtx = if (spiralReport.score >= 2)
            buildSpiralAlertContext(snapshot, spiralReport, cashFlow) else null

        val audit = subscriptionAuditor.audit(subscriptions = snapshot.subscriptions)
        val subCtx = if (audit.ghostCount > 0)
            buildSubscriptionAuditContext(snapshot, audit) else null

        val paydayCtx = buildPaydayContext(snapshot, cashFlow)
        val upcomingCtx = buildUpcomingObligationContext(snapshot, cashFlow)
        val patternCtx = buildPatternAlertContext(snapshot)
        val weeklyCtx = buildWeeklySummaryContext(snapshot, cashFlow)
        val wowCtx = buildWowMomentContext(snapshot)

        return MomentCandidates(
            wowMoment = wowCtx,
            payday = paydayCtx,
            upcomingObligation = upcomingCtx,
            patternAlert = patternCtx,
            subscriptionAudit = subCtx,
            spiralAlert = spiralCtx,
            weeklySummary = weeklyCtx
        )
    }

    private fun buildPaydayContext(snapshot: Snapshot, cashFlow: CashFlowAnalysis): PaydayContext? {
        val cal = RomanianDateFormatter.gregorianROCalendar()
        val nowMillis = snapshot.referenceDateEpochSeconds * 1000L
        cal.timeInMillis = nowMillis
        cal.add(Calendar.HOUR_OF_DAY, -48)
        val cutoffMillis = cal.timeInMillis

        val avgIncomeAmount = cashFlow.monthlyIncomeAvg.amount
        val declaredMid = snapshot.userProfile?.financials?.salaryRange?.midpointRON ?: 0
        val referenceIncome = maxOf(avgIncomeAmount, declaredMid)
        val minSalaryRON = if (referenceIncome > 0) (referenceIncome * 0.70).toInt() else 1500

        val recentIncoming = snapshot.transactions
            .filter { it.isIncoming && it.date >= cutoffMillis && it.date <= nowMillis && it.amount.amount >= minSalaryRON }
            .maxByOrNull { it.amount.amount }
            ?: return null

        val received = recentIncoming.amount.amount
        val isHigher = avgIncomeAmount > 0 && received > avgIncomeAmount * 1.10
        val isLower = avgIncomeAmount > 0 && received < avgIncomeAmount * 0.90

        val paydayDay = paydayDayOfMonth(snapshot)
        val daysUntilNext = daysUntilDayOfMonth(paydayDay, snapshot.referenceDateEpochSeconds * 1000L)

        val obligReserves = snapshot.obligations.map { o ->
            PaydayObligationReserve(name = o.name, amount = o.amount, status = PaydayReserveStatus.rezervat)
        }
        val obligTotal = snapshot.obligations.fold(Money(0)) { acc, o -> acc + o.amount }

        val activeSubs = snapshot.subscriptions.filter { !it.isGhost }
        val subReserves = activeSubs.take(5).map { s -> PaydaySubscriptionReserve(name = s.name, amount = s.amountMonthly) }
        val subTotal = activeSubs.fold(Money(0)) { acc, s -> acc + s.amountMonthly }

        val available = Money(maxOf(0, received - obligTotal.amount - subTotal.amount))
        val perDay = if (daysUntilNext > 0) Money(available.amount / daysUntilNext) else Money(0)

        val allocation = PaydayAllocation(
            obligationsReserved = obligReserves,
            subscriptionsReserved = subReserves,
            obligationsTotal = obligTotal,
            subscriptionsTotal = subTotal,
            savingsAuto = PaydaySavingsAuto(enabled = false),
            availableToSpend = available,
            daysUntilNextPayday = daysUntilNext,
            availablePerDay = perDay
        )

        val lastMonthAvailable = computeLastMonthAvailable(snapshot)
        val rawDiff = available.amount - lastMonthAvailable.amount
        val direction = when {
            rawDiff >= 100 -> ComparisonDirection.better
            rawDiff <= -100 -> ComparisonDirection.worse
            else -> ComparisonDirection.same
        }
        val comparisons = PaydayComparisons(
            vsLastMonthAvailable = lastMonthAvailable,
            vsLastMonthDiff = Money(kotlin.math.abs(rawDiff)),
            vsLastMonthDirection = direction
        )

        val budgets = cashFlow.spendingByCategory.entries
            .sortedByDescending { it.value.amount }
            .take(3)
            .map { (cat, amt) -> CategoryBudgetSuggestion(category = cat, amount = amt, basedOn = BudgetBasis.average) }

        val warnings = mutableListOf<PaydayWarning>()
        val oblRatio = if (avgIncomeAmount > 0) obligTotal.amount.toDouble() / avgIncomeAmount.toDouble() else 0.0
        if (oblRatio > 0.5) {
            warnings.add(PaydayWarning(
                type = PaydayWarningType.obligationsTooHigh,
                description = "Obligațiile reprezintă ${(oblRatio * 100).toInt()}% din venit",
                impact = "Rămân ${available.amount} RON disponibil"
            ))
        }
        if (available.amount < 500) {
            warnings.add(PaydayWarning(
                type = PaydayWarningType.lowAvailable,
                description = "Suma disponibilă după obligații: ${available.amount} RON",
                impact = "≈ ${perDay.amount} RON/zi"
            ))
        }

        return PaydayContext.create(
            user = buildMomentUser(snapshot),
            salary = PaydaySalary(
                amountReceived = recentIncoming.amount,
                receivedDate = recentIncoming.date.toString(),
                source = recentIncoming.merchant ?: "Angajator",
                isHigherThanAverage = isHigher,
                isLowerThanAverage = isLower
            ),
            autoAllocation = allocation,
            comparisons = comparisons,
            categoryBudgetsSuggested = budgets,
            warnings = warnings
        )
    }

    private fun buildUpcomingObligationContext(snapshot: Snapshot, cashFlow: CashFlowAnalysis): UpcomingObligationContext? {
        val cal = RomanianDateFormatter.gregorianROCalendar()
        val nowMillis = snapshot.referenceDateEpochSeconds * 1000L
        cal.timeInMillis = nowMillis
        val today = cal.get(Calendar.DAY_OF_MONTH)

        val candidate = snapshot.obligations.mapNotNull { o ->
            var days = o.dayOfMonth - today
            if (days <= 0) {
                val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                days = daysInMonth - today + o.dayOfMonth
            }
            if (days in 1..5) o to days else null
        }.minByOrNull { it.second } ?: return null

        val (obligation, daysUntil) = candidate
        val dueDate = cal.apply { add(Calendar.DAY_OF_MONTH, daysUntil) }.timeInMillis

        val calMonth = RomanianDateFormatter.gregorianROCalendar().apply {
            timeInMillis = nowMillis
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val monthStart = calMonth.timeInMillis
        val spentThisMonth = snapshot.transactions
            .filter { it.isOutgoing && it.date >= monthStart && it.date <= nowMillis }
            .sumOf { it.amount.amount }
        val currentBalance = Money(maxOf(0, cashFlow.monthlyIncomeAvg.amount - spentThisMonth))
        val afterPayment = Money(maxOf(0, currentBalance.amount - obligation.amount.amount))

        val paydayDay = paydayDayOfMonth(snapshot)
        val daysUntilPayday = daysUntilDayOfMonth(paydayDay, nowMillis)
        val perDayAfter = if (daysUntilPayday > 0) Money(afterPayment.amount / daysUntilPayday) else Money(0)

        val isTight = afterPayment.amount < 500
        val isAffordable = currentBalance.amount >= obligation.amount.amount
        val tone = when {
            !isAffordable -> AssessmentTone.urgent
            isTight -> AssessmentTone.alert
            daysUntil <= 1 -> AssessmentTone.calm
            else -> AssessmentTone.reassuring
        }

        val weekday = cal.get(Calendar.DAY_OF_WEEK)
        val isWeekend = weekday == Calendar.SATURDAY || weekday == Calendar.SUNDAY
        val weekendAvg = Money(cashFlow.monthlySpendingAvg.amount / 14)

        val upcomingItem = UpcomingObligationItem(
            name = obligation.name,
            amountEstimated = obligation.amount,
            dueDate = dueDate.toString(),
            daysUntilDue = daysUntil,
            amountEstimationConfidence = EstimationConfidence.high,
            basedOnHistory = "Obligație fixă lunară"
        )

        return UpcomingObligationContext.create(
            user = buildMomentUser(snapshot),
            upcoming = upcomingItem,
            context = UpcomingObligationCashContext(
                currentBalance = currentBalance,
                afterPayment = afterPayment,
                daysUntilNextPayday = daysUntilPayday,
                availablePerDayAfter = perDayAfter
            ),
            assessment = UpcomingObligationAssessment(
                isAffordable = isAffordable,
                isTight = isTight,
                tone = tone
            ),
            weekendWarning = WeekendWarning(
                isWeekendComing = isWeekend,
                weekendAvgSpend = weekendAvg,
                wouldCreateProblem = isWeekend && isTight
            )
        )
    }

    private fun buildPatternAlertContext(snapshot: Snapshot): PatternAlertContext? {
        if (snapshot.transactions.size < 10) return null
        val report = patternDetector.detect(
            transactions = snapshot.transactions,
            referenceDate = snapshot.referenceDateEpochSeconds * 1000L
        )

        val spike = report.frequencySpikes.firstOrNull()
        if (spike != null) {
            val pattern = PatternDetected(
                category = spike.category,
                merchantDominant = spike.merchantDominant,
                type = PatternType.frequency_spike,
                description = spike.description,
                amountPeriod = spike.amountLast7Days,
                amountProjectedMonthly = spike.monthlyProjection,
                vsBudget = spike.monthlyProjection,
                vsBudgetPct = 0,
                temporalConcentration = TemporalConcentration(
                    isTemporal = false,
                    pattern = "",
                    interpretation = ""
                )
            )
            return PatternAlertContext.create(
                user = buildMomentUser(snapshot),
                patternDetected = pattern,
                scenarios = buildPatternScenarios(pattern)
            )
        }

        if (report.weekendSpike.isSignificant) {
            val pct = ((report.weekendSpike.ratio - 1.0) * 100).toInt()
            val pattern = PatternDetected(
                category = TransactionCategory.entertainment,
                merchantDominant = null,
                type = PatternType.weekend_spike,
                description = "Cheltuielile din weekend sunt de ${"%.1f".format(report.weekendSpike.ratio)}x mai mari decât în zilele de lucru",
                amountPeriod = Money(report.weekendSpike.weekendAvgPerDay.amount * 8),
                amountProjectedMonthly = Money(report.weekendSpike.weekendAvgPerDay.amount * 8),
                vsBudget = report.weekendSpike.weekdayAvgPerDay,
                vsBudgetPct = pct,
                temporalConcentration = TemporalConcentration(
                    isTemporal = true,
                    pattern = "Weekend (sâmbătă–duminică)",
                    interpretation = "Cheltuielile se concentrează în weekend"
                )
            )
            return PatternAlertContext.create(
                user = buildMomentUser(snapshot),
                patternDetected = pattern,
                scenarios = buildPatternScenarios(pattern),
                toneCalibration = PatternToneCalibration.curiousReflective
            )
        }

        val cluster = report.temporalClusters.firstOrNull { it.isStrong }
        if (cluster != null) {
            val catSpending = report.topCategories.firstOrNull { it.category == cluster.category }
            val pattern = PatternDetected(
                category = cluster.category,
                merchantDominant = catSpending?.dominantMerchant,
                type = PatternType.temporal_clustering,
                description = cluster.description,
                amountPeriod = catSpending?.totalAmount ?: Money(0),
                amountProjectedMonthly = catSpending?.totalAmount?.let { Money(it.amount / 3) } ?: Money(0),
                vsBudget = Money(0),
                vsBudgetPct = 0,
                temporalConcentration = TemporalConcentration(
                    isTemporal = true,
                    pattern = cluster.description,
                    interpretation = "Pattern temporal consistent în ultimele 3 luni"
                )
            )
            return PatternAlertContext.create(
                user = buildMomentUser(snapshot),
                patternDetected = pattern,
                scenarios = buildPatternScenarios(pattern),
                toneCalibration = PatternToneCalibration.curiousReflective
            )
        }
        return null
    }

    private fun buildPatternScenarios(pattern: PatternDetected): List<PatternScenario> {
        val monthly = pattern.amountProjectedMonthly.amount
        val saving = maxOf(0, monthly / 4)
        return listOf(
            PatternScenario(
                scenarioId = PatternScenarioID.continueAsIs,
                description = "Continuă ca acum",
                monthEndOutcome = "Cheltuiești ~$monthly RON/lună pe ${pattern.category.displayNameRO}",
                goalImpact = "Impact neutru față de obiectivele actuale"
            ),
            PatternScenario(
                scenarioId = PatternScenarioID.reduce2PerWeek,
                description = "Reduce cu 2 vizite pe săptămână",
                monthEndOutcome = "Economisești ~$saving RON/lună",
                goalImpact = "Progres mai rapid spre obiective"
            )
        )
    }

    private fun buildWeeklySummaryContext(snapshot: Snapshot, cashFlow: CashFlowAnalysis): WeeklySummaryContext? {
        val cal = RomanianDateFormatter.gregorianROCalendar()
        val nowMillis = snapshot.referenceDateEpochSeconds * 1000L
        cal.timeInMillis = nowMillis
        val weekday = cal.get(Calendar.DAY_OF_WEEK)
        if (weekday != Calendar.SUNDAY && weekday != Calendar.MONDAY) return null

        cal.add(Calendar.DAY_OF_MONTH, -7)
        val weekAgo = cal.timeInMillis
        cal.timeInMillis = weekAgo
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        val weekStart = cal.timeInMillis
        cal.add(Calendar.DAY_OF_MONTH, 6)
        val weekEnd = cal.timeInMillis

        val weekTxs = snapshot.transactions
            .filter { it.isOutgoing && it.date >= weekStart && it.date <= weekEnd }
        val weekTotal = weekTxs.sumOf { it.amount.amount }
        val weeklyAvg = cashFlow.monthlySpendingAvg.amount / 4
        val diff = weekTotal - weeklyAvg
        val diffPct = if (weeklyAvg > 0) (kotlin.math.abs(diff).toDouble() / weeklyAvg * 100).toInt() else 0
        val direction = when {
            diff < -100 -> SpendingTrendDirection.below
            diff < -20 -> SpendingTrendDirection.slightlyBelow
            diff in -20..20 -> SpendingTrendDirection.onAverage
            diff <= 100 -> SpendingTrendDirection.slightlyAbove
            else -> SpendingTrendDirection.above
        }

        val spending = WeeklySpendingBlock(
            total = Money(weekTotal),
            vsWeeklyAvg = Money(weeklyAvg),
            diffPct = diffPct,
            direction = direction
        )

        val highlights = mutableListOf<WeeklyHighlight>()
        weekTxs.maxByOrNull { it.amount.amount }?.let { biggest ->
            highlights.add(WeeklyHighlight(
                type = WeeklyHighlightType.biggestExpense,
                category = biggest.category,
                amount = biggest.amount,
                context = "Cea mai mare cheltuială: ${biggest.merchant ?: biggest.category.displayNameRO}"
            ))
        }
        if (direction == SpendingTrendDirection.below || direction == SpendingTrendDirection.slightlyBelow) {
            highlights.add(WeeklyHighlight(
                type = WeeklyHighlightType.budgetKept,
                context = "Ai cheltuit cu $diffPct% mai puțin decât media săptămânală"
            ))
        }

        val nextWeekStart = cal.apply { add(Calendar.DAY_OF_MONTH, 1) }.timeInMillis
        val nextWeekEnd = cal.apply { add(Calendar.DAY_OF_MONTH, 6) }.timeInMillis
        val nextWeekObligations = snapshot.obligations.mapNotNull { o ->
            cal.timeInMillis = nextWeekStart
            cal.set(Calendar.DAY_OF_MONTH, o.dayOfMonth)
            val due = cal.timeInMillis
            if (due in nextWeekStart..nextWeekEnd) {
                UpcomingObligationRef(name = o.name, amount = o.amount, day = RomanianDateFormatter.weekdayName(cal.get(Calendar.DAY_OF_WEEK)))
            } else null
        }
        val smallWin = if (direction == SpendingTrendDirection.below) {
            SmallWin(exists = true, description = "Ai economisit ${kotlin.math.abs(diff)} RON față de media ta săptămânală!")
        } else SmallWin(exists = false)

        cal.timeInMillis = weekAgo
        val weekNumber = cal.get(Calendar.WEEK_OF_YEAR)
        return WeeklySummaryContext.create(
            user = buildMomentUser(snapshot),
            week = WeekRange(start = weekStart.toString(), end = weekEnd.toString(), weekNumber = weekNumber),
            spending = spending,
            highlights = highlights,
            nextWeekPreview = NextWeekPreview(obligationsDue = nextWeekObligations, eventsInCalendar = emptyList()),
            smallWin = smallWin
        )
    }

    private fun buildSubscriptionAuditContext(
        snapshot: Snapshot,
        audit: ro.solomon.analytics.SubscriptionAuditReport
    ): SubscriptionAuditContext {
        val ghosts = audit.ghostSubscriptions.map { sub ->
            GhostSubscriptionDetail(
                name = sub.name,
                amountMonthly = sub.amountMonthly,
                amountAnnual = sub.amountAnnual,
                lastUsedDaysAgo = sub.lastUsedDaysAgo ?: 0,
                cancellationDifficulty = sub.cancellationDifficulty,
                cancellationUrl = sub.cancellationUrl,
                cancellationStepsSummary = sub.cancellationStepsSummary,
                cancellationWarning = sub.cancellationWarning,
                alternativeSuggestion = sub.alternativeSuggestion
            )
        }
        return SubscriptionAuditContext.create(
            user = buildMomentUser(snapshot),
            auditPeriodDays = 30,
            ghostSubscriptions = ghosts,
            totals = SubscriptionAuditTotals(
                monthlyRecoverable = audit.monthlyRecoverable,
                annualRecoverable = audit.annualRecoverable,
                contextComparison = "≈ ${audit.annualRecoverable.amount} RON pe an"
            ),
            activeSubscriptionsKept = ActiveSubscriptionsKept(
                count = audit.activeSubscriptions.size,
                monthlyTotal = audit.monthlyKeptTotal,
                examples = audit.activeSubscriptions.take(3).map { it.name }
            )
        )
    }

    private fun buildSpiralAlertContext(
        snapshot: Snapshot,
        report: ro.solomon.analytics.SpiralReport,
        cashFlow: CashFlowAnalysis
    ): SpiralAlertContext {
        val audit = subscriptionAuditor.audit(subscriptions = snapshot.subscriptions)

        val step1 = audit.ghostSubscriptions.firstOrNull()?.let { ghost ->
            RecoveryStep(
                action = "Anulează ${ghost.name} (${ghost.amountMonthly.amount} RON/lună)",
                monthlySaving = ghost.amountMonthly,
                potentialSaving = "${ghost.amountAnnual.amount} RON/an",
                complexity = RecoveryComplexity.easy
            )
        } ?: RecoveryStep(action = "Identifică cel mai mare abonament nefolosit", complexity = RecoveryComplexity.easy)

        val topCategoryEntry = cashFlow.spendingByCategory.entries.maxByOrNull { it.value.amount }
        val step2 = if (topCategoryEntry != null && topCategoryEntry.value.amount > 100) {
            val (cat, amt) = topCategoryEntry
            val target = amt.amount / 3
            RecoveryStep(
                action = "Reduce cheltuielile pe ${cat.displayNameRO} cu 33% (~$target RON/lună)",
                monthlySaving = Money(target),
                complexity = RecoveryComplexity.medium
            )
        } else {
            RecoveryStep(action = "Stabilește un buget zilnic clar pentru cheltuieli discreționare", complexity = RecoveryComplexity.medium)
        }

        val csalbRelevant = report.severity >= SpiralSeverity.high &&
            snapshot.obligations.any { it.kind == ObligationKind.loan_ifn || it.kind == ObligationKind.bnpl }
        val step3 = if (csalbRelevant) {
            RecoveryStep(
                action = "Trimite cazul la CSALB pentru mediere gratuită cu IFN/banca",
                complexity = RecoveryComplexity.hard,
                tool = RecoveryTool.csalb
            )
        } else {
            RecoveryStep(action = "Construiește un fond de urgență de 3 luni cheltuieli", complexity = RecoveryComplexity.hard)
        }

        return SpiralAlertContext.create(
            user = buildMomentUser(snapshot),
            spiralScore = report.score,
            severity = report.severity,
            factorsDetected = report.factors,
            narrativeSummary = "Solomon a detectat ${report.factors.size} factori de risc. Verifică planul.",
            interventionNeeded = report.requiresIntervention,
            csalbRelevant = csalbRelevant,
            recoveryPlan = RecoveryPlan(step1 = step1, step2 = step2, step3 = step3)
        )
    }

    private fun buildWowMomentContext(snapshot: Snapshot): WowMomentContext {
        val cashFlow = if (snapshot.transactions.isEmpty()) {
            CashFlowAnalyzer.empty(windowDays = 180)
        } else {
            cashFlowAnalyzer.analyze(
                transactions = snapshot.transactions,
                referenceDate = snapshot.referenceDateEpochSeconds * 1000L
            )
        }
        val history = computeMonthlyBalanceHistory(snapshot, cashFlow)
        val audit = subscriptionAuditor.audit(subscriptions = snapshot.subscriptions)
        val spiralReport = spiralDetector.detect(
            transactions = snapshot.transactions,
            obligations = snapshot.obligations,
            monthlyIncomeAvg = cashFlow.monthlyIncomeAvg,
            monthlySpendingAvg = cashFlow.monthlySpendingAvg,
            monthlyBalanceHistory = history,
            referenceDate = snapshot.referenceDateEpochSeconds * 1000L
        )
        val obligTotal = snapshot.obligations.fold(0) { acc, o -> acc + o.amount.amount }
        val obligationsRatio = if (cashFlow.monthlyIncomeAvg.amount > 0) obligTotal.toDouble() / cashFlow.monthlyIncomeAvg.amount.toDouble() else 0.0

        val lowest = cashFlow.monthlyIncomeLowest?.let { mp ->
            LowestMonth(amount = mp.amount, month = monthName(mp.key.year, mp.key.month))
        } ?: LowestMonth(amount = cashFlow.monthlyIncomeAvg, month = "necunoscută")

        val cardCreditUsed = snapshot.obligations.any { it.kind == ObligationKind.loan_bank } ||
            snapshot.transactions.any { tx ->
                tx.merchant?.lowercase()?.let { it.contains("credit") || it.contains("card") } ?: false
            }

        val overdraftCount = history.takeLast(6).runningFold(0) { acc, m -> acc + m.amount }.count { it < 0 }

        val cutoff = snapshot.referenceDateEpochSeconds * 1000L - 180L * 24L * 3600L * 1000L
        val threshold = maxOf(cashFlow.monthlySpendingAvg.amount / 3, 500)
        val outliers = snapshot.transactions
            .filter { it.isOutgoing && it.date >= cutoff && it.amount.amount >= threshold }
            .sortedByDescending { it.amount.amount }
            .take(3)
            .mapIndexed { idx, tx ->
                OutlierItem(
                    rank = idx + 1,
                    type = OutlierType.single_large_purchase,
                    category = tx.category,
                    merchant = tx.merchant,
                    amount = tx.amount,
                    date = tx.date,
                    contextPhrase = "${tx.merchant ?: tx.category.displayNameRO} — ${tx.amount.amount} RON",
                    contextComparison = ""
                )
            }

        return WowMomentContext.create(
            user = buildMomentUser(snapshot),
            analysisPeriodDays = cashFlow.windowDays,
            income = WowIncome(
                monthlyAvg = cashFlow.monthlyIncomeAvg,
                stability = when (cashFlow.monthlyBalanceTrend) {
                    BalanceTrend.healthy -> IncomeStability.stable
                    BalanceTrend.breaking_even, BalanceTrend.barely_breakeven -> IncomeStability.slightly_variable
                    BalanceTrend.sliding_negative -> IncomeStability.variable
                    BalanceTrend.negative -> IncomeStability.unstable
                },
                lowestMonth = lowest,
                extraIncomeDetected = snapshot.userProfile?.financials?.hasSecondaryIncome ?: false,
                extraIncomeAvg = snapshot.userProfile?.financials?.secondaryIncomeAvg
            ),
            spending = WowSpending(
                monthlyAvg = cashFlow.monthlySpendingAvg,
                incomeConsumptionRatio = cashFlow.incomeConsumptionRatio,
                monthlyBalanceTrend = cashFlow.monthlyBalanceTrend,
                cardCreditUsed = cardCreditUsed,
                overdraftUsedCount180d = overdraftCount
            ),
            outliers = outliers,
            patterns = emptyList(),
            obligations = ObligationsBlock(
                monthlyTotalFixed = Money(obligTotal),
                items = snapshot.obligations.take(8).map { ObligationSummaryItem(name = it.name, amount = it.amount, dayOfMonth = it.dayOfMonth) },
                obligationsToIncomeRatio = obligationsRatio
            ),
            ghostSubscriptions = GhostSubscriptionsBlock(
                count = audit.ghostCount,
                monthlyTotal = audit.monthlyRecoverable,
                annualTotal = audit.annualRecoverable,
                items = audit.ghostSubscriptions.take(5).map { sub ->
                    GhostSubscriptionItem(name = sub.name, amount = sub.amountMonthly, lastUsedDaysAgo = sub.lastUsedDaysAgo ?: 0, confidence = sub.ghostConfidence)
                }
            ),
            positives = if (obligationsRatio < 0.3) {
                listOf(PositiveItem(type = PositiveType.rent_to_income_healthy, description = "Obligațiile sunt sub 30% din venit"))
            } else emptyList(),
            goal = GoalBlock(
                declared = snapshot.goals.isNotEmpty(),
                type = snapshot.goals.firstOrNull()?.kind,
                destination = snapshot.goals.firstOrNull()?.destination,
                amountTarget = snapshot.goals.firstOrNull()?.amountTarget,
                amountSaved = snapshot.goals.firstOrNull()?.amountSaved
            ),
            spiralRisk = SpiralBlock(score = spiralReport.score, severity = spiralReport.severity, factors = spiralReport.factors),
            nextActionSuggested = if (audit.ghostCount > 0) {
                NextActionSuggestion(
                    type = NextActionType.cancel_ghost_subscriptions,
                    rationale = "Anulând cele ${audit.ghostCount} abonamente fantomă recuperezi ${audit.monthlyRecoverable.amount} RON/lună",
                    monthlySaving = audit.monthlyRecoverable,
                    annualSaving = audit.annualRecoverable
                )
            } else NextActionSuggestion(
                type = NextActionType.no_action_needed,
                rationale = "Totul arată bine pentru momentul curent"
            )
        )
    }

    private fun buildMomentUser(snapshot: Snapshot): MomentUser {
        val p = snapshot.userProfile?.demographics
        return MomentUser(
            name = p?.name ?: "prietene",
            addressing = p?.addressing ?: Addressing.tu,
            ageRange = p?.ageRange
        )
    }

    private fun paydayDayOfMonth(snapshot: Snapshot): Int {
        val p = snapshot.userProfile ?: return 28
        return when (p.financials.salaryFrequency.type) {
            "monthly" -> p.financials.salaryFrequency.dayOfMonth ?: 28
            "bimonthly" -> p.financials.salaryFrequency.secondDay ?: 28
            else -> 28
        }
    }

    private fun daysUntilDayOfMonth(targetDay: Int, nowMillis: Long): Int {
        val cal = RomanianDateFormatter.gregorianROCalendar().apply { timeInMillis = nowMillis }
        val today = cal.get(Calendar.DAY_OF_MONTH)
        return if (targetDay > today) {
            targetDay - today
        } else {
            val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
            daysInMonth - today + targetDay
        }
    }

    private fun computeLastMonthAvailable(snapshot: Snapshot): Money {
        val cal = RomanianDateFormatter.gregorianROCalendar().apply {
            timeInMillis = snapshot.referenceDateEpochSeconds * 1000L
            add(Calendar.MONTH, -1)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val prevStart = cal.timeInMillis
        cal.add(Calendar.MONTH, 1)
        val prevEnd = cal.timeInMillis
        val inTxs = snapshot.transactions.filter { it.isIncoming && it.date in prevStart..prevEnd }
            .sumOf { it.amount.amount }
        val outTxs = snapshot.transactions.filter { it.isOutgoing && it.date in prevStart..prevEnd }
            .sumOf { it.amount.amount }
        return Money(maxOf(0, inTxs - outTxs))
    }

    private fun computeMonthlyBalanceHistory(snapshot: Snapshot, cashFlow: CashFlowAnalysis): List<Money> {
        val cal = RomanianDateFormatter.gregorianROCalendar()
        val byMonth = mutableMapOf<String, Int>()
        for (tx in snapshot.transactions) {
            cal.timeInMillis = tx.date
            val key = "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.MONTH)}"
            val signed = if (tx.isOutgoing) -tx.amount.amount else tx.amount.amount
            byMonth[key] = (byMonth[key] ?: 0) + signed
        }
        return byMonth.keys.sorted().map { Money(byMonth[it] ?: 0) }
    }

    private fun monthName(year: Int, month: Int): String {
        return RomanianDateFormatter.monthName(month).ifEmpty { "necunoscută" }
    }
}
