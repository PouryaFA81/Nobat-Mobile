package app.nobat.mobile.notify

/**
 * Per-account Bale Bot API settings (Integrations).
 * [botToken] is stored encrypted via [BaleSettingsStore]; never log it.
 */
data class BaleSettings(
    val botToken: String = "",
    val chatId: String = "",
    /** When true, send a Bale message to [chatId] after a successful book. */
    val notifyOnBook: Boolean = false,
) {
    fun isConfigured(): Boolean =
        botToken.isNotBlank() && chatId.isNotBlank()
}
