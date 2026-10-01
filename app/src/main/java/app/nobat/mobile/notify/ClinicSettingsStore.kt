package app.nobat.mobile.notify

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Per-account clinic relay settings in EncryptedSharedPreferences.
 * Keys are scoped by [accountId]. Never logs the token.
 *
 * App-level [relay_purchase_url] is editable under admin Relay advanced;
 * default blank — "Get a relay code" stays disabled until set.
 */
class ClinicSettingsStore(context: Context) {
    private val prefs: SharedPreferences = createPrefs(context.applicationContext)

    fun load(accountId: Long): ClinicSettings {
        val p = prefix(accountId)
        return ClinicSettings(
            baseUrl = prefs.getString(p + KEY_URL, "") ?: "",
            topic = prefs.getString(p + KEY_TOPIC, "") ?: "",
            token = prefs.getString(p + KEY_TOKEN, "") ?: "",
            connected = prefs.getBoolean(p + KEY_CONNECTED, false),
            lastMessageId = prefs.getString(p + KEY_LAST_ID, "") ?: "",
            hosted = prefs.getBoolean(p + KEY_HOSTED, false),
        )
    }

    fun save(accountId: Long, settings: ClinicSettings) {
        val p = prefix(accountId)
        prefs.edit()
            .putString(p + KEY_URL, settings.normalizedBaseUrl())
            .putString(p + KEY_TOPIC, settings.topic.trim())
            .putString(p + KEY_TOKEN, settings.token.trim())
            .putBoolean(p + KEY_CONNECTED, settings.connected)
            .putString(p + KEY_LAST_ID, settings.lastMessageId)
            .putBoolean(p + KEY_HOSTED, settings.hosted)
            .apply()
    }

    fun clear(accountId: Long) {
        val p = prefix(accountId)
        prefs.edit()
            .remove(p + KEY_URL)
            .remove(p + KEY_TOPIC)
            .remove(p + KEY_TOKEN)
            .remove(p + KEY_CONNECTED)
            .remove(p + KEY_LAST_ID)
            .remove(p + KEY_HOSTED)
            .apply()
    }

    /** Contact / purchase URL for "Get a relay code". Blank by default. */
    fun getRelayPurchaseUrl(): String =
        prefs.getString(KEY_PURCHASE_URL, "")?.trim().orEmpty()

    fun setRelayPurchaseUrl(url: String) {
        prefs.edit().putString(KEY_PURCHASE_URL, url.trim()).apply()
    }

    private fun prefix(accountId: Long) = "acct_${accountId}_"

    companion object {
        private const val PREFS_NAME = "nobat_clinic_enc"
        private const val KEY_URL = "clinic_url"
        private const val KEY_TOPIC = "clinic_topic"
        private const val KEY_TOKEN = "clinic_token"
        private const val KEY_CONNECTED = "clinic_connected"
        private const val KEY_LAST_ID = "clinic_last_id"
        private const val KEY_HOSTED = "clinic_hosted"
        /** EncryptedSharedPreferences key `relay_purchase_url` (app-level). */
        const val KEY_PURCHASE_URL = "relay_purchase_url"

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
