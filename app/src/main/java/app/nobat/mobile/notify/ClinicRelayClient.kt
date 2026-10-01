package app.nobat.mobile.notify

import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * HTTP publish + poll against the clinic relay (ntfy-compatible under the hood).
 * Never logs the bearer token.
 */
object ClinicRelayClient {

    data class RelayMessage(
        val id: String,
        val event: String,
        val personnelId: Long,
        val initials: String,
        val day: String,
        val time: String,
        val title: String,
        val raw: String,
    )

    suspend fun publish(
        settings: ClinicSettings,
        event: String,
        personnelId: Long,
        initials: String,
        day: String,
        time: String,
        title: String,
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            require(settings.isConfigured()) { "not configured" }
            val base = settings.normalizedBaseUrl()
            val topic = settings.topic.trim().trim('/')
            val url = URL("$base/$topic")
            val body = JSONObject()
                .put("v", 1)
                .put("event", event)
                .put("personnelId", personnelId)
                .put("initials", initials)
                .put("day", day)
                .put("time", time)
                .put("title", title)
                .toString()

            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 15_000
                readTimeout = 15_000
                doOutput = true
                setRequestProperty("Content-Type", "text/plain; charset=UTF-8")
                setRequestProperty("Authorization", "Bearer ${settings.token.trim()}")
                // Human-friendly title for stock ntfy clients; app parses body JSON.
                setRequestProperty("Title", title.take(120))
                setRequestProperty(
                    "Tags",
                    when (event) {
                        "cancel" -> "x"
                        "move" -> "arrows_counterclockwise"
                        else -> "calendar"
                    },
                )
            }
            try {
                conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                val code = conn.responseCode
                if (code in 200..299) {
                    Result.success(Unit)
                } else {
                    val err = (conn.errorStream ?: conn.inputStream)
                        ?.bufferedReader()
                        ?.use { it.readText() }
                        .orEmpty()
                    Result.failure(IllegalStateException("HTTP $code${if (err.isNotBlank()) ": $err" else ""}"))
                }
            } finally {
                conn.disconnect()
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * One-shot poll for messages since [sinceId] (ntfy message id).
     * Returns parsed app messages (event book/cancel/move) newest-last.
     */
    suspend fun poll(
        settings: ClinicSettings,
        sinceId: String,
    ): Result<List<RelayMessage>> = withContext(Dispatchers.IO) {
        try {
            require(settings.isConfigured()) { "not configured" }
            val base = settings.normalizedBaseUrl()
            val topic = settings.topic.trim().trim('/')
            val sinceParam = if (sinceId.isNotBlank()) {
                "&since=" + URLEncoder.encode(sinceId, Charsets.UTF_8.name())
            } else {
                "&since=10m"
            }
            val url = URL("$base/$topic/json?poll=1$sinceParam")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 15_000
                readTimeout = 30_000
                setRequestProperty("Authorization", "Bearer ${settings.token.trim()}")
                setRequestProperty("Accept", "application/x-ndjson, application/json, text/plain")
            }
            try {
                val code = conn.responseCode
                if (code !in 200..299) {
                    val err = (conn.errorStream ?: conn.inputStream)
                        ?.bufferedReader()
                        ?.use { it.readText() }
                        .orEmpty()
                    return@withContext Result.failure(
                        IllegalStateException("HTTP $code${if (err.isNotBlank()) ": $err" else ""}"),
                    )
                }
                val reader = BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8))
                val out = mutableListOf<RelayMessage>()
                reader.useLines { lines ->
                    for (line in lines) {
                        if (line.isBlank()) continue
                        parseNtfyLine(line)?.let { out.add(it) }
                    }
                }
                Result.success(out)
            } finally {
                conn.disconnect()
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Long-poll until one batch arrives or [readTimeoutMs] elapses.
     * Used while the app process is alive for snappier delivery.
     */
    suspend fun longPoll(
        settings: ClinicSettings,
        sinceId: String,
        readTimeoutMs: Int = 55_000,
    ): Result<List<RelayMessage>> = withContext(Dispatchers.IO) {
        try {
            require(settings.isConfigured()) { "not configured" }
            val base = settings.normalizedBaseUrl()
            val topic = settings.topic.trim().trim('/')
            val sinceParam = if (sinceId.isNotBlank()) {
                "?since=" + URLEncoder.encode(sinceId, Charsets.UTF_8.name())
            } else {
                "?since=10m"
            }
            val url = URL("$base/$topic/json$sinceParam")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 15_000
                readTimeout = readTimeoutMs
                setRequestProperty("Authorization", "Bearer ${settings.token.trim()}")
                setRequestProperty("Accept", "application/x-ndjson, application/json, text/plain")
            }
            try {
                val code = conn.responseCode
                if (code !in 200..299) {
                    val err = (conn.errorStream ?: conn.inputStream)
                        ?.bufferedReader()
                        ?.use { it.readText() }
                        .orEmpty()
                    return@withContext Result.failure(
                        IllegalStateException("HTTP $code${if (err.isNotBlank()) ": $err" else ""}"),
                    )
                }
                val reader = BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8))
                val out = mutableListOf<RelayMessage>()
                // Read available lines; long-poll may return one or more then close.
                reader.useLines { lines ->
                    for (line in lines) {
                        if (line.isBlank()) continue
                        parseNtfyLine(line)?.let { out.add(it) }
                        // After first message batch on open stream, return so cursor advances.
                        if (out.isNotEmpty()) break
                    }
                }
                Result.success(out)
            } finally {
                conn.disconnect()
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseNtfyLine(line: String): RelayMessage? {
        return try {
            val envelope = JSONObject(line)
            val eventType = envelope.optString("event", "")
            if (eventType == "open" || eventType == "keepalive") return null
            val id = envelope.optString("id", "").ifBlank {
                envelope.optLong("time", 0L).toString()
            }
            val message = envelope.optString("message", "")
            if (message.isBlank()) return null
            val payload = try {
                JSONObject(message)
            } catch (_: Exception) {
                return null
            }
            if (payload.optInt("v", 0) != 1) return null
            val event = payload.optString("event", "")
            if (event != "book" && event != "cancel" && event != "move") return null
            RelayMessage(
                id = id,
                event = event,
                personnelId = payload.optLong("personnelId", 0L),
                initials = payload.optString("initials", ""),
                day = payload.optString("day", ""),
                time = payload.optString("time", ""),
                title = payload.optString("title", ""),
                raw = message,
            )
        } catch (_: Exception) {
            null
        }
    }
}
