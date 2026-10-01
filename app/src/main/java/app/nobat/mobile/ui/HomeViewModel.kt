package app.nobat.mobile.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import app.nobat.mobile.R
import app.nobat.mobile.calendar.Jalali
import app.nobat.mobile.data.Account
import app.nobat.mobile.data.AccountRepository
import app.nobat.mobile.data.Appointment
import app.nobat.mobile.data.AppointmentDao
import app.nobat.mobile.data.DayCount
import app.nobat.mobile.notify.NotificationSettingsStore
import app.nobat.mobile.notify.SmtpClient
import app.nobat.mobile.remind.ReminderScheduler
import app.nobat.mobile.session.AccountSession
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    app: Application,
    private val dao: AppointmentDao,
    private val accountRepo: AccountRepository,
    private val session: AccountSession,
    private val notificationStore: NotificationSettingsStore,
) : AndroidViewModel(app) {
    private val dayFmt = DateTimeFormatter.ISO_LOCAL_DATE
    private val appContext = app.applicationContext

    private val _day = MutableStateFlow(LocalDate.now())
    val day: StateFlow<LocalDate> = _day

    /** Any day inside the currently visible calendar month (Gregorian storage). */
    private val _monthAnchor = MutableStateFlow(LocalDate.now().withDayOfMonth(1))
    val monthAnchor: StateFlow<LocalDate> = _monthAnchor

    /** @deprecated Prefer [monthAnchor]; kept for callers that still use YearMonth. */
    val month: StateFlow<YearMonth> = _monthAnchor
        .map { YearMonth.from(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), YearMonth.now())

    val accounts: StateFlow<List<Account>> = accountRepo.observeAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val unlockedAccountId: StateFlow<Long?> = session.unlockedAccountId

    val unlockedAccount: StateFlow<Account?> = combine(accounts, unlockedAccountId) { list, id ->
        list.firstOrNull { it.id == id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _needsOrphanMigration = MutableStateFlow(false)
    val needsOrphanMigration: StateFlow<Boolean> = _needsOrphanMigration

    private val _bootReady = MutableStateFlow(false)
    val bootReady: StateFlow<Boolean> = _bootReady

    /** When true, month grid / labels use Jalali (FA). Toggled from UI via [setUseJalali]. */
    private val _useJalali = MutableStateFlow(false)
    val useJalali: StateFlow<Boolean> = _useJalali

    fun setUseJalali(enabled: Boolean) {
        _useJalali.value = enabled
    }

    init {
        viewModelScope.launch {
            _needsOrphanMigration.value = accountRepo.needsOrphanMigration()
            _bootReady.value = true
        }
    }

    val appointments: StateFlow<List<Appointment>> =
        combine(_day, unlockedAccountId) { d, accountId -> d to accountId }
            .flatMapLatest { (d, accountId) ->
                if (accountId == null) flowOf(emptyList())
                else dao.forDay(accountId, d.format(dayFmt))
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Map of ISO day → appointment count for the visible calendar month. */
    val monthCounts: StateFlow<Map<String, Int>> =
        combine(_monthAnchor, unlockedAccountId, _useJalali) { anchor, accountId, jalali ->
            Triple(anchor, accountId, jalali)
        }
            .flatMapLatest { (anchor, accountId, jalali) ->
                if (accountId == null) flowOf(emptyMap())
                else {
                    val (start, end) = if (jalali) {
                        Jalali.monthBounds(anchor)
                    } else {
                        val ym = YearMonth.from(anchor)
                        ym.atDay(1) to ym.atEndOfMonth()
                    }
                    dao.countsBetween(accountId, start.format(dayFmt), end.format(dayFmt))
                        .map { list: List<DayCount> -> list.associate { it.day to it.count } }
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    fun prevDay() { _day.value = _day.value.minusDays(1) }
    fun nextDay() { _day.value = _day.value.plusDays(1) }
    fun goToday() {
        val today = LocalDate.now()
        _day.value = today
        _monthAnchor.value = today
    }

    fun prevMonth() {
        val jalali = _useJalali.value
        _monthAnchor.value = if (jalali) {
            Jalali.plusMonths(_monthAnchor.value, -1)
        } else {
            YearMonth.from(_monthAnchor.value).minusMonths(1).atDay(1)
        }
    }

    fun nextMonth() {
        val jalali = _useJalali.value
        _monthAnchor.value = if (jalali) {
            Jalali.plusMonths(_monthAnchor.value, 1)
        } else {
            YearMonth.from(_monthAnchor.value).plusMonths(1).atDay(1)
        }
    }

    fun selectDay(date: LocalDate) {
        _day.value = date
        _monthAnchor.value = date
    }

    sealed class BookResult {
        data class Ok(
            val reminderScheduled: Boolean,
            val confirmationSent: Boolean?,
            val needRecipient: Boolean,
            val needSmtp: Boolean,
        ) : BookResult()
        data object Failed : BookResult()
    }

    /**
     * Book appointment; schedule WorkManager reminder when reminders+SMTP ready;
     * optionally send confirmation email to reminder recipient.
     */
    suspend fun book(
        initials: String,
        startMinute: Int,
        durationMin: Int,
        note: String,
        sendConfirmation: Boolean = true,
    ): BookResult {
        val clean = initials.trim()
        if (clean.isEmpty()) return BookResult.Failed
        val accountId = session.unlockedAccountId.value ?: return BookResult.Failed
        val appt = Appointment(
            accountId = accountId,
            day = _day.value.format(dayFmt),
            startMinute = startMinute.coerceIn(0, 23 * 60 + 59),
            durationMin = durationMin.coerceAtLeast(15),
            initials = clean,
            note = note.trim(),
        )
        val newId = dao.upsert(appt)
        val saved = appt.copy(id = newId)
        val settings = notificationStore.load(accountId)

        var reminderScheduled = false
        var confirmationSent: Boolean? = null
        var needRecipient = false
        var needSmtp = false

        if (settings.remindersOn) {
            if (ReminderScheduler.isSmtpReady(settings)) {
                ReminderScheduler.schedule(appContext, saved, settings)
                reminderScheduled = true
            } else if (settings.smtpHost.isBlank()) {
                needSmtp = true
            } else if (settings.reminderRecipient.isBlank()) {
                needRecipient = true
            }
        }

        if (sendConfirmation) {
            if (ReminderScheduler.isSmtpReady(settings)) {
                val time = "%02d:%02d".format(saved.startMinute / 60, saved.startMinute % 60)
                val subject = appContext.getString(
                    R.string.confirmation_email_subject,
                    saved.initials,
                )
                val body = appContext.getString(
                    R.string.confirmation_email_body,
                    saved.initials,
                    saved.day,
                    time,
                    saved.durationMin,
                )
                val result = SmtpClient.send(
                    SmtpClient.MailRequest(
                        host = settings.smtpHost,
                        port = settings.smtpPort,
                        useTls = settings.smtpUseTls,
                        username = settings.smtpUsername,
                        password = settings.smtpPassword,
                        from = settings.smtpFrom,
                        to = settings.reminderRecipient,
                        subject = subject,
                        body = body,
                    ),
                )
                confirmationSent = result.isSuccess
            } else if (settings.smtpHost.isNotBlank() && settings.reminderRecipient.isBlank()) {
                needRecipient = true
            }
            // No SMTP / no recipient: skip confirmation silently (except recipient hint above)
        }

        return BookResult.Ok(
            reminderScheduled = reminderScheduled,
            confirmationSent = confirmationSent,
            needRecipient = needRecipient,
            needSmtp = needSmtp,
        )
    }

    fun cancel(id: Long) {
        val accountId = session.unlockedAccountId.value ?: return
        viewModelScope.launch {
            ReminderScheduler.cancel(appContext, id)
            dao.delete(accountId, id)
        }
    }

    /** After saving notification settings: reschedule or cancel all reminders for this account. */
    fun syncRemindersForAccount(accountId: Long) {
        viewModelScope.launch {
            val settings = notificationStore.load(accountId)
            val all = dao.allForAccount(accountId)
            if (!settings.remindersOn || !ReminderScheduler.isSmtpReady(settings)) {
                for (a in all) ReminderScheduler.cancel(appContext, a.id)
            } else {
                ReminderScheduler.rescheduleAll(appContext, all, settings)
            }
        }
    }

    suspend fun createAccount(
        displayName: String,
        password: CharArray,
        attachOrphans: Boolean,
    ): Boolean {
        val result = accountRepo.createAccount(displayName, password, attachOrphans)
        if (result.isSuccess) {
            _needsOrphanMigration.value = false
        }
        return result.isSuccess
    }

    suspend fun signIn(accountId: Long, password: CharArray): Boolean =
        accountRepo.signIn(accountId, password)

    suspend fun changePassword(current: CharArray, newPassword: CharArray): Boolean {
        val id = session.unlockedAccountId.value ?: return false
        return accountRepo.changePassword(id, current, newPassword)
    }

    suspend fun resetAccount(accountId: Long) {
        val all = dao.allForAccount(accountId)
        for (a in all) ReminderScheduler.cancel(appContext, a.id)
        accountRepo.resetAccount(accountId)
        _needsOrphanMigration.value = accountRepo.needsOrphanMigration()
    }

    fun switchAccount() {
        accountRepo.switchAccount()
    }

    fun lastAccountId(): Long? = session.lastAccountId

    class Factory(
        private val app: Application,
        private val dao: AppointmentDao,
        private val accountRepo: AccountRepository,
        private val session: AccountSession,
        private val notificationStore: NotificationSettingsStore,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
            HomeViewModel(app, dao, accountRepo, session, notificationStore) as T
    }
}
