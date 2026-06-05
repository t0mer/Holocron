package dev.tomerklein.holocron.data

import kotlinx.serialization.Serializable

/**
 * Export/import bundle for backup. Contains rules and destinations only — **secrets are never
 * exported** (MQTT credentials, HTTP headers live encrypted in [SecurePrefs] and must be
 * re-entered after import). Destinations carry a stable [DestinationBackup.refId] that rules
 * reference, so the link survives re-insertion with fresh database ids.
 */
@Serializable
data class ConfigBackup(
    val version: Int = 1,
    val destinations: List<DestinationBackup> = emptyList(),
    val rules: List<RuleBackup> = emptyList(),
)

@Serializable
data class DestinationBackup(
    val refId: Long,
    val name: String,
    val type: String,
    val config: String,
)

@Serializable
data class RuleBackup(
    val name: String,
    val senderPattern: String,
    val matchType: String,
    val destinationRefId: Long,
    val enabled: Boolean,
)
