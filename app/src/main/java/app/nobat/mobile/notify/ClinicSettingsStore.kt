package app.nobat.mobile.notify

import android.content.Context
import android.content.SharedPreferences
import app.nobat.mobile.security.SecurePrefs
import app.nobat.mobile.security.SecureStorageException

/**
 * Per-account clinic relay settings in EncryptedSharedPreferences.
 * Keys are scoped by [accountId]. Never logs the token.
 * Does not fall back to plaintext prefs on Keystore failure.
 *
 * App-level [relay_purchase_url] is editable under admin Relay advanced;
 * default blank — "Get a relay code" stays disabled until set.
 */
class ClinicSettingsStore(context: Context) {
    private val prefs: SharedPreferences?
    private val openError: SecureStorageException?

    init {
        var p: SharedPreferences? = null
        var err: SecureStorageException? = null
        try {
            p = SecurePrefs.open(context.applicationContext, PREFS_NAME)
        } catch (e: SecureStorageException) {
            err = e
        }
        prefs = p
        openError = err
    }

    fun isSecureStorageAvailable(): Boolean = prefs != null

    fun load(accountId: Long): ClinicSettings {
        val prefs = prefs ?: return ClinicSettings()
        val p = prefix(accountId)
        return ClinicSettings(
            baseUrl = prefs.getString(p + KEY_URL, "") ?: "",
            topic = prefs.getString(p + KEY_TOPIC, "") ?: "",
            token = prefs.getString(p + KEY_TOKEN, "") ?: "",
            connected = prefs.getBoolean(p + KEY_CONNECTED, false),
            lastMessageId = prefs.getString(p + KEY_LAST_ID, "") ?: "",
            hosted = prefs.getBoolean(p + KEY_HOSTED, false),
            hostedOpaqueId = prefs.getString(p + KEY_OPAQUE, "") ?: "",
        )
    }

    fun save(accountId: Long, settings: ClinicSettings): Result<Unit> {
        val prefs = prefs ?: return Result.failure(openError ?: SecureStorageException())
        val p = prefix(accountId)
        prefs.edit()
            .putString(p + KEY_URL, settings.normalizedBaseUrl())
            .putString(p + KEY_TOPIC, settings.topic.trim())
            .putString(p + KEY_TOKEN, settings.token.trim())
            .putBoolean(p + KEY_CONNECTED, settings.connected)
            .putString(p + KEY_LAST_ID, settings.lastMessageId)
            .putBoolean(p + KEY_HOSTED, settings.hosted)
            .putString(p + KEY_OPAQUE, settings.hostedOpaqueId.trim())
            .apply()
        return Result.success(Unit)
    }

    fun clear(accountId: Long): Result<Unit> {
        val prefs = prefs ?: return Result.failure(openError ?: SecureStorageException())
        val p = prefix(accountId)
        prefs.edit()
            .remove(p + KEY_URL)
            .remove(p + KEY_TOPIC)
            .remove(p + KEY_TOKEN)
            .remove(p + KEY_CONNECTED)
            .remove(p + KEY_LAST_ID)
            .remove(p + KEY_HOSTED)
            .remove(p + KEY_OPAQUE)
            .apply()
        return Result.success(Unit)
    }

    /** Contact / purchase URL for "Get a relay code". Blank by default. */
    fun getRelayPurchaseUrl(): String {
        val prefs = prefs ?: return ""
        return prefs.getString(KEY_PURCHASE_URL, "")?.trim().orEmpty()
    }

    fun setRelayPurchaseUrl(url: String): Result<Unit> {
        val prefs = prefs ?: return Result.failure(openError ?: SecureStorageException())
        prefs.edit().putString(KEY_PURCHASE_URL, url.trim()).apply()
        return Result.success(Unit)
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
        private const val KEY_OPAQUE = "clinic_opaque"
        /** EncryptedSharedPreferences key `relay_purchase_url` (app-level). */
        const val KEY_PURCHASE_URL = "relay_purchase_url"
    }
}
