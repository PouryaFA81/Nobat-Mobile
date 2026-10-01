package app.nobat.mobile.ui.shell

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.nobat.mobile.NobatApp
import app.nobat.mobile.R
import app.nobat.mobile.backup.LocalBackup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
        HorizontalDivider()
        IntegrationBlock(
            title = stringResource(R.string.google_calendar),
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
    accountId: Long,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val app = context.applicationContext as NobatApp
    val scope = rememberCoroutineScope()
    val soon = stringResource(R.string.coming_soon)
    val savedMsg = stringResource(R.string.backup_saved)
    val restoreCompleteMsg = stringResource(R.string.restore_complete)
    val backupFailedMsg = stringResource(R.string.backup_failed)
    val restoreFailedMsg = stringResource(R.string.restore_failed)
    val noAccountMsg = stringResource(R.string.backup_no_account)

    var busy by remember { mutableStateOf(false) }
    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }

    fun showSoon() {
        scope.launch { snackbar.showSnackbar(soon) }
    }

    val createDoc = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(LocalBackup.MIME_JSON),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        if (accountId <= 0L) {
            scope.launch { snackbar.showSnackbar(noAccountMsg) }
            return@rememberLauncherForActivityResult
        }
        busy = true
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                LocalBackup.exportToUri(
                    context = context,
                    db = app.database,
                    notifyStore = app.notificationStore,
                    accountId = accountId,
                    uri = uri,
                )
            }
            busy = false
            snackbar.showSnackbar(
                if (result.isSuccess) savedMsg else backupFailedMsg,
            )
        }
    }

    val openDoc = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        pendingRestoreUri = uri
    }

    fun runLocalBackup() {
        if (accountId <= 0L) {
            scope.launch { snackbar.showSnackbar(noAccountMsg) }
            return
        }
        busy = true
        scope.launch {
            val downloads = withContext(Dispatchers.IO) {
                LocalBackup.exportToDownloads(
                    context = context,
                    db = app.database,
                    notifyStore = app.notificationStore,
                    accountId = accountId,
                )
            }
            if (downloads.isSuccess) {
                busy = false
                snackbar.showSnackbar(savedMsg)
            } else {
                // Fallback: let the user pick a save location (SAF).
                busy = false
                createDoc.launch(LocalBackup.suggestedFileName())
            }
        }
    }

    fun confirmRestore() {
        val uri = pendingRestoreUri ?: return
        pendingRestoreUri = null
        if (accountId <= 0L) {
            scope.launch { snackbar.showSnackbar(noAccountMsg) }
            return
        }
        busy = true
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                LocalBackup.restoreFromUri(
                    context = context,
                    db = app.database,
                    notifyStore = app.notificationStore,
                    accountId = accountId,
                    uri = uri,
                )
            }
            busy = false
            snackbar.showSnackbar(
                if (result.isSuccess) restoreCompleteMsg else restoreFailedMsg,
            )
        }
    }

    if (pendingRestoreUri != null) {
        AlertDialog(
            onDismissRequest = { if (!busy) pendingRestoreUri = null },
            title = { Text(stringResource(R.string.restore_confirm_title)) },
            text = { Text(stringResource(R.string.restore_confirm_body)) },
            confirmButton = {
                TextButton(
                    onClick = { confirmRestore() },
                    enabled = !busy,
                ) {
                    Text(stringResource(R.string.restore))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { pendingRestoreUri = null },
                    enabled = !busy,
                ) {
                    Text(stringResource(R.string.restore_dialog_cancel))
                }
            },
        )
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
            title = stringResource(R.string.backup_locally),
            subtitle = stringResource(R.string.local),
            buttonLabel = stringResource(R.string.backup),
            enabled = !busy && accountId > 0L,
            onClick = { runLocalBackup() },
        )
        HorizontalDivider()
        ShellActionRow(
            icon = Icons.Outlined.Cloud,
            title = stringResource(R.string.backup),
            subtitle = stringResource(R.string.drive) + " · " + stringResource(R.string.coming_soon),
            buttonLabel = stringResource(R.string.backup),
            enabled = !busy,
            onClick = ::showSoon,
        )
        HorizontalDivider()
        ShellActionRow(
            icon = Icons.Outlined.Storage,
            title = stringResource(R.string.restore_from_local),
            subtitle = stringResource(R.string.local),
            buttonLabel = stringResource(R.string.restore),
            enabled = !busy && accountId > 0L,
            onClick = {
                openDoc.launch(arrayOf(LocalBackup.MIME_JSON, "application/*", "text/*", "*/*"))
            },
        )
        HorizontalDivider()
        ShellActionRow(
            icon = Icons.Outlined.Cloud,
            title = stringResource(R.string.restore),
            subtitle = stringResource(R.string.drive) + " · " + stringResource(R.string.coming_soon),
            buttonLabel = stringResource(R.string.restore),
            enabled = !busy,
            onClick = ::showSoon,
        )
        if (busy) {
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator()
            }
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
    enabled: Boolean = true,
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
        Button(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
            Text(buttonLabel)
        }
    }
}
