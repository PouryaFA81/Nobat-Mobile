package app.nobat.mobile.notify

/**
 * Per-account notification preferences (SMTP + SMS intent).
 * Password is stored encrypted via [NotificationSettingsStore]; never log it.
 */
data class NotificationSettings(
    val remindersOn: Boolean = false,
    val smtpHost: String = "",
    val smtpPort: Int = 587,
    val smtpUseTls: Boolean = true,
    val smtpUsername: String = "",
    val smtpPassword: String = "",
    val smtpFrom: String = "",
    val testRecipient: String = "",
)
