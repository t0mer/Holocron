package dev.tomerklein.holocron.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
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

    fun clearSecrets(destinationId: Long) {
        val prefix = "secret_${destinationId}_"
        prefs.edit().apply {
            prefs.all.keys.filter { it.startsWith(prefix) }.forEach { remove(it) }
        }.apply()
    }

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
    }
}
