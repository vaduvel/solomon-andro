package ro.solomon.core.format

import java.util.Calendar
import java.util.TimeZone

object RomanianDateFormatter {

    enum class Style { dayOfWeek, dayMonth, full, dayOfWeekDayMonth, iso }

    fun format(epochMillis: Long, style: Style): String {
        val cal = gregorianROCalendar()
        cal.timeInMillis = epochMillis
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val month = monthName(cal.get(Calendar.MONTH) + 1)
        val year = cal.get(Calendar.YEAR)
        val weekday = weekdayName(cal.get(Calendar.DAY_OF_WEEK))

        return when (style) {
            Style.dayOfWeek -> weekday
            Style.dayMonth -> "$day $month"
            Style.full -> "$day $month $year"
            Style.dayOfWeekDayMonth -> "$weekday, $day $month"
            Style.iso -> String.format("%04d-%02d-%02d", year, cal.get(Calendar.MONTH) + 1, day)
        }
    }

    fun dayOrdinal(day: Int): String = "data $day"

    fun dayOfWeekPhrase(weekday: Int): String = "ziua de ${weekdayName(weekday)}"

    fun monthName(month: Int): String = when (month) {
        1 -> "ianuarie"; 2 -> "februarie"; 3 -> "martie"; 4 -> "aprilie"
        5 -> "mai"; 6 -> "iunie"; 7 -> "iulie"; 8 -> "august"
        9 -> "septembrie"; 10 -> "octombrie"; 11 -> "noiembrie"; 12 -> "decembrie"
        else -> ""
    }

    fun weekdayName(weekday: Int): String = when (weekday) {
        1 -> "duminică"; 2 -> "luni"; 3 -> "marți"; 4 -> "miercuri"
        5 -> "joi"; 6 -> "vineri"; 7 -> "sâmbătă"
        else -> ""
    }

    fun gregorianROCalendar(): Calendar = Calendar.getInstance(TimeZone.getTimeZone("Europe/Bucharest")).apply {
        firstDayOfWeek = Calendar.MONDAY
        minimalDaysInFirstWeek = 4
    }
}
