package ro.solomon.app.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import ro.solomon.app.di.ServiceLocator
import ro.solomon.core.notifications.BankNotificationParser
import ro.solomon.web.ScamPatternMatcher

class SolomonNotificationListener : NotificationListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val scamMatcher = ScamPatternMatcher()

    override fun onCreate() {
        super.onCreate()
        ensureChannel(this, SolomonChannels.MOMENTS)
        ensureChannel(this, SolomonChannels.INGESTION)
        ensureChannel(this, SolomonChannels.ALERTS)
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val s = sbn ?: return
        val pkg = s.packageName ?: return
        val text = s.notification?.extras?.getCharSequence(android.app.Notification.EXTRA_TEXT)?.toString()
            ?: s.notification?.extras?.getCharSequence(android.app.Notification.EXTRA_BIG_TEXT)?.toString()
            ?: return
        if (text.isBlank()) return
        val isKnownApp = pkg in BankNotificationParser.knownAppPackages
        if (!isKnownApp && !BankNotificationParser.looksLikeBankNotification(text)) return

        scope.launch {
            runCatching {
                val parsed = BankNotificationParser.parse(
                    raw = text,
                    dateEpochMillis = System.currentTimeMillis()
                )
                if (parsed != null) {
                    ServiceLocator.txnRepo.save(parsed)
                    IngestionNotifier.notifyBankNotification(this@SolomonNotificationListener, 1, text)
                    // Event-driven reactivity: react the instant a bank notification
                    // is parsed into a transaction, not just on the daily worker.
                    ReactiveMomentEvaluator.onTransactionIngested(this@SolomonNotificationListener, parsed)
                }
            }.onFailure { e ->
                IngestionNotifier.notifyError(this@SolomonNotificationListener, "notif", e.message ?: "Eroare necunoscută")
            }

            // Play-safe fraud check: runs ONLY on text that already passed the
            // bank-notification gate above (known bank app or looksLikeBankNotification).
            // Catches bank-impersonation smishing without reading non-bank notifications.
            runCatching {
                val scam = scamMatcher.match(text)
                if (scam != null && scam.shouldAlert) {
                    IngestionNotifier.notifyScamAlert(this@SolomonNotificationListener, text.take(80))
                }
            }
        }
    }
}

data class SolomonChannels(val id: String, val name: String, val importance: Int) {
    companion object {
        val MOMENTS = SolomonChannels("solomon.moments", "Solomon — Momente", NotificationManager.IMPORTANCE_DEFAULT)
        val INGESTION = SolomonChannels("solomon.ingestion", "Solomon — Importări", NotificationManager.IMPORTANCE_LOW)
        val ALERTS = SolomonChannels("solomon.alerts", "Solomon — Alerte", NotificationManager.IMPORTANCE_HIGH)
    }
}

fun ensureChannel(ctx: Context, ch: SolomonChannels) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val nm = ctx.getSystemService(NotificationManager::class.java) ?: return
        if (nm.getNotificationChannel(ch.id) == null) {
            val c = NotificationChannel(ch.id, ch.name, ch.importance)
            nm.createNotificationChannel(c)
        }
    }
}
