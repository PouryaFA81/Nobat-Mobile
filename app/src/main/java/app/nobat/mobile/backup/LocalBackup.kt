package app.nobat.mobile.backup

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import app.nobat.mobile.BuildConfig
import app.nobat.mobile.data.AppDatabase
import app.nobat.mobile.data.Appointment
import app.nobat.mobile.data.Personnel
import app.nobat.mobile.notify.NotificationSettings
import app.nobat.mobile.notify.NotificationSettingsStore
import app.nobat.mobile.remind.ReminderScheduler
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Account-scoped local backup / restore as JSON.
 *
 * Includes appointments + personnel for one account, plus non-secret notification prefs
 * (SMTP password excluded). Account password hashes and app-lock PIN are never exported.
 */
object LocalBackup {
    const val FORMAT = "nobat-mobile-backup"
    const val FORMAT_VERSION = 1
    const val MIME_JSON = "application/json"
    const val FILE_EXTENSION = ".nobatbak.json"

    fun suggestedFileName(): String {
        val day = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
        return "nobat-backup-$day$FILE_EXTENSION"
    }

    /**
     * Build backup JSON for [accountId] and write it to [uri] (SAF CreateDocument).
     */
    suspend fun exportToUri(
        context: Context,
        db: AppDatabase,
        notifyStore: NotificationSettingsStore,
        accountId: Long,
        uri: Uri,
    ): Result<Unit> = runCatching {
        val json = buildJson(db, notifyStore, accountId)
        context.contentResolver.openOutputStream(uri)?.use { out ->
            OutputStreamWriter(out, StandardCharsets.UTF_8).use { it.write(json) }
        } ?: error("open_failed")
    }

    /**
     * Write backup JSON into the public Downloads folder (MediaStore on Q+, legacy path below).
     * Returns the display name written.
     */
    suspend fun exportToDownloads(
        context: Context,
        db: AppDatabase,
        notifyStore: NotificationSettingsStore,
        accountId: Long,
    ): Result<String> = runCatching {
        val json = buildJson(db, notifyStore, accountId)
        val name = suggestedFileName()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, name)
                put(MediaStore.Downloads.MIME_TYPE, MIME_JSON)
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: error("insert_failed")
            resolver.openOutputStream(uri)?.use { out ->
                OutputStreamWriter(out, StandardCharsets.UTF_8).use { it.write(json) }
            } ?: error("open_failed")
            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        } else {
            @Suppress("DEPRECATION")
            val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, name)
            file.writeText(json, StandardCharsets.UTF_8)
        }
        name
    }

    /**
     * Parse backup from [uri], replace [accountId]'s appointments + personnel,
     * restore non-secret notification prefs, and reschedule reminders.
     */
    suspend fun restoreFromUri(
        context: Context,
        db: AppDatabase,
        notifyStore: NotificationSettingsStore,
        accountId: Long,
        uri: Uri,
    ): Result<Unit> = runCatching {
        val text = context.contentResolver.openInputStream(uri)?.use { input ->
            BufferedReader(InputStreamReader(input, StandardCharsets.UTF_8)).readText()
        } ?: error("open_failed")
        restoreFromJson(context, db, notifyStore, accountId, text)
    }

    suspend fun restoreFromJson(
        context: Context,
        db: AppDatabase,
        notifyStore: NotificationSettingsStore,
        accountId: Long,
        text: String,
    ) {
        val root = JSONObject(text)
        if (root.optString("format") != FORMAT) error("bad_format")
        if (root.optInt("version", 0) < 1) error("bad_version")

        val personnelArr = root.optJSONArray("personnel") ?: JSONArray()
        val appointmentsArr = root.optJSONArray("appointments") ?: JSONArray()

        val oldAppointments = db.appointments().allForAccount(accountId)
        for (a in oldAppointments) {
            ReminderScheduler.cancel(context, a.id)
        }

        db.appointments().deleteAllForAccount(accountId)
        db.personnel().deleteAllForAccount(accountId)

        val idMap = mutableMapOf<Long, Long>() // old personnel id → new
        for (i in 0 until personnelArr.length()) {
            val o = personnelArr.getJSONObject(i)
            val oldId = o.optLong("id", 0L)
            val person = Personnel(
                id = 0,
                accountId = accountId,
                name = o.optString("name", ""),
                email = o.optString("email", ""),
                phone = o.optString("phone", ""),
                notes = o.optString("notes", ""),
            )
            val newId = db.personnel().insert(person)
            if (oldId > 0L) idMap[oldId] = newId
        }

        val restored = mutableListOf<Appointment>()
        for (i in 0 until appointmentsArr.length()) {
            val o = appointmentsArr.getJSONObject(i)
            val oldPersonnelId = o.optLong("personnelId", 0L)
            val mappedPersonnelId = when {
                oldPersonnelId <= 0L -> 0L
                else -> idMap[oldPersonnelId] ?: 0L
            }
            val appt = Appointment(
                id = 0,
                accountId = accountId,
                day = o.getString("day"),
                startMinute = o.getInt("startMinute"),
                durationMin = o.optInt("durationMin", 60),
                initials = o.optString("initials", ""),
                note = o.optString("note", ""),
                status = o.optString("status", "active"),
                personnelId = mappedPersonnelId,
                personnelEmail = o.optString("personnelEmail", ""),
            )
            val newId = db.appointments().upsert(appt)
            restored.add(appt.copy(id = newId))
        }

        val notifyObj = root.optJSONObject("notificationSettings")
        if (notifyObj != null) {
            val existing = notifyStore.load(accountId)
            notifyStore.save(
                accountId,
                NotificationSettings(
                    remindersOn = notifyObj.optBoolean("remindersOn", existing.remindersOn),
                    smtpHost = notifyObj.optString("smtpHost", existing.smtpHost),
                    smtpPort = notifyObj.optInt("smtpPort", existing.smtpPort),
                    smtpUseTls = notifyObj.optBoolean("smtpUseTls", existing.smtpUseTls),
                    smtpUsername = notifyObj.optString("smtpUsername", existing.smtpUsername),
                    // Never restore SMTP password from backup file.
                    smtpPassword = existing.smtpPassword,
                    smtpFrom = notifyObj.optString("smtpFrom", existing.smtpFrom),
                    testRecipient = notifyObj.optString("testRecipient", existing.testRecipient),
                ),
            )
        }

        val settings = notifyStore.load(accountId)
        ReminderScheduler.rescheduleAll(context, restored, settings)
    }

    private suspend fun buildJson(
        db: AppDatabase,
        notifyStore: NotificationSettingsStore,
        accountId: Long,
    ): String {
        val account = db.accounts().getById(accountId)
        val personnel = db.personnel().listForAccount(accountId)
        val appointments = db.appointments().allForAccount(accountId)
        val settings = notifyStore.load(accountId)

        val root = JSONObject()
        root.put("format", FORMAT)
        root.put("version", FORMAT_VERSION)
        root.put("appVersion", BuildConfig.VERSION_NAME)
        root.put("exportedAt", System.currentTimeMillis())
        root.put("accountId", accountId)
        if (account != null) {
            root.put(
                "account",
                JSONObject()
                    .put("displayName", account.displayName)
                    .put("createdAt", account.createdAt),
            )
        }

        val pArr = JSONArray()
        for (p in personnel) {
            pArr.put(
                JSONObject()
                    .put("id", p.id)
                    .put("name", p.name)
                    .put("email", p.email)
                    .put("phone", p.phone)
                    .put("notes", p.notes),
            )
        }
        root.put("personnel", pArr)

        val aArr = JSONArray()
        for (a in appointments) {
            aArr.put(
                JSONObject()
                    .put("day", a.day)
                    .put("startMinute", a.startMinute)
                    .put("durationMin", a.durationMin)
                    .put("initials", a.initials)
                    .put("note", a.note)
                    .put("status", a.status)
                    .put("personnelId", a.personnelId)
                    .put("personnelEmail", a.personnelEmail),
            )
        }
        root.put("appointments", aArr)

        // Non-secret notification prefs only — omit smtpPassword.
        root.put(
            "notificationSettings",
            JSONObject()
                .put("remindersOn", settings.remindersOn)
                .put("smtpHost", settings.smtpHost)
                .put("smtpPort", settings.smtpPort)
                .put("smtpUseTls", settings.smtpUseTls)
                .put("smtpUsername", settings.smtpUsername)
                .put("smtpFrom", settings.smtpFrom)
                .put("testRecipient", settings.testRecipient),
        )

        return root.toString(2)
    }
}
