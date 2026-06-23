package ro.solomon.app.ui.wallet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
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
    val amountText: String = "",
    val lastUsedDaysAgo: String = "",
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

    private val _state = MutableStateFlow(SubscriptionEditState(isNew = subscriptionId == null))
    val state: StateFlow<SubscriptionEditState> = _state.asStateFlow()

    init {
        if (subscriptionId != null) {
            viewModelScope.launch {
                val existing = ServiceLocator.subRepo.observeAll().first()
                    .firstOrNull { it.id == subscriptionId }
                if (existing != null) {
                    _state.value = _state.value.copy(
                        isNew = false,
                        id = existing.id,
                        name = existing.name,
                        amountText = (existing.amountMonthly.amount / 100).toString(),
                        lastUsedDaysAgo = existing.lastUsedDaysAgo?.toString() ?: "",
                        difficulty = existing.cancellationDifficulty,
                        cancellationUrl = existing.cancellationUrl ?: "",
                        notes = existing.cancellationStepsSummary ?: ""
                    )
                }
            }
        }
    }

    fun onName(v: String) { _state.value = _state.value.copy(name = v) }
    fun onAmount(v: String) { _state.value = _state.value.copy(amountText = v.filter { c -> c.isDigit() }) }
    fun onLastUsed(v: String) { _state.value = _state.value.copy(lastUsedDaysAgo = v.filter { c -> c.isDigit() }) }
    fun onDifficulty(v: CancellationDifficulty) { _state.value = _state.value.copy(difficulty = v) }
    fun onUrl(v: String) { _state.value = _state.value.copy(cancellationUrl = v) }
    fun onNotes(v: String) { _state.value = _state.value.copy(notes = v) }

    fun save() {
        val s = _state.value
        if (!s.canSave()) return
        val days = s.lastUsedDaysAgo.toIntOrNull() ?: 0
        val sub = Subscription(
            id = s.id,
            name = s.name.trim(),
            amountMonthly = Money.fromLei(s.amountText.toInt()),
            lastUsedDaysAgo = if (days > 0) days else null,
            cancellationDifficulty = s.difficulty,
            cancellationUrl = s.cancellationUrl.takeIf { it.isNotBlank() },
            cancellationStepsSummary = s.notes.takeIf { it.isNotBlank() }
        )
        viewModelScope.launch {
            try {
                ServiceLocator.subRepo.save(sub)
                _state.value = s.copy(isSaved = true, error = null)
            } catch (t: Throwable) {
                _state.value = s.copy(error = t.message ?: "Eroare")
            }
        }
    }

    fun delete() {
        viewModelScope.launch {
            try {
                ServiceLocator.subRepo.delete(_state.value.id)
                _state.value = _state.value.copy(isDeleted = true)
            } catch (_: Throwable) { }
        }
    }
}
