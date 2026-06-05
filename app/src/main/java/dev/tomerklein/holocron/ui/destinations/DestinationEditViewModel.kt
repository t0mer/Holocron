package dev.tomerklein.holocron.ui.destinations

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.tomerklein.holocron.data.Destination
import dev.tomerklein.holocron.data.DestinationType
import dev.tomerklein.holocron.data.HolocronRepository
import dev.tomerklein.holocron.data.SecurePrefs
import dev.tomerklein.holocron.data.config.ApiConfig
import dev.tomerklein.holocron.data.config.DestinationJson
import dev.tomerklein.holocron.data.config.MqttConfig
import dev.tomerklein.holocron.data.config.WebhookConfig
import dev.tomerklein.holocron.dispatch.DispatchResult
import dev.tomerklein.holocron.dispatch.MqttDispatcher
import dev.tomerklein.holocron.dispatch.TestSender
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** All editable fields; type-specific fields are ignored when [type] differs. */
data class DestinationForm(
    val id: Long = 0,
    val name: String = "",
    val type: DestinationType = DestinationType.WEBHOOK,
    // HTTP (webhook + api)
    val url: String = "",
    val method: String = "POST",
    val bodyTemplate: String = "",
    val headersText: String = "",
    // MQTT
    val brokerHost: String = "",
    val port: String = "1883",
    val useTls: Boolean = false,
    val topic: String = "",
    val qos: Int = 1,
    val retain: Boolean = false,
    val publishJson: Boolean = false,
    val username: String = "",
    val password: String = "",
    val loading: Boolean = true,
) {
    val isValid: Boolean
        get() = name.isNotBlank() && when (type) {
            DestinationType.WEBHOOK, DestinationType.API -> url.isNotBlank()
            DestinationType.MQTT -> brokerHost.isNotBlank() && topic.isNotBlank()
        }
}

@HiltViewModel
class DestinationEditViewModel @Inject constructor(
    private val repository: HolocronRepository,
    private val securePrefs: SecurePrefs,
    private val testSender: TestSender,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val destinationId: Long = savedStateHandle.get<Long>("destinationId") ?: -1L
    val isNew: Boolean = destinationId <= 0L

    private val _form = MutableStateFlow(DestinationForm(loading = !isNew))
    val form: StateFlow<DestinationForm> = _form

    val testMessage = MutableStateFlow<String?>(null)

    init {
        if (isNew) {
            _form.value = DestinationForm(loading = false)
        } else {
            viewModelScope.launch { loadExisting(destinationId) }
        }
    }

    fun update(transform: (DestinationForm) -> DestinationForm) = _form.update(transform)

    private suspend fun loadExisting(id: Long) {
        val d = repository.destination(id) ?: run {
            _form.value = DestinationForm(loading = false); return
        }
        val headers = securePrefs.getHeaders(id)
            .entries.joinToString("\n") { "${it.key}: ${it.value}" }
        val base = DestinationForm(id = d.id, name = d.name, type = d.type, headersText = headers, loading = false)
        _form.value = when (d.type) {
            DestinationType.WEBHOOK -> {
                val c = DestinationJson.decodeFromString(WebhookConfig.serializer(), d.config)
                base.copy(url = c.url, method = c.method, bodyTemplate = c.bodyTemplate.orEmpty())
            }
            DestinationType.API -> {
                val c = DestinationJson.decodeFromString(ApiConfig.serializer(), d.config)
                base.copy(url = c.url, method = c.method, bodyTemplate = c.bodyTemplate.orEmpty())
            }
            DestinationType.MQTT -> {
                val c = DestinationJson.decodeFromString(MqttConfig.serializer(), d.config)
                base.copy(
                    brokerHost = c.brokerHost,
                    port = c.port.toString(),
                    useTls = c.useTls,
                    topic = c.topic,
                    qos = c.qos,
                    retain = c.retain,
                    publishJson = c.publishJson,
                    username = securePrefs.getSecret(id, MqttDispatcher.FIELD_USERNAME).orEmpty(),
                    password = securePrefs.getSecret(id, MqttDispatcher.FIELD_PASSWORD).orEmpty(),
                )
            }
        }
    }

    /** Persists the destination and its secrets, returning the saved row (with id). */
    private suspend fun persist(): Destination {
        val f = _form.value
        val config = when (f.type) {
            DestinationType.WEBHOOK -> DestinationJson.encodeToString(
                WebhookConfig.serializer(),
                WebhookConfig(f.url.trim(), f.method.trim().uppercase(), f.bodyTemplate.ifBlank { null }),
            )
            DestinationType.API -> DestinationJson.encodeToString(
                ApiConfig.serializer(),
                ApiConfig(f.url.trim(), f.method.trim().uppercase(), f.bodyTemplate.ifBlank { null }),
            )
            DestinationType.MQTT -> DestinationJson.encodeToString(
                MqttConfig.serializer(),
                MqttConfig(
                    brokerHost = f.brokerHost.trim(),
                    port = f.port.toIntOrNull() ?: if (f.useTls) 8883 else 1883,
                    useTls = f.useTls,
                    topic = f.topic.trim(),
                    qos = f.qos,
                    retain = f.retain,
                    publishJson = f.publishJson,
                    hasCredentials = f.username.isNotBlank(),
                ),
            )
        }

        val id = repository.upsertDestination(
            Destination(id = f.id, name = f.name.trim(), type = f.type, config = config, enabled = true),
        )

        // Secrets, keyed by the (now stable) id.
        securePrefs.putHeaders(id, parseHeaders(f.headersText))
        if (f.type == DestinationType.MQTT) {
            securePrefs.putSecret(id, MqttDispatcher.FIELD_USERNAME, f.username.trim().ifBlank { null })
            securePrefs.putSecret(id, MqttDispatcher.FIELD_PASSWORD, f.password.ifBlank { null })
        }
        return repository.destination(id)!!
    }

    fun save(onSaved: () -> Unit) {
        if (!_form.value.isValid) return
        viewModelScope.launch {
            persist()
            onSaved()
        }
    }

    /** Persists entered values, then sends a real synthetic message to validate wiring. */
    fun saveAndTest() {
        if (!_form.value.isValid) return
        viewModelScope.launch {
            testMessage.value = "Saving and testing…"
            val saved = persist()
            _form.update { it.copy(id = saved.id) }
            testMessage.value = when (val r = testSender.sendTest(saved)) {
                is DispatchResult.Success -> "Test succeeded"
                is DispatchResult.Retryable -> "Failed (retryable): ${r.reason}"
                is DispatchResult.Permanent -> "Failed: ${r.reason}"
            }
        }
    }

    fun consumeTestMessage() {
        testMessage.value = null
    }

    private fun parseHeaders(text: String): Map<String, String> =
        text.lineSequence()
            .mapNotNull { line ->
                val idx = line.indexOf(':')
                if (idx <= 0) return@mapNotNull null
                val key = line.substring(0, idx).trim()
                val value = line.substring(idx + 1).trim()
                if (key.isEmpty()) null else key to value
            }
            .toMap()
}
