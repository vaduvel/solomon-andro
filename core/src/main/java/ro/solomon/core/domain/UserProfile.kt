package ro.solomon.core.domain

import kotlinx.serialization.Serializable

@Serializable
enum class AgeRange {
    under25, range25to35, range35to45, over45;

    val displayNameRO: String get() = when (this) {
        under25 -> "sub 25 ani"
        range25to35 -> "25–35 ani"
        range35to45 -> "35–45 ani"
        over45 -> "peste 45 ani"
    }
}

@Serializable
enum class SalaryRange {
    under3k, range3to5, range5to8, range8to15, over15k;

    val midpointRON: Int get() = when (this) {
        under3k -> 2_500
        range3to5 -> 4_000
        range5to8 -> 6_500
        range8to15 -> 11_500
        over15k -> 18_000
    }
}

@Serializable
data class SalaryFrequency(
    val type: String = "variable",
    val dayOfMonth: Int = 0,
    val firstDay: Int = 0,
    val secondDay: Int = 0
) {
    val isPredictable: Boolean get() = type != "variable"

    companion object {
        fun monthly(dayOfMonth: Int) = SalaryFrequency(type = "monthly", dayOfMonth = dayOfMonth)
        fun bimonthly(firstDay: Int, secondDay: Int) = SalaryFrequency(type = "bimonthly", firstDay = firstDay, secondDay = secondDay)
        fun variable() = SalaryFrequency(type = "variable")
    }
}

@Serializable
enum class Bank {
    BT, BCR, ING, Raiffeisen, BRD, Revolut, CEC, UniCredit,
    Patria, ProCredit, Libra, Garanti, FirstBank, Alpha, OTP, Idea, Other;

    val displayNameRO: String get() = when (this) {
        BT -> "Banca Transilvania"
        BCR -> "BCR"
        ING -> "ING Bank"
        Raiffeisen -> "Raiffeisen Bank"
        BRD -> "BRD"
        Revolut -> "Revolut"
        CEC -> "CEC Bank"
        UniCredit -> "UniCredit Bank"
        Patria -> "Patria Bank"
        ProCredit -> "ProCredit Bank"
        Libra -> "Libra Internet Bank"
        Garanti -> "Garanti BBVA"
        FirstBank -> "First Bank"
        Alpha -> "Alpha Bank"
        OTP -> "OTP Bank"
        Idea -> "Idea Bank"
        Other -> "Altă bancă"
    }
}

@Serializable
data class DemographicProfile(
    val name: String,
    val addressing: Addressing,
    val ageRange: AgeRange
)

@Serializable
data class FinancialProfile(
    val salaryRange: SalaryRange,
    val salaryFrequency: SalaryFrequency,
    val hasSecondaryIncome: Boolean,
    val secondaryIncomeAvg: Money? = null,
    val primaryBank: Bank
)

@Serializable
enum class FinancialPersonality {
    spender, saver, avoider, monk;

    val displayNameRO: String get() = when (this) {
        spender -> "Cheltuitor"
        saver -> "Econom"
        avoider -> "Evitant"
        monk -> "Călugăr"
    }

    val descriptionRO: String get() = when (this) {
        spender -> "Trăiește pentru azi, cheltuie ușor, e generos."
        saver -> "Pune deoparte instinctiv, planifică, uneori prea precaut."
        avoider -> "Evită subiectul banilor, simte anxietate când îl deschide."
        monk -> "Indiferent față de bani, nu se uită în cont, lasă lucrurile să curgă."
    }

    val emoji: String get() = when (this) {
        spender -> "\uD83D\uDCB8"
        saver -> "\uD83C\uDFE6"
        avoider -> "\uD83D\uDE48"
        monk -> "\uD83E\uDDD8"
    }
}

@Serializable
data class Partner(
    val name: String,
    val addressing: Addressing,
    val financialPersonality: FinancialPersonality,
    val monthlyIncomeRON: Int? = null,
    val sharedThresholdRON: Int = 500,
    val meetingDayOfMonth: Int = 5
)

@Serializable
data class UserProfile(
    val demographics: DemographicProfile,
    val financials: FinancialProfile,
    val financialPersonality: FinancialPersonality? = null,
    val partner: Partner? = null
) {
    val isCouplesMode: Boolean get() = partner != null
}
