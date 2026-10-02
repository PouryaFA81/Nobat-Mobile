package app.nobat.mobile.ui.shell

import android.net.Uri
import app.nobat.mobile.notify.TelegramSettingsStore
import app.nobat.mobile.notify.TelegramSettings
import app.nobat.mobile.notify.TelegramClient
import app.nobat.mobile.notify.BaleSettingsStore
import app.nobat.mobile.notify.BaleSettings
import app.nobat.mobile.notify.BaleClient
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.Switch
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.clickable
import android.content.Intent
import java.util.Locale
import java.time.format.DateTimeFormatter
import java.time.YearMonth
import java.time.LocalDate
import app.nobat.mobile.report.ReportPdf
import app.nobat.mobile.locale.AppLocale
import app.nobat.mobile.calendar.Jalali
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.FilterChip
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.foundation.layout.widthIn
import android.app.DatePickerDialog
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

private const val TELEGRAM_SETUP_GUIDE_URL_EN =
    "https://github.com/PouryaFA81/Nobat-Mobile/blob/main/docs/TELEGRAM.md"
private const val TELEGRAM_SETUP_GUIDE_URL_FA =
    "https://github.com/PouryaFA81/Nobat-Mobile/blob/main/docs/TELEGRAM.fa.md"
private const val BALE_SETUP_GUIDE_URL_EN =
    "https://github.com/PouryaFA81/Nobat-Mobile/blob/main/docs/BALE.md"
private const val BALE_SETUP_GUIDE_URL_FA =
    "https://github.com/PouryaFA81/Nobat-Mobile/blob/main/docs/BALE.fa.md"

@Composable
fun IntegrationsPane(
    snackbar: SnackbarHostState,
    accountId: Long,
    telegramStore: TelegramSettingsStore,
    baleStore: BaleSettingsStore,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val soon = stringResource(R.string.coming_soon)
    val savedMsg = stringResource(R.string.saved)
    val secureStorageFailedMsg = stringResource(R.string.secure_storage_failed)
    val sendOkMsg = stringResource(R.string.send_ok)
    val failedMsg = stringResource(R.string.send_failed)
    val testBody = stringResource(R.string.telegram_test_body)
    val baleTestBody = stringResource(R.string.bale_test_body)

    var loaded by remember { mutableStateOf(false) }
    var botToken by remember { mutableStateOf("") }
    var chatId by remember { mutableStateOf("") }
    var notifyOnBook by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var testing by remember { mutableStateOf(false) }

    var baleLoaded by remember { mutableStateOf(false) }
    var baleBotToken by remember { mutableStateOf("") }
    var baleChatId by remember { mutableStateOf("") }
    var baleNotifyOnBook by remember { mutableStateOf(false) }
    var baleSaving by remember { mutableStateOf(false) }
    var baleTesting by remember { mutableStateOf(false) }

    fun showSoon() {
        scope.launch { snackbar.showSnackbar(soon) }
    }

    LaunchedEffect(accountId) {
        if (accountId <= 0L) {
            botToken = ""
            chatId = ""
            notifyOnBook = false
            loaded = true
            baleBotToken = ""
            baleChatId = ""
            baleNotifyOnBook = false
            baleLoaded = true
            return@LaunchedEffect
        }
        val s = telegramStore.load(accountId)
        botToken = s.botToken
        chatId = s.chatId
        notifyOnBook = s.notifyOnBook
        loaded = true
        val b = baleStore.load(accountId)
        baleBotToken = b.botToken
        baleChatId = b.chatId
        baleNotifyOnBook = b.notifyOnBook
        baleLoaded = true
    }

    fun currentSettings() = TelegramSettings(
        botToken = botToken,
        chatId = chatId,
        notifyOnBook = notifyOnBook,
    )

    fun currentBaleSettings() = BaleSettings(
        botToken = baleBotToken,
        chatId = baleChatId,
        notifyOnBook = baleNotifyOnBook,
    )

    val configured = botToken.isNotBlank() && chatId.isNotBlank()
    val baleConfigured = baleBotToken.isNotBlank() && baleChatId.isNotBlank()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // —— Telegram (live) ——
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    stringResource(R.string.telegram),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                if (configured) {
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
                            TELEGRAM_SETUP_GUIDE_URL_FA
                        } else {
                            TELEGRAM_SETUP_GUIDE_URL_EN
                        }
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.telegram_setup_guide),
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

            if (!loaded) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            } else {
                OutlinedTextField(
                    value = botToken,
                    onValueChange = { botToken = it },
                    label = { Text(stringResource(R.string.bot_token)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Ltr),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = accountId > 0L,
                )
                OutlinedTextField(
                    value = chatId,
                    onValueChange = { chatId = it },
                    label = { Text(stringResource(R.string.chat_id)) },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Ltr),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = accountId > 0L,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(R.string.notify_on_book),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(
                        checked = notifyOnBook,
                        onCheckedChange = { notifyOnBook = it },
                        enabled = accountId > 0L,
                    )
                }
                Button(
                    onClick = {
                        if (saving || accountId <= 0L) return@Button
                        saving = true
                        scope.launch {
                            val saved = telegramStore.save(accountId, currentSettings())
                            saving = false
                            snackbar.showSnackbar(
                                if (saved.isSuccess) savedMsg else secureStorageFailedMsg,
                            )
                        }
                    },
                    enabled = !saving && !testing && accountId > 0L,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                ) {
                    Text(stringResource(R.string.notifications_save))
                }
                OutlinedButton(
                    onClick = {
                        if (testing || accountId <= 0L) return@OutlinedButton
                        testing = true
                        scope.launch {
                            val s = currentSettings()
                            val saved = telegramStore.save(accountId, s)
                            if (saved.isFailure) {
                                testing = false
                                snackbar.showSnackbar(secureStorageFailedMsg)
                                return@launch
                            }
                            val result = TelegramClient.sendMessage(
                                botToken = s.botToken,
                                chatId = s.chatId,
                                text = testBody,
                            )
                            testing = false
                            snackbar.showSnackbar(
                                if (result.isSuccess) sendOkMsg else failedMsg,
                            )
                        }
                    },
                    enabled = !saving && !testing && configured && accountId > 0L,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                ) {
                    if (testing) {
                        CircularProgressIndicator(
                            modifier = Modifier.height(20.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text(stringResource(R.string.test_send))
                    }
                }
            }
        }

        HorizontalDivider()

        // —— Bale (live) ——
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    stringResource(R.string.bale),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                if (baleConfigured) {
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
                            BALE_SETUP_GUIDE_URL_FA
                        } else {
                            BALE_SETUP_GUIDE_URL_EN
                        }
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.bale_setup_guide),
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

            if (!baleLoaded) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            } else {
                OutlinedTextField(
                    value = baleBotToken,
                    onValueChange = { baleBotToken = it },
                    label = { Text(stringResource(R.string.bot_token)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Ltr),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = accountId > 0L,
                )
                OutlinedTextField(
                    value = baleChatId,
                    onValueChange = { baleChatId = it },
                    label = { Text(stringResource(R.string.chat_id)) },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Ltr),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = accountId > 0L,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(R.string.notify_on_book),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(
                        checked = baleNotifyOnBook,
                        onCheckedChange = { baleNotifyOnBook = it },
                        enabled = accountId > 0L,
                    )
                }
                Button(
                    onClick = {
                        if (baleSaving || accountId <= 0L) return@Button
                        baleSaving = true
                        scope.launch {
                            val saved = baleStore.save(accountId, currentBaleSettings())
                            baleSaving = false
                            snackbar.showSnackbar(
                                if (saved.isSuccess) savedMsg else secureStorageFailedMsg,
                            )
                        }
                    },
                    enabled = !baleSaving && !baleTesting && accountId > 0L,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                ) {
                    Text(stringResource(R.string.notifications_save))
                }
                OutlinedButton(
                    onClick = {
                        if (baleTesting || accountId <= 0L) return@OutlinedButton
                        baleTesting = true
                        scope.launch {
                            val s = currentBaleSettings()
                            val saved = baleStore.save(accountId, s)
                            if (saved.isFailure) {
                                baleTesting = false
                                snackbar.showSnackbar(secureStorageFailedMsg)
                                return@launch
                            }
                            val result = BaleClient.sendMessage(
                                botToken = s.botToken,
                                chatId = s.chatId,
                                text = baleTestBody,
                            )
                            baleTesting = false
                            snackbar.showSnackbar(
                                if (result.isSuccess) sendOkMsg else failedMsg,
                            )
                        }
                    },
                    enabled = !baleSaving && !baleTesting && baleConfigured && accountId > 0L,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                ) {
                    if (baleTesting) {
                        CircularProgressIndicator(
                            modifier = Modifier.height(20.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text(stringResource(R.string.test_send))
                    }
                }
            }
        }

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
            Text(stringResource(R.string.connect) + " · " + stringResource(R.string.coming_soon))
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
    accountId: Long,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val app = context.applicationContext as NobatApp
    val scope = rememberCoroutineScope()
    val persian = AppLocale.isPersian(context)
    val emptyMsg = stringResource(R.string.report_empty)
    val failedMsg = stringResource(R.string.report_failed)
    val noAccountMsg = stringResource(R.string.report_no_account)

    var modeDay by remember { mutableStateOf(true) }
    var selectedDay by remember { mutableStateOf(LocalDate.now()) }
    var selectedMonthAnchor by remember { mutableStateOf(LocalDate.now()) }
    var busy by remember { mutableStateOf(false) }
    var showMonthPicker by remember { mutableStateOf(false) }

    val rangeLabel = remember(modeDay, selectedDay, selectedMonthAnchor, persian) {
        if (modeDay) {
            ReportPdf.dayRange(selectedDay, persian).label
        } else {
            ReportPdf.monthRange(selectedMonthAnchor, persian).label
        }
    }

    fun currentRange(): ReportPdf.Range =
        if (modeDay) ReportPdf.dayRange(selectedDay, persian)
        else ReportPdf.monthRange(selectedMonthAnchor, persian)

    fun openDayPicker() {
        val d = selectedDay
        DatePickerDialog(
            context,
            { _, y, m, day -> selectedDay = LocalDate.of(y, m + 1, day) },
            d.year,
            d.monthValue - 1,
            d.dayOfMonth,
        ).show()
    }

    data class PdfResult(val file: java.io.File, val count: Int)

    suspend fun buildPdf(): Result<PdfResult> {
        if (accountId <= 0L) return Result.failure(IllegalStateException("no account"))
        val range = currentRange()
        val start = range.start.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val end = range.end.format(DateTimeFormatter.ISO_LOCAL_DATE)
        return withContext(Dispatchers.IO) {
            runCatching {
                val appts = app.database.appointments().between(accountId, start, end)
                val personnel = app.database.personnel().listForAccount(accountId)
                    .associateBy { it.id }
                val file = ReportPdf.write(
                    context = context,
                    appointments = appts,
                    personnelById = personnel,
                    range = range,
                    labels = ReportPdf.labels(context),
                )
                PdfResult(file, appts.size)
            }
        }
    }

    fun runShare() {
        if (accountId <= 0L) {
            scope.launch { snackbar.showSnackbar(noAccountMsg) }
            return
        }
        busy = true
        scope.launch {
            val result = buildPdf()
            busy = false
            result.fold(
                onSuccess = { pdf ->
                    if (pdf.count == 0) {
                        snackbar.showSnackbar(emptyMsg)
                    } else {
                        ReportPdf.share(context, pdf.file)
                    }
                },
                onFailure = { snackbar.showSnackbar(failedMsg) },
            )
        }
    }

    fun runPrint() {
        if (accountId <= 0L) {
            scope.launch { snackbar.showSnackbar(noAccountMsg) }
            return
        }
        busy = true
        scope.launch {
            val result = buildPdf()
            busy = false
            result.fold(
                onSuccess = { pdf ->
                    if (pdf.count == 0) {
                        snackbar.showSnackbar(emptyMsg)
                    } else {
                        try {
                            ReportPdf.print(
                                context,
                                pdf.file,
                                context.getString(R.string.print_pdf),
                            )
                        } catch (_: Exception) {
                            ReportPdf.share(context, pdf.file)
                        }
                    }
                },
                onFailure = { snackbar.showSnackbar(failedMsg) },
            )
        }
    }

    if (showMonthPicker) {
        AlertDialog(
            onDismissRequest = { showMonthPicker = false },
            title = { Text(stringResource(R.string.report_pick_month)) },
            text = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    TextButton(
                        onClick = {
                            selectedMonthAnchor = if (persian) {
                                Jalali.plusMonths(selectedMonthAnchor, -1)
                            } else {
                                YearMonth.from(selectedMonthAnchor).minusMonths(1).atDay(1)
                            }
                        },
                    ) { Text("‹") }
                    Text(
                        text = if (persian) {
                            Jalali.monthLabel(selectedMonthAnchor)
                        } else {
                            YearMonth.from(selectedMonthAnchor)
                                .format(DateTimeFormatter.ofPattern("yyyy-MM", Locale.US))
                        },
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.widthIn(min = 120.dp),
                    )
                    TextButton(
                        onClick = {
                            selectedMonthAnchor = if (persian) {
                                Jalali.plusMonths(selectedMonthAnchor, 1)
                            } else {
                                YearMonth.from(selectedMonthAnchor).plusMonths(1).atDay(1)
                            }
                        },
                    ) { Text("›") }
                }
            },
            confirmButton = {
                TextButton(onClick = { showMonthPicker = false }) {
                    Text(stringResource(R.string.back))
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
            text = stringResource(R.string.reports),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilterChip(
                selected = modeDay,
                onClick = { modeDay = true },
                label = { Text(stringResource(R.string.report_range_day)) },
                leadingIcon = {
                    Icon(Icons.Outlined.CalendarToday, contentDescription = null)
                },
            )
            FilterChip(
                selected = !modeDay,
                onClick = { modeDay = false },
                label = { Text(stringResource(R.string.report_range_month)) },
                leadingIcon = {
                    Icon(Icons.Outlined.CalendarMonth, contentDescription = null)
                },
            )
        }

        Text(
            text = stringResource(R.string.report_selected) + ": " + rangeLabel,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (modeDay) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = { selectedDay = LocalDate.now() },
                    modifier = Modifier.weight(1f),
                    enabled = !busy,
                ) { Text(stringResource(R.string.report_this_day)) }
                OutlinedButton(
                    onClick = { openDayPicker() },
                    modifier = Modifier.weight(1f),
                    enabled = !busy,
                ) { Text(stringResource(R.string.report_pick_day)) }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = { selectedMonthAnchor = LocalDate.now() },
                    modifier = Modifier.weight(1f),
                    enabled = !busy,
                ) { Text(stringResource(R.string.report_this_month)) }
                OutlinedButton(
                    onClick = { showMonthPicker = true },
                    modifier = Modifier.weight(1f),
                    enabled = !busy,
                ) { Text(stringResource(R.string.report_pick_month)) }
            }
        }

        HorizontalDivider()

        Button(
            onClick = { runPrint() },
            enabled = !busy && accountId > 0L,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Outlined.Print, contentDescription = null)
            Text("  " + stringResource(R.string.print_pdf))
        }
        Button(
            onClick = { runShare() },
            enabled = !busy && accountId > 0L,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Outlined.Share, contentDescription = null)
            Text("  " + stringResource(R.string.share))
        }

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
