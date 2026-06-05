package dev.tomerklein.holocron.ui.rules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import dev.tomerklein.holocron.data.MatchType
import dev.tomerklein.holocron.ui.components.DropdownField
import dev.tomerklein.holocron.ui.components.EditScaffold

@Composable
fun RuleEditScreen(
    onDone: () -> Unit,
    viewModel: RuleEditViewModel = hiltViewModel(),
) {
    val form by viewModel.form.collectAsState()
    val destinations by viewModel.destinations.collectAsState()

    EditScaffold(
        title = if (viewModel.isNew) "New rule" else "Edit rule",
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
            label = "Match type",
            selected = form.matchType,
            options = MatchType.entries,
            optionLabel = { it.name },
            onSelect = { v -> viewModel.update { it.copy(matchType = v) } },
        )

        OutlinedTextField(
            value = form.senderPattern,
            onValueChange = { v -> viewModel.update { it.copy(senderPattern = v) } },
            label = { Text(senderLabel(form.matchType)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        DropdownField(
            label = "Destination",
            selected = destinations.firstOrNull { it.id == form.destinationId },
            options = destinations,
            optionLabel = { it.name },
            onSelect = { v -> viewModel.update { it.copy(destinationId = v.id) } },
        )
        if (destinations.isEmpty()) {
            Text(
                "Create a destination first (Destinations tab).",
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                color = androidx.compose.material3.MaterialTheme.colorScheme.error,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Enabled")
            Switch(checked = form.enabled, onCheckedChange = { v -> viewModel.update { it.copy(enabled = v) } })
        }
    }
}

private fun senderLabel(type: MatchType): String = when (type) {
    MatchType.EXACT -> "Sender number (any format)"
    MatchType.CONTAINS -> "Substring to match in sender"
    MatchType.REGEX -> "Regular expression"
}
