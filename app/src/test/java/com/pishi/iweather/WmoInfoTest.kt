package com.pishi.iweather

import com.pishi.iweather.data.SkyKind
import com.pishi.iweather.data.wmoInfo
import org.junit.Assert.assertEquals
import org.junit.Test

/** AUD-001: WMO weather-code → scene-kind mapping spot checks. */
class WmoInfoTest {

    @Test
    fun mapping() {
        assertEquals(SkyKind.CLEAR_DAY, wmoInfo(0, true).kind)
        assertEquals(SkyKind.CLEAR_NIGHT, wmoInfo(0, false).kind)
        assertEquals(SkyKind.CLOUDY, wmoInfo(3, true).kind)
        assertEquals(SkyKind.FOG, wmoInfo(45, true).kind)
        assertEquals(SkyKind.RAIN, wmoInfo(61, true).kind)
        assertEquals(SkyKind.SNOW, wmoInfo(71, true).kind)
        assertEquals(SkyKind.STORM, wmoInfo(95, true).kind)
        assertEquals(SkyKind.STORM, wmoInfo(99, false).kind)
    }
}
