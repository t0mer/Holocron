package dev.tomerklein.holocron.dispatch

import dev.tomerklein.holocron.data.Destination
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sends a synthetic message straight to a destination (bypassing rules and WorkManager) so the
 * user can validate wiring from the Destinations screen without waiting for a real SMS.
 */
@Singleton
class TestSender @Inject constructor(
    private val registry: DispatcherRegistry,
) {
    suspend fun sendTest(destination: Destination): DispatchResult {
        val message = IncomingMessage(
            sender = "+10000000000",
            body = "Holocron test message",
            timestamp = System.currentTimeMillis(),
            defaultRegion = null,
            ruleName = "Test",
        )
        return runCatching { registry.forType(destination.type).send(message, destination) }
            .getOrElse { DispatchResult.Permanent(it.message ?: "Test failed") }
    }
}
