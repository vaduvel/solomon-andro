package ro.solomon.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ro.solomon.app.di.ServiceLocator
import ro.solomon.app.services.SolomonMission
import ro.solomon.core.domain.Obligation
import ro.solomon.core.domain.Subscription
import ro.solomon.core.domain.Transaction
import ro.solomon.core.domain.TransactionCategory
import ro.solomon.moments.MomentEngine

class TodayViewModel : ViewModel() {

    data class UpcomingBillItem(
        val name: String,
        val amount: Int,
        val dayOfMonth: Int,
        val daysRemaining: Int,
        val isEssential: Boolean,
        val kindLabel: String
    )

    data class State(
        val userName: String = "",
        val balanceAvailable: Int = 0,
        val safeToSpendPerDay: Int = 0,
        val daysUntilPayday: Int = 30,
        val paydayDayOfMonth: Int = 28,
        val incomingToday: Int = 0,
        val outgoingToday: Int = 0,
        val recentTransactions: List<Transaction> = emptyList(),
        val momentText: String = "Salut! Apas\u0103 aici ca s\u0103-\u021bi generez primul moment financiar personalizat.",
        val generatingMoment: Boolean = false,
        val hasUnreadAlert: Boolean = false,
        val activeMission: SolomonMission? = null,
        val pendingMission: SolomonMission? = null,
        val lastCommitment: String? = null,
        val topCategory: TransactionCategory? = null,
        val topCategoryAmount: Int = 0,
        val avgDailySpending30d: Int = 0,
        val upcomingBills: List<UpcomingBillItem> = emptyList(),
        val upcomingBillsTotal: Int = 0,
        val upcomingBillsCount: Int = 0,
        val financialSafetyDays: Int = 0,
        val safetyDaysTier: SafetyTier = SafetyTier.DANGER,
        val zeroBalanceSentence: String = ""
    )

    enum class SafetyTier(val label: String) {
        DANGER("Zon\u0103 de risc"),
        CAUTION("Te descurci"),
        STABLE("Stabil"),
        FREE("Liber de salariu \u2B50");

        companion object {
            fun from(days: Int): SafetyTier = when {
                days < 7 -> DANGER
                days < 15 -> CAUTION
                days < 30 -> STABLE
                else -> FREE
            }
        }
    }

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            ServiceLocator.missionEngine.loadAndRefresh(System.currentTimeMillis() / 1000L)
            _state.value = _state.value.copy(
                activeMission = ServiceLocator.missionEngine.active.value,
                pendingMission = ServiceLocator.missionEngine.pending.value
            )
        }
        refreshCommitment()
        ServiceLocator.missionEngine.active.let { /* no-op */ }
        viewModelScope.launch {
            ServiceLocator.missionEngine.active.collect { m ->
                _state.value = _state.value.copy(activeMission = m)
            }
        }
        viewModelScope.launch {
            ServiceLocator.missionEngine.pending.collect { m ->
                _state.value = _state.value.copy(pendingMission = m)
            }
        }
        ServiceLocator.txnRepo.observeAll()
            .combine(ServiceLocator.userRepo.observeProfile()) { txns, profile -> txns to profile }
            .combine(ServiceLocator.obligationRepo.observeAll()) { (txns, profile), obligs -> Triple(txns, profile, obligs) }
            .combine(ServiceLocator.subRepo.observeAll()) { (txns, profile, obligs), subs ->
                val nowMillis = System.currentTimeMillis()
                val dayStart = nowMillis - (nowMillis % 86_400_000L)
                val dayEnd = dayStart + 86_400_000L
                val todayTxns = txns.filter { it.date in dayStart until dayEnd }
                val incoming = todayTxns.filter { it.isIncoming }.sumOf { it.amount.amount }
                val outgoing = todayTxns.filter { it.isOutgoing }.sumOf { it.amount.amount }

                val cash = ServiceLocator.cashFlow.analyze(transactions = txns, referenceDate = System.currentTimeMillis())
                val obligations = obligs
                val monthlyIncome = cash.monthlyIncomeAvg.amount
                val monthlySpending = cash.monthlySpendingAvg.amount
                val obligTotal = obligations.fold(0) { acc, o -> acc + o.amount.amount }
                val balance = monthlyIncome - monthlySpending - obligTotal / 4
                val payday = profile?.financials?.salaryFrequency?.dayOfMonth ?: 28
                val days = daysUntilDayOfMonth(payday, System.currentTimeMillis()).coerceAtLeast(1)
                val safePerDay = (balance / days).coerceAtLeast(0)

                val monthStart = dayStart - 30L * 86_400_000L
                val monthlyByCat = txns
                    .filter { it.isOutgoing && it.date >= monthStart }
                    .groupBy { it.category }
                    .mapValues { (_, list) -> list.sumOf { it.amount.amount } }

                val limits = ro.solomon.app.services.CategoryLimitsStore.limits()
                val hasOverLimit = limits.any { (cat, limit) ->
                    val used = ro.solomon.app.services.CategoryLimitsStore.usedFor(cat, monthlyByCat[cat] ?: 0)
                    ro.solomon.app.services.CategoryLimitsStore.isOverLimit(used)
                }

                val topCategoryEntry = monthlyByCat.maxByOrNull { it.value }
                val topCategory = topCategoryEntry?.key
                val topCategoryAmount = topCategoryEntry?.value ?: 0
                val avgDailySpending30d = (monthlyByCat.values.sum() / 30).coerceAtLeast(0)

                val upcomingBills = computeUpcomingBills(obligations, subs, payday)
                val upcomingBillsTotal = upcomingBills.sumOf { it.amount }
                val upcomingBillsCount = upcomingBills.size

                val financialSafetyDays = if (safePerDay > 0) balance / safePerDay else days
                val safetyDaysTier = SafetyTier.from(financialSafetyDays)
                val zeroBalanceSentence = if (safePerDay <= 0) {
                    "Rezerva disponibil\u0103 e aproape de zero."
                } else if (financialSafetyDays < days) {
                    val cal = java.util.Calendar.getInstance()
                    cal.add(java.util.Calendar.DAY_OF_YEAR, financialSafetyDays)
                    val fmt = java.text.SimpleDateFormat("d MMMM", java.util.Locale("ro", "RO"))
                    "La ritmul curent, rezerva ajunge la 0 pe ${fmt.format(cal.time)}."
                } else {
                    "Rezerva acoper\u0103 p\u00E2n\u0103 la salariul urm\u0103tor. \uD83D\uDC4D"
                }

                _state.value.copy(
                    userName = profile?.demographics?.name ?: "",
                    balanceAvailable = balance.coerceAtLeast(0),
                    safeToSpendPerDay = safePerDay,
                    daysUntilPayday = days,
                    paydayDayOfMonth = payday,
                    incomingToday = incoming,
                    outgoingToday = outgoing,
                    recentTransactions = txns.sortedByDescending { it.date }.take(15),
                    hasUnreadAlert = _state.value.hasUnreadAlert || hasOverLimit,
                    topCategory = topCategory,
                    topCategoryAmount = topCategoryAmount,
                    avgDailySpending30d = avgDailySpending30d,
                    upcomingBills = upcomingBills,
                    upcomingBillsTotal = upcomingBillsTotal,
                    upcomingBillsCount = upcomingBillsCount,
                    financialSafetyDays = financialSafetyDays,
                    safetyDaysTier = safetyDaysTier,
                    zeroBalanceSentence = zeroBalanceSentence
                )
            }
            .onEach { _state.value = it }
            .launchIn(viewModelScope)
    }

    private fun computeUpcomingBills(
        obligations: List<Obligation>,
        subscriptions: List<Subscription>,
        paydayDay: Int
    ): List<UpcomingBillItem> {
        val cal = java.util.Calendar.getInstance()
        val today = cal.get(java.util.Calendar.DAY_OF_MONTH)
        val daysInMonth = cal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)

        val billItems = mutableListOf<UpcomingBillItem>()

        fun daysUntil(target: Int): Int {
            return if (target > today) target - today else (daysInMonth - today + target)
        }

        for (o in obligations) {
            val remaining = daysUntil(o.dayOfMonth)
            if (remaining <= 7) {
                billItems.add(UpcomingBillItem(
                    name = o.name,
                    amount = o.amount.amount,
                    dayOfMonth = o.dayOfMonth,
                    daysRemaining = remaining,
                    isEssential = o.isEssential,
                    kindLabel = o.kind.displayNameRO
                ))
            }
        }

        val activeSubs = subscriptions.filter { !it.isGhost }
        for (s in activeSubs) {
            billItems.add(UpcomingBillItem(
                name = s.name,
                amount = s.amountMonthly.amount,
                dayOfMonth = paydayDay,
                daysRemaining = daysUntil(paydayDay),
                isEssential = false,
                kindLabel = "Abonament"
            ))
        }

        return billItems.sortedBy { it.daysRemaining }
    }

    fun offerMissionIfReady() = viewModelScope.launch {
        if (ServiceLocator.missionEngine.pending.value != null) return@launch
        if (ServiceLocator.missionEngine.active.value != null) return@launch
        val txns = ServiceLocator.txnRepo.fetchAll()
        if (txns.isEmpty()) return@launch
        val last30Start = System.currentTimeMillis() - 30L * 86_400_000L
        val recent = txns.filter { it.date >= last30Start && it.isOutgoing }
        if (recent.isEmpty()) return@launch
        val grouped = recent.groupBy { it.category }
        val topEntry = grouped.maxByOrNull { (_, list) -> list.sumOf { it.amount.amount } }
        val topCat = topEntry?.key
        val topAmount = topEntry?.value?.sumOf { it.amount.amount } ?: 0
        val goals = ServiceLocator.goalRepo.fetchAll()
        val unreachedGoal = goals.firstOrNull { !it.isReached }
        val linked = unreachedGoal?.destination
        val savedRecently = recent.any { it.category == TransactionCategory.savings }
        val hasGoalWithoutContribution = unreachedGoal != null && !savedRecently
        val obligations = ServiceLocator.obligationRepo.fetchAll()
        val monthlyDebt = obligations.sumOf { it.amount.amount }
        val vulnerability = withContext(Dispatchers.IO) {
            ro.solomon.app.services.SolomonCoachMemory.vulnerability(ServiceLocator.appContext)
        }
        val m = ServiceLocator.missionEngine.generateAny(
            nowEpoch = System.currentTimeMillis() / 1000L,
            topCategory = topCat,
            topCategoryAmountRON = topAmount,
            linkedGoalName = linked,
            monthlyDebtPaymentRON = monthlyDebt,
            hasGoalWithoutContribution = hasGoalWithoutContribution,
            vulnerability = vulnerability
        ) ?: return@launch
        ServiceLocator.missionEngine.offer(m)
    }

    fun acceptMission() = viewModelScope.launch {
        ServiceLocator.missionEngine.accept(ServiceLocator.appContext)
        refreshCommitment()
    }

    fun dismissMission() = viewModelScope.launch {
        ServiceLocator.missionEngine.dismiss(ServiceLocator.appContext)
    }

    fun completeMission() = viewModelScope.launch {
        ServiceLocator.missionEngine.complete(ServiceLocator.appContext)
        refreshCommitment()
    }

    private fun refreshCommitment() = viewModelScope.launch {
        val commitment = withContext(Dispatchers.IO) {
            ro.solomon.app.services.CoachProfileStore.load(ServiceLocator.appContext).lastCommitment
        }
        _state.value = _state.value.copy(lastCommitment = commitment)
    }

    fun generateMoment() = viewModelScope.launch {
        _state.value = _state.value.copy(generatingMoment = true)
        try {
            val profile = ServiceLocator.userRepo.fetchProfile()
            val txns = ServiceLocator.txnRepo.fetchAll()
            val obligs = ServiceLocator.obligationRepo.fetchAll()
            val subs = ServiceLocator.subRepo.fetchAll()
            val goals = ServiceLocator.goalRepo.fetchAll()
            val out = ServiceLocator.momentEngine.generateWowMoment(
                MomentEngine.Snapshot(
                    userProfile = profile,
                    transactions = txns,
                    obligations = obligs,
                    subscriptions = subs,
                    goals = goals,
                    referenceDateEpochSeconds = System.currentTimeMillis() / 1000L
                )
            )
            val script = withContext(Dispatchers.IO) {
                ro.solomon.app.services.CoachProfileStore.load(ServiceLocator.appContext).moneyScript
            } ?: ro.solomon.app.services.MoneyScriptInference.infer(txns)
            val cleaned = ro.solomon.core.util.AdvisorTextCleaner.clean(out.llmResponse)
            val toned = cleaned + "\n\n" + ro.solomon.app.services.CoachingVoice.closingNudge(script)
            _state.value = _state.value.copy(momentText = toned, generatingMoment = false)
            ro.solomon.app.services.TodayContextBridge.detectedToday =
                ro.solomon.app.services.TodayContextBridge.DetectedMoment(
                    typeRaw = "wow_moment",
                    title = "Moment wow",
                    detectedAt = System.currentTimeMillis()
                )
        } catch (e: Throwable) {
            _state.value = _state.value.copy(
                momentText = "\u00CEnc\u0103 nu am destule date. Adaug\u0103 c\u00E2teva tranzac\u021bii \u0219i revin-o \u00een c\u00E2teva minute.",
                generatingMoment = false
            )
        }
    }

    private fun daysUntilDayOfMonth(targetDay: Int, nowMillis: Long): Int {
        return ro.solomon.core.util.CalendarSafeDate.daysUntilDayOfMonthClamped(targetDay, nowMillis)
    }
}
