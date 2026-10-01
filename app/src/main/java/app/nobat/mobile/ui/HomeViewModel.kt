package app.nobat.mobile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import app.nobat.mobile.data.Account
import app.nobat.mobile.data.AccountRepository
import app.nobat.mobile.data.Appointment
import app.nobat.mobile.data.AppointmentDao
import app.nobat.mobile.data.DayCount
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
    private val dao: AppointmentDao,
    private val accountRepo: AccountRepository,
    private val session: AccountSession,
) : ViewModel() {
    private val dayFmt = DateTimeFormatter.ISO_LOCAL_DATE

    private val _day = MutableStateFlow(LocalDate.now())
    val day: StateFlow<LocalDate> = _day

    private val _month = MutableStateFlow(YearMonth.now())
    val month: StateFlow<YearMonth> = _month

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

    /** Map of ISO day → appointment count for the visible month. */
    val monthCounts: StateFlow<Map<String, Int>> =
        combine(_month, unlockedAccountId) { ym, accountId -> ym to accountId }
            .flatMapLatest { (ym, accountId) ->
                if (accountId == null) flowOf(emptyMap())
                else {
                    val start = ym.atDay(1).format(dayFmt)
                    val end = ym.atEndOfMonth().format(dayFmt)
                    dao.countsBetween(accountId, start, end)
                        .map { list: List<DayCount> -> list.associate { it.day to it.count } }
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    fun prevDay() { _day.value = _day.value.minusDays(1) }
    fun nextDay() { _day.value = _day.value.plusDays(1) }
    fun goToday() {
        val today = LocalDate.now()
        _day.value = today
        _month.value = YearMonth.from(today)
    }

    fun prevMonth() { _month.value = _month.value.minusMonths(1) }
    fun nextMonth() { _month.value = _month.value.plusMonths(1) }

    fun selectDay(date: LocalDate) {
        _day.value = date
        _month.value = YearMonth.from(date)
    }

    fun book(initials: String, startMinute: Int, durationMin: Int, note: String) {
        val clean = initials.trim()
        if (clean.isEmpty()) return
        val accountId = session.unlockedAccountId.value ?: return
        viewModelScope.launch {
            dao.upsert(
                Appointment(
                    accountId = accountId,
                    day = _day.value.format(dayFmt),
                    startMinute = startMinute.coerceIn(0, 23 * 60 + 59),
                    durationMin = durationMin.coerceAtLeast(15),
                    initials = clean,
                    note = note.trim(),
                ),
            )
        }
    }

    fun cancel(id: Long) {
        val accountId = session.unlockedAccountId.value ?: return
        viewModelScope.launch { dao.delete(accountId, id) }
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
        accountRepo.resetAccount(accountId)
        _needsOrphanMigration.value = accountRepo.needsOrphanMigration()
    }

    fun switchAccount() {
        accountRepo.switchAccount()
    }

    fun lastAccountId(): Long? = session.lastAccountId

    class Factory(
        private val dao: AppointmentDao,
        private val accountRepo: AccountRepository,
        private val session: AccountSession,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            HomeViewModel(dao, accountRepo, session) as T
    }
}
