package app.nobat.mobile.ui.security

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Backspace
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import app.nobat.mobile.R
import app.nobat.mobile.security.AppLockStore

@Composable
fun UnlockGatePane(
    store: AppLockStore,
    onUnlocked: () -> Unit,
    onUseAccount: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    val wrongPin = stringResource(R.string.wrong_pin)

    val biometricOk = remember {
        store.isBiometricEnabled() &&
            BiometricManager.from(context)
                .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) ==
            BiometricManager.BIOMETRIC_SUCCESS
    }

    fun launchBiometric() {
        val activity = context as? FragmentActivity ?: return
        val executor = ContextCompat.getMainExecutor(context)
        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    store.markUnlocked()
                    onUnlocked()
                }
            },
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(context.getString(R.string.unlock))
            .setSubtitle(context.getString(R.string.app_lock))
            .setNegativeButtonText(context.getString(R.string.cancel))
            .build()
        prompt.authenticate(info)
    }

    LaunchedEffect(biometricOk) {
        if (biometricOk) launchBiometric()
    }

    fun appendDigit(d: String) {
        if (pin.length >= AppLockStore.MAX_PIN_LENGTH) return
        error = false
        val next = pin + d
        pin = next
        if (next.length < AppLockStore.MIN_PIN_LENGTH) return
        if (store.verifyPin(next.toCharArray())) {
            store.markUnlocked()
            pin = ""
            error = false
            onUnlocked()
        } else if (next.length == AppLockStore.MAX_PIN_LENGTH) {
            error = true
            pin = ""
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Spacer(Modifier.height(24.dp))
        Icon(
            Icons.Outlined.Lock,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(48.dp),
        )
        Text(
            text = stringResource(R.string.unlock),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = stringResource(R.string.app_lock),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        PinDots(length = pin.length, error = error)
        if (error) {
            Text(
                text = wrongPin,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Spacer(Modifier.height(8.dp))
        PinPad(
            onDigit = { appendDigit(it) },
            onBackspace = {
                if (pin.isNotEmpty()) {
                    pin = pin.dropLast(1)
                    error = false
                }
            },
            onBiometric = if (biometricOk) ({ launchBiometric() }) else null,
        )
        Spacer(Modifier.weight(1f))
        TextButton(onClick = onUseAccount) {
            Text(stringResource(R.string.switch_account))
        }
    }
}

@Composable
private fun PinDots(length: Int, error: Boolean) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(AppLockStore.MAX_PIN_LENGTH) { i ->
            val filled = i < length
            Text(
                text = if (filled) "●" else "○",
                color = when {
                    error -> MaterialTheme.colorScheme.error
                    filled -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

@Composable
private fun PinPad(
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit,
    onBiometric: (() -> Unit)?,
) {
    val keys = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("bio", "0", "del"),
    )
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth(0.85f),
    ) {
        keys.forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                row.forEach { key ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1.2f),
                        contentAlignment = Alignment.Center,
                    ) {
                        when (key) {
                            "bio" -> {
                                if (onBiometric != null) {
                                    IconButton(onClick = onBiometric) {
                                        Icon(
                                            Icons.Outlined.Fingerprint,
                                            contentDescription = stringResource(R.string.fingerprint),
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(32.dp),
                                        )
                                    }
                                }
                            }
                            "del" -> {
                                IconButton(onClick = onBackspace) {
                                    Icon(
                                        Icons.Outlined.Backspace,
                                        contentDescription = stringResource(R.string.back),
                                    )
                                }
                            }
                            else -> {
                                FilledTonalButton(
                                    onClick = { onDigit(key) },
                                    shape = CircleShape,
                                    modifier = Modifier.fillMaxSize(0.9f),
                                ) {
                                    Text(
                                        text = key,
                                        style = MaterialTheme.typography.headlineSmall,
                                        textAlign = TextAlign.Center,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
