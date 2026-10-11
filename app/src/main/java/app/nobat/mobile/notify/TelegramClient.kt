package app.nobat.mobile.notify

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Telegram Bot API [sendMessage] via HTTPS.
 * Runs on [Dispatchers.IO]. Never logs the bot token.
 *
 * @see <a href="https://core.telegram.org/bots/api#sendmessage">sendMessage</a>
 */
object TelegramClient {

    suspend fun sendMessage(
        botToken: String,
        chatId: String,
        text: String,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        BotSend.post("https://api.telegram.org", botToken, chatId, text)
    }
}
