package ro.solomon.app.services

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import ro.solomon.app.MainActivity
import ro.solomon.app.R

object IngestionNotifier {

    fun notifyBankNotification(ctx: Context, count: Int, original: String) {
        ensureChannels(ctx)
        IngestionEventBus.publish(IngestionEvent.BankNotificationIngested(count, original))
        show(ctx, NOTIF_ID_BANK, "Solomon a importat $count ${if (count == 1) "tranzacție" else "tranzacții"}", original.take(80))
    }

    fun notifyShareIntent(ctx: Context, count: Int) {
        ensureChannels(ctx)
        IngestionEventBus.publish(IngestionEvent.ShareIntentIngested(count))
        show(ctx, NOTIF_ID_SHARE, "Solomon a importat $count ${if (count == 1) "tranzacție partajată" else "tranzacții partajate"}", "Deschide Solomon pentru detalii")
    }

    fun notifyError(ctx: Context, source: String, message: String) {
        ensureChannels(ctx)
        IngestionEventBus.publish(IngestionEvent.ErrorOccurred(source, message))
        show(ctx, NOTIF_ID_ERROR, "Eroare import $source", message)
    }

    /**
     * High-importance fraud warning, raised when a scam pattern is detected in an
     * already-permitted bank-notification text. Uses the ALERTS channel so it is
     * visible immediately. Wording stays informational (no "consilier financiar").
     */
    fun notifyScamAlert(ctx: Context, snippet: String) {
        ensureChannels(ctx)
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val intent = Intent(ctx, MainActivity::class.java)
        val pi = PendingIntent.getActivity(ctx, NOTIF_ID_SCAM, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val text = "Mesaj cu semne de fraudă: \"$snippet\". Nu accesa linkuri și nu trimite date. Verifică direct cu banca ta."
        val n = NotificationCompat.Builder(ctx, SolomonChannels.ALERTS.id)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("⚠️ Atenție — posibilă tentativă de fraudă")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()
        nm.notify(NOTIF_ID_SCAM, n)
    }

    private fun show(ctx: Context, id: Int, title: String, text: String) {
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val intent = Intent(ctx, MainActivity::class.java)
        val pi = PendingIntent.getActivity(ctx, id, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val n = NotificationCompat.Builder(ctx, SolomonChannels.INGESTION.id)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()
        nm.notify(id, n)
    }

    private fun ensureChannels(ctx: Context) {
        ensureChannel(ctx, SolomonChannels.MOMENTS)
        ensureChannel(ctx, SolomonChannels.INGESTION)
        ensureChannel(ctx, SolomonChannels.ALERTS)
    }

    private const val NOTIF_ID_BANK = 2001
    private const val NOTIF_ID_SHARE = 2003
    private const val NOTIF_ID_ERROR = 2004
    private const val NOTIF_ID_SCAM = 2005
}
