package app.nobat.mobile.ui.account

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
import androidx.compose.material3.Icon
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
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
import app.nobat.mobile.locale.AppLocale
import app.nobat.mobile.digest.EveningDigestScheduler
import app.nobat.mobile.notify.NotificationSettings
import app.nobat.mobile.remind.ReminderScheduler
import app.nobat.mobile.notify.NotificationSettingsStore
import app.nobat.mobile.notify.SmsIntent
import app.nobat.mobile.notify.SmtpClient
import kotlinx.coroutines.launch

private const val SMTP_SETUP_GUIDE_URL_EN =
    "https://github.com/PouryaFA81/Nobat-Mobile/blob/main/docs/SMTP.md"
private const val SMTP_SETUP_GUIDE_URL_FA =
    "https://github.com/PouryaFA81/Nobat-Mobile/blob/main/docs/SMTP.fa.md"

@Composable
fun NotificationsPane(
    accountId: Long,
    store: NotificationSettingsStore,
    snackbar: SnackbarHostState,
    onSettingsSaved: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var loaded by remember { mutableStateOf(false) }
    var remindersOn by remember { mutableStateOf(false) }
    var eveningDigestOn by remember { mutableStateOf(false) }
    var digestTime by remember { mutableStateOf("20:00") }
    var host by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("587") }
    var useTls by remember { mutableStateOf(true) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var from by remember { mutableStateOf("") }
    var testTo by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    var testing by remember { mutableStateOf(false) }

    val savedMsg = stringResource(R.string.saved)
    val sendOkMsg = stringResource(R.string.send_ok)
    val failedMsg = stringResource(R.string.send_failed)
    val sampleBody = stringResource(R.string.sms_reminder_sample)
    val testSubject = stringResource(R.string.smtp_test_subject)
    val testBody = stringResource(R.string.smtp_test_body)
    val minutesSuffix = stringResource(R.string.minutes_suffix)
    val digestNeedsSmtpMsg = stringResource(R.string.digest_needs_smtp)

    LaunchedEffect(accountId) {
        val s = store.load(accountId)
        remindersOn = s.remindersOn
        eveningDigestOn = s.eveningDigestOn
        digestTime = s.digestTime
        host = s.smtpHost
        port = s.smtpPort.toString()
        useTls = s.smtpUseTls
        username = s.smtpUsername
        password = s.smtpPassword
        from = s.smtpFrom
        testTo = s.testRecipient
        loaded = true
    }

    fun currentSettings(): NotificationSettings = NotificationSettings(
        remindersOn = remindersOn,
        eveningDigestOn = eveningDigestOn,
        digestTime = digestTime,
        smtpHost = host,
        smtpPort = port.toIntOrNull() ?: 587,
        smtpUseTls = useTls,
        smtpUsername = username,
        smtpPassword = password,
        smtpFrom = from,
        testRecipient = testTo,
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
                text = stringResource(R.string.reminders_on),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Switch(checked = remindersOn, onCheckedChange = { remindersOn = it })
        }
        Text(
            text = stringResource(R.string.remind_1h_before),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        HorizontalDivider()

        Text(
            text = stringResource(R.string.evening_digest),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.evening_digest_on),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Switch(checked = eveningDigestOn, onCheckedChange = { eveningDigestOn = it })
        }
        OutlinedTextField(
            value = digestTime,
            onValueChange = { v ->
                // Allow HH:mm while typing (digits + one colon).
                if (v.length <= 5 && v.all { it.isDigit() || it == ':' }) {
                    digestTime = v
                }
            },
            label = { Text(stringResource(R.string.digest_time)) },
            singleLine = true,
            enabled = eveningDigestOn,
            textStyle = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Ltr),
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("20:00") },
        )
        Text(
            text = stringResource(R.string.digest_via_email),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        HorizontalDivider()

        Text(
            text = stringResource(R.string.email_smtp),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    val url = if (AppLocale.isPersian(context)) {
                        SMTP_SETUP_GUIDE_URL_FA
                    } else {
                        SMTP_SETUP_GUIDE_URL_EN
                    }
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.smtp_setup_guide),
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

        OutlinedTextField(
            value = host,
            onValueChange = { host = it },
            label = { Text(stringResource(R.string.smtp_host)) },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Ltr),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = port,
            onValueChange = { v ->
                if (v.isEmpty() || v.all { it.isDigit() }) port = v.take(5)
            },
            label = { Text(stringResource(R.string.smtp_port)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            textStyle = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Ltr),
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.smtp_use_tls),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Switch(checked = useTls, onCheckedChange = { useTls = it })
        }
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text(stringResource(R.string.smtp_username)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(stringResource(R.string.smtp_password)) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = from,
            onValueChange = { from = it },
            label = { Text(stringResource(R.string.smtp_from)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            textStyle = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Ltr),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = testTo,
            onValueChange = { testTo = it },
            label = { Text(stringResource(R.string.reminder_recipient)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            textStyle = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Ltr),
            modifier = Modifier.fillMaxWidth(),
        )

        Text(
            text = stringResource(R.string.send_confirmation_now),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Button(
            onClick = {
                if (saving) return@Button
                saving = true
                scope.launch {
                    val s = currentSettings()
                    store.save(accountId, s)
                    EveningDigestScheduler.schedule(context, accountId, s)
                    onSettingsSaved()
                    saving = false
                    if (s.eveningDigestOn && !ReminderScheduler.isSmtpConfigured(s)) {
                        snackbar.showSnackbar(digestNeedsSmtpMsg)
                    } else {
                        snackbar.showSnackbar(savedMsg)
                    }
                }
            },
            enabled = !saving && !testing,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
        ) {
            Text(stringResource(R.string.notifications_save))
        }

        OutlinedButton(
            onClick = {
                if (testing) return@OutlinedButton
                testing = true
                scope.launch {
                    store.save(accountId, currentSettings())
                    val s = currentSettings()
                    val result = SmtpClient.send(
                        SmtpClient.MailRequest(
                            host = s.smtpHost,
                            port = s.smtpPort,
                            useTls = s.smtpUseTls,
                            username = s.smtpUsername,
                            password = s.smtpPassword,
                            from = s.smtpFrom,
                            to = s.testRecipient,
                            subject = testSubject,
                            body = testBody,
                        ),
                    )
                    testing = false
                    if (result.isSuccess) {
                        snackbar.showSnackbar(sendOkMsg)
                    } else {
                        snackbar.showSnackbar(failedMsg)
                    }
                }
            },
            enabled = !saving && !testing && host.isNotBlank() && testTo.isNotBlank(),
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

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        Text(
            text = stringResource(R.string.sms_section),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(R.string.sms_intent_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        OutlinedButton(
            onClick = {
                val body = SmsIntent.reminderBody(
                    initials = "A.M.",
                    date = "2026-10-01",
                    time = "10:00",
                    durationMin = 60,
                    minutesSuffix = minutesSuffix,
                    template = sampleBody,
                )
                val ok = SmsIntent.openComposer(context, body)
                if (!ok) {
                    scope.launch { snackbar.showSnackbar(failedMsg) }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
        ) {
            Text(stringResource(R.string.sms_send_via_device))
        }

        Spacer(Modifier.height(24.dp))
    }
}
