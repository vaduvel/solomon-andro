package ro.solomon.app.ui.caniafford

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ro.solomon.app.di.ServiceLocator
import ro.solomon.core.domain.TransactionCategory

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
        val txns = ServiceLocator.txnRepo.fetchAll()
        val cash = ServiceLocator.cashFlow.analyze(transactions = txns, referenceDate = System.currentTimeMillis())
        val dailySafe = (cash.monthlyIncomeAvg.amount - cash.monthlySpendingAvg.amount).coerceAtLeast(0) / 30
        val affordable = s.amount <= dailySafe * 7
        val title = if (affordable) "Da, încape în buget" else "Mai bine nu acum"
        val msg = if (affordable) {
            "Cu ${s.amount} RON pe ${s.category.displayNameRO}, rămâi cu ${(dailySafe * 7 - s.amount).coerceAtLeast(0)} RON din bugetul pe 7 zile."
        } else {
            "Asta e cam mult. Mai sigur amâni sau redu suma la ${(dailySafe * 7).coerceAtLeast(0)} RON."
        }
        _state.value = s.copy(verdict = Verdict(affordable, title, msg))
    }
}
