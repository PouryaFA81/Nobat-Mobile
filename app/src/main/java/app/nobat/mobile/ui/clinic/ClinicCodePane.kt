package app.nobat.mobile.ui.clinic

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import app.nobat.mobile.R
import app.nobat.mobile.data.AccountRole
import app.nobat.mobile.locale.AppLocale
import app.nobat.mobile.notify.ClinicCode
import app.nobat.mobile.notify.ClinicSettings
import app.nobat.mobile.notify.ClinicSettingsStore
import app.nobat.mobile.notify.ClinicSubscribe
import kotlinx.coroutines.launch

private const val CLINIC_SETUP_GUIDE_URL_EN =
    "https://github.com/PouryaFA81/Nobat-Mobile/blob/main/docs/CLINIC-NOTIFICATIONS.md"
private const val CLINIC_SETUP_GUIDE_URL_FA =
    "https://github.com/PouryaFA81/Nobat-Mobile/blob/main/docs/CLINIC-NOTIFICATIONS.fa.md"

@Composable
fun ClinicCodePane(
    accountId: Long,
    role: String,
    store: ClinicSettingsStore,
    snackbar: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var loaded by remember { mutableStateOf(false) }
    var baseUrl by remember { mutableStateOf("") }
    var topic by remember { mutableStateOf("") }
    var token by remember { mutableStateOf("") }
    var connected by remember { mutableStateOf(false) }
    var pasteCode by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }

    val savedMsg = stringResource(R.string.saved)
    val invalidMsg = stringResource(R.string.invalid_clinic_code)
    val isStaff = AccountRole.isStaff(role)

    fun reload() {
        val s = store.load(accountId)
        baseUrl = s.baseUrl
        topic = s.topic
        token = s.token
        connected = s.connected && s.isConfigured()
        loaded = true
    }

    LaunchedEffect(accountId) {
        reload()
    }

    fun currentAdminSettings(connectedFlag: Boolean) = ClinicSettings(
        baseUrl = baseUrl,
        topic = topic,
        token = token,
        connected = connectedFlag,
        lastMessageId = store.load(accountId).lastMessageId,
    )

    if (!loaded) {
        Column(
            modifier = modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            CircularProgressIndicator()
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.clinic_notifications),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            if (connected) {
                Text(
                    text = stringResource(R.string.connected),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    val url = if (AppLocale.isPersian(context)) {
                        CLINIC_SETUP_GUIDE_URL_FA
                    } else {
                        CLINIC_SETUP_GUIDE_URL_EN
                    }
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.setup_guide),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
            )
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        }

        HorizontalDivider()

        if (isStaff) {
            StaffClinicSection(
                pasteCode = pasteCode,
                onPasteChange = { pasteCode = it },
                connected = connected,
                saving = saving,
                onConnect = {
                    if (saving) return@StaffClinicSection
                    saving = true
                    scope.launch {
                        val decoded = ClinicCode.decode(pasteCode)
                        if (decoded == null) {
                            saving = false
                            snackbar.showSnackbar(invalidMsg)
                            return@launch
                        }
                        store.save(
                            accountId,
                            decoded.copy(
                                connected = true,
                                lastMessageId = store.load(accountId).lastMessageId,
                            ),
                        )
                        ClinicSubscribe.restartLive(context)
                        reload()
                        pasteCode = ""
                        saving = false
                        snackbar.showSnackbar(savedMsg)
                    }
                },
                onDisconnect = {
                    if (saving) return@StaffClinicSection
                    saving = true
                    scope.launch {
                        store.clear(accountId)
                        ClinicSubscribe.restartLive(context)
                        reload()
                        saving = false
                        snackbar.showSnackbar(savedMsg)
                    }
                },
            )
        } else {
            AdminClinicSection(
                baseUrl = baseUrl,
                topic = topic,
                token = token,
                connected = connected,
                saving = saving,
                onUrl = { baseUrl = it },
                onTopic = { topic = it },
                onToken = { token = it },
                onSave = {
                    if (saving) return@AdminClinicSection
                    saving = true
                    scope.launch {
                        val s = currentAdminSettings(connectedFlag = true)
                        if (!s.isConfigured()) {
                            saving = false
                            snackbar.showSnackbar(invalidMsg)
                            return@launch
                        }
                        store.save(accountId, s.copy(connected = true))
                        reload()
                        saving = false
                        snackbar.showSnackbar(savedMsg)
                    }
                },
                onShare = {
                    val code = ClinicCode.encode(currentAdminSettings(true))
                    if (code == null) {
                        scope.launch { snackbar.showSnackbar(invalidMsg) }
                        return@AdminClinicSection
                    }
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, code)
                    }
                    context.startActivity(
                        Intent.createChooser(send, context.getString(R.string.share_code)),
                    )
                },
                onRegenerate = {
                    if (saving) return@AdminClinicSection
                    saving = true
                    scope.launch {
                        val existing = store.load(accountId)
                        val next = ClinicSettings(
                            baseUrl = existing.baseUrl.ifBlank { baseUrl },
                            topic = ClinicCode.suggestTopic(),
                            token = "",
                            connected = false,
                            lastMessageId = "",
                        )
                        store.save(accountId, next)
                        reload()
                        saving = false
                        snackbar.showSnackbar(savedMsg)
                    }
                },
            )
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun StaffClinicSection(
    pasteCode: String,
    onPasteChange: (String) -> Unit,
    connected: Boolean,
    saving: Boolean,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
) {
    Text(
        text = stringResource(R.string.enter_clinic_code),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    OutlinedTextField(
        value = pasteCode,
        onValueChange = onPasteChange,
        label = { Text(stringResource(R.string.clinic_code)) },
        singleLine = false,
        minLines = 2,
        textStyle = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Ltr),
        modifier = Modifier.fillMaxWidth(),
        enabled = !saving,
    )
    Button(
        onClick = onConnect,
        enabled = !saving && pasteCode.isNotBlank(),
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
    ) {
        Text(stringResource(R.string.connect))
    }
    if (connected) {
        OutlinedButton(
            onClick = onDisconnect,
            enabled = !saving,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
        ) {
            Text(stringResource(R.string.disconnect))
        }
    }
}

@Composable
private fun AdminClinicSection(
    baseUrl: String,
    topic: String,
    token: String,
    connected: Boolean,
    saving: Boolean,
    onUrl: (String) -> Unit,
    onTopic: (String) -> Unit,
    onToken: (String) -> Unit,
    onSave: () -> Unit,
    onShare: () -> Unit,
    onRegenerate: () -> Unit,
) {
    val code = remember(baseUrl, topic, token) {
        ClinicCode.encode(
            ClinicSettings(baseUrl = baseUrl, topic = topic, token = token, connected = true),
        )
    }

    if (connected && code != null) {
        Text(
            text = stringResource(R.string.clinic_code),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = code,
            onValueChange = {},
            readOnly = true,
            singleLine = false,
            minLines = 2,
            textStyle = MaterialTheme.typography.bodySmall.copy(textDirection = TextDirection.Ltr),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedButton(
            onClick = onShare,
            enabled = !saving,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
        ) {
            Text(stringResource(R.string.share_code))
        }
        OutlinedButton(
            onClick = onRegenerate,
            enabled = !saving,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
        ) {
            Text(stringResource(R.string.regenerate))
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
    }

    Text(
        text = stringResource(R.string.relay),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    OutlinedTextField(
        value = baseUrl,
        onValueChange = onUrl,
        label = { Text(stringResource(R.string.relay_url)) },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Ltr),
        modifier = Modifier.fillMaxWidth(),
        enabled = !saving,
    )
    OutlinedTextField(
        value = topic,
        onValueChange = onTopic,
        label = { Text(stringResource(R.string.relay_topic)) },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Ltr),
        modifier = Modifier.fillMaxWidth(),
        enabled = !saving,
    )
    OutlinedTextField(
        value = token,
        onValueChange = onToken,
        label = { Text(stringResource(R.string.relay_token)) },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        textStyle = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Ltr),
        modifier = Modifier.fillMaxWidth(),
        enabled = !saving,
    )
    Button(
        onClick = onSave,
        enabled = !saving && baseUrl.isNotBlank() && topic.isNotBlank() && token.isNotBlank(),
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
    ) {
        Text(stringResource(R.string.notifications_save))
    }
}
