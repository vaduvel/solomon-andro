package ro.solomon.storage.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ro.solomon.core.domain.*
import ro.solomon.storage.UserConsent
import ro.solomon.storage.dao.UserProfileDao
import ro.solomon.storage.entity.UserProfileEntity

class UserProfileRepository(private val dao: UserProfileDao) {

    fun observeProfile(): Flow<UserProfile?> = dao.observe().map { it?.toProfile() }

    fun observeConsent(): Flow<UserConsent?> = dao.observe().map { it?.toConsent() }

    suspend fun saveProfile(profile: UserProfile) {
        val sf = profile.financials.salaryFrequency
        val entity = dao.fetch()?.copy(
            name = profile.demographics.name,
            addressingRaw = profile.demographics.addressing.name,
            ageRangeRaw = profile.demographics.ageRange.name,
            salaryRangeRaw = profile.financials.salaryRange.name,
            salaryFreqType = sf.type,
            salaryFreqDay1 = sf.dayOfMonth.coerceAtLeast(sf.firstDay),
            salaryFreqDay2 = sf.secondDay,
            hasSecondaryIncome = profile.financials.hasSecondaryIncome,
            secondaryIncomeRON = profile.financials.secondaryIncomeAvg?.amount?.toLong(),
            primaryBankRaw = profile.financials.primaryBank.name
        ) ?: UserProfileEntity(
            name = profile.demographics.name,
            addressingRaw = profile.demographics.addressing.name,
            ageRangeRaw = profile.demographics.ageRange.name,
            salaryRangeRaw = profile.financials.salaryRange.name,
            salaryFreqType = sf.type,
            salaryFreqDay1 = sf.dayOfMonth.coerceAtLeast(sf.firstDay),
            salaryFreqDay2 = sf.secondDay,
            hasSecondaryIncome = profile.financials.hasSecondaryIncome,
            secondaryIncomeRON = profile.financials.secondaryIncomeAvg?.amount?.toLong(),
            primaryBankRaw = profile.financials.primaryBank.name
        )
        dao.upsert(entity)
    }

    suspend fun saveConsent(consent: UserConsent) {
        val entity = dao.fetch()?.copy(
            emailAccessGranted = consent.emailAccessGranted,
            notificationsGranted = consent.notificationsGranted,
            datasetOptIn = consent.datasetOptIn,
            onboardingComplete = consent.onboardingComplete
        ) ?: UserProfileEntity(
            emailAccessGranted = consent.emailAccessGranted,
            notificationsGranted = consent.notificationsGranted,
            datasetOptIn = consent.datasetOptIn,
            onboardingComplete = consent.onboardingComplete
        )
        dao.upsert(entity)
    }

    suspend fun fetchProfile(): UserProfile? = dao.fetch()?.toProfile()

    suspend fun fetchConsent(): UserConsent? = dao.fetch()?.toConsent()

    suspend fun fetchCreatedAt(): Long? = dao.fetch()?.createdAt

    suspend fun deleteProfile() = dao.deleteAll()
}

private fun UserProfileEntity.toProfile(): UserProfile? {
    val addressing = try { Addressing.valueOf(addressingRaw) } catch (_: Exception) { return null }
    val ageRange = try { AgeRange.valueOf(ageRangeRaw) } catch (_: Exception) { return null }
    val salaryRange = try { SalaryRange.valueOf(salaryRangeRaw) } catch (_: Exception) { return null }
    val bank = try { Bank.valueOf(primaryBankRaw) } catch (_: Exception) { return null }

    val salaryFrequency = when (salaryFreqType) {
        "monthly" -> SalaryFrequency.monthly(salaryFreqDay1)
        "bimonthly" -> SalaryFrequency.bimonthly(salaryFreqDay1, salaryFreqDay2)
        else -> SalaryFrequency.variable()
    }

    return UserProfile(
        demographics = DemographicProfile(name = name, addressing = addressing, ageRange = ageRange),
        financials = FinancialProfile(
            salaryRange = salaryRange,
            salaryFrequency = salaryFrequency,
            hasSecondaryIncome = hasSecondaryIncome,
            secondaryIncomeAvg = secondaryIncomeRON?.let { Money(it.toInt()) },
            primaryBank = bank
        )
    )
}

private fun UserProfileEntity.toConsent(): UserConsent = UserConsent(
    emailAccessGranted = emailAccessGranted,
    notificationsGranted = notificationsGranted,
    datasetOptIn = datasetOptIn,
    onboardingComplete = onboardingComplete
)
