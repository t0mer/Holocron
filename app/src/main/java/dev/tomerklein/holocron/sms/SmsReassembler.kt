package dev.tomerklein.holocron.sms

import android.telephony.SmsMessage

/** A fully reassembled inbound SMS. */
data class ReassembledSms(
    val sender: String,
    val body: String,
    val timestampMillis: Long,
)

/**
 * Reassembles the multiple PDUs of a (possibly multipart/concatenated) SMS into one message
 * per sender. A long SMS arrives as several [SmsMessage] parts; their bodies must be
 * concatenated **in arrival order**, grouped by originating address.
 *
 * Kept free of Android framework lookups (takes already-parsed [SmsMessage]s) so it is
 * straightforward to unit-test.
 */
object SmsReassembler {
    fun reassemble(parts: List<SmsMessage>): List<ReassembledSms> {
        if (parts.isEmpty()) return emptyList()

        // Preserve first-seen order of senders while concatenating their parts.
        val order = ArrayList<String>()
        val bodies = LinkedHashMap<String, StringBuilder>()
        val timestamps = HashMap<String, Long>()

        for (part in parts) {
            val sender = part.originatingAddress ?: ""
            val sb = bodies.getOrPut(sender) {
                order.add(sender)
                StringBuilder()
            }
            sb.append(part.messageBody ?: "")
            // First part's timestamp represents the message.
            timestamps.putIfAbsent(sender, part.timestampMillis)
        }

        return order.map { sender ->
            ReassembledSms(
                sender = sender,
                body = bodies.getValue(sender).toString(),
                timestampMillis = timestamps[sender] ?: System.currentTimeMillis(),
            )
        }
    }
}
