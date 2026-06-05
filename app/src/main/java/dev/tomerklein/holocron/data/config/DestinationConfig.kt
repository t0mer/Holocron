package dev.tomerklein.holocron.data.config

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** JSON codec for the non-secret slice of a destination's configuration. */
val DestinationJson: Json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

@Serializable
data class WebhookConfig(
    val url: String = "",
    val method: String = "POST",
    val headers: Map<String, String> = emptyMap(),
    /** Optional body template; null/blank uses the default JSON envelope. */
    val bodyTemplate: String? = null,
)

@Serializable
data class ApiConfig(
    val url: String = "",
    val method: String = "POST",
    val headers: Map<String, String> = emptyMap(),
    val bodyTemplate: String? = null,
)

@Serializable
data class MqttConfig(
    val brokerHost: String = "",
    val port: Int = 1883,
    val useTls: Boolean = false,
    val topic: String = "",
    val qos: Int = 1,
    val retain: Boolean = false,
    /** When true, publish the full JSON envelope; otherwise the raw body. */
    val publishJson: Boolean = false,
    /** Whether a username/password pair is stored in SecurePrefs for this destination. */
    val hasCredentials: Boolean = false,
)
