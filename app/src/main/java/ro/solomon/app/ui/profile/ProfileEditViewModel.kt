package ro.solomon.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ro.solomon.app.di.ServiceLocator
import ro.solomon.core.domain.Addressing
import ro.solomon.core.domain.AgeRange
import ro.solomon.core.domain.Bank
import ro.solomon.core.domain.DemographicProfile
import ro.solomon.core.domain.FinancialProfile
import ro.solomon.core.domain.FinancialPersonality
import ro.solomon.core.domain.Money
import ro.solomon.core.domain.SalaryFrequency
import ro.solomon.core.domain.SalaryRange
import ro.solomon.core.domain.UserProfile

data class ProfileEditState(
    val profile: UserProfile? = null,
    val name: String = "",
    val addressing: Addressing = Addressing.tu,
    val ageRange: AgeRange = AgeRange.range25to35,
    val salaryRange: SalaryRange = SalaryRange.range3to5,
    val salaryType: String = "monthly",
    val paydayDay: Int = 5,
    val hasSecondary: Boolean = false,
    val secondaryAvg: String = "0",
    val bank: Bank = Bank.Revolut,
    val personality: FinancialPersonality? = null,
    val isSaved: Boolean = false,
    val error: String? = null
) {
    fun canSave(): Boolean = name.isNotBlank() && paydayDay in 1..31
}

class ProfileEditViewModel : ViewModel() {

    private val _local = MutableStateFlow(ProfileEditState())

    val state: StateFlow<ProfileEditState> = combine(
        ServiceLocator.userRepo.observeProfile(),
        _local
    ) { profile, local ->
        if (local.profile == null && profile != null) {
            local.copy(
                profile = profile,
                name = profile.demographics.name,
                addressing = profile.demographics.addressing,
                ageRange = profile.demographics.ageRange,
                salaryRange = profile.financials.salaryRange,
                salaryType = profile.financials.salaryFrequency.type,
                paydayDay = profile.financials.salaryFrequency.dayOfMonth,
                hasSecondary = profile.financials.hasSecondaryIncome,
                secondaryAvg = profile.financials.secondaryIncomeAvg?.amount?.toString() ?: "0",
                bank = profile.financials.primaryBank,
                personality = profile.financialPersonality
            )
        } else local
    }.stateIn(viewModelScope, SharingStarted.Eagerly, ProfileEditState())

    fun onName(v: String) { _local.value = _local.value.copy(name = v) }
    fun onAddressing(v: Addressing) { _local.value = _local.value.copy(addressing = v) }
    fun onAgeRange(v: AgeRange) { _local.value = _local.value.copy(ageRange = v) }
    fun onSalaryRange(v: SalaryRange) { _local.value = _local.value.copy(salaryRange = v) }
    fun onSalaryType(v: String) { _local.value = _local.value.copy(salaryType = v) }
    fun onPaydayDay(v: Int) { _local.value = _local.value.copy(paydayDay = v.coerceIn(1, 31)) }
    fun onHasSecondary(v: Boolean) { _local.value = _local.value.copy(hasSecondary = v) }
    fun onSecondaryAvg(v: String) { _local.value = _local.value.copy(secondaryAvg = v.filter { c -> c.isDigit() }) }
    fun onBank(v: Bank) { _local.value = _local.value.copy(bank = v) }
    fun onPersonality(v: FinancialPersonality?) { _local.value = _local.value.copy(personality = v) }

    fun save() {
        val s = _local.value
        val profile = s.profile ?: return
        val demo = DemographicProfile(name = s.name.trim(), addressing = s.addressing, ageRange = s.ageRange)
        val salary = when (s.salaryType) {
            "monthly" -> SalaryFrequency.monthly(s.paydayDay)
            else -> SalaryFrequency.variable()
        }
        val fin = FinancialProfile(
            salaryRange = s.salaryRange,
            salaryFrequency = salary,
            hasSecondaryIncome = s.hasSecondary,
            secondaryIncomeAvg = if (s.hasSecondary) Money(s.secondaryAvg.toIntOrNull() ?: 0) else null,
            primaryBank = s.bank
        )
        val updated = profile.copy(
            demographics = demo,
            financials = fin,
            financialPersonality = s.personality
        )
        viewModelScope.launch {
            try {
                ServiceLocator.userRepo.saveProfile(updated)
                _local.value = _local.value.copy(isSaved = true, error = null)
            } catch (t: Throwable) {
                _local.value = _local.value.copy(error = t.message ?: "Eroare")
            }
        }
    }
}
