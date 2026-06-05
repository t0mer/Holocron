package dev.tomerklein.holocron.ui.destinations

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dev.tomerklein.holocron.data.DestinationType
import dev.tomerklein.holocron.ui.components.DropdownField
import dev.tomerklein.holocron.ui.components.EditScaffold

@Composable
fun DestinationEditScreen(
    onDone: () -> Unit,
    viewModel: DestinationEditViewModel = hiltViewModel(),
) {
    val form by viewModel.form.collectAsState()
    val testMessage by viewModel.testMessage.collectAsState()

    EditScaffold(
        title = if (viewModel.isNew) "New destination" else "Edit destination",
        onBack = onDone,
        saveEnabled = form.isValid,
        onSave = { viewModel.save(onDone) },
    ) {
        OutlinedTextField(
            value = form.name,
            onValueChange = { v -> viewModel.update { it.copy(name = v) } },
            label = { Text("Name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        DropdownField(
            label = "Type",
            selected = form.type,
            options = DestinationType.entries,
            optionLabel = { it.name },
            onSelect = { v -> viewModel.update { it.copy(type = v) } },
        )

        when (form.type) {
            DestinationType.WEBHOOK, DestinationType.API -> HttpFields(form, viewModel)
            DestinationType.MQTT -> MqttFields(form, viewModel)
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { viewModel.saveAndTest() }, modifier = Modifier.weight(1f), enabled = form.isValid) {
                Text("Save & Test")
            }
            Button(onClick = { viewModel.save(onDone) }, modifier = Modifier.weight(1f), enabled = form.isValid) {
                Text("Save")
            }
        }
        testMessage?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun HttpFields(form: DestinationForm, viewModel: DestinationEditViewModel) {
    OutlinedTextField(
        value = form.url,
        onValueChange = { v -> viewModel.update { it.copy(url = v) } },
        label = { Text("URL") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    if (form.url.startsWith("http://")) {
        Text(
            "Plaintext HTTP — credentials and message content are not encrypted in transit. Prefer HTTPS.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
    }
    DropdownField(
        label = "Method",
        selected = form.method,
        options = listOf("GET", "POST", "PUT", "PATCH", "DELETE"),
        optionLabel = { it },
        onSelect = { v -> viewModel.update { it.copy(method = v) } },
    )
    OutlinedTextField(
        value = form.headersText,
        onValueChange = { v -> viewModel.update { it.copy(headersText = v) } },
        label = { Text("Headers (one per line: Key: Value)") },
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = form.bodyTemplate,
        onValueChange = { v -> viewModel.update { it.copy(bodyTemplate = v) } },
        label = { Text("Body template (blank = JSON envelope)") },
        placeholder = { Text("{{sender}} {{body}} {{timestamp}} {{ruleName}}") },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun MqttFields(form: DestinationForm, viewModel: DestinationEditViewModel) {
    OutlinedTextField(
        value = form.brokerHost,
        onValueChange = { v -> viewModel.update { it.copy(brokerHost = v) } },
        label = { Text("Broker host") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = form.port,
        onValueChange = { v -> viewModel.update { it.copy(port = v.filter(Char::isDigit)) } },
        label = { Text("Port") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
    )
    SwitchRow("Use TLS (MQTTS)", form.useTls) { checked ->
        // Toggling TLS moves the port (1883 <-> 8883) unless the user set a custom one.
        viewModel.update {
            val newPort = when (it.port) {
                "1883" -> if (checked) "8883" else "1883"
                "8883" -> if (checked) "8883" else "1883"
                else -> it.port
            }
            it.copy(useTls = checked, port = newPort)
        }
    }
    if (!form.useTls) {
        Text(
            "Plaintext MQTT — credentials and payloads are sent unencrypted. Prefer TLS.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
    }
    OutlinedTextField(
        value = form.topic,
        onValueChange = { v -> viewModel.update { it.copy(topic = v) } },
        label = { Text("Topic") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    DropdownField(
        label = "QoS",
        selected = form.qos,
        options = listOf(0, 1, 2),
        optionLabel = { "QoS $it" },
        onSelect = { v -> viewModel.update { it.copy(qos = v) } },
    )
    SwitchRow("Retain", form.retain) { c -> viewModel.update { it.copy(retain = c) } }
    SwitchRow("Publish full JSON (else raw body)", form.publishJson) { c -> viewModel.update { it.copy(publishJson = c) } }
    OutlinedTextField(
        value = form.username,
        onValueChange = { v -> viewModel.update { it.copy(username = v) } },
        label = { Text("Username (optional)") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = form.password,
        onValueChange = { v -> viewModel.update { it.copy(password = v) } },
        label = { Text("Password (optional)") },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
