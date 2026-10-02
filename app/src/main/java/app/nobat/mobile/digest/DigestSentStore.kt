package app.nobat.mobile.digest

import android.content.Context

/**
 * Persists digest per-channel success markers (non-sensitive) in ordinary prefs.
 * Keyed by account + fire day + digest time slot so a time change gets a fresh slot.
 */
class DigestSentStore(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(accountId: Long, fireDayIso: String, hour: Int, minute: Int): DigestSentState {
        val p = prefix(accountId, fireDayIso, hour, minute)
        val emailsRaw = prefs.getString(p + KEY_SMTP_EMAILS, "") ?: ""
        val emails = emailsRaw.split('\u001e')
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }
            .toSet()
        return DigestSentState(
            local = prefs.getBoolean(p + KEY_LOCAL, false),
            telegram = prefs.getBoolean(p + KEY_TELEGRAM, false),
            bale = prefs.getBoolean(p + KEY_BALE, false),
            clinic = prefs.getBoolean(p + KEY_CLINIC, false),
            smtpEmails = emails,
        )
    }

    fun save(accountId: Long, fireDayIso: String, hour: Int, minute: Int, state: DigestSentState) {
        val p = prefix(accountId, fireDayIso, hour, minute)
        prefs.edit()
            .putBoolean(p + KEY_LOCAL, state.local)
            .putBoolean(p + KEY_TELEGRAM, state.telegram)
            .putBoolean(p + KEY_BALE, state.bale)
            .putBoolean(p + KEY_CLINIC, state.clinic)
            .putString(p + KEY_SMTP_EMAILS, state.smtpEmails.joinToString("\u001e"))
            .apply()
    }

    private fun prefix(accountId: Long, fireDayIso: String, hour: Int, minute: Int): String {
        val slot = DigestIdempotency.slotKey(fireDayIso, hour, minute)
        return "acct_${accountId}_$slot|"
    }

    companion object {
        private const val PREFS_NAME = "nobat_digest_sent"
        private const val KEY_LOCAL = "local"
        private const val KEY_TELEGRAM = "telegram"
        private const val KEY_BALE = "bale"
        private const val KEY_CLINIC = "clinic"
        private const val KEY_SMTP_EMAILS = "smtp_emails"
    }
}
