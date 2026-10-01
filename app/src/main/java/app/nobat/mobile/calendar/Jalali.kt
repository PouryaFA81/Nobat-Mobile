package app.nobat.mobile.calendar

import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Jalali (Shamsi) ↔ Gregorian conversion.
 * Ported from Nobat `app/jalali.py` (jalaali-js algorithm, MIT).
 * Accurate for Jalali years -61 .. 3177.
 */
object Jalali {
    val MONTHS_FA = listOf(
        "فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند",
    )

    /** Week header Saturday-first: شنبه … جمعه */
    val WEEK_HEADER_FA = listOf("ش", "ی", "د", "س", "چ", "پ", "ج")

    private val BREAKS = intArrayOf(
        -61, 9, 38, 199, 426, 686, 756, 818, 1111, 1181, 1210,
        1635, 2060, 2097, 2192, 2262, 2324, 2394, 2456, 3178,
    )

    data class Date(val year: Int, val month: Int, val day: Int)

    fun toJalali(g: LocalDate): Date = d2j(g2d(g.year, g.monthValue, g.dayOfMonth))

    fun toGregorian(jy: Int, jm: Int, jd: Int): LocalDate {
        val (gy, gm, gd) = d2g(j2d(jy, jm, jd))
        return LocalDate.of(gy, gm, gd)
    }

    fun isLeap(jy: Int): Boolean = jalCal(jy).leap == 0

    fun monthLength(jy: Int, jm: Int): Int = when {
        jm <= 6 -> 31
        jm <= 11 -> 30
        else -> if (isLeap(jy)) 30 else 29
    }

    /** First and last Gregorian dates of the Jalali month containing [g]. */
    fun monthBounds(g: LocalDate): Pair<LocalDate, LocalDate> {
        val j = toJalali(g)
        val start = toGregorian(j.year, j.month, 1)
        val end = toGregorian(j.year, j.month, monthLength(j.year, j.month))
        return start to end
    }

    fun plusMonths(g: LocalDate, delta: Int): LocalDate {
        val j = toJalali(g)
        var y = j.year
        var m = j.month + delta
        while (m > 12) {
            m -= 12
            y++
        }
        while (m < 1) {
            m += 12
            y--
        }
        val d = minOf(j.day, monthLength(y, m))
        return toGregorian(y, m, d)
    }

    /** Month title e.g. «مهر ۱۴۰۵» (Latin digits for LTR numerals). */
    fun monthLabel(g: LocalDate): String {
        val j = toJalali(g)
        return "${MONTHS_FA[j.month - 1]} ${j.year}"
    }

    /** Day label e.g. «1405-07-09». */
    fun dayLabel(g: LocalDate): String {
        val j = toJalali(g)
        return "%04d-%02d-%02d".format(j.year, j.month, j.day)
    }

    /** Jalali day-of-month for a Gregorian date (for grid cells). */
    fun dayOfMonth(g: LocalDate): Int = toJalali(g).day

    /**
     * Grid of Gregorian dates for the Jalali month containing [anchor],
     * Saturday-first, null = empty cell.
     */
    fun monthGrid(anchor: LocalDate): List<LocalDate?> {
        val (start, end) = monthBounds(anchor)
        val lead = saturdayFirstIndex(start.dayOfWeek)
        val cells = MutableList<LocalDate?>(lead) { null }
        var d = start
        while (!d.isAfter(end)) {
            cells.add(d)
            d = d.plusDays(1)
        }
        while (cells.size % 7 != 0) cells.add(null)
        return cells
    }

    fun saturdayFirstIndex(dow: DayOfWeek): Int = when (dow) {
        DayOfWeek.SATURDAY -> 0
        DayOfWeek.SUNDAY -> 1
        DayOfWeek.MONDAY -> 2
        DayOfWeek.TUESDAY -> 3
        DayOfWeek.WEDNESDAY -> 4
        DayOfWeek.THURSDAY -> 5
        DayOfWeek.FRIDAY -> 6
    }

    // --- conversion internals (jalaali-js) ---

    private data class JalCal(val leap: Int, val gy: Int, val march: Int)

    private fun div(a: Int, b: Int) = a / b
    private fun mod(a: Int, b: Int) = a - div(a, b) * b

    private fun jalCal(jy: Int): JalCal {
        var gy = jy + 621
        var leapJ = -14
        var jp = BREAKS[0]
        var jump = 0
        for (i in 1 until BREAKS.size) {
            val jm = BREAKS[i]
            jump = jm - jp
            if (jy < jm) break
            leapJ += div(jump, 33) * 8 + div(mod(jump, 33), 4)
            jp = jm
        }
        var n = jy - jp
        leapJ += div(n, 33) * 8 + div(mod(n, 33) + 3, 4)
        if (mod(jump, 33) == 4 && jump - n == 4) leapJ += 1
        val leapG = div(gy, 4) - div((div(gy, 100) + 1) * 3, 4) - 150
        val march = 20 + leapJ - leapG
        if (jump - n < 6) {
            n = n - jump + div(jump + 4, 33) * 33
        }
        var leap = mod(mod(n + 1, 33) - 1, 4)
        if (leap == -1) leap = 4
        return JalCal(leap, gy, march)
    }

    private fun g2d(gy: Int, gm: Int, gd: Int): Int {
        var d = (
            div((gy + div(gm - 8, 6) + 100100) * 1461, 4) +
                div(153 * mod(gm + 9, 12) + 2, 5) + gd - 34840408
            )
        d = d - div(div(gy + 100100 + div(gm - 8, 6), 100) * 3, 4) + 752
        return d
    }

    private fun d2g(jdn: Int): Triple<Int, Int, Int> {
        var j = 4 * jdn + 139361631
        j = j + div(div(4 * jdn + 183187720, 146097) * 3, 4) * 4 - 3908
        val i = div(mod(j, 1461), 4) * 5 + 308
        val gd = div(mod(i, 153), 5) + 1
        val gm = mod(div(i, 153), 12) + 1
        val gy = div(j, 1461) - 100100 + div(8 - gm, 6)
        return Triple(gy, gm, gd)
    }

    private fun j2d(jy: Int, jm: Int, jd: Int): Int {
        val cal = jalCal(jy)
        return g2d(cal.gy, 3, cal.march) + (jm - 1) * 31 - div(jm, 7) * (jm - 7) + jd - 1
    }

    private fun d2j(jdn: Int): Date {
        val gy = d2g(jdn).first
        var jy = gy - 621
        val cal = jalCal(jy)
        val jdn1f = g2d(gy, 3, cal.march)
        var k = jdn - jdn1f
        return if (k >= 0) {
            if (k <= 185) {
                Date(jy, 1 + div(k, 31), mod(k, 31) + 1)
            } else {
                k -= 186
                Date(jy, 7 + div(k, 30), mod(k, 30) + 1)
            }
        } else {
            jy -= 1
            k += 179
            if (cal.leap == 1) k += 1
            Date(jy, 7 + div(k, 30), mod(k, 30) + 1)
        }
    }
}
