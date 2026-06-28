package ro.solomon.app.ui.focus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import ro.solomon.analytics.FocusOverview
import ro.solomon.app.di.ServiceLocator
import ro.solomon.core.domain.Focus
import ro.solomon.core.domain.FocusType
import ro.solomon.core.domain.Money
import ro.solomon.core.domain.SolBucket
import ro.solomon.core.domain.TransactionCategory
import ro.solomon.core.util.CalendarSafeDate
import java.util.UUID

class FocusViewModel : ViewModel() {

    data class State(
        val loading: Boolean = true,
        val overview: FocusOverview? = null,
        val focuses: List<Focus> = emptyList(),
        val daysUntilPayday: Int = 1
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    init {
        combine(
            ServiceLocator.txnRepo.observeAll(),
            ServiceLocator.userRepo.observeProfile(),
            ServiceLocator.goalRepo.observeAll(),
            ServiceLocator.focusRepo.observeAll(),
            ServiceLocator.bucketOverrideRepo.observeOverrides()
        ) { txns, profile, goals, focuses, overrides ->
            val now = System.currentTimeMillis()
            val cash = ServiceLocator.cashFlow.analyze(transactions = txns, referenceDate = now)
            val payday = profile?.financials?.salaryFrequency?.dayOfMonth ?: 28
            val days = CalendarSafeDate.daysUntilDayOfMonthClamped(payday, now).coerceAtLeast(1)
            val overview = ServiceLocator.focusEngine.calculate(
                cashFlow = cash,
                focuses = focuses,
                goals = goals,
                daysUntilNextPayday = days,
                referenceDate = now,
                bucketOverrides = overrides
            )
            State(
                loading = false,
                overview = overview,
                focuses = focuses,
                daysUntilPayday = days
            )
        }.onEach { next ->
            _state.value = next
        }.launchIn(viewModelScope)
    }

    fun quickAdd(type: FocusType, detoxPercent: Int? = null, makePrimary: Boolean = false) = viewModelScope.launch {
        val now = System.currentTimeMillis()
        val shouldBePrimary = makePrimary || _state.value.focuses.none { it.isActive && it.isPrimary }
        val focus = Focus(
            id = UUID.randomUUID().toString(),
            type = type,
            title = defaultTitle(type),
            targetAmount = Money.zero,
            savedAmount = Money.zero,
            deadline = null,
            detoxPercent = detoxPercent,
            plannedMonthlyContribution = Money.zero,
            isPrimary = shouldBePrimary,
            isActive = true,
            createdAt = now
        )
        ServiceLocator.focusRepo.save(focus)
        if (shouldBePrimary) {
            ServiceLocator.focusRepo.setPrimary(focus.id)
        }
    }

    fun save(focus: Focus) = viewModelScope.launch {
        ServiceLocator.focusRepo.save(focus)
    }

    fun makePrimary(id: String) = viewModelScope.launch {
        ServiceLocator.focusRepo.setPrimary(id)
    }

    fun remove(id: String) = viewModelScope.launch {
        ServiceLocator.focusRepo.delete(id)
    }

    fun setBucketOverride(category: TransactionCategory, bucket: SolBucket) = viewModelScope.launch {
        ServiceLocator.bucketOverrideRepo.setOverride(category, bucket)
    }

    private fun defaultTitle(type: FocusType): String = when (type) {
        FocusType.runway -> "Rămâi pe plus până la salariu"
        FocusType.moft_detox -> "Detox de mofturi"
        FocusType.emergency_fund -> "Fond de urgență"
        FocusType.early_repayment -> "Rată anticipată"
        FocusType.event -> "Eveniment"
    }
}
