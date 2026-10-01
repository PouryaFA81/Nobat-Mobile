package app.nobat.mobile.ui.shell

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.nobat.mobile.R
import kotlinx.coroutines.launch

@Composable
fun IntegrationsPane(
    snackbar: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val soon = stringResource(R.string.coming_soon)
    fun showSoon() {
        scope.launch { snackbar.showSnackbar(soon) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        IntegrationBlock(
            title = stringResource(R.string.telegram),
            tokenLabel = stringResource(R.string.bot_token),
            onConnect = ::showSoon,
        )
        HorizontalDivider()
        IntegrationBlock(
            title = stringResource(R.string.bale),
            tokenLabel = stringResource(R.string.bot_token),
            onConnect = ::showSoon,
        )
        HorizontalDivider()
        IntegrationBlock(
            title = stringResource(R.string.google_drive),
            tokenLabel = null,
            onConnect = ::showSoon,
        )
    }
}

@Composable
private fun IntegrationBlock(
    title: String,
    tokenLabel: String?,
    onConnect: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        if (tokenLabel != null) {
            OutlinedTextField(
                value = "",
                onValueChange = {},
                label = { Text(tokenLabel) },
                enabled = false,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Button(
            onClick = onConnect,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.connect))
        }
    }
}

@Composable
fun BackupPane(
    snackbar: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val soon = stringResource(R.string.coming_soon)
    fun showSoon() {
        scope.launch { snackbar.showSnackbar(soon) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.data),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ShellActionRow(
            icon = Icons.Outlined.Storage,
            title = stringResource(R.string.backup),
            subtitle = stringResource(R.string.local),
            buttonLabel = stringResource(R.string.backup),
            onClick = ::showSoon,
        )
        HorizontalDivider()
        ShellActionRow(
            icon = Icons.Outlined.Cloud,
            title = stringResource(R.string.backup),
            subtitle = stringResource(R.string.drive),
            buttonLabel = stringResource(R.string.backup),
            onClick = ::showSoon,
        )
        HorizontalDivider()
        ShellActionRow(
            icon = Icons.Outlined.Storage,
            title = stringResource(R.string.restore),
            subtitle = stringResource(R.string.local),
            buttonLabel = stringResource(R.string.restore),
            onClick = ::showSoon,
        )
        HorizontalDivider()
        ShellActionRow(
            icon = Icons.Outlined.Cloud,
            title = stringResource(R.string.restore),
            subtitle = stringResource(R.string.drive),
            buttonLabel = stringResource(R.string.restore),
            onClick = ::showSoon,
        )
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = ::showSoon,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.save))
        }
    }
}

@Composable
fun ReportsPane(
    snackbar: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val soon = stringResource(R.string.coming_soon)
    fun showSoon() {
        scope.launch { snackbar.showSnackbar(soon) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.reports_print),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ShellActionRow(
            icon = Icons.Outlined.Print,
            title = stringResource(R.string.reports),
            subtitle = stringResource(R.string.coming_soon),
            buttonLabel = stringResource(R.string.reports),
            onClick = ::showSoon,
        )
        HorizontalDivider()
        ShellActionRow(
            icon = Icons.Outlined.Print,
            title = stringResource(R.string.print_pdf),
            subtitle = stringResource(R.string.coming_soon),
            buttonLabel = stringResource(R.string.print_pdf),
            onClick = ::showSoon,
        )
    }
}

@Composable
private fun ShellActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    buttonLabel: String,
    onClick: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                if (subtitle != null) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Button(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
            Text(buttonLabel)
        }
    }
}
