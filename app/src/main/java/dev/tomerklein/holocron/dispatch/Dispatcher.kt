package dev.tomerklein.holocron.dispatch

import dev.tomerklein.holocron.data.Destination

/** A reassembled inbound SMS being forwarded. */
data class IncomingMessage(
    val sender: String,
    val body: String,
    val timestamp: Long,
    val defaultRegion: String?,
    /** Name of the matched rule, used for payload templating. Empty until dispatch time. */
    val ruleName: String = "",
)

/** Outcome of a single send attempt. Maps to WorkManager Result in [DispatchWorker]. */
sealed interface DispatchResult {
    data object Success : DispatchResult

    /** Transient failure (timeout, IO, 5xx) — worth retrying with backoff. */
    data class Retryable(val reason: String) : DispatchResult

    /** Permanent failure (e.g. 4xx, malformed config) — retrying won't help. */
    data class Permanent(val reason: String) : DispatchResult
}

/** Common contract every destination type implements. */
interface Dispatcher {
    suspend fun send(message: IncomingMessage, destination: Destination): DispatchResult
}
