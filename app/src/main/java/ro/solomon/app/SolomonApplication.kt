package ro.solomon.app

import android.app.Application
import androidx.core.content.getSystemService
import ro.solomon.app.di.ServiceLocator
import ro.solomon.app.services.SolomonChannels
import ro.solomon.app.services.ensureChannel

class SolomonApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        ServiceLocator.init(this)
        createNotificationChannels()
        scheduleBackground()
    }

    private fun createNotificationChannels() {
        ensureChannel(this, SolomonChannels.MOMENTS)
        ensureChannel(this, SolomonChannels.INGESTION)
        ensureChannel(this, SolomonChannels.ALERTS)
    }

    private fun scheduleBackground() {
        ro.solomon.app.services.SolomonWorkScheduler.schedule(this)
    }
}
