package ro.solomon.core.util

import java.util.Calendar
import kotlin.math.max
import kotlin.math.min

object CalendarSafeDate {

    fun safeDayOfMonth(dayOfMonth: Int, referenceMillis: Long): Int {
        val cal = Calendar.getInstance().apply { timeInMillis = referenceMillis }
        val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        return min(max(dayOfMonth, 1), maxDay)
    }

    fun safeDateMillis(dayOfMonth: Int, referenceMillis: Long): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = referenceMillis }
        val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val clamped = min(max(dayOfMonth, 1), maxDay)
        cal.set(Calendar.DAY_OF_MONTH, clamped)
        return cal.timeInMillis
    }

    fun daysUntilDayOfMonthClamped(targetDay: Int, nowMillis: Long): Int {
        val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val today = cal.get(Calendar.DAY_OF_MONTH)
        val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val clampedTarget = min(max(targetDay, 1), maxDay)
        return if (clampedTarget > today) {
            clampedTarget - today
        } else if (clampedTarget == today) {
            0
        } else {
            maxDay - today + clampedTarget
        }
    }
}
