package app.nobat.mobile.remind

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import app.nobat.mobile.NobatApp
import app.nobat.mobile.R
import app.nobat.mobile.notify.SmtpClient

/**
 * Sends the scheduled SMTP reminder email to the assigned personnel email
 * (passed in work data as a snapshot). Never logs the SMTP password.
 */
class ReminderWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val accountId = inputData.getLong(KEY_ACCOUNT_ID, -1L)
        val initials = inputData.getString(KEY_INITIALS).orEmpty()
        val day = inputData.getString(KEY_DAY).orEmpty()
        val startMinute = inputData.getInt(KEY_START_MINUTE, 0)
        val durationMin = inputData.getInt(KEY_DURATION_MIN, 60)
        val recipient = inputData.getString(KEY_RECIPIENT_EMAIL).orEmpty().trim()
        if (accountId < 0L || day.isBlank() || recipient.isBlank()) return Result.failure()

        val app = applicationContext as? NobatApp
            ?: return Result.failure()
        val settings = app.notificationStore.load(accountId)
        if (!settings.remindersOn || !ReminderScheduler.isSmtpConfigured(settings)) {
            return Result.success()
        }

        val time = "%02d:%02d".format(startMinute / 60, startMinute % 60)
        val subject = applicationContext.getString(
            R.string.reminder_email_subject,
            initials,
        )
        val body = applicationContext.getString(
            R.string.reminder_email_body,
            initials,
            day,
            time,
            durationMin,
        )
        val result = SmtpClient.send(
            SmtpClient.MailRequest(
                host = settings.smtpHost,
                port = settings.smtpPort,
                useTls = settings.smtpUseTls,
                username = settings.smtpUsername,
                password = settings.smtpPassword,
                from = settings.smtpFrom,
                to = recipient,
                subject = subject,
                body = body,
            ),
        )
        return if (result.isSuccess) Result.success() else Result.retry()
    }

    companion object {
        const val KEY_ACCOUNT_ID = "account_id"
        const val KEY_APPOINTMENT_ID = "appointment_id"
        const val KEY_INITIALS = "initials"
        const val KEY_DAY = "day"
        const val KEY_START_MINUTE = "start_minute"
        const val KEY_DURATION_MIN = "duration_min"
        const val KEY_RECIPIENT_EMAIL = "recipient_email"
    }
}
