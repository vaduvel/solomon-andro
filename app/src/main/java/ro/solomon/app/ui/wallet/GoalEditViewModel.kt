package ro.solomon.app.ui.wallet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import ro.solomon.app.di.ServiceLocator
import ro.solomon.core.domain.Goal
import ro.solomon.core.domain.GoalKind
import ro.solomon.core.domain.Money
import java.util.UUID

private const val DAY_MILLIS = 86_400_000L

data class GoalEditState(
    val isNew: Boolean = true,
    val id: String = UUID.randomUUID().toString(),
    val kind: GoalKind = GoalKind.vacation,
    val destination: String = "",
    val amountTargetText: String = "",
    val amountSavedText: String = "",
    val deadlineEpochMillis: Long = System.currentTimeMillis() + 180L * DAY_MILLIS,
    val deadlineLabel: String = "6 luni",
    val isSaved: Boolean = false,
    val isDeleted: Boolean = false,
    val error: String? = null
) {
    fun canSave(): Boolean = (amountTargetText.toIntOrNull() ?: 0) > 0 &&
        deadlineEpochMillis > System.currentTimeMillis()
}

class GoalEditViewModel(
    private val goalId: String? = null
) : ViewModel() {

    private val _state = MutableStateFlow(GoalEditState(isNew = goalId == null))
    val state: StateFlow<GoalEditState> = _state.asStateFlow()

    init {
        if (goalId != null) {
            viewModelScope.launch {
                val existing = ServiceLocator.goalRepo.observeAll().first()
                    .firstOrNull { it.id == goalId }
                if (existing != null) {
                    _state.value = _state.value.copy(
                        isNew = false,
                        id = existing.id,
                        kind = existing.kind,
                        destination = existing.destination ?: "",
                        amountTargetText = (existing.amountTarget.amount / 100).toString(),
                        amountSavedText = (existing.amountSaved.amount / 100).toString(),
                        deadlineEpochMillis = existing.deadline,
                        deadlineLabel = humanize(existing.deadline)
                    )
                }
            }
        }
    }

    fun onKind(v: GoalKind) { _state.value = _state.value.copy(kind = v) }
    fun onDestination(v: String) { _state.value = _state.value.copy(destination = v) }
    fun onTarget(v: String) { _state.value = _state.value.copy(amountTargetText = v.filter { c -> c.isDigit() }) }
    fun onSaved(v: String) { _state.value = _state.value.copy(amountSavedText = v.filter { c -> c.isDigit() }) }
    fun onDeadlinePreset(months: Int, label: String) {
        val epoch = System.currentTimeMillis() + months * 30L * DAY_MILLIS
        _state.value = _state.value.copy(deadlineEpochMillis = epoch, deadlineLabel = label)
    }

    fun save() {
        val s = _state.value
        if (!s.canSave()) return
        val g = Goal(
            id = s.id,
            kind = s.kind,
            destination = s.destination.takeIf { it.isNotBlank() },
            amountTarget = Money.fromLei(s.amountTargetText.toInt()),
            amountSaved = Money.fromLei(s.amountSavedText.toIntOrNull() ?: 0),
            deadline = s.deadlineEpochMillis
        )
        viewModelScope.launch {
            try {
                ServiceLocator.goalRepo.save(g)
                _state.value = s.copy(isSaved = true, error = null)
            } catch (t: Throwable) {
                _state.value = s.copy(error = t.message ?: "Eroare")
            }
        }
    }

    fun delete() {
        viewModelScope.launch {
            try {
                ServiceLocator.goalRepo.delete(_state.value.id)
                _state.value = _state.value.copy(isDeleted = true)
            } catch (_: Throwable) { }
        }
    }

    private fun humanize(epochMillis: Long): String {
        val days = ((epochMillis - System.currentTimeMillis()) / DAY_MILLIS).toInt()
        val months = days / 30
        return when {
            months < 1 -> "$days zile"
            months == 1 -> "1 lună"
            else -> "$months luni"
        }
    }
}
