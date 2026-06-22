package ro.solomon.app.services

import java.util.Calendar

object TodayContextBridge {

    data class DetectedMoment(
        val typeRaw: String,
        val title: String,
        val detectedAt: Long
    ) {
        val humanReadable: String
            get() = when (typeRaw) {
                "spiral_alert" -> "un semn de spiral\u0103 financiar"
                "pattern_alert" -> "un pattern neobi\u0219nuit \u00EEn cheltuieli"
                "subscription_audit" -> "abonamente nefolosite care pot fi anulate"
                "upcoming_obligation" -> "o obliga\u021Bie financiar\u0103 scadent\u0103 \u00EEn cur\u00E2nd"
                "payday" -> "intrarea salariului"
                "weekly_summary" -> "un rezumat al cheltuielilor s\u0103pt\u0103m\u00E2nale"
                "wow_moment" -> "o analiz\u0103 complet\u0103 a situa\u021Biei financiare"
                "can_i_afford" -> "o \u00EEntrebare despre o achizi\u021Bie posibil\u0103"
                else -> title
            }

        val isFromToday: Boolean
            get() {
                val now = Calendar.getInstance()
                val det = Calendar.getInstance().apply { timeInMillis = detectedAt }
                return now.get(Calendar.YEAR) == det.get(Calendar.YEAR) &&
                    now.get(Calendar.DAY_OF_YEAR) == det.get(Calendar.DAY_OF_YEAR)
            }
    }

    @Volatile
    var detectedToday: DetectedMoment? = null
}
