package dev.tomerklein.holocron.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dev.tomerklein.holocron.service.ListeningService
import dev.tomerklein.holocron.util.Logx

/**
 * Re-arms listening after a reboot. The manifest-registered [SmsReceiver] already survives
 * reboot on its own; this re-starts the optional foreground [ListeningService] when the user
 * had it enabled, so always-on delivery resumes.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            -> {
                Logx.i(TAG, "Boot completed — re-arming listening")
                // TODO: read the user's foreground-service preference and start it if enabled.
                // ListeningService.start(context)
            }
        }
    }

    private companion object {
        const val TAG = "BootReceiver"
    }
}
