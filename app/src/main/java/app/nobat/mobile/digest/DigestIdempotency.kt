package app.nobat.mobile.digest

/**
 * Pure per-channel digest idempotency for one fire day + time slot.
 * Mark a channel only after that channel succeeds; retries skip succeeded channels.
 */
data class DigestSentState(
    val local: Boolean = false,
    val telegram: Boolean = false,
    val bale: Boolean = false,
    val clinic: Boolean = false,
    /** Lowercased emails already delivered for this slot (SMTP may be multi-recipient). */
    val smtpEmails: Set<String> = emptySet(),
) {
    fun withLocal() = copy(local = true)
    fun withTelegram() = copy(telegram = true)
    fun withBale() = copy(bale = true)
    fun withClinic() = copy(clinic = true)
    fun withSmtpEmail(email: String): DigestSentState {
        val key = email.trim().lowercase()
        if (key.isEmpty()) return this
        return copy(smtpEmails = smtpEmails + key)
    }

    fun smtpAlreadySent(email: String): Boolean =
        email.trim().lowercase().let { it.isNotEmpty() && it in smtpEmails }
}

object DigestIdempotency {
    /** Stable key for one digest run: calendar fire-day + HH:mm slot. */
    fun slotKey(fireDayIso: String, hour: Int, minute: Int): String =
        "$fireDayIso|${"%02d:%02d".format(hour.coerceIn(0, 23), minute.coerceIn(0, 59))}"

    fun shouldSendLocal(state: DigestSentState): Boolean = !state.local
    fun shouldSendTelegram(state: DigestSentState): Boolean = !state.telegram
    fun shouldSendBale(state: DigestSentState): Boolean = !state.bale
    fun shouldSendClinic(state: DigestSentState): Boolean = !state.clinic
    fun shouldSendSmtp(state: DigestSentState, email: String): Boolean =
        !state.smtpAlreadySent(email)
}
