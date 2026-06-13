package ro.solomon.storage.repository

import ro.solomon.core.domain.Goal
import ro.solomon.core.domain.Obligation
import ro.solomon.core.domain.UserProfile
import ro.solomon.storage.UserConsent

class OnboardingPersistence(
    private val userProfileRepo: UserProfileRepository,
    private val obligationRepo: ObligationRepository,
    private val goalRepo: GoalRepository
) {
    suspend fun persistOnboardingFinal(
        profile: UserProfile,
        consent: UserConsent,
        obligations: List<Obligation>,
        goals: List<Goal> = emptyList()
    ) {
        userProfileRepo.saveProfile(profile)
        userProfileRepo.saveConsent(consent)
        if (obligations.isNotEmpty()) obligationRepo.saveAll(obligations)
        if (goals.isNotEmpty()) goalRepo.saveAll(goals)
    }
}
