package ro.solomon.storage

data class UserConsent(
    val emailAccessGranted: Boolean = false,
    val notificationsGranted: Boolean = false,
    val datasetOptIn: Boolean = false,
    val onboardingComplete: Boolean = false
)
