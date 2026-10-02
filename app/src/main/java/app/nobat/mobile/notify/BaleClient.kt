package app.nobat.mobile.notify

import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
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
        try {
            require(botToken.isNotBlank()) { "token" }
            require(chatId.isNotBlank()) { "chat_id" }
            require(text.isNotBlank()) { "text" }

            val token = botToken.trim()
            val url = URL("https://tapi.bale.ai/bot$token/sendMessage")
            val body = buildString {
                append("chat_id=")
                append(URLEncoder.encode(chatId.trim(), Charsets.UTF_8.name()))
                append("&text=")
                append(URLEncoder.encode(text, Charsets.UTF_8.name()))
            }

            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15_000
                readTimeout = 15_000
                doOutput = true
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
            }
            try {
                conn.outputStream.use { os ->
                    os.write(body.toByteArray(Charsets.UTF_8))
                }
                val code = conn.responseCode
                if (code in 200..299) {
                    Result.success(Unit)
                } else {
                    val err = (conn.errorStream ?: conn.inputStream)
                        ?.bufferedReader()
                        ?.use { it.readText() }
                        .orEmpty()
                    Result.failure(IllegalStateException("HTTP $code${if (err.isNotBlank()) ": $err" else ""}"))
                }
            } finally {
                conn.disconnect()
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
