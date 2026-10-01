package app.nobat.mobile.ui.security

import androidx.biometric.BiometricManager
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import app.nobat.mobile.R
import app.nobat.mobile.security.AppLockStore
import kotlinx.coroutines.launch

@Composable
fun SecurityPane(
    store: AppLockStore,
    snackbar: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pinEnabled by remember { mutableStateOf(store.isPinEnabled()) }
    var biometricEnabled by remember { mutableStateOf(store.isBiometricEnabled()) }
    var showSetPin by remember { mutableStateOf(false) }
    var showChangePin by remember { mutableStateOf(false) }
    var showDisablePin by remember { mutableStateOf(false) }

    val biometricAvailable = remember {
        val bm = BiometricManager.from(context)
        val can = bm.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)
        can == BiometricManager.BIOMETRIC_SUCCESS
    }

    val savedMsg = stringResource(R.string.saved)
    val wrongPin = stringResource(R.string.wrong_pin)
    val bioUnavailable = stringResource(R.string.biometric_unavailable)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 8.dp),
    ) {
        Text(
            text = stringResource(R.string.app_lock),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                Text(stringResource(R.string.pin), style = MaterialTheme.typography.bodyLarge)
                Text(
                    stringResource(R.string.pin_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = pinEnabled,
                onCheckedChange = { on ->
                    if (on) {
                        showSetPin = true
                    } else if (pinEnabled) {
                        showDisablePin = true
                    }
                },
            )
        }
        if (pinEnabled) {
            TextButton(
                onClick = { showChangePin = true },
                modifier = Modifier.padding(horizontal = 12.dp),
            ) {
                Text(stringResource(R.string.change_pin))
            }
        }
        HorizontalDivider()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                Text(stringResource(R.string.fingerprint), style = MaterialTheme.typography.bodyLarge)
                Text(
                    if (biometricAvailable) {
                        stringResource(R.string.fingerprint_hint)
                    } else {
                        bioUnavailable
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = biometricEnabled && biometricAvailable,
                enabled = pinEnabled && biometricAvailable,
                onCheckedChange = { on ->
                    if (!biometricAvailable) {
                        scope.launch { snackbar.showSnackbar(bioUnavailable) }
                        return@Switch
                    }
                    store.setBiometricEnabled(on)
                    biometricEnabled = store.isBiometricEnabled()
                    scope.launch { snackbar.showSnackbar(savedMsg) }
                },
            )
        }
        Spacer(Modifier.height(16.dp))
    }

    if (showSetPin) {
        PinSetupDialog(
            title = stringResource(R.string.set_pin),
            requireCurrent = false,
            onDismiss = { showSetPin = false },
            onConfirm = { _, newPin ->
                val ok = store.setPin(newPin)
                if (ok) {
                    pinEnabled = true
                    biometricEnabled = store.isBiometricEnabled()
                    store.markUnlocked()
                    showSetPin = false
                    scope.launch { snackbar.showSnackbar(savedMsg) }
                }
                ok
            },
            mismatchMsg = stringResource(R.string.pin_mismatch),
            shortMsg = stringResource(R.string.pin_too_short),
            wrongMsg = wrongPin,
        )
    }

    if (showChangePin) {
        PinSetupDialog(
            title = stringResource(R.string.change_pin),
            requireCurrent = true,
            onDismiss = { showChangePin = false },
            onConfirm = { current, newPin ->
                val ok = store.changePin(current!!, newPin)
                if (ok) {
                    showChangePin = false
                    scope.launch { snackbar.showSnackbar(savedMsg) }
                }
                ok
            },
            mismatchMsg = stringResource(R.string.pin_mismatch),
            shortMsg = stringResource(R.string.pin_too_short),
            wrongMsg = wrongPin,
        )
    }

    if (showDisablePin) {
        PinConfirmDialog(
            title = stringResource(R.string.disable_pin),
            onDismiss = { showDisablePin = false },
            onConfirm = { pin ->
                val ok = store.disablePin(pin)
                if (ok) {
                    pinEnabled = false
                    biometricEnabled = false
                    showDisablePin = false
                    scope.launch { snackbar.showSnackbar(savedMsg) }
                }
                ok
            },
            wrongMsg = wrongPin,
        )
    }
}

@Composable
private fun PinSetupDialog(
    title: String,
    requireCurrent: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (CharArray?, CharArray) -> Boolean,
    mismatchMsg: String,
    shortMsg: String,
    wrongMsg: String,
) {
    var current by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (requireCurrent) {
                    OutlinedTextField(
                        value = current,
                        onValueChange = {
                            if (it.length <= AppLockStore.MAX_PIN_LENGTH && it.all { c -> c.isDigit() }) {
                                current = it
                                error = null
                            }
                        },
                        label = { Text(stringResource(R.string.current_pin)) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                OutlinedTextField(
                    value = pin,
                    onValueChange = {
                        if (it.length <= AppLockStore.MAX_PIN_LENGTH && it.all { c -> c.isDigit() }) {
                            pin = it
                            error = null
                        }
                    },
                    label = { Text(stringResource(R.string.pin)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = confirm,
                    onValueChange = {
                        if (it.length <= AppLockStore.MAX_PIN_LENGTH && it.all { c -> c.isDigit() }) {
                            confirm = it
                            error = null
                        }
                    },
                    label = { Text(stringResource(R.string.confirm_pin)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    isError = error != null,
                    supportingText = error?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    when {
                        pin.length < AppLockStore.MIN_PIN_LENGTH -> error = shortMsg
                        pin != confirm -> error = mismatchMsg
                        else -> {
                            val cur = if (requireCurrent) current.toCharArray() else null
                            val ok = onConfirm(cur, pin.toCharArray())
                            if (!ok) error = wrongMsg
                        }
                    }
                },
                enabled = pin.isNotEmpty() && confirm.isNotEmpty() &&
                    (!requireCurrent || current.isNotEmpty()),
            ) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Composable
private fun PinConfirmDialog(
    title: String,
    onDismiss: () -> Unit,
    onConfirm: (CharArray) -> Boolean,
    wrongMsg: String,
) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = pin,
                onValueChange = {
                    if (it.length <= AppLockStore.MAX_PIN_LENGTH && it.all { c -> c.isDigit() }) {
                        pin = it
                        error = null
                    }
                },
                label = { Text(stringResource(R.string.pin)) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                isError = error != null,
                supportingText = error?.let { { Text(it) } },
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    val ok = onConfirm(pin.toCharArray())
                    if (!ok) error = wrongMsg
                },
                enabled = pin.length >= AppLockStore.MIN_PIN_LENGTH,
            ) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}
