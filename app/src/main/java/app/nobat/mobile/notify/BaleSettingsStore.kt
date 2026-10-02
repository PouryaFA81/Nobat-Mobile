package app.nobat.mobile.notify

import android.content.Context
import android.content.SharedPreferences
import app.nobat.mobile.security.SecurePrefs
import app.nobat.mobile.security.SecureStorageException

/**
 * Per-account Bale settings in EncryptedSharedPreferences.
 * Keys are scoped by [accountId]. Never logs the bot token.
 * Does not fall back to plaintext prefs on Keystore failure.
 */
class BaleSettingsStore(context: Context) {
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

    fun load(accountId: Long): BaleSettings {
        val prefs = prefs ?: return BaleSettings()
        val p = prefix(accountId)
        return BaleSettings(
            botToken = prefs.getString(p + KEY_TOKEN, "") ?: "",
            chatId = prefs.getString(p + KEY_CHAT, "") ?: "",
            notifyOnBook = prefs.getBoolean(p + KEY_NOTIFY_ON_BOOK, false),
        )
    }

    fun save(accountId: Long, settings: BaleSettings): Result<Unit> {
        val prefs = prefs ?: return Result.failure(openError ?: SecureStorageException())
        val p = prefix(accountId)
        prefs.edit()
            .putString(p + KEY_TOKEN, settings.botToken.trim())
            .putString(p + KEY_CHAT, settings.chatId.trim())
            .putBoolean(p + KEY_NOTIFY_ON_BOOK, settings.notifyOnBook)
            .apply()
        return Result.success(Unit)
    }

    /** Remove all keys for an account. */
    fun clear(accountId: Long): Result<Unit> {
        val prefs = prefs ?: return Result.failure(openError ?: SecureStorageException())
        val p = prefix(accountId)
        prefs.edit()
            .remove(p + KEY_TOKEN)
            .remove(p + KEY_CHAT)
            .remove(p + KEY_NOTIFY_ON_BOOK)
            .apply()
        return Result.success(Unit)
    }

    private fun prefix(accountId: Long) = "acct_${accountId}_"

    companion object {
        private const val PREFS_NAME = "nobat_bale_enc"
        private const val KEY_TOKEN = "bale_bot_token"
        private const val KEY_CHAT = "bale_chat_id"
        private const val KEY_NOTIFY_ON_BOOK = "bale_notify_on_book"
    }
}
