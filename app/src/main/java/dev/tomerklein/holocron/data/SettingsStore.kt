package dev.tomerklein.holocron.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "holocron_settings")

/** App-level (non-secret) settings. Secrets live in [SecurePrefs]. */
data class AppSettings(
    val foregroundServiceEnabled: Boolean = false,
    val logRetention: Int = DEFAULT_RETENTION,
    val redactBodies: Boolean = true,
    val debugLogging: Boolean = false,
    val rcsForwardingEnabled: Boolean = false,
) {
    companion object {
        const val DEFAULT_RETENTION = 200
    }
}

@Singleton
class SettingsStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            foregroundServiceEnabled = prefs[KEY_FG_SERVICE] ?: false,
            logRetention = prefs[KEY_RETENTION] ?: AppSettings.DEFAULT_RETENTION,
            redactBodies = prefs[KEY_REDACT] ?: true,
            debugLogging = prefs[KEY_DEBUG_LOG] ?: false,
            rcsForwardingEnabled = prefs[KEY_RCS] ?: false,
        )
    }

    suspend fun setForegroundServiceEnabled(enabled: Boolean) =
        context.dataStore.edit { it[KEY_FG_SERVICE] = enabled }.let { }

    suspend fun setLogRetention(value: Int) =
        context.dataStore.edit { it[KEY_RETENTION] = value.coerceIn(20, 5000) }.let { }

    suspend fun setRedactBodies(redact: Boolean) =
        context.dataStore.edit { it[KEY_REDACT] = redact }.let { }

    suspend fun setDebugLogging(enabled: Boolean) =
        context.dataStore.edit { it[KEY_DEBUG_LOG] = enabled }.let { }

    suspend fun setRcsForwardingEnabled(enabled: Boolean) =
        context.dataStore.edit { it[KEY_RCS] = enabled }.let { }

    private companion object {
        val KEY_FG_SERVICE = booleanPreferencesKey("foreground_service_enabled")
        val KEY_RETENTION = intPreferencesKey("log_retention")
        val KEY_REDACT = booleanPreferencesKey("redact_bodies")
        val KEY_DEBUG_LOG = booleanPreferencesKey("debug_logging")
        val KEY_RCS = booleanPreferencesKey("rcs_forwarding_enabled")
    }
}
