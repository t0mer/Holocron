package dev.tomerklein.holocron.ui.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.tomerklein.holocron.data.AppSettings
import dev.tomerklein.holocron.data.ConfigBackup
import dev.tomerklein.holocron.data.Destination
import dev.tomerklein.holocron.data.DestinationBackup
import dev.tomerklein.holocron.data.DestinationType
import dev.tomerklein.holocron.data.HolocronRepository
import dev.tomerklein.holocron.data.MatchType
import dev.tomerklein.holocron.data.Rule
import dev.tomerklein.holocron.data.RuleBackup
import dev.tomerklein.holocron.data.SettingsStore
import dev.tomerklein.holocron.service.ListeningService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val repository: HolocronRepository,
    private val settingsStore: SettingsStore,
) : ViewModel() {

    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    val settings: StateFlow<AppSettings> =
        settingsStore.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    val message = MutableStateFlow<String?>(null)

    fun setForegroundService(enabled: Boolean) {
        viewModelScope.launch {
            settingsStore.setForegroundServiceEnabled(enabled)
            if (enabled) ListeningService.start(appContext) else ListeningService.stop(appContext)
        }
    }

    fun setRetention(value: Int) {
        viewModelScope.launch { settingsStore.setLogRetention(value) }
    }

    fun setRedactBodies(redact: Boolean) {
        viewModelScope.launch { settingsStore.setRedactBodies(redact) }
    }

    fun exportTo(uri: Uri) {
        viewModelScope.launch {
            runCatching {
                val bundle = buildBundle()
                withContext(Dispatchers.IO) {
                    appContext.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(json.encodeToString(ConfigBackup.serializer(), bundle).toByteArray())
                    } ?: error("Could not open file")
                }
            }.onSuccess { message.value = "Configuration exported (secrets excluded)" }
                .onFailure { message.value = "Export failed: ${it.message}" }
        }
    }

    fun importFrom(uri: Uri) {
        viewModelScope.launch {
            runCatching {
                val text = withContext(Dispatchers.IO) {
                    appContext.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() }
                        ?: error("Could not open file")
                }
                val bundle = json.decodeFromString(ConfigBackup.serializer(), text)
                applyBundle(bundle)
            }.onSuccess { message.value = "Imported ${it} rule(s). Re-enter secrets in each destination." }
                .onFailure { message.value = "Import failed: ${it.message}" }
        }
    }

    fun consumeMessage() {
        message.value = null
    }

    private suspend fun buildBundle(): ConfigBackup {
        val destinations = repository.observeDestinations().first()
        val rules = repository.observeRules().first()
        return ConfigBackup(
            destinations = destinations.map {
                DestinationBackup(refId = it.id, name = it.name, type = it.type.name, config = it.config)
            },
            rules = rules.map {
                RuleBackup(
                    name = it.name,
                    senderPattern = it.senderPattern,
                    matchType = it.matchType.name,
                    destinationRefId = it.destinationId,
                    enabled = it.enabled,
                )
            },
        )
    }

    /** Inserts the backup, remapping destination ids; returns the number of rules imported. */
    private suspend fun applyBundle(bundle: ConfigBackup): Int {
        val refToNewId = HashMap<Long, Long>()
        for (d in bundle.destinations) {
            val newId = repository.upsertDestination(
                Destination(
                    name = d.name,
                    type = runCatching { DestinationType.valueOf(d.type) }.getOrDefault(DestinationType.WEBHOOK),
                    config = d.config,
                    enabled = true,
                ),
            )
            refToNewId[d.refId] = newId
        }
        var imported = 0
        val now = System.currentTimeMillis()
        for (r in bundle.rules) {
            val destId = refToNewId[r.destinationRefId] ?: continue
            repository.upsertRule(
                Rule(
                    name = r.name,
                    senderPattern = r.senderPattern,
                    matchType = runCatching { MatchType.valueOf(r.matchType) }.getOrDefault(MatchType.EXACT),
                    destinationId = destId,
                    enabled = r.enabled,
                    createdAt = now,
                    updatedAt = now,
                ),
            )
            imported++
        }
        return imported
    }
}
