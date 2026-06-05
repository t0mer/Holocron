package dev.tomerklein.holocron.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.EntryPointAccessors
import dev.tomerklein.holocron.di.SmsReceiverEntryPoint
import dev.tomerklein.holocron.service.ListeningService
import dev.tomerklein.holocron.util.Logx
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Re-arms listening after a reboot. The manifest-registered [SmsReceiver] already survives
 * reboot on its own; this re-starts the optional foreground [ListeningService] when the user
 * had it enabled, so always-on delivery resumes.
 */
class BootReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            -> {
                val appContext = context.applicationContext
                val settingsStore = EntryPointAccessors
                    .fromApplication(appContext, SmsReceiverEntryPoint::class.java)
                    .settingsStore()
                val pending = goAsync()
                scope.launch {
                    try {
                        if (settingsStore.settings.first().foregroundServiceEnabled) {
                            Logx.i(TAG, "Boot completed — restarting listening service")
                            ListeningService.start(appContext)
                        }
                    } catch (t: Throwable) {
                        Logx.e(TAG, "Failed to re-arm listening on boot", t)
                    } finally {
                        pending.finish()
                    }
                }
            }
        }
    }

    private companion object {
        const val TAG = "BootReceiver"
    }
}
