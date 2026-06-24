package ro.solomon.app.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ro.solomon.app.di.ServiceLocator
import ro.solomon.app.services.MoneyScript
import ro.solomon.moments.MomentEngine
import ro.solomon.core.domain.Addressing
import ro.solomon.core.domain.AgeRange
import ro.solomon.core.domain.Bank
import ro.solomon.core.domain.DemographicProfile
import ro.solomon.core.domain.FinancialProfile
import ro.solomon.core.domain.Goal
import ro.solomon.core.domain.GoalKind
import ro.solomon.core.domain.Money
import ro.solomon.core.domain.Obligation
import ro.solomon.core.domain.ObligationKind
import ro.solomon.core.domain.SalaryFrequency
import ro.solomon.core.domain.SalaryRange
import ro.solomon.core.domain.UserProfile
import ro.solomon.core.enablebanking.BankConnectionService
import ro.solomon.storage.UserConsent
import java.util.UUID

class OnboardingViewModel : ViewModel() {

    enum class GoalChip(val label: String) {
        NoZero22("Să nu mai fiu pe zero pe 22"),
        SaveForVacation("Să strâng pentru vacanță"),
        ClearDebts("Să scap de datorii"),
        SaveMonthly("Să economisesc lunar"),
        UnderstandWhere("Să înțeleg unde se duc banii")
    }

    data class DraftObligation(
        val id: String = UUID.randomUUID().toString(),
        val name: String = "",
        val amountRON: Int = 0,
        val dayOfMonth: Int = 1,
        val kind: ObligationKind = ObligationKind.subscription
    )

    data class ProcessingTask(val id: String, val title: String, val state: TaskState = TaskState.Pending)
    enum class TaskState { Pending, Running, Done }

    data class State(
        val currentStep: Int = 0,
        val name: String = "",
        val addressing: Addressing = Addressing.tu,
        val ageRange: AgeRange = AgeRange.range25to35,
        val salaryRange: SalaryRange? = null,
        val paydayDay: Int = 28,
        val hasSecondaryIncome: Boolean = false,
        val secondaryIncomeApprox: Int = 0,
        val primaryBank: Bank? = null,
        val draftObligations: List<DraftObligation> = emptyList(),
        val selectedGoals: Set<GoalChip> = emptySet(),
        val moneyScript: MoneyScript? = null,
        val firstGoalKind: GoalKind = GoalKind.vacation,
        val firstGoalDestination: String = "",
        val firstGoalTargetText: String = "",
        val firstGoalDeadlineMillis: Long = System.currentTimeMillis() + 6L * 30 * 24 * 3600 * 1000,
        val gmailConnected: Boolean = false,
        val pushAllowed: Boolean = false,
        val trainingOptIn: Boolean = false,
        val processingTasks: List<ProcessingTask> = listOf(
            ProcessingTask("t1", "Sincronizez datele financiare..."),
            ProcessingTask("t2", "Identific tranzacții și abonamente..."),
            ProcessingTask("t3", "Caut pattern-uri..."),
            ProcessingTask("t4", "Pregătesc primul raport...")
        ),
        val isGeneratingWow: Boolean = false,
        val wowMomentText: String = "",
        val finished: Boolean = false
    ) {
        val totalSteps: Int get() = 9
        val isLastStep: Boolean get() = currentStep == 8
        val canGoBack: Boolean get() = currentStep > 0 && currentStep < 8
        val canProceed: Boolean get() = when (currentStep) {
            0 -> true
            1 -> name.trim().isNotEmpty()
            2 -> salaryRange != null
            3 -> primaryBank != null
            4 -> true
            5 -> selectedGoals.isNotEmpty()
            6 -> true
            7 -> processingTasks.all { it.state == TaskState.Done }
            8 -> true
            else -> false
        }
    }

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    fun next() = update { if (it.currentStep < 8) it.copy(currentStep = it.currentStep + 1) else it }
    fun back() = update { if (it.canGoBack) it.copy(currentStep = it.currentStep - 1) else it }
    fun jumpTo(step: Int) = update { if (step in 0..8) it.copy(currentStep = step) else it }

    fun setName(v: String) = update { it.copy(name = v) }
    fun setAddressing(v: Addressing) = update { it.copy(addressing = v) }
    fun setAgeRange(v: AgeRange) = update { it.copy(ageRange = v) }
    fun setSalaryRange(v: SalaryRange) = update { it.copy(salaryRange = v) }
    fun setPaydayDay(v: Int) = update { it.copy(paydayDay = v.coerceIn(1, 31)) }
    fun setHasSecondaryIncome(v: Boolean) = update { it.copy(hasSecondaryIncome = v) }
    fun setSecondaryIncomeApprox(v: Int) = update { it.copy(secondaryIncomeApprox = v.coerceAtLeast(0)) }
    fun setPrimaryBank(v: Bank) = update { it.copy(primaryBank = v) }

    fun addDraftObligation() = update {
        it.copy(draftObligations = it.draftObligations + DraftObligation())
    }
    fun removeDraftObligation(id: String) = update {
        it.copy(draftObligations = it.draftObligations.filterNot { o -> o.id == id })
    }
    fun updateDraftObligation(id: String, transform: (DraftObligation) -> DraftObligation) = update {
        it.copy(draftObligations = it.draftObligations.map { o -> if (o.id == id) transform(o) else o })
    }

    fun toggleGoal(g: GoalChip) = update {
        val s = it.selectedGoals.toMutableSet()
        if (!s.add(g)) s.remove(g)
        it.copy(selectedGoals = s)
    }
    fun setMoneyScript(v: MoneyScript) = update { it.copy(moneyScript = v) }
    fun setFirstGoalKind(v: GoalKind) = update { it.copy(firstGoalKind = v) }
    fun setFirstGoalDestination(v: String) = update { it.copy(firstGoalDestination = v) }
    fun setFirstGoalTargetText(v: String) = update { it.copy(firstGoalTargetText = v.filter(Char::isDigit)) }
    fun setFirstGoalDeadlineMillis(v: Long) = update { it.copy(firstGoalDeadlineMillis = v) }

    fun setGmailConnected(v: Boolean) = update { it.copy(gmailConnected = v) }
    fun setPushAllowed(v: Boolean) = update { it.copy(pushAllowed = v) }
    fun setTrainingOptIn(v: Boolean) = update { it.copy(trainingOptIn = v) }

    private fun setTaskState(index: Int, taskState: TaskState) = update { st ->
        st.copy(processingTasks = st.processingTasks.mapIndexed { i, t -> if (i == index) t.copy(state = taskState) else t })
    }

    fun runProcessing() = viewModelScope.launch {
        val now = System.currentTimeMillis()

        // t1 — real ingestion: pull from connected bank(s). No-op (0) if none connected.
        setTaskState(0, TaskState.Running)
        runCatching { BankConnectionService.syncAll() }
        val txns = ServiceLocator.txnRepo.fetchAll()
        setTaskState(0, TaskState.Done)

        // t2 — real subscription audit over stored subscriptions.
        setTaskState(1, TaskState.Running)
        runCatching { ServiceLocator.subscriptionAuditor.audit(subscriptions = ServiceLocator.subRepo.fetchAll()) }
        setTaskState(1, TaskState.Done)

        // t3 — real pattern detection over the ledger.
        setTaskState(2, TaskState.Running)
        runCatching { ServiceLocator.patternDetector.detect(transactions = txns, referenceDate = now) }
        setTaskState(2, TaskState.Done)

        // t4 — real cash-flow analysis to prime the first report.
        setTaskState(3, TaskState.Running)
        runCatching { ServiceLocator.cashFlow.analyze(transactions = txns, referenceDate = now) }
        setTaskState(3, TaskState.Done)
    }

    fun generateWowMoment() = viewModelScope.launch {
        update { it.copy(isGeneratingWow = true, wowMomentText = "") }
        try {
            val snap = MomentEngine.Snapshot(
                userProfile = buildProfile(),
                transactions = ServiceLocator.txnRepo.fetchAll(),
                obligations = ServiceLocator.obligationRepo.fetchAll(),
                subscriptions = ServiceLocator.subRepo.fetchAll(),
                goals = buildGoals(),
                referenceDateEpochSeconds = System.currentTimeMillis() / 1000L
            )
            val out = ServiceLocator.momentEngine.generateWowMoment(snap)
            update { it.copy(isGeneratingWow = false, wowMomentText = out.llmResponse) }
        } catch (e: Throwable) {
            update { it.copy(isGeneratingWow = false, wowMomentText = "Întâmpin o problemă tehnică, dar contul tău e configurat. Deschide Azi pentru a vedea primul moment.") }
        }
    }

    fun finish() = viewModelScope.launch {
        try {
            ServiceLocator.onboardingPersistence.persistOnboardingFinal(
                profile = buildProfile(),
                consent = UserConsent(
                    emailAccessGranted = _state.value.gmailConnected,
                    notificationsGranted = _state.value.pushAllowed,
                    datasetOptIn = _state.value.trainingOptIn,
                    onboardingComplete = true
                ),
                obligations = buildObligations(),
                goals = buildGoals()
            )
            update { it.copy(finished = true) }
        } catch (_: Throwable) {
            update { it.copy(finished = true) }
        }
    }

    private fun buildProfile(): UserProfile {
        val s = _state.value
        return UserProfile(
            demographics = DemographicProfile(name = s.name.trim(), addressing = s.addressing, ageRange = s.ageRange),
            financials = FinancialProfile(
                salaryRange = s.salaryRange ?: SalaryRange.range3to5,
                salaryFrequency = SalaryFrequency.monthly(dayOfMonth = s.paydayDay),
                hasSecondaryIncome = s.hasSecondaryIncome,
                secondaryIncomeAvg = if (s.hasSecondaryIncome && s.secondaryIncomeApprox > 0) Money.fromLei(s.secondaryIncomeApprox) else null,
                primaryBank = s.primaryBank ?: Bank.Other
            )
        )
    }

    private fun buildObligations(): List<Obligation> = _state.value.draftObligations
        .filter { it.name.isNotBlank() && it.amountRON > 0 }
        .map { d ->
            Obligation(
                id = UUID.randomUUID().toString(),
                name = d.name.trim(),
                amount = Money.fromLei(d.amountRON),
                dayOfMonth = d.dayOfMonth,
                kind = d.kind,
                confidence = ro.solomon.core.domain.ObligationConfidence.declared
            )
        }

    private fun buildGoals(): List<Goal> {
        val s = _state.value
        val target = s.firstGoalTargetText.toIntOrNull() ?: 0
        if (target <= 0) return emptyList()
        return listOf(
            Goal(
                id = UUID.randomUUID().toString(),
                kind = s.firstGoalKind,
                destination = s.firstGoalDestination.ifBlank { null },
                amountTarget = Money.fromLei(target),
                amountSaved = Money.zero,
                deadline = s.firstGoalDeadlineMillis
            )
        )
    }

    private inline fun update(transform: (State) -> State) {
        _state.value = transform(_state.value)
    }
}
