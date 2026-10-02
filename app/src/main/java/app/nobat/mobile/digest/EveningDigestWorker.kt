package app.nobat.mobile.digest

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import app.nobat.mobile.NobatApp
import app.nobat.mobile.R
import app.nobat.mobile.data.Appointment
import app.nobat.mobile.notify.ClinicNotifier
import app.nobat.mobile.notify.ClinicRelayClient
import app.nobat.mobile.notify.SmtpClient
import app.nobat.mobile.notify.TelegramClient
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Builds tomorrow’s schedule digest: SMTP per personnel, optional Telegram,
 * Clinic relay event=digest, and a local notification. Then reschedules the next day.
 *
 * Per-channel idempotency: WorkManager retries skip channels already marked sent
 * for this fire-day + digest time slot (e.g. SMTP fail must not re-send Telegram).
 * Never logs SMTP / Telegram / relay secrets.
 */
class EveningDigestWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val accountId = inputData.getLong(KEY_ACCOUNT_ID, -1L)
        if (accountId < 0L) return Result.failure()

        val app = applicationContext as? NobatApp ?: return Result.failure()
        val settings = app.notificationStore.load(accountId)

        // Always try to chain the next run when still enabled.
        if (settings.eveningDigestOn) {
            EveningDigestScheduler.schedule(applicationContext, accountId, settings)
        } else {
            EveningDigestScheduler.cancel(applicationContext, accountId)
            return Result.success()
        }

        val fireDay = LocalDate.now().format(DAY_FMT)
        val hour = settings.digestHour
        val minute = settings.digestMinute
        val sentStore = DigestSentStore(applicationContext)
        var state = sentStore.load(accountId, fireDay, hour, minute)

        val tomorrow = LocalDate.now().plusDays(1)
        val dayIso = tomorrow.format(DAY_FMT)
        val all = app.database.appointments().between(accountId, dayIso, dayIso)
            .filter { it.status != "cancelled" }
            .sortedBy { it.startMinute }

        var anyHardFailure = false

        // Local in-app notification (this device).
        if (DigestIdempotency.shouldSendLocal(state)) {
            ClinicNotifier.notifyDigest(applicationContext, dayIso, all.size)
            state = state.withLocal()
            sentStore.save(accountId, fireDay, hour, minute, state)
        }

        // Telegram: one message when Notify on book is configured.
        val tg = app.telegramStore.load(accountId)
        if (tg.notifyOnBook && tg.isConfigured() && DigestIdempotency.shouldSendTelegram(state)) {
            val tgBody = buildTelegramBody(dayIso, all)
            val tgResult = TelegramClient.sendMessage(tg.botToken, tg.chatId, tgBody)
            if (tgResult.isSuccess) {
                state = state.withTelegram()
                sentStore.save(accountId, fireDay, hour, minute, state)
            } else {
                anyHardFailure = true
            }
        }

        // Clinic relay: one publish per personnel who has slots tomorrow.
        val clinic = app.clinicStore.load(accountId)
        if (clinic.isConfigured() && all.isNotEmpty() && DigestIdempotency.shouldSendClinic(state)) {
            val byPersonnel = all.groupBy { it.personnelId }
            val title = applicationContext.getString(R.string.evening_digest)
            var clinicOk = true
            for ((personnelId, slots) in byPersonnel) {
                if (personnelId <= 0L) continue
                val first = slots.first()
                val time = formatTime(first.startMinute)
                val pub = ClinicRelayClient.publish(
                    settings = clinic,
                    event = "digest",
                    personnelId = personnelId,
                    initials = slots.size.toString(),
                    day = dayIso,
                    time = time,
                    title = title,
                )
                if (pub.isFailure) {
                    clinicOk = false
                    break
                }
            }
            if (clinicOk) {
                state = state.withClinic()
                sentStore.save(accountId, fireDay, hour, minute, state)
            } else {
                anyHardFailure = true
            }
        }

        // SMTP: one email per personnel email that has slots tomorrow.
        if (EveningDigestScheduler.isSmtpConfigured(settings) && all.isNotEmpty()) {
            val byEmail = all.groupBy { it.personnelEmail.trim().lowercase() }
            for ((emailKey, slots) in byEmail) {
                if (emailKey.isBlank()) continue
                if (!DigestIdempotency.shouldSendSmtp(state, emailKey)) continue
                val to = slots.first().personnelEmail.trim()
                val subject = applicationContext.getString(R.string.digest_subject, dayIso)
                val body = buildEmailBody(dayIso, slots)
                val result = SmtpClient.send(
                    SmtpClient.MailRequest(
                        host = settings.smtpHost,
                        port = settings.smtpPort,
                        useTls = settings.smtpUseTls,
                        username = settings.smtpUsername,
                        password = settings.smtpPassword,
                        from = settings.smtpFrom,
                        to = to,
                        subject = subject,
                        body = body,
                    ),
                )
                if (result.isSuccess) {
                    state = state.withSmtpEmail(emailKey)
                    sentStore.save(accountId, fireDay, hour, minute, state)
                } else {
                    anyHardFailure = true
                }
            }
        }

        return if (anyHardFailure) Result.retry() else Result.success()
    }

    private fun buildEmailBody(dayIso: String, slots: List<Appointment>): String {
        val header = applicationContext.getString(R.string.digest_header, dayIso)
        if (slots.isEmpty()) {
            return header + "\n" + applicationContext.getString(R.string.digest_empty)
        }
        val lines = slots.joinToString("\n") { a ->
            applicationContext.getString(
                R.string.digest_line,
                a.initials,
                formatTime(a.startMinute),
                a.durationMin,
            )
        }
        return "$header\n$lines"
    }

    private fun buildTelegramBody(dayIso: String, slots: List<Appointment>): String {
        val header = applicationContext.getString(R.string.digest_header, dayIso)
        if (slots.isEmpty()) {
            return header + "\n" + applicationContext.getString(R.string.digest_empty)
        }
        val lines = slots.joinToString("\n") { a ->
            applicationContext.getString(
                R.string.digest_line,
                a.initials,
                formatTime(a.startMinute),
                a.durationMin,
            )
        }
        return "$header\n$lines"
    }

    private fun formatTime(startMinute: Int): String =
        "%02d:%02d".format(startMinute / 60, startMinute % 60)

    companion object {
        const val KEY_ACCOUNT_ID = "account_id"
        private val DAY_FMT = DateTimeFormatter.ISO_LOCAL_DATE
    }
}
