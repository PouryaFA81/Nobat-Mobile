package app.nobat.mobile.notify

/**
 * Per-account clinic relay settings (URL + topic + token).
 * Token is stored encrypted via [ClinicSettingsStore]; never log it.
 * UI never names the underlying relay product.
 *
 * [hosted] is true when credentials came from redeeming a Relay code
 * (hosted purchase path). The UI then hides the real host and shows a Hosted chip.
 *
 * [hostedOpaqueId] is a non-secret id used only for hosted Share (`nobatH1:`).
 * It never embeds host/topic/token.
 */
data class ClinicSettings(
    val baseUrl: String = "",
    val topic: String = "",
    val token: String = "",
    /** Staff (or admin) opted in: subscribe for remote alerts. */
    val connected: Boolean = false,
    /** Last relay message id consumed by the poller (opaque string). */
    val lastMessageId: String = "",
    /**
     * True when this account redeemed a Relay code (hosted path).
     * Never show [baseUrl] in the UI while this is true.
     */
    val hosted: Boolean = false,
    /**
     * Opaque hosted-relay identifier for Share (`nobatH1:`). Empty when not hosted.
     * Does not contain host, topic, or token.
     */
    val hostedOpaqueId: String = "",
) {
    fun isConfigured(): Boolean =
        baseUrl.isNotBlank() && topic.isNotBlank() && token.isNotBlank()

    fun normalizedBaseUrl(): String =
        baseUrl.trim().trimEnd('/')
}
