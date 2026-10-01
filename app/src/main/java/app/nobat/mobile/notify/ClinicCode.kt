package app.nobat.mobile.notify

import android.util.Base64
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.security.SecureRandom

/**
 * Portable Clinic / Relay codes.
 *
 * - `nobat1:` + base64url(JSON `{"u","t","k"}`) — Clinic code (share with staff)
 *   or a plain Relay code (same blob; seller generates offline).
 * - `nobatR1:` + same payload — optional hosted-purchase wrapper; decode marks [ClinicSettings.hosted].
 *
 * `u` = relay base URL, `t` = topic, `k` = bearer token.
 * Never bake a real host into the APK; operators mint codes offline.
 */
object ClinicCode {
    const val PREFIX = "nobat1:"
    /** Hosted-purchase thin wrapper; same JSON as [PREFIX]. */
    const val RELAY_PREFIX = "nobatR1:"

    fun encode(settings: ClinicSettings): String? {
        if (!settings.isConfigured()) return null
        val json = JSONObject()
            .put("u", settings.normalizedBaseUrl())
            .put("t", settings.topic.trim())
            .put("k", settings.token.trim())
        val bytes = json.toString().toByteArray(StandardCharsets.UTF_8)
        val b64 = Base64.encodeToString(
            bytes,
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING,
        )
        // Always share as Clinic code (nobat1:) so staff Connect stays unchanged.
        return PREFIX + b64
    }

    /**
     * Decode a Clinic code (`nobat1:`) or Relay code (`nobat1:` / `nobatR1:`).
     * [forceHosted] marks settings hosted even for `nobat1:` (admin Redeem path).
     * `nobatR1:` always sets hosted=true.
     */
    fun decode(raw: String, forceHosted: Boolean = false): ClinicSettings? {
        val trimmed = raw.trim()
        val fromRelayPrefix = trimmed.startsWith(RELAY_PREFIX)
        val prefix = when {
            fromRelayPrefix -> RELAY_PREFIX
            trimmed.startsWith(PREFIX) -> PREFIX
            else -> return null
        }
        return try {
            val b64 = trimmed.removePrefix(prefix)
            val bytes = Base64.decode(
                b64,
                Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING,
            )
            val json = JSONObject(String(bytes, StandardCharsets.UTF_8))
            val u = json.optString("u", "").trim()
            val t = json.optString("t", "").trim()
            val k = json.optString("k", "").trim()
            if (u.isBlank() || t.isBlank() || k.isBlank()) return null
            ClinicSettings(
                baseUrl = u.trimEnd('/'),
                topic = t,
                token = k,
                connected = true,
                hosted = forceHosted || fromRelayPrefix,
            )
        } catch (_: Exception) {
            null
        }
    }

    /** Suggest a fresh topic name; token must be refilled by the operator. */
    fun suggestTopic(): String {
        val alphabet = "abcdefghijklmnopqrstuvwxyz0123456789"
        val rnd = SecureRandom()
        val suffix = buildString(8) {
            repeat(8) { append(alphabet[rnd.nextInt(alphabet.length)]) }
        }
        return "nobat-m-$suffix"
    }
}
