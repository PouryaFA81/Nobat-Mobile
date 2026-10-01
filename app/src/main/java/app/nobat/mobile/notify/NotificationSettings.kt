package app.nobat.mobile.notify

/**
 * Per-account notification preferences (SMTP + SMS intent + evening digest).
 * Password is stored encrypted via [NotificationSettingsStore]; never log it.
 *
 * [testRecipient] is the reminder / confirmation recipient (same field; UI label
 * may say “test recipient” or “reminder recipient”).
 */
data class NotificationSettings(
    val remindersOn: Boolean = false,
    /** Daily evening digest of tomorrow’s appointments (default off). */
    val eveningDigestOn: Boolean = false,
    /** Local wall-clock time HH:mm for the digest (default 20:00). */
    val digestTime: String = DEFAULT_DIGEST_TIME,
    val smtpHost: String = "",
    val smtpPort: Int = 587,
    val smtpUseTls: Boolean = true,
    val smtpUsername: String = "",
    val smtpPassword: String = "",
    val smtpFrom: String = "",
    /** Reminder + confirmation + test-send destination. */
    val testRecipient: String = "",
) {
    /** Alias for [testRecipient] — default reminder / confirmation inbox. */
    val reminderRecipient: String get() = testRecipient

    /** Parsed digest hour (0–23); falls back to 20. */
    val digestHour: Int
        get() = parseDigestTime(digestTime).first

    /** Parsed digest minute (0–59); falls back to 0. */
    val digestMinute: Int
        get() = parseDigestTime(digestTime).second

    companion object {
        const val DEFAULT_DIGEST_TIME = "20:00"

        fun parseDigestTime(raw: String): Pair<Int, Int> {
            val parts = raw.trim().split(':')
            if (parts.size != 2) return 20 to 0
            val h = parts[0].toIntOrNull() ?: return 20 to 0
            val m = parts[1].toIntOrNull() ?: return 20 to 0
            if (h !in 0..23 || m !in 0..59) return 20 to 0
            return h to m
        }

        fun formatDigestTime(hour: Int, minute: Int): String =
            "%02d:%02d".format(hour.coerceIn(0, 23), minute.coerceIn(0, 59))
    }
}
