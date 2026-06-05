package dev.tomerklein.holocron.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dev.tomerklein.holocron.data.DeliveryLog
import dev.tomerklein.holocron.data.DeliveryStatus
import dev.tomerklein.holocron.ui.components.openAppSettings
import dev.tomerklein.holocron.ui.components.rememberPermissionRequester
import dev.tomerklein.holocron.ui.components.rememberPermissionStatus
import dev.tomerklein.holocron.ui.components.requestIgnoreBatteryOptimizations
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(viewModel: HomeViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val (permissions, refresh) = rememberPermissionStatus()
    val requestPermissions = rememberPermissionRequester(onResult = refresh)

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("Holocron", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Forwarding incoming SMS to your home lab",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Status", style = MaterialTheme.typography.titleMedium)
                    StatusRow("SMS permission", permissions.smsGranted)
                    StatusRow("Notifications", permissions.notificationsGranted)
                    StatusRow("Battery exemption", permissions.ignoringBatteryOptimizations)

                    if (!permissions.smsGranted || !permissions.notificationsGranted) {
                        Button(onClick = requestPermissions, modifier = Modifier.fillMaxWidth()) {
                            Text("Grant permissions")
                        }
                        Text(
                            "If a permission was permanently denied, open app settings.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Button(onClick = { openAppSettings(context) }, modifier = Modifier.fillMaxWidth()) {
                            Text("Open app settings")
                        }
                    }
                    if (!permissions.ignoringBatteryOptimizations) {
                        Button(
                            onClick = { requestIgnoreBatteryOptimizations(context) },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Request battery exemption") }
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Foreground listening", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Keeps a persistent notification for maximum reliability",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = state.foregroundServiceEnabled,
                        onCheckedChange = { viewModel.setForegroundService(it) },
                    )
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                CountCard("Rules", state.ruleCount, Modifier.weight(1f))
                CountCard("Destinations", state.destinationCount, Modifier.weight(1f))
            }
        }

        item { Text("Recent activity", style = MaterialTheme.typography.titleMedium) }

        if (state.recent.isEmpty()) {
            item {
                Text(
                    "No deliveries yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            items(state.recent, key = { it.id }) { RecentRow(it) }
        }
    }
}

@Composable
private fun StatusRow(label: String, ok: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(
            imageVector = if (ok) Icons.Filled.CheckCircle else Icons.Filled.Error,
            contentDescription = null,
            tint = if (ok) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
            modifier = Modifier.size(20.dp),
        )
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.weight(1f))
        Text(if (ok) "OK" else "Action needed", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun CountCard(label: String, count: Int, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(Modifier.padding(16.dp)) {
            Text("$count", style = MaterialTheme.typography.headlineMedium)
            Text(label, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun RecentRow(log: DeliveryLog) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(log.sender, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    formatTime(log.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                log.status.name,
                style = MaterialTheme.typography.labelMedium,
                color = statusColor(log.status),
            )
        }
    }
}

internal fun statusColor(status: DeliveryStatus): Color = when (status) {
    DeliveryStatus.SUCCESS -> Color(0xFF2E7D32)
    DeliveryStatus.FAILED -> Color(0xFFC62828)
    DeliveryStatus.RETRYING -> Color(0xFFEF6C00)
    DeliveryStatus.PENDING -> Color(0xFF1565C0)
}

private val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
internal fun formatTime(millis: Long): String = timeFormat.format(Date(millis))
