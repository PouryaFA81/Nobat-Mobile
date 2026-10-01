package app.nobat.mobile.update

import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

/**
 * Checks GitHub Releases for PouryaFA81/Nobat-Mobile (includes prereleases).
 * Uses the releases list (newest first) so prerelease tags are visible.
 */
object UpdateChecker {
    private const val RELEASES_URL =
        "https://api.github.com/repos/PouryaFA81/Nobat-Mobile/releases"
    private const val RELEASE_PAGE_PREFIX =
        "https://github.com/PouryaFA81/Nobat-Mobile/releases/tag/"

    data class Result(
        val tagName: String,
        val remoteVersion: String,
        val localVersion: String,
        val updateAvailable: Boolean,
    ) {
        val releaseUrl: String get() = RELEASE_PAGE_PREFIX + tagName
    }

    suspend fun check(localVersionName: String): Result? = withContext(Dispatchers.IO) {
        val conn = (URL(RELEASES_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "Nobat-Mobile")
            connectTimeout = 12_000
            readTimeout = 12_000
        }
        try {
            if (conn.responseCode != HttpURLConnection.HTTP_OK) return@withContext null
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val arr = JSONArray(body)
            if (arr.length() == 0) return@withContext null
            // Prefer first non-draft; GitHub returns newest first.
            var tag: String? = null
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                if (obj.optBoolean("draft", false)) continue
                tag = obj.getString("tag_name")
                break
            }
            if (tag.isNullOrBlank()) return@withContext null
            val remote = normalizeVersion(tag)
            val local = normalizeVersion(localVersionName)
            Result(
                tagName = tag,
                remoteVersion = remote,
                localVersion = local,
                updateAvailable = compareSemVer(remote, local) > 0,
            )
        } catch (_: Exception) {
            null
        } finally {
            conn.disconnect()
        }
    }

    /** Strip leading "v" / "V" and trim. */
    fun normalizeVersion(raw: String): String =
        raw.trim().removePrefix("v").removePrefix("V").trim()

    /**
     * Compare dotted numeric versions (e.g. 0.5.1). Missing segments treated as 0.
     * @return >0 if a > b, 0 if equal, <0 if a < b
     */
    fun compareSemVer(a: String, b: String): Int {
        val pa = a.split('.').map { it.toIntOrNull() ?: 0 }
        val pb = b.split('.').map { it.toIntOrNull() ?: 0 }
        val n = maxOf(pa.size, pb.size)
        for (i in 0 until n) {
            val av = pa.getOrElse(i) { 0 }
            val bv = pb.getOrElse(i) { 0 }
            if (av != bv) return av - bv
        }
        return 0
    }
}
