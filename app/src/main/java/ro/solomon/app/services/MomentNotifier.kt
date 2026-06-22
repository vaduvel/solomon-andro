package ro.solomon.app.services

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import ro.solomon.app.MainActivity
import ro.solomon.app.R
import ro.solomon.core.moments.MomentType

object MomentNotifier {

    private const val NOTIF_ID_BASE = 3000

    fun notifyMoment(ctx: Context, type: MomentType, body: String) {
        val (title, text) = when (type) {
            MomentType.payday -> "Salariul a intrat \uD83D\uDC9A" to "Solomon a preg\u0103tit alocarea automat\u0103. Deschide app-ul s\u0103 vezi."
            MomentType.upcomingObligation -> "Plat\u0103 obligatorie se apropie \u23F0" to "Ai o obliga\u021Bie care scade \u00EEn cur\u00E2nd. Deschide Solomon s\u0103 verifici."
            MomentType.patternAlert -> "Solomon a observat ceva" to "Cheltuielile tale arat\u0103 un pattern. Tap pentru detalii."
            MomentType.weeklySummary -> "Rezumatul t\u0103u s\u0103pt\u0103m\u00E2nal \uD83D\uDCCA" to "Cum a mers s\u0103pt\u0103m\u00E2na financiar? Deschide Solomon s\u0103 afli."
            MomentType.spiralAlert -> "Aten\u021Bie \u2014 alert\u0103 financiar\u0103 \uD83D\uDD34" to "Solomon a detectat presiune financiar\u0103. Deschide app-ul acum."
            MomentType.subscriptionAudit -> "Abonamente nefolosite g\u0103site \uD83D\uDCA8" to "Po\u021Bi recupera bani anul\u00E2nd abonamente fantom\u0103. Tap s\u0103 vezi."
            MomentType.canIAfford, MomentType.wowMoment -> return
        }

        ensureChannel(ctx, SolomonChannels.MOMENTS)

        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val intent = Intent(ctx, MainActivity::class.java)
        val pi = PendingIntent.getActivity(
            ctx, NOTIF_ID_BASE + type.ordinal, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val channel = if (type == MomentType.spiralAlert) SolomonChannels.ALERTS else SolomonChannels.MOMENTS
        val n = NotificationCompat.Builder(ctx, channel.id)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body.take(200)))
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()
        nm.notify(NOTIF_ID_BASE + type.ordinal, n)
    }
}
