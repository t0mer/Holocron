package dev.tomerklein.holocron.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.TelephonyManager
import dagger.hilt.android.EntryPointAccessors
import dev.tomerklein.holocron.dispatch.DispatchEnqueuer
import dev.tomerklein.holocron.dispatch.IncomingMessage
import dev.tomerklein.holocron.di.SmsReceiverEntryPoint
import dev.tomerklein.holocron.rules.NumberMatcher
import dev.tomerklein.holocron.util.Logx
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Receives `SMS_RECEIVED`, reassembles multipart messages, matches enabled rules by sender,
 * and enqueues a WorkManager dispatch per match. Does **no** network I/O here: it uses
 * [goAsync] for the brief DB read + enqueue, then returns.
 */
class SmsReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val parts = Telephony.Sms.Intents.getMessagesFromIntent(intent)?.toList().orEmpty()
        if (parts.isEmpty()) return

        val messages = SmsReassembler.reassemble(parts)
        if (messages.isEmpty()) return

        val region = deviceRegion(context)
        val ep = EntryPointAccessors.fromApplication(
            context.applicationContext,
            SmsReceiverEntryPoint::class.java,
        )
        val repository = ep.repository()
        val matcher: NumberMatcher = ep.matcher()
        val enqueuer: DispatchEnqueuer = ep.enqueuer()

        val pending = goAsync()
        scope.launch {
            try {
                val rules = repository.enabledRules()
                for (msg in messages) {
                    Logx.body(TAG, "incoming from ${msg.sender}", msg.body)
                    val incoming = IncomingMessage(
                        sender = msg.sender,
                        body = msg.body,
                        timestamp = msg.timestampMillis,
                        defaultRegion = region,
                    )
                    val matched = rules.filter { matcher.matches(it, msg.sender, region) }
                    for (rule in matched) {
                        enqueuer.enqueue(incoming, rule)
                    }
                }
            } catch (t: Throwable) {
                Logx.e(TAG, "Failed handling incoming SMS", t)
            } finally {
                pending.finish()
            }
        }
    }

    private fun deviceRegion(context: Context): String? {
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        return (tm?.simCountryIso?.takeIf { it.isNotBlank() }
            ?: tm?.networkCountryIso?.takeIf { it.isNotBlank() })
            ?.uppercase()
    }

    private companion object {
        const val TAG = "SmsReceiver"
    }
}
