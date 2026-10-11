package app.nobat.mobile.notify

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** Why a bot send failed. Never carries the bot token. */
enum class BotFailureReason { Network, Auth, Chat, Other }

/** Failure from [TelegramClient]/[BaleClient]; [message] never contains the token. */
class BotSendException(
    val reason: BotFailureReason,
    val httpCode: Int? = null,
    message: String,
) : Exception(message)

/** Reason of a failed [Result], or null when it is not a bot send failure / is a success. */
fun Result<*>.botFailure(): BotSendException? = exceptionOrNull() as? BotSendException

/** Pure helpers shared by the Telegram and Bale clients (no network, unit-testable). */
internal object BotSend {

    /** Telegram/Bale error_code, parsed only to classify failures on non-2xx. */
    private val errorCodeRegex = Regex("\"error_code\"\\s*:\\s*(\\d{3})")

    fun classifyHttp(code: Int, body: String?): BotFailureReason {
        val effective = if (code in 400..499) code
        else body?.let { errorCodeRegex.find(it)?.groupValues?.get(1)?.toIntOrNull() } ?: code
        return when (effective) {
            401, 404 -> BotFailureReason.Auth
            400, 403 -> BotFailureReason.Chat
            else -> BotFailureReason.Other
        }
    }

    fun httpFailure(code: Int, body: String?): BotSendException {
        val reason = classifyHttp(code, body)
        return BotSendException(reason, code, "HTTP $code")
    }

    /** Maps a thrown exception to a failure with a token-free message. */
    fun fromThrowable(t: Throwable, token: String): BotSendException {
        if (t is BotSendException) return t
        val reason = if (t is IOException) BotFailureReason.Network else BotFailureReason.Other
        // Only the exception class name is kept: messages may embed the URL (and so the token).
        return BotSendException(reason, null, redact(t.javaClass.simpleName, token))
    }

    fun redact(text: String, token: String): String {
        if (token.isEmpty()) return text
        return text.replace(token, "<redacted>")
    }

    fun post(baseUrl: String, token: String, chatId: String, text: String): Result<Unit> {
        return try {
            val t = BotInput.sanitizeToken(token)
            val c = BotInput.sanitizeChatId(chatId)
            require(t.isNotEmpty()) { "token" }
            require(c.isNotEmpty()) { "chat_id" }
            require(text.isNotBlank()) { "text" }
            val url = URL("$baseUrl/bot$t/sendMessage")
            val body = "chat_id=" + URLEncoder.encode(c, "UTF-8") +
                "&text=" + URLEncoder.encode(text, "UTF-8")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15_000
                readTimeout = 15_000
                doOutput = true
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "NobatMobile")
            }
            try {
                conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                val code = conn.responseCode
                if (code in 200..299) {
                    runCatching { conn.inputStream?.use { it.readBytes() } }
                    Result.success(Unit)
                } else {
                    // Body is read only to classify; it is never surfaced.
                    val err = runCatching {
                        (conn.errorStream ?: conn.inputStream)?.bufferedReader()?.use { it.readText() }
                    }.getOrNull()
                    Result.failure(httpFailure(code, err))
                }
            } finally {
                conn.disconnect()
            }
        } catch (e: Exception) {
            Result.failure(fromThrowable(e, token))
        }
    }
}

/** Normalizes user-entered token / chat ID (pasted text may hide invisible or Persian characters). */
object BotInput {

    fun sanitizeToken(raw: String): String = clean(raw)

    fun sanitizeChatId(raw: String): String = clean(raw)

    private fun clean(raw: String): String {
        val sb = StringBuilder(raw.length)
        for (ch in raw) {
            val c = when (ch) {
                in '\u06F0'..'\u06F9' -> '0' + (ch - '\u06F0') // Persian digits
                in '\u0660'..'\u0669' -> '0' + (ch - '\u0660') // Arabic-Indic digits
                '\u2212', '\u2013', '\u2012' -> '-' // minus / dashes in negative chat IDs
                else -> ch
            }
            if (c.isWhitespace()) continue
            when (Character.getType(c).toByte()) {
                Character.FORMAT, Character.CONTROL,
                Character.SPACE_SEPARATOR, Character.LINE_SEPARATOR, Character.PARAGRAPH_SEPARATOR,
                -> continue
            }
            sb.append(c)
        }
        return sb.toString()
    }
}
