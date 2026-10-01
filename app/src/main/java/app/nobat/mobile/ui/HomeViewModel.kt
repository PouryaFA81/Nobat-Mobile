package app.nobat.mobile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import app.nobat.mobile.data.Appointment
import app.nobat.mobile.data.AppointmentDao
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(private val dao: AppointmentDao) : ViewModel() {
    private val dayFmt = DateTimeFormatter.ISO_LOCAL_DATE

    private val _day = MutableStateFlow(LocalDate.now())
    val day: StateFlow<LocalDate> = _day

    val appointments: StateFlow<List<Appointment>> = _day
        .flatMapLatest { d -> dao.forDay(d.format(dayFmt)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun prevDay() { _day.value = _day.value.minusDays(1) }
    fun nextDay() { _day.value = _day.value.plusDays(1) }
    fun goToday() { _day.value = LocalDate.now() }

    fun book(initials: String, startMinute: Int, durationMin: Int, note: String) {
        val clean = initials.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch {
            dao.upsert(
                Appointment(
                    day = _day.value.format(dayFmt),
                    startMinute = startMinute.coerceIn(0, 23 * 60 + 59),
                    durationMin = durationMin.coerceAtLeast(15),
                    initials = clean,
                    note = note.trim(),
                )
            )
        }
    }

    fun cancel(id: Long) {
        viewModelScope.launch { dao.delete(id) }
    }

    class Factory(private val dao: AppointmentDao) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            HomeViewModel(dao) as T
    }
}
