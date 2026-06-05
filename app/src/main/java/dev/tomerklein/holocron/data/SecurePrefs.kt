package dev.tomerklein.holocron.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keystore-backed encrypted storage for secrets (MQTT credentials, API keys/headers)
 * and the persisted, per-install MQTT client ID.
 *
 * Secrets are keyed by destination id so they survive config edits to the (non-secret)
 * [Destination.config] JSON.
 */
@Singleton
class SecurePrefs @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "holocron_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    fun putSecret(destinationId: Long, field: String, value: String?) {
        prefs.edit().apply {
            if (value == null) remove(secretKey(destinationId, field))
            else putString(secretKey(destinationId, field), value)
        }.apply()
    }

    fun getSecret(destinationId: Long, field: String): String? =
        prefs.getString(secretKey(destinationId, field), null)

    /** Store custom HTTP headers (which may contain auth tokens) encrypted at rest. */
    fun putHeaders(destinationId: Long, headers: Map<String, String>) {
        val json = headerJson.encodeToString(headerSerializer, headers)
        putSecret(destinationId, FIELD_HEADERS, json.takeIf { headers.isNotEmpty() })
    }

    fun getHeaders(destinationId: Long): Map<String, String> {
        val json = getSecret(destinationId, FIELD_HEADERS) ?: return emptyMap()
        return runCatching { headerJson.decodeFromString(headerSerializer, json) }.getOrDefault(emptyMap())
    }

    fun clearSecrets(destinationId: Long) {
        val prefix = "secret_${destinationId}_"
        prefs.edit().apply {
            prefs.all.keys.filter { it.startsWith(prefix) }.forEach { remove(it) }
        }.apply()
    }

    /**
     * The outbound body of an in-flight delivery, stored encrypted and keyed by delivery-log id.
     * Kept off WorkManager's plaintext input table; retained on failure so a delivery can be
     * retried manually, and cleared on success.
     */
    fun putPendingBody(logId: Long, body: String) =
        prefs.edit().putString(pendingBodyKey(logId), body).apply()

    fun getPendingBody(logId: Long): String? = prefs.getString(pendingBodyKey(logId), null)

    fun clearPendingBody(logId: Long) = prefs.edit().remove(pendingBodyKey(logId)).apply()

    private fun pendingBodyKey(logId: Long) = "pending_body_$logId"

    /**
     * The MQTT client ID, generated once on first use and reused forever — never derive
     * it from non-unique data, and never let a settings save wipe it (see CLAUDE.md / mqtt.md).
     */
    fun mqttClientId(): String {
        prefs.getString(KEY_MQTT_CLIENT_ID, null)?.let { return it }
        val id = "holocron-${UUID.randomUUID()}"
        prefs.edit().putString(KEY_MQTT_CLIENT_ID, id).apply()
        return id
    }

    private fun secretKey(destinationId: Long, field: String) = "secret_${destinationId}_$field"

    private companion object {
        const val KEY_MQTT_CLIENT_ID = "mqtt_client_id"
        const val FIELD_HEADERS = "http_headers"
        val headerJson = Json { encodeDefaults = true }
        val headerSerializer = MapSerializer(String.serializer(), String.serializer())
    }
}
