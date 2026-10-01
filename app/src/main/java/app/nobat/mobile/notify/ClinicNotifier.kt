package app.nobat.mobile.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import app.nobat.mobile.R

/**
 * In-app NotificationCompat for book + cancel (local + clinic relay).
 */
object ClinicNotifier {
    const val CHANNEL_ID = "clinic_notifications"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val existing = manager.getNotificationChannel(CHANNEL_ID)
        if (existing != null) return
        val name = context.getString(R.string.clinic_notifications_channel)
        val channel = NotificationChannel(
            CHANNEL_ID,
            name,
            NotificationManager.IMPORTANCE_DEFAULT,
        )
        manager.createNotificationChannel(channel)
    }

    fun notifyBooked(
        context: Context,
        initials: String,
        day: String,
        time: String,
    ) {
        ensureChannel(context)
        val title = context.getString(R.string.notif_new_appointment)
        val body = "$initials — $day $time"
        post(context, id = System.currentTimeMillis().toInt(), title = title, body = body)
    }

    fun notifyCancelled(
        context: Context,
        initials: String,
        day: String,
        time: String,
    ) {
        ensureChannel(context)
        val title = context.getString(R.string.notif_appointment_cancelled)
        val body = "$initials — $day $time"
        post(context, id = System.currentTimeMillis().toInt(), title = title, body = body)
    }

    private fun post(context: Context, id: Int, title: String, body: String) {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS denied on API 33+ — ignore.
        }
    }
}
