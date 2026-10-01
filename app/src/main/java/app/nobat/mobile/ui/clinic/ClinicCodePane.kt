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
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
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

/** Admin setup path before Connected. */
private enum class AdminPath {
    PICK,
    RELAY_CODE,
    OWN_RELAY,
}

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
    var hosted by remember { mutableStateOf(false) }
    var pasteCode by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    var adminPath by remember { mutableStateOf(AdminPath.PICK) }
    var purchaseUrl by remember { mutableStateOf("") }

    val savedMsg = stringResource(R.string.saved)
    val invalidMsg = stringResource(R.string.invalid_clinic_code)
    val invalidOrExpiredMsg = stringResource(R.string.invalid_or_expired_code)
    val purchaseUrlUnsetMsg = stringResource(R.string.relay_purchase_url_unset)
    val isStaff = AccountRole.isStaff(role)

    fun reload() {
        val s = store.load(accountId)
        baseUrl = s.baseUrl
        topic = s.topic
        token = s.token
        connected = s.connected && s.isConfigured()
        hosted = s.hosted
        purchaseUrl = store.getRelayPurchaseUrl()
        if (!connected) {
            adminPath = AdminPath.PICK
        }
        loaded = true
    }

    LaunchedEffect(accountId) {
        reload()
    }

    fun currentAdminSettings(connectedFlag: Boolean, hostedFlag: Boolean = false) = ClinicSettings(
        baseUrl = baseUrl,
        topic = topic,
        token = token,
        connected = connectedFlag,
        lastMessageId = store.load(accountId).lastMessageId,
        hosted = hostedFlag,
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
                text = if (connected && hosted) {
                    stringResource(R.string.hosted_clinic_notifications)
                } else {
                    stringResource(R.string.clinic_notifications)
                },
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

        if (connected && hosted) {
            AssistChip(
                onClick = {},
                enabled = false,
                label = { Text(stringResource(R.string.hosted_chip)) },
            )
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
                                hosted = false,
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
            AdminClinicHub(
                connected = connected,
                hosted = hosted,
                adminPath = adminPath,
                onPath = { adminPath = it },
                baseUrl = baseUrl,
                topic = topic,
                token = token,
                pasteCode = pasteCode,
                purchaseUrl = purchaseUrl,
                saving = saving,
                onUrl = { baseUrl = it },
                onTopic = { topic = it },
                onToken = { token = it },
                onPasteChange = { pasteCode = it },
                onPurchaseUrl = { purchaseUrl = it },
                onSavePurchaseUrl = {
                    store.setRelayPurchaseUrl(purchaseUrl)
                    scope.launch { snackbar.showSnackbar(savedMsg) }
                },
                onRedeem = {
                    if (saving) return@AdminClinicHub
                    saving = true
                    scope.launch {
                        val decoded = ClinicCode.decode(pasteCode, forceHosted = true)
                        if (decoded == null || !decoded.isConfigured()) {
                            saving = false
                            snackbar.showSnackbar(invalidOrExpiredMsg)
                            return@launch
                        }
                        store.save(
                            accountId,
                            decoded.copy(
                                connected = true,
                                hosted = true,
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
                onGetRelayCode = {
                    val url = store.getRelayPurchaseUrl().ifBlank { purchaseUrl.trim() }
                    if (url.isBlank()) {
                        scope.launch { snackbar.showSnackbar(purchaseUrlUnsetMsg) }
                        return@AdminClinicHub
                    }
                    try {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    } catch (_: Exception) {
                        scope.launch { snackbar.showSnackbar(purchaseUrlUnsetMsg) }
                    }
                },
                onSaveOwn = {
                    if (saving) return@AdminClinicHub
                    saving = true
                    scope.launch {
                        store.setRelayPurchaseUrl(purchaseUrl)
                        val s = currentAdminSettings(connectedFlag = true, hostedFlag = false)
                        if (!s.isConfigured()) {
                            saving = false
                            snackbar.showSnackbar(invalidMsg)
                            return@launch
                        }
                        store.save(accountId, s.copy(connected = true, hosted = false))
                        reload()
                        saving = false
                        snackbar.showSnackbar(savedMsg)
                    }
                },
                onShare = {
                    val s = store.load(accountId)
                    val code = ClinicCode.encode(s)
                    if (code == null) {
                        scope.launch { snackbar.showSnackbar(invalidMsg) }
                        return@AdminClinicHub
                    }
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, code)
                    }
                    context.startActivity(
                        Intent.createChooser(send, context.getString(R.string.share_code)),
                    )
                },
                onDisconnect = {
                    if (saving) return@AdminClinicHub
                    saving = true
                    scope.launch {
                        store.clear(accountId)
                        ClinicSubscribe.restartLive(context)
                        reload()
                        saving = false
                        snackbar.showSnackbar(savedMsg)
                    }
                },
                onRegenerate = {
                    if (saving || hosted) return@AdminClinicHub
                    saving = true
                    scope.launch {
                        val existing = store.load(accountId)
                        val next = ClinicSettings(
                            baseUrl = existing.baseUrl.ifBlank { baseUrl },
                            topic = ClinicCode.suggestTopic(),
                            token = "",
                            connected = false,
                            lastMessageId = "",
                            hosted = false,
                        )
                        store.save(accountId, next)
                        reload()
                        adminPath = AdminPath.OWN_RELAY
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
private fun AdminClinicHub(
    connected: Boolean,
    hosted: Boolean,
    adminPath: AdminPath,
    onPath: (AdminPath) -> Unit,
    baseUrl: String,
    topic: String,
    token: String,
    pasteCode: String,
    purchaseUrl: String,
    saving: Boolean,
    onUrl: (String) -> Unit,
    onTopic: (String) -> Unit,
    onToken: (String) -> Unit,
    onPasteChange: (String) -> Unit,
    onPurchaseUrl: (String) -> Unit,
    onSavePurchaseUrl: () -> Unit,
    onRedeem: () -> Unit,
    onGetRelayCode: () -> Unit,
    onSaveOwn: () -> Unit,
    onShare: () -> Unit,
    onDisconnect: () -> Unit,
    onRegenerate: () -> Unit,
) {
    val code = remember(baseUrl, topic, token, connected, hosted) {
        if (!connected) null
        else ClinicCode.encode(
            ClinicSettings(
                baseUrl = baseUrl,
                topic = topic,
                token = token,
                connected = true,
                hosted = hosted,
            ),
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
        if (!hosted) {
            OutlinedButton(
                onClick = onRegenerate,
                enabled = !saving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
            ) {
                Text(stringResource(R.string.regenerate))
            }
        }
        OutlinedButton(
            onClick = onDisconnect,
            enabled = !saving,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
        ) {
            Text(stringResource(R.string.disconnect))
        }
        // Hosted path: never show real host / topic / token.
        if (!hosted) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            OwnRelayFields(
                baseUrl = baseUrl,
                topic = topic,
                token = token,
                purchaseUrl = purchaseUrl,
                saving = saving,
                onUrl = onUrl,
                onTopic = onTopic,
                onToken = onToken,
                onPurchaseUrl = onPurchaseUrl,
                onSavePurchaseUrl = onSavePurchaseUrl,
                onSave = onSaveOwn,
            )
        }
        return
    }

    // Not connected — path picker / dual setup
    when (adminPath) {
        AdminPath.PICK -> {
            Text(
                text = stringResource(R.string.choose_clinic_path),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = { onPath(AdminPath.RELAY_CODE) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
            ) {
                Text(stringResource(R.string.relay_code))
            }
            OutlinedButton(
                onClick = { onPath(AdminPath.OWN_RELAY) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
            ) {
                Text(stringResource(R.string.your_own_relay))
            }
        }
        AdminPath.RELAY_CODE -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = true,
                    onClick = {},
                    label = { Text(stringResource(R.string.relay_code)) },
                )
                FilterChip(
                    selected = false,
                    onClick = { onPath(AdminPath.OWN_RELAY) },
                    label = { Text(stringResource(R.string.your_own_relay)) },
                )
            }
            Text(
                text = stringResource(R.string.i_have_a_code),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = pasteCode,
                onValueChange = onPasteChange,
                label = { Text(stringResource(R.string.relay_code)) },
                singleLine = false,
                minLines = 2,
                textStyle = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Ltr),
                modifier = Modifier.fillMaxWidth(),
                enabled = !saving,
            )
            Button(
                onClick = onRedeem,
                enabled = !saving && pasteCode.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
            ) {
                Text(stringResource(R.string.redeem))
            }
            OutlinedButton(
                onClick = onGetRelayCode,
                enabled = !saving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
            ) {
                Text(stringResource(R.string.get_a_relay_code))
            }
            OutlinedButton(
                onClick = { onPath(AdminPath.PICK) },
                enabled = !saving,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.back))
            }
        }
        AdminPath.OWN_RELAY -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = false,
                    onClick = { onPath(AdminPath.RELAY_CODE) },
                    label = { Text(stringResource(R.string.relay_code)) },
                )
                FilterChip(
                    selected = true,
                    onClick = {},
                    label = { Text(stringResource(R.string.your_own_relay)) },
                )
            }
            OwnRelayFields(
                baseUrl = baseUrl,
                topic = topic,
                token = token,
                purchaseUrl = purchaseUrl,
                saving = saving,
                onUrl = onUrl,
                onTopic = onTopic,
                onToken = onToken,
                onPurchaseUrl = onPurchaseUrl,
                onSavePurchaseUrl = onSavePurchaseUrl,
                onSave = onSaveOwn,
            )
            OutlinedButton(
                onClick = { onPath(AdminPath.PICK) },
                enabled = !saving,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.back))
            }
        }
    }
}

@Composable
private fun OwnRelayFields(
    baseUrl: String,
    topic: String,
    token: String,
    purchaseUrl: String,
    saving: Boolean,
    onUrl: (String) -> Unit,
    onTopic: (String) -> Unit,
    onToken: (String) -> Unit,
    onPurchaseUrl: (String) -> Unit,
    onSavePurchaseUrl: () -> Unit,
    onSave: () -> Unit,
) {
    Text(
        text = stringResource(R.string.your_own_relay),
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
        Text(stringResource(R.string.save_and_create_clinic_code))
    }
    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
    Text(
        text = stringResource(R.string.relay_advanced),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    OutlinedTextField(
        value = purchaseUrl,
        onValueChange = onPurchaseUrl,
        label = { Text(stringResource(R.string.relay_purchase_url)) },
        placeholder = { Text("https://example.com/relay") },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Ltr),
        modifier = Modifier.fillMaxWidth(),
        enabled = !saving,
    )
    OutlinedButton(
        onClick = onSavePurchaseUrl,
        enabled = !saving,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
    ) {
        Text(stringResource(R.string.notifications_save))
    }
}
