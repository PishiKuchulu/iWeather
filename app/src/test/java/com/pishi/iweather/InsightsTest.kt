package com.pishi.iweather

import com.pishi.iweather.data.City
import com.pishi.iweather.data.CurrentWeather
import com.pishi.iweather.data.DayForecast
import com.pishi.iweather.data.HourPoint
import com.pishi.iweather.data.WeatherReport
import com.pishi.iweather.util.AlertType
import com.pishi.iweather.util.AppSettings
import com.pishi.iweather.util.Insights
import com.pishi.iweather.util.Lang
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** AUD-001/R3: insight/alert scenario tables, incl. the AUD-009 duplicate-rain guard. */
class InsightsTest {

    private val en = AppSettings(lang = Lang.EN)
    private val tehran = City("Tehran", "Iran", "Tehran", 35.6892, 51.3890)

    private fun hour(prob: Int, code: Int = 0, visM: Double = 20_000.0) = HourPoint(
        timeIso = "2026-11-17T10:00", hourLabel = "10:00",
        temperature = 20.0, precipProbability = prob, weatherCode = code,
        isDay = true, visibilityM = visM,
    )

    private fun report(
        hours: List<HourPoint>,
        currentCode: Int = 0,
        todayCode: Int = 0,
    ) = WeatherReport(
        city = tehran,
        current = CurrentWeather(
            temperature = 20.0, feelsLike = 20.0, humidity = 50, isDay = true,
            weatherCode = currentCode, windSpeed = 5.0, windDirection = 0,
            pressure = 1013.0, precipitation = 0.0, timeIso = "2026-11-17T10:00",
        ),
        hourly = hours,
        daily = listOf(
            DayForecast(
                dateIso = "2026-11-17", weatherCode = todayCode, tempMax = 24.0, tempMin = 14.0,
                sunriseIso = "2026-11-17T06:41", sunsetIso = "2026-11-17T17:12",
                uvMax = 3.0, precipProbabilityMax = 40,
            ),
        ),
    )

    @Test
    fun neverTwoRainAlerts() {
        // 40% in the next 12 h (probability alert fires) AND a >=50% hour at +20 h
        // (rain window exists): the list must contain exactly one RAIN item (AUD-009).
        val hours = MutableList(48) { hour(10) }
        for (i in 0 until 12) hours[i] = hour(40)
        hours[20] = hour(55)
        val alerts = Insights.alerts(report(hours), en)
        assertEquals(1, alerts.count { it.type == AlertType.RAIN })
    }

    @Test
    fun rainWindowAloneStillAlerts() {
        // No probability alert (<35% in 12 h) but a >=50% hour later: one RAIN item.
        val hours = MutableList(48) { hour(5) }
        hours[30] = hour(60)
        val alerts = Insights.alerts(report(hours), en)
        assertEquals(1, alerts.count { it.type == AlertType.RAIN })
    }

    @Test
    fun snowHurtsDrivingScore() {
        val clear = report(MutableList(48) { hour(0) })
        val snow = report(MutableList(48) { hour(0, code = 71, visM = 500.0) }, currentCode = 71)
        val driveClear = Insights.activities(clear, Lang.EN).first { it.emoji == "🚗" }.score
        val driveSnow = Insights.activities(snow, Lang.EN).first { it.emoji == "🚗" }.score
        assertTrue("snow drive $driveSnow < clear drive $driveClear", driveSnow < driveClear)
    }

    @Test
    fun comfortScoreInRange() {
        val score = Insights.comfortScore(report(MutableList(48) { hour(0) }))
        assertTrue(score in 0..100)
    }
}
