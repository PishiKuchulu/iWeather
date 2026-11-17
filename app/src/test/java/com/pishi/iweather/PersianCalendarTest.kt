package com.pishi.iweather

import com.pishi.iweather.util.Jalali
import com.pishi.iweather.util.JalaliCalendar
import org.junit.Assert.assertEquals
import org.junit.Test

/** AUD-001/R2: the audit-verified Gregorian→Jalali pairs, including leap boundaries. */
class PersianCalendarTest {

    private fun check(gy: Int, gm: Int, gd: Int, jy: Int, jm: Int, jd: Int) {
        assertEquals("$gy-$gm-$gd", Jalali(jy, jm, jd), JalaliCalendar.fromGregorian(gy, gm, gd))
    }

    @Test
    fun knownDates() {
        check(2024, 3, 20, 1403, 1, 1)   // Nowruz 1403 (anchor)
        check(2025, 3, 21, 1404, 1, 1)   // Nowruz 1404
        check(2025, 3, 20, 1403, 12, 30) // last day of leap year 1403
        check(2024, 2, 29, 1402, 12, 10) // Gregorian leap day
        check(2000, 3, 20, 1379, 1, 1)
        check(2021, 3, 21, 1400, 1, 1)
        check(2026, 11, 17, 1405, 8, 26)
    }

    @Test
    fun monthNames() {
        assertEquals("فروردین", Jalali(1403, 1, 1).monthName)
        assertEquals("اسفند", Jalali(1403, 12, 30).monthName)
    }
}
