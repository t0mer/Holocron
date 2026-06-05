package dev.tomerklein.holocron.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** The kind of endpoint a [Destination] forwards to. */
enum class DestinationType { WEBHOOK, API, MQTT }

/**
 * A forwarding target. [config] holds the serialized, type-specific, **non-secret**
 * configuration (see [dev.tomerklein.holocron.data.config.DestinationConfig]); secrets
 * (MQTT credentials, API keys/headers) live in
 * [dev.tomerklein.holocron.data.SecurePrefs], keyed by [id].
 */
@Entity(tableName = "destinations")
data class Destination(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: DestinationType,
    val config: String,
    val enabled: Boolean = true,
)
