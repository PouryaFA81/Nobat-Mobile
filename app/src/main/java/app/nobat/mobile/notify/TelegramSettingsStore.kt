package app.nobat.mobile.notify

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Per-account Telegram settings in EncryptedSharedPreferences.
 * Keys are scoped by [accountId]. Never logs the bot token.
 */
class TelegramSettingsStore(context: Context) {
    private val prefs: SharedPreferences = createPrefs(context.applicationContext)

    fun load(accountId: Long): TelegramSettings {
        val p = prefix(accountId)
        return TelegramSettings(
            botToken = prefs.getString(p + KEY_TOKEN, "") ?: "",
            chatId = prefs.getString(p + KEY_CHAT, "") ?: "",
            notifyOnBook = prefs.getBoolean(p + KEY_NOTIFY_ON_BOOK, false),
        )
    }

    fun save(accountId: Long, settings: TelegramSettings) {
        val p = prefix(accountId)
        prefs.edit()
            .putString(p + KEY_TOKEN, settings.botToken.trim())
            .putString(p + KEY_CHAT, settings.chatId.trim())
            .putBoolean(p + KEY_NOTIFY_ON_BOOK, settings.notifyOnBook)
            .apply()
    }

    /** Remove all keys for an account. */
    fun clear(accountId: Long) {
        val p = prefix(accountId)
        prefs.edit()
            .remove(p + KEY_TOKEN)
            .remove(p + KEY_CHAT)
            .remove(p + KEY_NOTIFY_ON_BOOK)
            .apply()
    }

    private fun prefix(accountId: Long) = "acct_${accountId}_"

    companion object {
        private const val PREFS_NAME = "nobat_telegram_enc"
        private const val KEY_TOKEN = "tg_bot_token"
        private const val KEY_CHAT = "tg_chat_id"
        private const val KEY_NOTIFY_ON_BOOK = "tg_notify_on_book"

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
