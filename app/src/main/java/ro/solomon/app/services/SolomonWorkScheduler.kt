package ro.solomon.app.services

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object SolomonWorkScheduler {
    private const val DAILY_MOMENT = "solomon_daily_moment"
    private const val HOURLY_INGEST = "solomon_hourly_ingest"
    private const val FORECAST_WORK = "solomon_forecast"

    fun schedule(ctx: Context) {
        val wm = WorkManager.getInstance(ctx)

        val daily = PeriodicWorkRequestBuilder<DailyMomentWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(15, TimeUnit.MINUTES)
            .build()
        wm.enqueueUniquePeriodicWork(DAILY_MOMENT, ExistingPeriodicWorkPolicy.KEEP, daily)

        val ingest = PeriodicWorkRequestBuilder<HourlyIngestWorker>(6, TimeUnit.HOURS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.NOT_REQUIRED).build())
            .build()
        wm.enqueueUniquePeriodicWork(HOURLY_INGEST, ExistingPeriodicWorkPolicy.KEEP, ingest)

        val forecast = PeriodicWorkRequestBuilder<ForecastRefreshWorker>(12, TimeUnit.HOURS)
            .build()
        wm.enqueueUniquePeriodicWork(FORECAST_WORK, ExistingPeriodicWorkPolicy.KEEP, forecast)
    }
}
