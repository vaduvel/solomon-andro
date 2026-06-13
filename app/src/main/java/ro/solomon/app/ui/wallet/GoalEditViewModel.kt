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
import ro.solomon.core.domain.Goal
import ro.solomon.core.domain.GoalKind
import ro.solomon.core.domain.Money
import java.util.UUID

data class GoalEditState(
    val isNew: Boolean = true,
    val id: String = UUID.randomUUID().toString(),
    val kind: GoalKind = GoalKind.vacation,
    val destination: String = "",
    val amountTargetText: String = "0",
    val amountSavedText: String = "0",
    val deadlineEpoch: Long = (System.currentTimeMillis() / 1000) + 180L * 86400L,
    val deadlineLabel: String = "6 luni",
    val isSaved: Boolean = false,
    val isDeleted: Boolean = false,
    val error: String? = null
) {
    fun canSave(): Boolean = (amountTargetText.toIntOrNull() ?: 0) > 0 && deadlineEpoch > System.currentTimeMillis() / 1000
}

class GoalEditViewModel(
    private val goalId: String? = null
) : ViewModel() {

    private val _local = MutableStateFlow(GoalEditState(isNew = goalId == null))

    val state: StateFlow<GoalEditState> = combine(
        ServiceLocator.goalRepo.observeAll(),
        _local
    ) { all, local ->
        if (local.isNew || local.amountTargetText != "0" || local.destination.isNotBlank()) return@combine local
        val existing = all.firstOrNull { it.id == goalId } ?: return@combine local
        local.copy(
            id = existing.id,
            kind = existing.kind,
            destination = existing.destination ?: "",
            amountTargetText = existing.amountTarget.amount.toString(),
            amountSavedText = existing.amountSaved.amount.toString(),
            deadlineEpoch = existing.deadline,
            deadlineLabel = humanize(existing.deadline)
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, _local.value)

    fun onKind(v: GoalKind) { _local.value = _local.value.copy(kind = v) }
    fun onDestination(v: String) { _local.value = _local.value.copy(destination = v) }
    fun onTarget(v: String) { _local.value = _local.value.copy(amountTargetText = v.filter { c -> c.isDigit() }) }
    fun onSaved(v: String) { _local.value = _local.value.copy(amountSavedText = v.filter { c -> c.isDigit() }) }
    fun onDeadlinePreset(months: Int, label: String) {
        val epoch = (System.currentTimeMillis() / 1000) + months * 30L * 86400L
        _local.value = _local.value.copy(deadlineEpoch = epoch, deadlineLabel = label)
    }

    fun save() {
        val s = _local.value
        if (!s.canSave()) return
        val g = Goal(
            id = s.id,
            kind = s.kind,
            destination = s.destination.takeIf { it.isNotBlank() },
            amountTarget = Money(s.amountTargetText.toInt()),
            amountSaved = Money(s.amountSavedText.toIntOrNull() ?: 0),
            deadline = s.deadlineEpoch
        )
        viewModelScope.launch {
            try {
                ServiceLocator.goalRepo.save(g)
                _local.value = s.copy(isSaved = true, error = null)
            } catch (t: Throwable) {
                _local.value = s.copy(error = t.message ?: "Eroare")
            }
        }
    }

    fun delete() {
        viewModelScope.launch {
            try {
                ServiceLocator.goalRepo.delete(_local.value.id)
                _local.value = _local.value.copy(isDeleted = true)
            } catch (_: Throwable) { }
        }
    }

    private fun humanize(epoch: Long): String {
        val days = ((epoch - System.currentTimeMillis() / 1000) / 86400L).toInt()
        val months = days / 30
        return when {
            months < 1 -> "$days zile"
            months == 1 -> "1 lună"
            else -> "$months luni"
        }
    }
}
