package app.nobat.mobile.notify

import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class BotSendTest {
    private val token = "123456:ABC-secretTokenValue"

    @Test
    fun classifyHttp_codes() {
        assertEquals(BotFailureReason.Auth, BotSend.classifyHttp(401, null))
        assertEquals(BotFailureReason.Auth, BotSend.classifyHttp(404, "{\"ok\":false}"))
        assertEquals(BotFailureReason.Chat, BotSend.classifyHttp(400, "{\"ok\":false,\"error_code\":400,\"description\":\"Bad Request: chat not found\"}"))
        assertEquals(BotFailureReason.Chat, BotSend.classifyHttp(403, "{\"error_code\":403,\"description\":\"Forbidden: bot was blocked by the user\"}"))
        assertEquals(BotFailureReason.Other, BotSend.classifyHttp(429, "{\"error_code\":429}"))
        assertEquals(BotFailureReason.Other, BotSend.classifyHttp(500, ""))
    }

    @Test
    fun classifyHttp_non4xxUsesBodyErrorCode() {
        assertEquals(BotFailureReason.Chat, BotSend.classifyHttp(302, "{\"error_code\": 403}"))
        assertEquals(BotFailureReason.Other, BotSend.classifyHttp(502, "garbage"))
    }

    @Test
    fun httpFailure_carriesCodeOnly() {
        val e = BotSend.httpFailure(400, "{\"description\":\"chat not found $token\"}")
        assertEquals(400, e.httpCode)
        assertEquals("HTTP 400", e.message)
    }

    @Test
    fun throwable_networkVsOther_andTokenNeverInMessage() {
        val leaky = listOf(
            UnknownHostException("api.telegram.org/bot$token/sendMessage"),
            SocketTimeoutException("timeout https://api.telegram.org/bot$token/sendMessage"),
            SSLException("failed for /bot$token/"),
        )
        for (t in leaky) {
            val e = BotSend.fromThrowable(t, token)
            assertEquals(BotFailureReason.Network, e.reason)
            assertFalse(e.message!!.contains(token))
            assertFalse(e.toString().contains(token))
            assertNull(e.httpCode)
        }
        val other = BotSend.fromThrowable(IllegalStateException("bot$token"), token)
        assertEquals(BotFailureReason.Other, other.reason)
        assertFalse(other.message!!.contains(token))
    }

    @Test
    fun redact_replacesToken() {
        assertEquals("x <redacted> y", BotSend.redact("x $token y", token))
    }

    @Test
    fun sanitizer_persianDigitsAndInvisibles() {
        assertEquals("123456", BotInput.sanitizeChatId("۱۲۳۴۵۶"))
        assertEquals("-100123", BotInput.sanitizeChatId("\u200F−۱۰۰۱۲۳\u200E"))
        assertEquals("789", BotInput.sanitizeChatId("٧٨٩"))
        assertEquals(token, BotInput.sanitizeToken("\uFEFF $token\u200C\n\t\u200F "))
        assertEquals("12", BotInput.sanitizeChatId(" 1\u00A0\u200C2\r\n"))
    }
}
