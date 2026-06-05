package dev.tomerklein.holocron.dispatch

import com.hivemq.client.mqtt.datatypes.MqttQos
import com.hivemq.client.mqtt.mqtt3.Mqtt3Client
import dev.tomerklein.holocron.data.Destination
import dev.tomerklein.holocron.data.SecurePrefs
import dev.tomerklein.holocron.data.config.DestinationJson
import dev.tomerklein.holocron.data.config.MqttConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Publishes the message to an MQTT topic. Credentials are read from [SecurePrefs]; the
 * client ID is the persisted per-install ID (never derived from non-unique data).
 *
 * Scaffold note: connects per-send for simplicity. A production build should reuse a
 * long-lived connection (see guidelines/mqtt.md) and apply LWT/keep-alive/backoff tuning.
 */
@Singleton
class MqttDispatcher @Inject constructor(
    private val securePrefs: SecurePrefs,
) : Dispatcher {

    override suspend fun send(message: IncomingMessage, destination: Destination): DispatchResult =
        withContext(Dispatchers.IO) {
            val cfg = DestinationJson.decodeFromString(MqttConfig.serializer(), destination.config)
            if (cfg.brokerHost.isBlank() || cfg.topic.isBlank()) {
                return@withContext DispatchResult.Permanent("MQTT host/topic not configured")
            }

            val payload = if (cfg.publishJson) {
                PayloadTemplate.defaultJsonEnvelope(message, message.ruleName)
            } else {
                message.body
            }

            val builder = Mqtt3Client.builder()
                .identifier(securePrefs.mqttClientId())
                .serverHost(cfg.brokerHost)
                .serverPort(cfg.port)
            if (cfg.useTls) builder.sslWithDefaultConfig()
            val client = builder.buildBlocking()

            try {
                val connect = client.connectWith()
                if (cfg.hasCredentials) {
                    val user = securePrefs.getSecret(destination.id, FIELD_USERNAME)
                    val pass = securePrefs.getSecret(destination.id, FIELD_PASSWORD)
                    if (user != null) {
                        connect.simpleAuth()
                            .username(user)
                            .apply { pass?.let { password(it.toByteArray()) } }
                            .applySimpleAuth()
                    }
                }
                connect.send()

                client.publishWith()
                    .topic(cfg.topic)
                    .qos(MqttQos.fromCode(cfg.qos) ?: MqttQos.AT_LEAST_ONCE)
                    .payload(payload.toByteArray())
                    .retain(cfg.retain)
                    .send()

                DispatchResult.Success
            } catch (t: Throwable) {
                // Connection/publish problems are typically transient → retry with backoff.
                DispatchResult.Retryable(t.message ?: "MQTT error")
            } finally {
                runCatching { if (client.state.isConnected) client.disconnect() }
            }
        }

    companion object {
        const val FIELD_USERNAME = "mqtt_username"
        const val FIELD_PASSWORD = "mqtt_password"
    }
}
