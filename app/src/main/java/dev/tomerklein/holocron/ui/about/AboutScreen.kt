package dev.tomerklein.holocron.ui.about

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.tomerklein.holocron.BuildConfig

private const val AUTHOR_NAME = "Tomer Klein"
private const val AUTHOR_EMAIL = "tomer.klein@gmail.com"
private const val REPO_URL = "https://github.com/t0mer/Holocron"
private const val ISSUES_URL = "https://github.com/t0mer/Holocron/issues"

@Composable
fun AboutScreen() {
    val context = LocalContext.current

    fun open(url: String) {
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
    }

    fun email(address: String) {
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$address")).apply {
            putExtra(Intent.EXTRA_SUBJECT, "Holocron ${BuildConfig.APP_VERSION}")
        }
        runCatching { context.startActivity(intent) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("About", style = MaterialTheme.typography.headlineMedium)
        Text(
            "SMS forwarding for automation",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                InfoRow(label = "Author", value = AUTHOR_NAME)
                HorizontalDivider()
                InfoRow(label = "Email", value = AUTHOR_EMAIL, link = true) { email(AUTHOR_EMAIL) }
                HorizontalDivider()
                InfoRow(
                    label = "Version",
                    value = "${BuildConfig.APP_VERSION} (built ${BuildConfig.BUILD_DATE})",
                )
                HorizontalDivider()
                InfoRow(label = "Repository", value = REPO_URL, link = true) { open(REPO_URL) }
                HorizontalDivider()
                InfoRow(label = "Report an issue", value = ISSUES_URL, link = true) { open(ISSUES_URL) }
            }
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    link: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 12.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyLarge,
            color = if (link) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        )
    }
}
