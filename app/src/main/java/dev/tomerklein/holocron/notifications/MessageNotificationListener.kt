package dev.tomerklein.holocron.notifications

import android.app.Notification
import android.net.Uri
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import dagger.hilt.android.AndroidEntryPoint
import dev.tomerklein.holocron.ingest.IncomingMessageRouter
import dev.tomerklein.holocron.util.Logx
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Captures incoming RCS (and SMS) messages from messaging apps by reading their notifications,
 * since RCS content is never exposed through the SMS broadcast. Requires the user to grant
 * Notification access; gated behind the `rcsForwardingEnabled` setting in the router.
 *
 * Limitations (inherent to this approach): the "sender" is the messaging app's notification
 * title — a contact's **display name** for saved contacts, or the number for unknown senders —
 * so RCS rules generally need CONTAINS/REGEX matching rather than EXACT number matching. Long
 * messages may be truncated to the notification preview, and messages can be missed if the user
 * has silenced or cleared the notification before it posts.
 */
@AndroidEntryPoint
class MessageNotificationListener : NotificationListenerService() {

    @Inject lateinit var router: IncomingMessageRouter

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName !in MESSAGING_PACKAGES) return
        val notification = sbn.notification ?: return
        // Skip group summaries and ongoing/transient notifications (e.g. "sending…").
        if (notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return
        if (notification.flags and Notification.FLAG_ONGOING_EVENT != 0) return

        val parsed = extract(notification) ?: return
        val (sender, body) = parsed
        if (sender.isBlank() || body.isBlank()) return

        scope.launch {
            runCatching {
                router.route(
                    sender = sender,
                    body = body,
                    timestamp = System.currentTimeMillis(),
                    region = null,
                    source = IncomingMessageRouter.Source.RCS,
                )
            }.onFailure { Logx.e(TAG, "Failed routing RCS notification", it) }
        }
    }

    /** Prefer MessagingStyle (reliable sender + latest message); fall back to title/text. */
    private fun extract(notification: Notification): Pair<String, String>? {
        val style = NotificationCompat.MessagingStyle
            .extractMessagingStyleFromNotification(notification)
        if (style != null) {
            val last = style.messages.lastOrNull() ?: return null
            val sender = senderFrom(last.person)
                ?: style.conversationTitle?.toString()
                ?: notification.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
                ?: return null
            val body = last.text?.toString() ?: return null
            return sender to body
        }

        // Fallback: only treat as a message when the app categorized it as one.
        if (notification.category != null && notification.category != Notification.CATEGORY_MESSAGE) {
            return null
        }
        val title = notification.extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: return null
        val text = notification.extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: return null
        return title to text
    }

    /**
     * Resolve the sender. Prefer the [Person]'s `tel:` URI (when the messaging app attaches it)
     * so RCS resolves to the actual phone number and matches the same number-based rules as SMS;
     * otherwise fall back to the display name (use CONTAINS/REGEX rules for those).
     */
    private fun senderFrom(person: Person?): String? {
        person ?: return null
        val uri = person.uri
        if (uri != null && uri.startsWith("tel:")) {
            val number = Uri.decode(uri.removePrefix("tel:")).trim()
            if (number.isNotEmpty()) return number
        }
        return person.name?.toString()
    }

    private companion object {
        const val TAG = "MsgNotifListener"

        /** Messaging apps whose notifications represent inbound SMS/RCS. */
        val MESSAGING_PACKAGES = setOf(
            "com.google.android.apps.messaging", // Google Messages
            "com.samsung.android.messaging", // Samsung Messages
        )
    }
}
