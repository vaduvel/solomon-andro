package ro.solomon.app.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import ro.solomon.app.di.ServiceLocator
import ro.solomon.email.SmsPaymentParser

class SmsPaymentReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        if (messages.isEmpty()) return

        val pendingResult = goAsync()
        scope.launch {
            try {
                var imported = 0
                for (msg in messages) {
                    val sender = msg.displayOriginatingAddress ?: msg.originatingAddress ?: continue
                    val body = msg.displayMessageBody ?: msg.messageBody ?: continue
                    if (!SmsPaymentParser.isBankSms(sender)) continue
                    val parsed = SmsPaymentParser.parse(sender, body) ?: continue
                    ServiceLocator.txnRepo.save(parsed.toTransaction())
                    imported++
                }
                if (imported > 0) {
                    IngestionNotifier.notifyBankNotification(context, imported, "Import SMS intern/debug")
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
