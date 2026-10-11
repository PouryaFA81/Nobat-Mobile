package app.nobat.mobile.notify

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Bale Bot API [sendMessage] via HTTPS (Telegram-compatible shape).
 * Official base: `https://tapi.bale.ai/bot<token>/METHOD_NAME`
 * Runs on [Dispatchers.IO]. Never logs the bot token.
 *
 * @see <a href="https://docs.bale.ai/">Bale bot API docs</a>
 */
object BaleClient {

    suspend fun sendMessage(
        botToken: String,
        chatId: String,
        text: String,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        BotSend.post("https://tapi.bale.ai", botToken, chatId, text)
    }
}
