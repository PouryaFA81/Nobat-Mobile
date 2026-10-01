package app.nobat.mobile.notify

import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Opens the device SMS app via ACTION_SENDTO / smsto: — no SEND_SMS permission.
 * User confirms send in their messaging app.
 */
object SmsIntent {

    fun openComposer(
        context: Context,
        body: String,
        phone: String = "",
    ): Boolean {
        val uri = if (phone.isBlank()) {
            Uri.parse("smsto:")
        } else {
            Uri.parse("smsto:${Uri.encode(phone.trim())}")
        }
        val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
            putExtra("sms_body", body)
            // Some OEMs read this:
            putExtra(Intent.EXTRA_TEXT, body)
        }
        return try {
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    /** Sample / appointment reminder template (caller supplies localized strings). */
    fun reminderBody(
        initials: String,
        date: String,
        time: String,
        durationMin: Int,
        minutesSuffix: String,
        template: String,
    ): String = template
        .replace("{initials}", initials)
        .replace("{date}", date)
        .replace("{time}", time)
        .replace("{duration}", "$durationMin $minutesSuffix")
}
