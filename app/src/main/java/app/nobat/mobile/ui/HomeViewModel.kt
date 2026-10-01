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
import app.nobat.mobile.data.Personnel
import app.nobat.mobile.data.PersonnelDao
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
    private val personnelDao: PersonnelDao,
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

    val personnel: StateFlow<List<Personnel>> =
        unlockedAccountId.flatMapLatest { accountId ->
            if (accountId == null) flowOf(emptyList())
            else personnelDao.observeForAccount(accountId)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

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
            val needPersonnel: Boolean,
            val needSmtp: Boolean,
        ) : BookResult()
        data object Failed : BookResult()
        /** No staff rows — block mail and prompt user to add personnel. */
        data object NeedPersonnel : BookResult()
    }

    /**
     * Book appointment assigned to [personnelId]; confirmation + 1h reminder go to that
     * person’s email. If no personnel exist, returns [BookResult.NeedPersonnel] without saving.
     */
    suspend fun book(
        initials: String,
        startMinute: Int,
        durationMin: Int,
        note: String,
        personnelId: Long,
        sendConfirmation: Boolean = true,
    ): BookResult {
        val clean = initials.trim()
        if (clean.isEmpty()) return BookResult.Failed
        val accountId = session.unlockedAccountId.value ?: return BookResult.Failed

        val staffList = personnelDao.listForAccount(accountId)
        if (staffList.isEmpty()) return BookResult.NeedPersonnel

        val person = staffList.firstOrNull { it.id == personnelId }
            ?: return BookResult.Failed
        val email = person.email.trim()
        if (email.isEmpty()) return BookResult.Failed

        val appt = Appointment(
            accountId = accountId,
            day = _day.value.format(dayFmt),
            startMinute = startMinute.coerceIn(0, 23 * 60 + 59),
            durationMin = durationMin.coerceAtLeast(15),
            initials = clean,
            note = note.trim(),
            personnelId = person.id,
            personnelEmail = email,
        )
        val newId = dao.upsert(appt)
        val saved = appt.copy(id = newId)
        val settings = notificationStore.load(accountId)

        var reminderScheduled = false
        var confirmationSent: Boolean? = null
        var needSmtp = false

        if (settings.remindersOn) {
            if (ReminderScheduler.canSendToPersonnel(settings, saved)) {
                ReminderScheduler.schedule(appContext, saved, settings)
                reminderScheduled = true
            } else if (!ReminderScheduler.isSmtpConfigured(settings)) {
                needSmtp = true
            }
        }

        if (sendConfirmation) {
            if (ReminderScheduler.canSendToPersonnel(settings, saved)) {
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
                        to = email,
                        subject = subject,
                        body = body,
                    ),
                )
                confirmationSent = result.isSuccess
            } else if (!ReminderScheduler.isSmtpConfigured(settings) && settings.smtpHost.isNotBlank()) {
                needSmtp = true
            }
        }

        return BookResult.Ok(
            reminderScheduled = reminderScheduled,
            confirmationSent = confirmationSent,
            needPersonnel = false,
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
            if (!settings.remindersOn || !ReminderScheduler.isSmtpConfigured(settings)) {
                for (a in all) ReminderScheduler.cancel(appContext, a.id)
            } else {
                ReminderScheduler.rescheduleAll(appContext, all, settings)
            }
        }
    }

    suspend fun upsertPersonnel(
        id: Long,
        name: String,
        email: String,
        phone: String,
    ): Boolean {
        val accountId = session.unlockedAccountId.value ?: return false
        val cleanName = name.trim()
        val cleanEmail = email.trim()
        if (cleanName.isEmpty() || cleanEmail.isEmpty()) return false
        if (id == 0L) {
            personnelDao.insert(
                Personnel(
                    accountId = accountId,
                    name = cleanName,
                    email = cleanEmail,
                    phone = phone.trim(),
                ),
            )
        } else {
            val existing = personnelDao.get(accountId, id) ?: return false
            personnelDao.update(
                existing.copy(
                    name = cleanName,
                    email = cleanEmail,
                    phone = phone.trim(),
                ),
            )
        }
        return true
    }

    fun deletePersonnel(id: Long) {
        val accountId = session.unlockedAccountId.value ?: return
        viewModelScope.launch {
            personnelDao.delete(accountId, id)
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
        private val personnelDao: PersonnelDao,
        private val accountRepo: AccountRepository,
        private val session: AccountSession,
        private val notificationStore: NotificationSettingsStore,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
            HomeViewModel(app, dao, personnelDao, accountRepo, session, notificationStore) as T
    }
}
