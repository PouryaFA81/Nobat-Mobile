package app.nobat.mobile.notify

import android.util.Base64
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.security.SecureRandom

/**
 * Portable Clinic code: `nobat1:` + base64url(JSON `{"u","t","k"}`).
 * `u` = relay base URL, `t` = topic, `k` = bearer token.
 */
object ClinicCode {
    const val PREFIX = "nobat1:"

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
        return PREFIX + b64
    }

    fun decode(raw: String): ClinicSettings? {
        val trimmed = raw.trim()
        if (!trimmed.startsWith(PREFIX)) return null
        return try {
            val b64 = trimmed.removePrefix(PREFIX)
            val bytes = Base64.decode(
                b64,
                Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING,
            )
            val json = JSONObject(String(bytes, StandardCharsets.UTF_8))
            val u = json.optString("u", "").trim()
            val t = json.optString("t", "").trim()
            val k = json.optString("k", "").trim()
            if (u.isBlank() || t.isBlank() || k.isBlank()) return null
            ClinicSettings(baseUrl = u.trimEnd('/'), topic = t, token = k, connected = true)
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
