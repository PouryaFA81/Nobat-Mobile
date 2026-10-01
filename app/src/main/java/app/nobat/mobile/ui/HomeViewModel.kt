package app.nobat.mobile.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import app.nobat.mobile.R
import app.nobat.mobile.calendar.Jalali
import app.nobat.mobile.data.Account
import app.nobat.mobile.data.AccountRepository
import app.nobat.mobile.data.AccountRole
import app.nobat.mobile.data.Appointment
import app.nobat.mobile.data.AppointmentDao
import app.nobat.mobile.data.DayCount
import app.nobat.mobile.data.Personnel
import app.nobat.mobile.data.PersonnelDao
import app.nobat.mobile.notify.ClinicNotifier
import app.nobat.mobile.notify.ClinicRelayClient
import app.nobat.mobile.notify.ClinicSettingsStore
import app.nobat.mobile.notify.NotificationSettingsStore
import app.nobat.mobile.notify.SmtpClient
import app.nobat.mobile.notify.TelegramClient
import app.nobat.mobile.notify.TelegramSettingsStore
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

/** Who's schedule is shown on day/month lists. */
enum class ScheduleFilter {
    EVERYONE,
    MY_SCHEDULE,
}

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    app: Application,
    private val dao: AppointmentDao,
    private val personnelDao: PersonnelDao,
    private val accountRepo: AccountRepository,
    private val session: AccountSession,
    private val notificationStore: NotificationSettingsStore,
    private val telegramStore: TelegramSettingsStore,
    private val clinicStore: ClinicSettingsStore,
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

    /** Admin tab selection; Staff always uses My schedule. */
    private val _scheduleFilter = MutableStateFlow(ScheduleFilter.EVERYONE)
    val scheduleFilter: StateFlow<ScheduleFilter> = _scheduleFilter

    /** Effective filter: Staff → My schedule; Admin → tab choice. */
    val effectiveScheduleFilter: StateFlow<ScheduleFilter> =
        combine(unlockedAccount, _scheduleFilter) { account, tab ->
            when {
                account == null -> ScheduleFilter.EVERYONE
                AccountRole.isStaff(account.role) -> ScheduleFilter.MY_SCHEDULE
                else -> tab
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ScheduleFilter.EVERYONE)

    val canBook: StateFlow<Boolean> =
        unlockedAccount.map { account ->
            account != null && AccountRole.isAdmin(account.role)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val showScheduleTabs: StateFlow<Boolean> =
        unlockedAccount.map { account ->
            account != null && AccountRole.isAdmin(account.role)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /** Linked personnel id on the unlocked account (0 = none). */
    val linkedPersonnelId: StateFlow<Long> =
        unlockedAccount.map { it?.linkedPersonnelId ?: 0L }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    /**
     * When My schedule is active and no personnel link, day/month should show the
     * "Link yourself under Account first" empty state.
     */
    val needsPersonnelLink: StateFlow<Boolean> =
        combine(effectiveScheduleFilter, linkedPersonnelId) { filter, linked ->
            filter == ScheduleFilter.MY_SCHEDULE && linked <= 0L
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

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

    fun setScheduleFilter(filter: ScheduleFilter) {
        _scheduleFilter.value = filter
    }

    init {
        viewModelScope.launch {
            _needsOrphanMigration.value = accountRepo.needsOrphanMigration()
            _bootReady.value = true
        }
        ClinicNotifier.ensureChannel(appContext)
    }

    val appointments: StateFlow<List<Appointment>> =
        combine(_day, unlockedAccountId, effectiveScheduleFilter, linkedPersonnelId) { d, accountId, filter, linked ->
            ScheduleQuery(d, accountId, filter, linked)
        }
            .flatMapLatest { q ->
                if (q.accountId == null) flowOf(emptyList())
                else when {
                    q.filter == ScheduleFilter.MY_SCHEDULE && q.linked <= 0L ->
                        flowOf(emptyList())
                    q.filter == ScheduleFilter.MY_SCHEDULE ->
                        dao.forDayPersonnel(q.accountId, q.day.format(dayFmt), q.linked)
                    else ->
                        dao.forDay(q.accountId, q.day.format(dayFmt))
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Map of ISO day → appointment count for the visible calendar month. */
    val monthCounts: StateFlow<Map<String, Int>> =
        combine(_monthAnchor, unlockedAccountId, _useJalali, effectiveScheduleFilter, linkedPersonnelId) {
                anchor, accountId, jalali, filter, linked ->
            MonthQuery(anchor, accountId, jalali, filter, linked)
        }
            .flatMapLatest { q ->
                if (q.accountId == null) flowOf(emptyMap())
                else if (q.filter == ScheduleFilter.MY_SCHEDULE && q.linked <= 0L) {
                    flowOf(emptyMap())
                } else {
                    val (start, end) = if (q.jalali) {
                        Jalali.monthBounds(q.anchor)
                    } else {
                        val ym = YearMonth.from(q.anchor)
                        ym.atDay(1) to ym.atEndOfMonth()
                    }
                    val startIso = start.format(dayFmt)
                    val endIso = end.format(dayFmt)
                    if (q.filter == ScheduleFilter.MY_SCHEDULE) {
                        dao.countsBetweenPersonnel(q.accountId, startIso, endIso, q.linked)
                            .map { list: List<DayCount> -> list.associate { it.day to it.count } }
                    } else {
                        dao.countsBetween(q.accountId, startIso, endIso)
                            .map { list: List<DayCount> -> list.associate { it.day to it.count } }
                    }
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
     * Staff accounts cannot book (UI hides FAB); still guarded here.
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
        val account = accountRepo.getAccount(accountId) ?: return BookResult.Failed
        if (!AccountRole.isAdmin(account.role)) return BookResult.Failed

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

        // Telegram notify-on-book (Integrations chat ID; staff name in body). Skip if off/missing.
        val tg = telegramStore.load(accountId)
        if (tg.notifyOnBook && tg.isConfigured()) {
            val time = "%02d:%02d".format(saved.startMinute / 60, saved.startMinute % 60)
            val tgText = appContext.getString(
                R.string.telegram_book_message,
                saved.initials,
                saved.day,
                time,
                saved.durationMin,
                person.name,
            )
            // Fire-and-forget style: failures do not fail the book.
            TelegramClient.sendMessage(tg.botToken, tg.chatId, tgText)
        }

        // Local in-app notification on this device (Phase 1).
        val time = "%02d:%02d".format(saved.startMinute / 60, saved.startMinute % 60)
        ClinicNotifier.notifyBooked(appContext, saved.initials, saved.day, time)

        // Clinic relay publish for staff phones (Phase 2). Failures do not fail the book.
        val clinic = clinicStore.load(accountId)
        if (clinic.isConfigured()) {
            val title = appContext.getString(R.string.notif_new_appointment)
            ClinicRelayClient.publish(
                settings = clinic,
                event = "book",
                personnelId = person.id,
                initials = saved.initials,
                day = saved.day,
                time = time,
                title = title,
            )
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
            val existing = dao.get(accountId, id)
            ReminderScheduler.cancel(appContext, id)
            dao.delete(accountId, id)
            if (existing != null) {
                val time = "%02d:%02d".format(existing.startMinute / 60, existing.startMinute % 60)
                ClinicNotifier.notifyCancelled(
                    appContext,
                    existing.initials,
                    existing.day,
                    time,
                )
                val clinic = clinicStore.load(accountId)
                if (clinic.isConfigured()) {
                    val title = appContext.getString(R.string.notif_appointment_cancelled)
                    ClinicRelayClient.publish(
                        settings = clinic,
                        event = "cancel",
                        personnelId = existing.personnelId,
                        initials = existing.initials,
                        day = existing.day,
                        time = time,
                        title = title,
                    )
                }
            }
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
            // Clear link if this account pointed at the deleted person.
            val account = accountRepo.getAccount(accountId) ?: return@launch
            if (account.linkedPersonnelId == id) {
                accountRepo.setLinkedPersonnel(accountId, 0L)
            }
        }
    }

    suspend fun setRole(role: String): Boolean {
        val id = session.unlockedAccountId.value ?: return false
        val ok = accountRepo.setRole(id, role)
        if (ok && AccountRole.isStaff(role)) {
            _scheduleFilter.value = ScheduleFilter.MY_SCHEDULE
        }
        return ok
    }

    suspend fun setLinkedPersonnel(personnelId: Long): Boolean {
        val id = session.unlockedAccountId.value ?: return false
        return accountRepo.setLinkedPersonnel(id, personnelId)
    }

    suspend fun createAccount(
        displayName: String,
        password: CharArray,
        attachOrphans: Boolean,
    ): Boolean {
        val result = accountRepo.createAccount(displayName, password, attachOrphans)
        if (result.isSuccess) {
            _needsOrphanMigration.value = false
            _scheduleFilter.value = ScheduleFilter.EVERYONE
        }
        return result.isSuccess
    }

    /** Open a local profile without password — day-to-day unlock is PIN/biometric. */
    fun openAccount(accountId: Long) {
        session.unlock(accountId)
        viewModelScope.launch {
            val account = accountRepo.getAccount(accountId)
            _scheduleFilter.value = if (account != null && AccountRole.isStaff(account.role)) {
                ScheduleFilter.MY_SCHEDULE
            } else {
                ScheduleFilter.EVERYONE
            }
        }
    }

    suspend fun changePassword(current: CharArray, newPassword: CharArray): Boolean {
        val id = session.unlockedAccountId.value ?: return false
        return accountRepo.changePassword(id, current, newPassword)
    }

    fun switchAccount() {
        accountRepo.switchAccount()
    }

    fun lastAccountId(): Long? = session.lastAccountId

    private data class ScheduleQuery(
        val day: LocalDate,
        val accountId: Long?,
        val filter: ScheduleFilter,
        val linked: Long,
    )

    private data class MonthQuery(
        val anchor: LocalDate,
        val accountId: Long?,
        val jalali: Boolean,
        val filter: ScheduleFilter,
        val linked: Long,
    )

    class Factory(
        private val app: Application,
        private val dao: AppointmentDao,
        private val personnelDao: PersonnelDao,
        private val accountRepo: AccountRepository,
        private val session: AccountSession,
        private val notificationStore: NotificationSettingsStore,
        private val telegramStore: TelegramSettingsStore,
        private val clinicStore: ClinicSettingsStore,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
            HomeViewModel(app, dao, personnelDao, accountRepo, session, notificationStore, telegramStore, clinicStore) as T
    }
}
