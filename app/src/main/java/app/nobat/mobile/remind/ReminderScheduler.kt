package app.nobat.mobile.remind

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import app.nobat.mobile.data.Appointment
import app.nobat.mobile.notify.NotificationSettings
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

/**
 * Schedules email reminders with **WorkManager** (OneTimeWorkRequest + initial delay).
 *
 * Recipient is the appointment’s [Appointment.personnelEmail] snapshot (assigned staff),
 * not a global inbox. SMTP credentials still come from per-account notification settings.
 *
 * Unique work name: `reminder_{appointmentId}` — replace on reschedule, cancel on delete.
 */
object ReminderScheduler {
    const val LEAD_MINUTES = 60L

    fun workName(appointmentId: Long) = "reminder_$appointmentId"

    /** SMTP host + from/username ready (recipient comes from personnel on the appointment). */
    fun isSmtpConfigured(settings: NotificationSettings): Boolean =
        settings.smtpHost.isNotBlank() &&
            (settings.smtpFrom.isNotBlank() || settings.smtpUsername.isNotBlank())

    /**
     * Ready to schedule: SMTP configured and appointment has a personnel email snapshot.
     */
    fun canSendToPersonnel(settings: NotificationSettings, appointment: Appointment): Boolean =
        isSmtpConfigured(settings) && appointment.personnelEmail.isNotBlank()

    /**
     * Schedule or replace a reminder if [settings].remindersOn and SMTP + personnel email ready.
     * Skips (and cancels any prior work) when the fire time is already past.
     */
    fun schedule(
        context: Context,
        appointment: Appointment,
        settings: NotificationSettings,
    ) {
        if (appointment.id <= 0L) return
        if (!settings.remindersOn || !canSendToPersonnel(settings, appointment)) {
            cancel(context, appointment.id)
            return
        }
        val start = appointmentStart(appointment) ?: run {
            cancel(context, appointment.id)
            return
        }
        val fireAt = start.minusMinutes(LEAD_MINUTES)
        val now = LocalDateTime.now()
        if (!fireAt.isAfter(now)) {
            cancel(context, appointment.id)
            return
        }
        val delayMs = fireAt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() -
            System.currentTimeMillis()
        if (delayMs <= 0L) {
            cancel(context, appointment.id)
            return
        }
        val data = workDataOf(
            ReminderWorker.KEY_ACCOUNT_ID to appointment.accountId,
            ReminderWorker.KEY_APPOINTMENT_ID to appointment.id,
            ReminderWorker.KEY_INITIALS to appointment.initials,
            ReminderWorker.KEY_DAY to appointment.day,
            ReminderWorker.KEY_START_MINUTE to appointment.startMinute,
            ReminderWorker.KEY_DURATION_MIN to appointment.durationMin,
            ReminderWorker.KEY_RECIPIENT_EMAIL to appointment.personnelEmail,
        )
        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .addTag(workName(appointment.id))
            .build()
        WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
            workName(appointment.id),
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    fun cancel(context: Context, appointmentId: Long) {
        if (appointmentId <= 0L) return
        WorkManager.getInstance(context.applicationContext)
            .cancelUniqueWork(workName(appointmentId))
    }

    /** Reschedule all future appointments for an account (e.g. after enabling reminders). */
    fun rescheduleAll(
        context: Context,
        appointments: List<Appointment>,
        settings: NotificationSettings,
    ) {
        for (a in appointments) {
            schedule(context, a, settings)
        }
    }

    private fun appointmentStart(a: Appointment): LocalDateTime? {
        return try {
            val day = LocalDate.parse(a.day)
            val h = a.startMinute / 60
            val m = a.startMinute % 60
            LocalDateTime.of(day, java.time.LocalTime.of(h, m))
        } catch (_: Exception) {
            null
        }
    }
}
