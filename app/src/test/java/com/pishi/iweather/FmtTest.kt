package com.pishi.iweather

import com.pishi.iweather.util.AppSettings
import com.pishi.iweather.util.Fmt
import com.pishi.iweather.util.Lang
import com.pishi.iweather.util.PressureUnit
import com.pishi.iweather.util.WindUnit
import org.junit.Assert.assertEquals
import org.junit.Test

/** AUD-001/R4: golden-value formatting table in both languages. */
class FmtTest {

    private val en = AppSettings(lang = Lang.EN)
    private val fa = AppSettings(lang = Lang.FA)

    @Test
    fun numbers() {
        assertEquals("42", Fmt.num(42, Lang.EN))
        assertEquals("۴۲", Fmt.num(42, Lang.FA))
        assertEquals("—", Fmt.num(null, Lang.EN))
        assertEquals("35%", Fmt.percent(35, Lang.EN))
        assertEquals("۳۵٪", Fmt.percent(35, Lang.FA))
    }

    @Test
    fun temperatures() {
        assertEquals("20°C", Fmt.temp(20.0, en))
        assertEquals("۲۰°", Fmt.temp(20.0, fa))
        assertEquals("-3°C", Fmt.temp(-3.4, en))
    }

    @Test
    fun windConversions() {
        assertEquals("36 km/h", Fmt.wind(36.0, en))
        assertEquals("10 m/s", Fmt.wind(36.0, en.copy(windUnit = WindUnit.MS)))
        assertEquals("22 mph", Fmt.wind(36.0, en.copy(windUnit = WindUnit.MPH)))
    }

    @Test
    fun pressureConversions() {
        assertEquals("1013 hPa", Fmt.pressure(1013.0, en))
        assertEquals("760 mmHg", Fmt.pressure(1013.0, en.copy(pressureUnit = PressureUnit.MMHG)))
    }

    @Test
    fun visibility() {
        assertEquals("500 m", Fmt.visibility(500.0, en))
        assertEquals("10 km", Fmt.visibility(10_000.0, en))
        assertEquals("—", Fmt.visibility(0.0, en))
    }

    @Test
    fun clocks() {
        assertEquals("06:41", Fmt.clock("06:41", en))
        assertEquals("6:41 AM", Fmt.clock("06:41", en.copy(time24 = false)))
        assertEquals("1:05 PM", Fmt.clock("13:05", en.copy(time24 = false)))
    }
}
