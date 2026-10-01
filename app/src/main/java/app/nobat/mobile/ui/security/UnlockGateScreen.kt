package app.nobat.mobile.ui.security

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import app.nobat.mobile.R
import app.nobat.mobile.security.AppLockStore
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

private val KeySize = 76.dp
private val KeyGap = 18.dp

@Composable
fun UnlockGatePane(
    store: AppLockStore,
    onUnlocked: () -> Unit,
    onUseAccount: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    val wrongPin = stringResource(R.string.wrong_pin)
    val enterPin = stringResource(R.string.enter_pin)
    val configuredLength = remember { store.pinLength() }
    val shake = remember { Animatable(0f) }

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

    fun shakeAndClear() {
        error = true
        pin = ""
        scope.launch {
            shake.snapTo(0f)
            repeat(4) { i ->
                shake.animateTo(if (i % 2 == 0) 14f else -14f, tween(40))
            }
            shake.animateTo(0f, tween(40))
        }
    }

    fun appendDigit(d: String) {
        if (pin.length >= configuredLength) return
        error = false
        val next = pin + d
        pin = next
        if (next.length < configuredLength) return
        if (store.verifyPin(next.toCharArray())) {
            store.markUnlocked()
            pin = ""
            error = false
            onUnlocked()
        } else {
            shakeAndClear()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp, vertical = 16.dp),
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
            text = enterPin,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        PinDots(
            filled = pin.length,
            total = configuredLength,
            error = error,
            modifier = Modifier.offset { IntOffset(shake.value.roundToInt(), 0) },
        )
        if (error) {
            Text(
                text = wrongPin,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        } else {
            Spacer(Modifier.height(16.dp))
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
private fun PinDots(
    filled: Int,
    total: Int,
    error: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = modifier,
    ) {
        repeat(total) { i ->
            val isFilled = i < filled
            Text(
                text = if (isFilled) "●" else "○",
                color = when {
                    error -> MaterialTheme.colorScheme.error
                    isFilled -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                style = MaterialTheme.typography.titleLarge,
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
    val deleteDesc = stringResource(R.string.delete)
    val fingerprintDesc = stringResource(R.string.fingerprint)
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("bio", "0", "del"),
    )
    Column(
        verticalArrangement = Arrangement.spacedBy(KeyGap),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(KeyGap)) {
                row.forEach { key ->
                    when (key) {
                        "bio" -> {
                            if (onBiometric != null) {
                                PadCircleButton(
                                    onClick = onBiometric,
                                    contentDescription = fingerprintDesc,
                                ) {
                                    Icon(
                                        Icons.Outlined.Fingerprint,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(30.dp),
                                    )
                                }
                            } else {
                                Spacer(Modifier.size(KeySize))
                            }
                        }
                        "del" -> {
                            PadCircleButton(
                                onClick = onBackspace,
                                contentDescription = deleteDesc,
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Outlined.Backspace,
                                    contentDescription = null,
                                    modifier = Modifier.size(26.dp),
                                )
                            }
                        }
                        else -> {
                            PadCircleButton(
                                onClick = { onDigit(key) },
                                contentDescription = key,
                            ) {
                                Text(
                                    text = key,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Medium,
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

@Composable
private fun PadCircleButton(
    onClick: () -> Unit,
    contentDescription: String,
    content: @Composable () -> Unit,
) {
    FilledTonalButton(
        onClick = onClick,
        shape = CircleShape,
        contentPadding = PaddingValues(0.dp),
        modifier = Modifier
            .size(KeySize)
            .semantics { this.contentDescription = contentDescription },
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize(),
        ) {
            content()
        }
    }
}
