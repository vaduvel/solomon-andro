package ro.solomon.app.ui.caniafford

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ro.solomon.app.di.ServiceLocator
import ro.solomon.core.domain.Money
import ro.solomon.core.domain.TransactionCategory
import java.util.Calendar

class CanIAffordViewModel : ViewModel() {

    data class State(
        val amountText: String = "",
        val amount: Int = 0,
        val category: TransactionCategory = TransactionCategory.unknown,
        val note: String = "",
        val verdict: Verdict? = null
    )

    data class Verdict(val canAfford: Boolean, val title: String, val message: String)

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    fun setAmount(text: String) {
        val v = text.filter(Char::isDigit).toIntOrNull() ?: 0
        _state.value = _state.value.copy(amountText = text.filter(Char::isDigit), amount = v)
    }
    fun setCategory(c: TransactionCategory) { _state.value = _state.value.copy(category = c) }
    fun setNote(n: String) { _state.value = _state.value.copy(note = n) }

    fun evaluate() = viewModelScope.launch {
        val s = _state.value
        if (s.amount <= 0) return@launch

        val now = System.currentTimeMillis()
        val txns = ServiceLocator.txnRepo.fetchAll()
        val cash = ServiceLocator.cashFlow.analyze(transactions = txns, referenceDate = now)
        val profile = ServiceLocator.userRepo.fetchProfile()
        val obligations = ServiceLocator.obligationRepo.fetchAll()

        val cal = Calendar.getInstance().apply { timeInMillis = now }
        val today = cal.get(Calendar.DAY_OF_MONTH)
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val monthStart = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        // Real balance proxy: this month's income reference minus what was actually spent so far.
        val spentThisMonth = txns
            .filter { it.isOutgoing && it.date in monthStart..now }
            .sumOf { it.amount.amount }
        val currentBalance = Money(maxOf(0, cash.monthlyIncomeAvg.amount - spentThisMonth))

        // Obligations still due before month end (real, from the ledger).
        val obligationsRemaining = obligations
            .filter { it.dayOfMonth >= today }
            .fold(Money.zero) { acc, o -> acc + o.amount }

        // Days until the user's real next payday.
        val paydayDay = profile?.let { p ->
            when (p.financials.salaryFrequency.type) {
                "monthly" -> p.financials.salaryFrequency.dayOfMonth ?: 28
                "bimonthly" -> p.financials.salaryFrequency.secondDay ?: 28
                else -> 28
            }
        } ?: 28
        val daysUntilPayday = if (paydayDay > today) paydayDay - today else daysInMonth - today + paydayDay

        val velocity = Money(cash.monthlySpendingAvg.amount / 30)

        val budget = ServiceLocator.safeToSpend.calculate(
            currentBalance = currentBalance,
            obligationsRemaining = obligationsRemaining,
            daysUntilNextPayday = daysUntilPayday,
            velocityRONPerDay = velocity,
            monthlyIncomeReference = cash.monthlyIncomeAvg
        )

        val target = Money.fromLei(s.amount)
        val catRO = s.category.displayNameRO
        val verdict = when (val r = budget.verdictFor(target)) {
            is ro.solomon.analytics.Verdict.Yes -> Verdict(
                canAfford = true,
                title = "Da, îți permiți",
                message = "După ${s.amount} RON pe $catRO îți rămân ~${r.projectedPerDay.lei.toInt()} RON/zi până la salariu (în $daysUntilPayday zile). Ești pe verde."
            )
            is ro.solomon.analytics.Verdict.YesWithCaution -> Verdict(
                canAfford = true,
                title = "Da, dar cu atenție",
                message = "Îți permiți, dar te lasă strâmt: ~${r.projectedPerDay.lei.toInt()} RON/zi pentru următoarele $daysUntilPayday zile. Dacă apare ceva neprevăzut, devine incomod."
            )
            is ro.solomon.analytics.Verdict.No -> Verdict(
                canAfford = false,
                title = "Mai bine nu acum",
                message = "Cei ${s.amount} RON ar intra peste banii necesari obligațiilor rămase luna asta (${obligationsRemaining.lei.toInt()} RON). Riști să nu acoperi o rată sau o factură."
            )
        }
        _state.value = s.copy(verdict = verdict)
    }
}
