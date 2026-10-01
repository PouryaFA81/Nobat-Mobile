package app.nobat.mobile.digest

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import app.nobat.mobile.NobatApp
import app.nobat.mobile.notify.NotificationSettings
import app.nobat.mobile.remind.ReminderScheduler
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Schedules the daily evening digest with WorkManager (OneTime + self-reschedule).
 *
 * Unique work name: `evening_digest_{accountId}` — replace when time/toggle changes.
 */
object EveningDigestScheduler {
    fun workName(accountId: Long) = "evening_digest_$accountId"

    /**
     * Schedule or cancel digest for one account based on [settings].
     * Fires at the next [NotificationSettings.digestTime] (today if still ahead, else tomorrow).
     */
    fun schedule(context: Context, accountId: Long, settings: NotificationSettings) {
        if (accountId <= 0L) return
        if (!settings.eveningDigestOn) {
            cancel(context, accountId)
            return
        }
        val delayMs = delayUntilNextDigestMs(settings.digestHour, settings.digestMinute)
        if (delayMs <= 0L) {
            cancel(context, accountId)
            return
        }
        val data = workDataOf(EveningDigestWorker.KEY_ACCOUNT_ID to accountId)
        val request = OneTimeWorkRequestBuilder<EveningDigestWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .addTag(workName(accountId))
            .build()
        WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
            workName(accountId),
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    fun cancel(context: Context, accountId: Long) {
        if (accountId <= 0L) return
        WorkManager.getInstance(context.applicationContext)
            .cancelUniqueWork(workName(accountId))
    }

    /** After boot / app start: (re)schedule digests for every local account. */
    suspend fun ensureAll(context: Context) = withContext(Dispatchers.IO) {
        val app = context.applicationContext as? NobatApp ?: return@withContext
        val accounts = app.database.accounts().listAll()
        for (account in accounts) {
            val settings = app.notificationStore.load(account.id)
            schedule(context, account.id, settings)
        }
    }

    fun delayUntilNextDigestMs(hour: Int, minute: Int): Long {
        val zone = ZoneId.systemDefault()
        val now = LocalDateTime.now(zone)
        var fire = LocalDateTime.of(LocalDate.now(zone), LocalTime.of(hour, minute))
        if (!fire.isAfter(now)) {
            fire = fire.plusDays(1)
        }
        return fire.atZone(zone).toInstant().toEpochMilli() - System.currentTimeMillis()
    }

    fun isSmtpConfigured(settings: NotificationSettings): Boolean =
        ReminderScheduler.isSmtpConfigured(settings)
}
