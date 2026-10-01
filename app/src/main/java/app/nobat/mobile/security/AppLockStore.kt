package app.nobat.mobile.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * How long after leaving the app (or when the phone locks) before PIN/biometric is required again.
 */
enum class LockAfterOption(val prefValue: String, val timeoutMs: Long?) {
    FIVE_MINUTES("5m", 5L * 60L * 1000L),
    THIRTY_MINUTES("30m", 30L * 60L * 1000L),
    ONE_HOUR("1h", 60L * 60L * 1000L),
    /** Lock when the screen turns off / keyguard engages — not merely when the app backgrounds. */
    WHEN_PHONE_LOCKS("phone", null);

    companion object {
        fun fromPref(value: String?): LockAfterOption =
            entries.firstOrNull { it.prefValue == value } ?: WHEN_PHONE_LOCKS
    }
}

/**
 * App-level lock: PIN hash (PBKDF2) + biometric toggle + lock-after preference
 * in EncryptedSharedPreferences. In-memory [unlocked] clears per lock-after rules.
 */
class AppLockStore(context: Context) {
    private val appContext = context.applicationContext
    private val prefs: SharedPreferences = createPrefs(appContext)

    private val _unlocked = MutableStateFlow(false)
    val unlocked: StateFlow<Boolean> = _unlocked.asStateFlow()

    /** Wall-clock ms when the app last went to background (timer modes). */
    private var backgroundedAtMs: Long = 0L

    fun isPinEnabled(): Boolean =
        prefs.getBoolean(KEY_PIN_ENABLED, false) && hasPinStored()

    fun isBiometricEnabled(): Boolean =
        prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false) && isPinEnabled()

    /** True when PIN and/or biometric should gate sensitive UI. */
    fun isLockEnabled(): Boolean = isPinEnabled()

    fun requiresUnlock(): Boolean = isLockEnabled() && !_unlocked.value

    fun markUnlocked() {
        _unlocked.value = true
        backgroundedAtMs = 0L
    }

    fun lockSession() {
        _unlocked.value = false
        backgroundedAtMs = 0L
    }

    fun getLockAfter(): LockAfterOption =
        LockAfterOption.fromPref(prefs.getString(KEY_LOCK_AFTER, null))

    fun setLockAfter(option: LockAfterOption) {
        prefs.edit().putString(KEY_LOCK_AFTER, option.prefValue).apply()
    }

    /** Call on ON_STOP when a timed lock-after is selected. */
    fun markBackgrounded(nowMs: Long = System.currentTimeMillis()) {
        if (!isLockEnabled()) return
        if (getLockAfter().timeoutMs == null) return
        if (!_unlocked.value) return
        backgroundedAtMs = nowMs
    }

    /**
     * Call on ON_START. For timer modes, locks if the timeout elapsed while backgrounded.
     * [WHEN_PHONE_LOCKS] is handled via screen-off broadcast, not here.
     */
    fun evaluateResumeLock(nowMs: Long = System.currentTimeMillis()) {
        if (!isLockEnabled() || !_unlocked.value) return
        val timeout = getLockAfter().timeoutMs ?: return
        val started = backgroundedAtMs
        if (started > 0L && nowMs - started >= timeout) {
            lockSession()
        }
        backgroundedAtMs = 0L
    }

    fun hasPinStored(): Boolean {
        val hash = prefs.getString(KEY_PIN_HASH, null)
        val salt = prefs.getString(KEY_PIN_SALT, null)
        return !hash.isNullOrBlank() && !salt.isNullOrBlank()
    }

    /** Configured PIN length (4–6). Falls back to MAX when unset (legacy). */
    fun pinLength(): Int =
        prefs.getInt(KEY_PIN_LENGTH, MAX_PIN_LENGTH).coerceIn(MIN_PIN_LENGTH, MAX_PIN_LENGTH)

    fun setPin(pin: CharArray): Boolean {
        if (pin.size < MIN_PIN_LENGTH || pin.size > MAX_PIN_LENGTH) return false
        if (!pin.all { it.isDigit() }) return false
        val salt = PasswordHasher.generateSalt()
        val hash = PasswordHasher.hash(pin, salt)
        prefs.edit()
            .putString(KEY_PIN_HASH, PasswordHasher.encode(hash))
            .putString(KEY_PIN_SALT, PasswordHasher.encode(salt))
            .putInt(KEY_PIN_LENGTH, pin.size)
            .putBoolean(KEY_PIN_ENABLED, true)
            .apply()
        return true
    }

    fun verifyPin(pin: CharArray): Boolean {
        val hashEnc = prefs.getString(KEY_PIN_HASH, null) ?: return false
        val saltEnc = prefs.getString(KEY_PIN_SALT, null) ?: return false
        return try {
            PasswordHasher.verify(
                pin,
                PasswordHasher.decode(saltEnc),
                PasswordHasher.decode(hashEnc),
            )
        } catch (_: Exception) {
            false
        }
    }

    fun changePin(current: CharArray, newPin: CharArray): Boolean {
        if (!verifyPin(current)) return false
        return setPin(newPin)
    }

    fun disablePin(current: CharArray): Boolean {
        if (!verifyPin(current)) return false
        prefs.edit()
            .remove(KEY_PIN_HASH)
            .remove(KEY_PIN_SALT)
            .remove(KEY_PIN_LENGTH)
            .putBoolean(KEY_PIN_ENABLED, false)
            .putBoolean(KEY_BIOMETRIC_ENABLED, false)
            .apply()
        _unlocked.value = true
        return true
    }

    fun setBiometricEnabled(enabled: Boolean) {
        if (!isPinEnabled()) return
        prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
    }

    companion object {
        const val MIN_PIN_LENGTH = 4
        const val MAX_PIN_LENGTH = 6

        private const val PREFS_NAME = "nobat_applock_enc"
        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_PIN_SALT = "pin_salt"
        private const val KEY_PIN_LENGTH = "pin_length"
        private const val KEY_PIN_ENABLED = "pin_enabled"
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        private const val KEY_LOCK_AFTER = "lock_after"

        private fun createPrefs(context: Context): SharedPreferences {
            return try {
                val masterKey = MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()
                EncryptedSharedPreferences.create(
                    context,
                    PREFS_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
                )
            } catch (_: Exception) {
                context.getSharedPreferences(PREFS_NAME + "_fallback", Context.MODE_PRIVATE)
            }
        }
    }
}
