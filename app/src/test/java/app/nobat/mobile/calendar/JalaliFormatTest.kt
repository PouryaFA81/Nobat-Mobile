package app.nobat.mobile.calendar

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class JalaliFormatTest {

    @Test
    fun formatStoredDay_faUsesDayLabel_enKeepsIso() {
        val iso = "2026-09-30"
        val jalali = Jalali.dayLabel(LocalDate.parse(iso))
        assertNotEquals(iso, jalali)
        assertEquals(jalali, Jalali.formatStoredDay(iso, "fa"))
        assertEquals(jalali, Jalali.formatStoredDay(iso, "FA"))
        assertEquals(iso, Jalali.formatStoredDay(iso, "en"))
        assertEquals(iso, Jalali.formatStoredDay(iso, "en-US"))
    }

    @Test
    fun dayLabel_nowruz1405_isLatinDigits() {
        assertEquals("1405-01-01", Jalali.dayLabel(LocalDate.of(2026, 3, 21)))
    }

    @Test
    fun formatStoredDay_unparseableStaysAsIs() {
        assertEquals("not-a-day", Jalali.formatStoredDay("not-a-day", "fa"))
    }
}
