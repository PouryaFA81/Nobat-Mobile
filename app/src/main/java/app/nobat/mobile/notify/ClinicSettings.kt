package app.nobat.mobile.notify

/**
 * Per-account clinic relay settings (URL + topic + token).
 * Token is stored encrypted via [ClinicSettingsStore]; never log it.
 * UI never names the underlying relay product.
 */
data class ClinicSettings(
    val baseUrl: String = "",
    val topic: String = "",
    val token: String = "",
    /** Staff (or admin) opted in: subscribe for remote alerts. */
    val connected: Boolean = false,
    /** Last ntfy message id consumed by the poller (opaque string). */
    val lastMessageId: String = "",
) {
    fun isConfigured(): Boolean =
        baseUrl.isNotBlank() && topic.isNotBlank() && token.isNotBlank()

    fun normalizedBaseUrl(): String =
        baseUrl.trim().trimEnd('/')
}
