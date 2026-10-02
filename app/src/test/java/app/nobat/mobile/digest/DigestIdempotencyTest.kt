package app.nobat.mobile.digest

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DigestIdempotencyTest {

    @Test
    fun slotKey_padsHourAndMinute() {
        assertEquals("2026-10-02|08:05", DigestIdempotency.slotKey("2026-10-02", 8, 5))
        assertEquals("2026-10-02|20:00", DigestIdempotency.slotKey("2026-10-02", 20, 0))
    }

    @Test
    fun changingSlot_isIndependentKey() {
        val a = DigestIdempotency.slotKey("2026-10-02", 20, 0)
        val b = DigestIdempotency.slotKey("2026-10-02", 21, 0)
        val c = DigestIdempotency.slotKey("2026-10-03", 20, 0)
        assertTrue(a != b)
        assertTrue(a != c)
    }

    @Test
    fun perChannel_skipAlreadySent_allowUnsent() {
        var state = DigestSentState()
        assertTrue(DigestIdempotency.shouldSendLocal(state))
        assertTrue(DigestIdempotency.shouldSendTelegram(state))
        assertTrue(DigestIdempotency.shouldSendClinic(state))

        state = state.withTelegram()
        assertFalse(DigestIdempotency.shouldSendTelegram(state))
        assertTrue(DigestIdempotency.shouldSendLocal(state))
        assertTrue(DigestIdempotency.shouldSendClinic(state))

        state = state.withLocal().withClinic()
        assertFalse(DigestIdempotency.shouldSendLocal(state))
        assertFalse(DigestIdempotency.shouldSendClinic(state))
    }

    @Test
    fun smtp_tracksRecipientsIndependently_caseInsensitive() {
        var state = DigestSentState()
        assertTrue(DigestIdempotency.shouldSendSmtp(state, "a@example.com"))
        state = state.withSmtpEmail("A@Example.com")
        assertFalse(DigestIdempotency.shouldSendSmtp(state, "a@example.com"))
        assertTrue(DigestIdempotency.shouldSendSmtp(state, "b@example.com"))
    }

    @Test
    fun smtpFailThenRetry_doesNotResendSucceededChannels() {
        // Simulate: local+telegram ok, smtp failed for one recipient.
        var state = DigestSentState()
            .withLocal()
            .withTelegram()
            .withSmtpEmail("ok@example.com")

        assertFalse(DigestIdempotency.shouldSendLocal(state))
        assertFalse(DigestIdempotency.shouldSendTelegram(state))
        assertFalse(DigestIdempotency.shouldSendSmtp(state, "ok@example.com"))
        assertTrue(DigestIdempotency.shouldSendSmtp(state, "fail@example.com"))
        assertTrue(DigestIdempotency.shouldSendClinic(state))
    }
}
