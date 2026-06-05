package dev.tomerklein.holocron.ingest

/**
 * Suppresses duplicate forwards within a time window. A real SMS fires both the SMS broadcast
 * and a messaging-app notification; without dedup the same message would be forwarded twice.
 *
 * Keyed by rule id + body so that:
 * - the SMS path and the RCS/notification path for the *same* message and *same* rule collapse
 *   to one forward, but
 * - a message that legitimately matches two different rules still forwards to each.
 *
 * Pure and clock-injected (caller passes `nowMs`) so it is unit-testable.
 */
class EnqueueDedup(private val windowMs: Long = DEFAULT_WINDOW_MS) {

    private val seen = HashMap<String, Long>()

    /** Returns true if this key should be enqueued now; false if it's a duplicate in-window. */
    @Synchronized
    fun allow(key: String, nowMs: Long): Boolean {
        val cutoff = nowMs - windowMs
        seen.entries.removeAll { it.value < cutoff }
        val last = seen[key]
        if (last != null && last >= cutoff) return false
        seen[key] = nowMs
        return true
    }

    companion object {
        const val DEFAULT_WINDOW_MS = 8_000L
    }
}
