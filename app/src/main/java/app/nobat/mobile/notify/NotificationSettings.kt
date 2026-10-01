package app.nobat.mobile.notify

/**
 * Per-account notification preferences (SMTP + SMS intent).
 * Password is stored encrypted via [NotificationSettingsStore]; never log it.
 *
 * [testRecipient] is the reminder / confirmation recipient (same field; UI label
 * may say “test recipient” or “reminder recipient”).
 */
data class NotificationSettings(
    val remindersOn: Boolean = false,
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
}
