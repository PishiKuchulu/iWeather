package com.pishi.iweather

import com.pishi.iweather.data.City
import com.pishi.iweather.data.WeatherApi
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * AUD-001/R1: parse the REAL Open-Meteo response captured during the forensic audit
 * (Tehran, 2026-11-17), plus AUD-015 mutations: a missing non-critical current field
 * must degrade to the hourly fallback, never discard the whole report.
 */
class WeatherApiParseTest {

    private val tehran = City("تهران", "ایران", "تهران", 35.6892, 51.3890)
    private val api = WeatherApi()

    private fun resource(name: String): String =
        javaClass.getResourceAsStream(name)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
            ?: error("missing test resource $name")

    private fun raw(forecast: String) = WeatherApi.RawWeather(forecast, resource("/air-tehran.json"))

    @Test
    fun parsesRealCapture() {
        val forecast = resource("/forecast-tehran.json")
        val expected = JSONObject(forecast)
        val report = api.parseReport(tehran, raw(forecast), 1_700_000_000_000L)
        assertEquals(48, report.hourly.size)
        assertEquals(16, report.daily.size)
        assertEquals(
            expected.getJSONObject("current").getString("time"),
            report.current.timeIso,
        )
        assertEquals(
            expected.getJSONObject("current").getDouble("temperature_2m"),
            report.current.temperature, 0.001,
        )
        assertTrue("air quality parsed", report.air != null)
    }

    @Test
    fun missingPressureDegradesToHourly() {
        val root = JSONObject(resource("/forecast-tehran.json"))
        root.getJSONObject("current").remove("surface_pressure")
        val report = api.parseReport(tehran, raw(root.toString()), 0L)
        assertEquals(report.hourly.first().pressure, report.current.pressure, 0.001)
    }

    @Test
    fun missingTemperatureDegradesToHourly() {
        val root = JSONObject(resource("/forecast-tehran.json"))
        root.getJSONObject("current").remove("temperature_2m")
        val report = api.parseReport(tehran, raw(root.toString()), 0L)
        assertEquals(report.hourly.first().temperature, report.current.temperature, 0.001)
    }
}
