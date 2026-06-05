package dev.tomerklein.holocron.ingest

import dev.tomerklein.holocron.data.HolocronRepository
import dev.tomerklein.holocron.data.SettingsStore
import dev.tomerklein.holocron.dispatch.DispatchEnqueuer
import dev.tomerklein.holocron.dispatch.IncomingMessage
import dev.tomerklein.holocron.rules.NumberMatcher
import dev.tomerklein.holocron.util.Logx
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single entry point for inbound messages from any source (SMS broadcast or RCS/notification).
 * Applies debug logging, source gating, rule matching, and per-(rule, body) de-duplication
 * before enqueuing forwards.
 */
@Singleton
class IncomingMessageRouter @Inject constructor(
    private val repository: HolocronRepository,
    private val matcher: NumberMatcher,
    private val enqueuer: DispatchEnqueuer,
    private val settingsStore: SettingsStore,
) {
    enum class Source { SMS, RCS }

    private val dedup = EnqueueDedup()

    suspend fun route(
        sender: String,
        body: String,
        timestamp: Long,
        region: String?,
        source: Source,
    ) {
        if (sender.isBlank() || body.isBlank()) return

        val settings = settingsStore.settings.first()
        // RCS is opt-in (needs notification access); SMS always flows.
        if (source == Source.RCS && !settings.rcsForwardingEnabled) return

        if (settings.debugLogging) Logx.sms(TAG, source.name, sender, body)

        val incoming = IncomingMessage(sender = sender, body = body, timestamp = timestamp, defaultRegion = region)
        val rules = repository.enabledRules()
        val now = System.currentTimeMillis()
        val fingerprintBody = body.trim()
        for (rule in rules) {
            if (!matcher.matches(rule, sender, region)) continue
            if (!dedup.allow("${rule.id}|$fingerprintBody", now)) continue
            enqueuer.enqueue(incoming, rule)
        }
    }

    private companion object {
        const val TAG = "MsgRouter"
    }
}
