package ro.solomon.app.services

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import ro.solomon.app.MainActivity
import ro.solomon.app.R
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlin.math.abs

/**
 * Real reminder scheduling backed by WorkManager.
 *
 * Replaces the previous stub that only returned a text string without
 * scheduling anything. A scheduled reminder is persisted by WorkManager,
 * survives process death and device reboot (the app already declares
 * RECEIVE_BOOT_COMPLETED) and fires a real notification on the existing
 * MOMENTS channel when its moment arrives.
 *
 * Two modes are supported, matching the schedule_reminder tool contract:
 *  - one-shot, N days from now (days_from_now)
 *  - recurring monthly on a given day-of-month (day_of_month); the worker
 *    re-enqueues itself for the next cycle after firing.
 */
object ReminderScheduler {

    const val KEY_TITLE = "reminder_title"
    const val KEY_BODY = "reminder_body"
    const val KEY_DAY_OF_MONTH = "reminder_day_of_month"

    private const val WORK_PREFIX = "solomon_reminder_"
    private const val DEFAULT_HOUR = 9
    private const val NOTIF_ID_BASE = 4000

    /**
     * Schedule a one-shot reminder [daysFromNow] days from now.
     * Returns a human-readable Romanian label describing when it will fire.
     */
    fun scheduleInDays(ctx: Context, title: String, body: String, daysFromNow: Int): String {
        val days = daysFromNow.coerceAtLeast(0)
        val delayMs = days * 24L * 60L * 60L * 1000L
        enqueue(ctx, title, body, dayOfMonth = -1, delayMs = delayMs)
        return when (days) {
            0 -> "astazi"
            1 -> "maine"
            else -> "peste $days zile"
        }
    }

    /**
     * Schedule a recurring monthly reminder on [dayOfMonth] (1..28, clamped).
     * The reminder fires at [DEFAULT_HOUR]:00 and re-enqueues for next month.
     */
    fun scheduleMonthly(ctx: Context, title: String, body: String, dayOfMonth: Int): String {
        val day = dayOfMonth.coerceIn(1, 28)
        val delayMs = millisUntilNextDayOfMonth(day, DEFAULT_HOUR)
        enqueue(ctx, title, body, dayOfMonth = day, delayMs = delayMs)
        return "in fiecare luna, ziua $day"
    }

    /** Cancel a previously scheduled reminder by its title. */
    fun cancel(ctx: Context, title: String) {
        WorkManager.getInstance(ctx).cancelUniqueWork(WORK_PREFIX + sanitize(title))
    }

    private fun enqueue(ctx: Context, title: String, body: String, dayOfMonth: Int, delayMs: Long) {
        val data = Data.Builder()
            .putString(KEY_TITLE, title)
            .putString(KEY_BODY, body)
            .putInt(KEY_DAY_OF_MONTH, dayOfMonth)
            .build()
        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .build()
        WorkManager.getInstance(ctx).enqueueUniqueWork(
            WORK_PREFIX + sanitize(title),
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    /** Milliseconds from now until the next [day]-of-month at [hour]:00. */
    fun millisUntilNextDayOfMonth(day: Int, hour: Int): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            set(Calendar.DAY_OF_MONTH, day.coerceIn(1, getActualMaximum(Calendar.DAY_OF_MONTH)))
        }
        if (!target.after(now)) {
            target.add(Calendar.MONTH, 1)
            target.set(
                Calendar.DAY_OF_MONTH,
                day.coerceIn(1, target.getActualMaximum(Calendar.DAY_OF_MONTH))
            )
        }
        return (target.timeInMillis - now.timeInMillis).coerceAtLeast(0L)
    }

    private fun sanitize(s: String): String =
        s.lowercase().filter { it.isLetterOrDigit() }.take(40).ifEmpty { "generic" }

    /** Post the actual notification. Reuses the existing MOMENTS channel. */
    internal fun fire(ctx: Context, title: String, body: String) {
        ensureChannel(ctx, SolomonChannels.MOMENTS)
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val id = NOTIF_ID_BASE + abs(title.hashCode() % 1000)
        val intent = Intent(ctx, MainActivity::class.java)
        val pi = PendingIntent.getActivity(
            ctx,
            id,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val text = body.ifBlank { title }
        val n = NotificationCompat.Builder(ctx, SolomonChannels.MOMENTS.id)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()
        nm.notify(id, n)
    }
}

/**
 * Worker that fires a scheduled reminder notification and, for recurring
 * monthly reminders, re-enqueues itself for the next cycle.
 */
class ReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val title = inputData.getString(ReminderScheduler.KEY_TITLE) ?: return Result.success()
        val body = inputData.getString(ReminderScheduler.KEY_BODY) ?: ""
        val dayOfMonth = inputData.getInt(ReminderScheduler.KEY_DAY_OF_MONTH, -1)

        ReminderScheduler.fire(applicationContext, title, body)

        // Recurring monthly reminders schedule their next occurrence.
        if (dayOfMonth in 1..31) {
            ReminderScheduler.scheduleMonthly(applicationContext, title, body, dayOfMonth)
        }
        return Result.success()
    }
}
