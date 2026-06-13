package ro.solomon.app

import android.content.Context
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import ro.solomon.app.di.ServiceLocator

object ServiceLocatorInitializer {
    fun onboardingComplete(ctx: Context): Boolean = runBlocking {
        if (!ServiceLocatorOnboardInit.initialized) {
            ServiceLocator.init(ctx.applicationContext)
            ServiceLocatorOnboardInit.initialized = true
        }
        val c = ServiceLocator.userRepo.observeConsent().first()
        c?.onboardingComplete == true
    }
}

object ServiceLocatorOnboardInit {
    @Volatile var initialized: Boolean = false
}
