package app.nobat.mobile.notify

/**
 * Per-account Telegram Bot API settings (Integrations).
 * [botToken] is stored encrypted via [TelegramSettingsStore]; never log it.
 */
data class TelegramSettings(
    val botToken: String = "",
    val chatId: String = "",
    /** When true, send a Telegram message to [chatId] after a successful book. */
    val notifyOnBook: Boolean = false,
) {
    fun isConfigured(): Boolean =
        botToken.isNotBlank() && chatId.isNotBlank()
}
