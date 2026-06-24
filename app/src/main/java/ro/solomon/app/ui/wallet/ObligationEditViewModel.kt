package ro.solomon.app.ui.wallet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import ro.solomon.app.di.ServiceLocator
import ro.solomon.core.domain.Money
import ro.solomon.core.domain.Obligation
import ro.solomon.core.domain.ObligationConfidence
import ro.solomon.core.domain.ObligationKind
import java.util.UUID

data class ObligationEditState(
    val isNew: Boolean = true,
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val amountText: String = "",
    val dayOfMonth: Int = 1,
    val kind: ObligationKind = ObligationKind.utility,
    val isSaved: Boolean = false,
    val isDeleted: Boolean = false,
    val error: String? = null
) {
    fun canSave(): Boolean = name.isNotBlank() &&
        (amountText.toIntOrNull() ?: 0) > 0 &&
        dayOfMonth in 1..31
}

class ObligationEditViewModel(
    private val obligationId: String? = null
) : ViewModel() {

    private val _state = MutableStateFlow(ObligationEditState(isNew = obligationId == null))
    val state: StateFlow<ObligationEditState> = _state.asStateFlow()

    init {
        if (obligationId != null) {
            viewModelScope.launch {
                val existing = ServiceLocator.obligationRepo.observeAll().first()
                    .firstOrNull { it.id == obligationId }
                if (existing != null) {
                    _state.value = _state.value.copy(
                        isNew = false,
                        id = existing.id,
                        name = existing.name,
                        amountText = (existing.amount.amount / 100).toString(),
                        dayOfMonth = existing.dayOfMonth,
                        kind = existing.kind
                    )
                }
            }
        }
    }

    fun onName(v: String) { _state.value = _state.value.copy(name = v) }
    fun onAmount(v: String) { _state.value = _state.value.copy(amountText = v.filter { c -> c.isDigit() }) }
    fun onDay(v: Int) { _state.value = _state.value.copy(dayOfMonth = v.coerceIn(1, 31)) }
    fun onKind(v: ObligationKind) { _state.value = _state.value.copy(kind = v) }

    fun save() {
        val s = _state.value
        if (!s.canSave()) return
        val o = Obligation(
            id = s.id,
            name = s.name.trim(),
            amount = Money.fromLei(s.amountText.toInt()),
            dayOfMonth = s.dayOfMonth,
            kind = s.kind,
            confidence = ObligationConfidence.declared
        )
        viewModelScope.launch {
            try {
                ServiceLocator.obligationRepo.save(o)
                _state.value = s.copy(isSaved = true, error = null)
            } catch (t: Throwable) {
                _state.value = s.copy(error = t.message ?: "Eroare")
            }
        }
    }

    fun delete() {
        viewModelScope.launch {
            try {
                ServiceLocator.obligationRepo.delete(_state.value.id)
                _state.value = _state.value.copy(isDeleted = true)
            } catch (_: Throwable) { }
        }
    }
}
