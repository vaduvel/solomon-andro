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
import ro.solomon.core.domain.CancellationDifficulty
import ro.solomon.core.domain.Money
import ro.solomon.core.domain.Subscription
import java.util.UUID

data class SubscriptionEditState(
    val isNew: Boolean = true,
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val amountText: String = "0",
    val lastUsedDaysAgo: String = "0",
    val difficulty: CancellationDifficulty = CancellationDifficulty.medium,
    val cancellationUrl: String = "",
    val notes: String = "",
    val isSaved: Boolean = false,
    val isDeleted: Boolean = false,
    val error: String? = null
) {
    fun canSave(): Boolean = name.isNotBlank() && (amountText.toIntOrNull() ?: 0) > 0
}

class SubscriptionEditViewModel(
    private val subscriptionId: String? = null
) : ViewModel() {

    private val _local = MutableStateFlow(SubscriptionEditState(isNew = subscriptionId == null))

    val state: StateFlow<SubscriptionEditState> = combine(
        ServiceLocator.subRepo.observeAll(),
        _local
    ) { all, local ->
        if (local.isNew || local.name.isNotBlank() || local.amountText != "0") return@combine local
        val existing = all.firstOrNull { it.id == subscriptionId } ?: return@combine local
        local.copy(
            id = existing.id,
            name = existing.name,
            amountText = existing.amountMonthly.amount.toString(),
            lastUsedDaysAgo = (existing.lastUsedDaysAgo ?: 0).toString(),
            difficulty = existing.cancellationDifficulty,
            cancellationUrl = existing.cancellationUrl ?: "",
            notes = existing.cancellationStepsSummary ?: ""
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, _local.value)

    fun onName(v: String) { _local.value = _local.value.copy(name = v) }
    fun onAmount(v: String) { _local.value = _local.value.copy(amountText = v.filter { c -> c.isDigit() }) }
    fun onLastUsed(v: String) { _local.value = _local.value.copy(lastUsedDaysAgo = v.filter { c -> c.isDigit() }) }
    fun onDifficulty(v: CancellationDifficulty) { _local.value = _local.value.copy(difficulty = v) }
    fun onUrl(v: String) { _local.value = _local.value.copy(cancellationUrl = v) }
    fun onNotes(v: String) { _local.value = _local.value.copy(notes = v) }

    fun save() {
        val s = _local.value
        if (!s.canSave()) return
        val days = s.lastUsedDaysAgo.toIntOrNull() ?: 0
        val sub = Subscription(
            id = s.id,
            name = s.name.trim(),
            amountMonthly = Money(s.amountText.toInt()),
            lastUsedDaysAgo = if (days > 0) days else null,
            cancellationDifficulty = s.difficulty,
            cancellationUrl = s.cancellationUrl.takeIf { it.isNotBlank() },
            cancellationStepsSummary = s.notes.takeIf { it.isNotBlank() }
        )
        viewModelScope.launch {
            try {
                ServiceLocator.subRepo.save(sub)
                _local.value = s.copy(isSaved = true, error = null)
            } catch (t: Throwable) {
                _local.value = s.copy(error = t.message ?: "Eroare")
            }
        }
    }

    fun delete() {
        viewModelScope.launch {
            try {
                ServiceLocator.subRepo.delete(_local.value.id)
                _local.value = _local.value.copy(isDeleted = true)
            } catch (_: Throwable) { }
        }
    }
}
