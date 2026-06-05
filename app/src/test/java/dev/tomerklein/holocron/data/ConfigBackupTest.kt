package dev.tomerklein.holocron.data

import dev.tomerklein.holocron.data.config.DestinationJson
import dev.tomerklein.holocron.data.config.MqttConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.serialization.json.Json

class ConfigBackupTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun backup_roundTripsRulesAndDestinations() {
        val bundle = ConfigBackup(
            destinations = listOf(
                DestinationBackup(refId = 7, name = "MQTT", type = "MQTT", config = "{\"brokerHost\":\"h\"}"),
            ),
            rules = listOf(
                RuleBackup("Bank", "MyBank", "CONTAINS", destinationRefId = 7, enabled = true),
            ),
        )
        val text = json.encodeToString(ConfigBackup.serializer(), bundle)
        val decoded = json.decodeFromString(ConfigBackup.serializer(), text)

        assertEquals(bundle, decoded)
        assertEquals(7, decoded.rules.single().destinationRefId)
    }

    @Test
    fun backup_excludesSecrets_byConstruction() {
        // The backup model has no field for credentials/headers — encoding can't leak them.
        val text = json.encodeToString(
            ConfigBackup.serializer(),
            ConfigBackup(destinations = listOf(DestinationBackup(1, "d", "MQTT", "{}"))),
        )
        assertFalse(text.contains("password"))
        assertFalse(text.contains("username"))
    }

    @Test
    fun mqttConfig_defaultsAndDecode() {
        val cfg = MqttConfig(brokerHost = "broker.local", topic = "sms/in", useTls = true, port = 8883)
        val text = DestinationJson.encodeToString(MqttConfig.serializer(), cfg)
        val decoded = DestinationJson.decodeFromString(MqttConfig.serializer(), text)
        assertEquals(cfg, decoded)
        assertTrue(decoded.useTls)
        assertEquals(8883, decoded.port)
    }
}
