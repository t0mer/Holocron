package dev.tomerklein.holocron.dispatch

import dev.tomerklein.holocron.data.DestinationType
import javax.inject.Inject
import javax.inject.Singleton

/** Resolves the right [Dispatcher] for a destination type. */
@Singleton
class DispatcherRegistry @Inject constructor(
    private val webhook: WebhookDispatcher,
    private val api: ApiDispatcher,
    private val mqtt: MqttDispatcher,
) {
    fun forType(type: DestinationType): Dispatcher = when (type) {
        DestinationType.WEBHOOK -> webhook
        DestinationType.API -> api
        DestinationType.MQTT -> mqtt
    }
}
