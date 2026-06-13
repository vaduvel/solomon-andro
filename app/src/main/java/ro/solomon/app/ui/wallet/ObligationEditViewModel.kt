package ro.solomon.app.ui.wallet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
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
    val amountText: String = "0",
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

    private val _local = MutableStateFlow(ObligationEditState(isNew = obligationId == null))

    val state: StateFlow<ObligationEditState> = combine(
        ServiceLocator.obligationRepo.observeAll(),
        _local
    ) { all, local ->
        if (local.isNew || local.name.isNotBlank() || local.amountText != "0") return@combine local
        val existing = all.firstOrNull { it.id == obligationId } ?: return@combine local
        local.copy(
            id = existing.id,
            name = existing.name,
            amountText = existing.amount.amount.toString(),
            dayOfMonth = existing.dayOfMonth,
            kind = existing.kind
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, _local.value)

    fun onName(v: String) { _local.value = _local.value.copy(name = v) }
    fun onAmount(v: String) { _local.value = _local.value.copy(amountText = v.filter { c -> c.isDigit() }) }
    fun onDay(v: Int) { _local.value = _local.value.copy(dayOfMonth = v.coerceIn(1, 31)) }
    fun onKind(v: ObligationKind) { _local.value = _local.value.copy(kind = v) }

    fun save() {
        val s = _local.value
        if (!s.canSave()) return
        val o = Obligation(
            id = s.id,
            name = s.name.trim(),
            amount = Money(s.amountText.toInt()),
            dayOfMonth = s.dayOfMonth,
            kind = s.kind,
            confidence = ObligationConfidence.declared
        )
        viewModelScope.launch {
            try {
                ServiceLocator.obligationRepo.save(o)
                _local.value = s.copy(isSaved = true, error = null)
            } catch (t: Throwable) {
                _local.value = s.copy(error = t.message ?: "Eroare")
            }
        }
    }

    fun delete() {
        viewModelScope.launch {
            try {
                ServiceLocator.obligationRepo.delete(_local.value.id)
                _local.value = _local.value.copy(isDeleted = true)
            } catch (_: Throwable) { }
        }
    }
}
