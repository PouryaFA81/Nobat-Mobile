package app.nobat.mobile.notify

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Per-account notification settings in EncryptedSharedPreferences.
 * Keys are scoped by [accountId]. Never logs passwords.
 */
class NotificationSettingsStore(context: Context) {
    private val prefs: SharedPreferences = createPrefs(context.applicationContext)

    fun load(accountId: Long): NotificationSettings {
        val p = prefix(accountId)
        return NotificationSettings(
            remindersOn = prefs.getBoolean(p + KEY_REMINDERS_ON, false),
            smtpHost = prefs.getString(p + KEY_HOST, "") ?: "",
            smtpPort = prefs.getInt(p + KEY_PORT, 587),
            smtpUseTls = prefs.getBoolean(p + KEY_TLS, true),
            smtpUsername = prefs.getString(p + KEY_USER, "") ?: "",
            smtpPassword = prefs.getString(p + KEY_PASS, "") ?: "",
            smtpFrom = prefs.getString(p + KEY_FROM, "") ?: "",
            testRecipient = prefs.getString(p + KEY_TEST_TO, "") ?: "",
        )
    }

    fun save(accountId: Long, settings: NotificationSettings) {
        val p = prefix(accountId)
        prefs.edit()
            .putBoolean(p + KEY_REMINDERS_ON, settings.remindersOn)
            .putString(p + KEY_HOST, settings.smtpHost.trim())
            .putInt(p + KEY_PORT, settings.smtpPort.coerceIn(1, 65535))
            .putBoolean(p + KEY_TLS, settings.smtpUseTls)
            .putString(p + KEY_USER, settings.smtpUsername.trim())
            .putString(p + KEY_PASS, settings.smtpPassword)
            .putString(p + KEY_FROM, settings.smtpFrom.trim())
            .putString(p + KEY_TEST_TO, settings.testRecipient.trim())
            .apply()
    }

    /** Remove all keys for an account (e.g. after reset/delete). */
    fun clear(accountId: Long) {
        val p = prefix(accountId)
        prefs.edit()
            .remove(p + KEY_REMINDERS_ON)
            .remove(p + KEY_HOST)
            .remove(p + KEY_PORT)
            .remove(p + KEY_TLS)
            .remove(p + KEY_USER)
            .remove(p + KEY_PASS)
            .remove(p + KEY_FROM)
            .remove(p + KEY_TEST_TO)
            .apply()
    }

    private fun prefix(accountId: Long) = "acct_${accountId}_"

    companion object {
        private const val PREFS_NAME = "nobat_notify_enc"
        private const val KEY_REMINDERS_ON = "reminders_on"
        private const val KEY_HOST = "smtp_host"
        private const val KEY_PORT = "smtp_port"
        private const val KEY_TLS = "smtp_tls"
        private const val KEY_USER = "smtp_user"
        private const val KEY_PASS = "smtp_pass"
        private const val KEY_FROM = "smtp_from"
        private const val KEY_TEST_TO = "test_to"

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
                // Fallback if Keystore unavailable (rare on API 26+); still per-account scoped.
                context.getSharedPreferences(PREFS_NAME + "_fallback", Context.MODE_PRIVATE)
            }
        }
    }
}
