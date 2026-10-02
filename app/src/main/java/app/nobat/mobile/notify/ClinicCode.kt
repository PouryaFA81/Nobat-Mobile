package app.nobat.mobile.notify

import android.util.Base64
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.util.UUID

/**
 * Portable Clinic / Relay codes.
 *
 * - `nobat1:` + base64url(JSON `{"u","t","k"}`) — own-relay Clinic code (share with staff)
 *   or a plain Relay code (same blob; seller generates offline).
 * - `nobatR1:` + same payload — optional hosted-purchase wrapper; decode marks [ClinicSettings.hosted].
 * - `nobatH1:` + base64url(JSON `{"i"}`) — hosted Share only; opaque id, **no** host/topic/token.
 *
 * `u` = relay base URL, `t` = topic, `k` = bearer token, `i` = opaque hosted id.
 * Never bake a real host into the APK; operators mint codes offline.
 */
object ClinicCode {
    const val PREFIX = "nobat1:"
    /** Hosted-purchase thin wrapper; same JSON as [PREFIX]. */
    const val RELAY_PREFIX = "nobatR1:"
    /**
     * Hosted Share prefix — opaque id only. Staff Connect cannot resolve credentials
     * until a future resolution service exists; admin publish still uses local redeem data.
     */
    const val HOSTED_SHARE_PREFIX = "nobatH1:"

    /**
     * Share string for the current settings.
     * Hosted → [HOSTED_SHARE_PREFIX] opaque id only (never u/t/k).
     * Own-relay → [PREFIX] with u/t/k.
     */
    fun encode(settings: ClinicSettings): String? {
        if (settings.hosted) {
            val id = settings.hostedOpaqueId.trim()
            if (id.isBlank()) return null
            return encodeHostedOpaque(id)
        }
        if (!settings.isConfigured()) return null
        val json = JSONObject()
            .put("u", settings.normalizedBaseUrl())
            .put("t", settings.topic.trim())
            .put("k", settings.token.trim())
        return PREFIX + b64(json.toString().toByteArray(StandardCharsets.UTF_8))
    }

    fun encodeHostedOpaque(opaqueId: String): String {
        val json = JSONObject().put("i", opaqueId.trim())
        return HOSTED_SHARE_PREFIX + b64(json.toString().toByteArray(StandardCharsets.UTF_8))
    }

    fun newOpaqueId(): String = UUID.randomUUID().toString()

    /**
     * Decode a Clinic / Relay / hosted-opaque code.
     * [forceHosted] marks settings hosted even for `nobat1:` (admin Redeem path).
     * `nobatR1:` always sets hosted=true.
     * `nobatH1:` returns hosted settings with [ClinicSettings.hostedOpaqueId] only
     * (no u/t/k — not [ClinicSettings.isConfigured] until a resolution service exists).
     */
    fun decode(raw: String, forceHosted: Boolean = false): ClinicSettings? {
        val trimmed = raw.trim()
        when {
            trimmed.startsWith(HOSTED_SHARE_PREFIX) -> return decodeHostedOpaque(trimmed)
            trimmed.startsWith(RELAY_PREFIX) -> return decodeCredentialBlob(
                trimmed.removePrefix(RELAY_PREFIX),
                hosted = true,
            )
            trimmed.startsWith(PREFIX) -> return decodeCredentialBlob(
                trimmed.removePrefix(PREFIX),
                hosted = forceHosted,
            )
            else -> return null
        }
    }

    private fun decodeHostedOpaque(trimmed: String): ClinicSettings? {
        return try {
            val b64 = trimmed.removePrefix(HOSTED_SHARE_PREFIX)
            val bytes = Base64.decode(
                b64,
                Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING,
            )
            val json = JSONObject(String(bytes, StandardCharsets.UTF_8))
            val id = json.optString("i", "").trim()
            if (id.isBlank()) return null
            ClinicSettings(
                connected = false,
                hosted = true,
                hostedOpaqueId = id,
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun decodeCredentialBlob(b64: String, hosted: Boolean): ClinicSettings? {
        return try {
            val bytes = Base64.decode(
                b64,
                Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING,
            )
            val json = JSONObject(String(bytes, StandardCharsets.UTF_8))
            val u = json.optString("u", "").trim()
            val t = json.optString("t", "").trim()
            val k = json.optString("k", "").trim()
            if (u.isBlank() || t.isBlank() || k.isBlank()) return null
            val opaque = if (hosted) {
                json.optString("i", "").trim().ifBlank { newOpaqueId() }
            } else {
                ""
            }
            ClinicSettings(
                baseUrl = u.trimEnd('/'),
                topic = t,
                token = k,
                connected = true,
                hosted = hosted,
                hostedOpaqueId = opaque,
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

    private fun b64(bytes: ByteArray): String =
        Base64.encodeToString(
            bytes,
            Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING,
        )
}
