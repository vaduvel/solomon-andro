package ro.solomon.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ro.solomon.app.di.ServiceLocator
import ro.solomon.app.ui.SolomonApp
import ro.solomon.app.ui.onboarding.OnboardingScreen
import ro.solomon.core.domain.TransactionSource
import ro.solomon.core.notifications.BankNotificationParser

class MainActivity : ComponentActivity() {

    private val requestNotificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op */ }

    private var pendingSharedText: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        askForNotificationPermission()
        handleShareIntent(intent)
        setContent {
            var showOnboarding by remember { mutableStateOf<Boolean?>(null) }
            LaunchedEffect(Unit) {
                val done = withContext(Dispatchers.IO) {
                    ServiceLocatorInitializer.onboardingComplete(this@MainActivity)
                }
                showOnboarding = !done
            }
            val flag = showOnboarding
            when (flag) {
                null -> { /* splash: nothing yet */ }
                true -> OnboardingScreen(onFinished = { showOnboarding = false })
                false -> SolomonApp()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleShareIntent(intent)
    }

    private fun handleShareIntent(intent: Intent?) {
        if (intent?.action != Intent.ACTION_SEND) return
        val type = intent.type ?: return
        if (!type.startsWith("text/")) return
        val text = intent.getStringExtra(Intent.EXTRA_TEXT) ?: return
        if (text.isBlank()) return
        pendingSharedText = text
        lifecycleScope.launch {
            runCatching {
                val parsed = BankNotificationParser.parse(
                    raw = text,
                    dateEpochSeconds = System.currentTimeMillis() / 1000L
                )
                if (parsed != null) {
                    val withShareSource = parsed.copy(source = TransactionSource.share_intent_parsed)
                    ServiceLocator.txnRepo.save(withShareSource)
                    ro.solomon.app.services.IngestionNotifier.notifyShareIntent(this@MainActivity, 1)
                } else {
                    ro.solomon.app.services.IngestionNotifier.notifyError(
                        this@MainActivity,
                        "share",
                        "N-am putut extrage o tranzacție din text."
                    )
                }
            }.onFailure { e ->
                ro.solomon.app.services.IngestionNotifier.notifyError(this@MainActivity, "share", e.message ?: "Eroare necunoscută")
            }
        }
    }

    private fun askForNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        if (!granted) requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
