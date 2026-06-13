package ro.solomon.core.domain

import kotlinx.serialization.Serializable

@Serializable
enum class CancellationDifficulty {
    easy, medium, hard;

    val displayNameRO: String get() = when (this) {
        easy -> "ușor"
        medium -> "moderat"
        hard -> "dificil"
    }
}

@Serializable
enum class GhostConfidence {
    low, medium, high, very_high
}

@Serializable
data class Subscription(
    val id: String,
    val name: String,
    val amountMonthly: Money,
    val lastUsedDaysAgo: Int? = null,
    val cancellationDifficulty: CancellationDifficulty = CancellationDifficulty.medium,
    val cancellationUrl: String? = null,
    val cancellationStepsSummary: String? = null,
    val alternativeSuggestion: String? = null,
    val cancellationWarning: String? = null
) {
    val amountAnnual: Money get() = amountMonthly * 12

    val isGhost: Boolean get() {
        val days = lastUsedDaysAgo ?: return false
        return days > 30
    }

    val ghostConfidence: GhostConfidence get() {
        val days = lastUsedDaysAgo ?: return GhostConfidence.low
        return when {
            days < 31 -> GhostConfidence.low
            days < 60 -> GhostConfidence.medium
            days < 120 -> GhostConfidence.high
            else -> GhostConfidence.very_high
        }
    }
}
