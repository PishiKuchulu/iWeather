package com.pishi.iweather.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Open-Meteo — free, no API key, no account.
 * Forecast:  api.open-meteo.com/v1/forecast
 * Geocoding: geocoding-api.open-meteo.com/v1/search
 * Air:       air-quality-api.open-meteo.com/v1/air-quality (CAMS)
 *
 * The provider layer is deliberately isolated: raw JSON is fetched once and
 * parsed into provider-neutral models, so another provider can be added
 * behind this class without touching the UI.
 */
class WeatherApi {

    data class RawWeather(val forecastJson: String, val airJson: String?)

    private suspend fun getText(url: String): String = withContext(Dispatchers.IO) {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 12_000
            readTimeout = 12_000
            requestMethod = "GET"
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "iWeather/1.4 (Android)")
        }
        try {
            val code = conn.responseCode
            if (code !in 200..299) {
                // errorStream can be null (e.g. 204 / dropped connection) — never feed it to a reader blindly.
                runCatching { conn.errorStream?.close() }
                throw IllegalStateException("HTTP $code")
            }
            BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8)).use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    suspend fun fetchRaw(city: City): RawWeather {
        val lat = city.latitude
        val lon = city.longitude
        val url = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon" +
            "&current=temperature_2m,relative_humidity_2m,apparent_temperature,is_day,precipitation,weather_code,wind_speed_10m,wind_direction_10m,wind_gusts_10m,surface_pressure,cloud_cover,dew_point_2m" +
            "&hourly=temperature_2m,apparent_temperature,precipitation_probability,precipitation,weather_code,is_day,relative_humidity_2m,dew_point_2m,visibility,cloud_cover,wind_speed_10m,wind_direction_10m,wind_gusts_10m,uv_index,surface_pressure" +
            "&daily=weather_code,temperature_2m_max,temperature_2m_min,sunrise,sunset,uv_index_max,precipitation_probability_max,precipitation_sum,wind_speed_10m_max,wind_gusts_10m_max,wind_direction_10m_dominant,daylight_duration,sunshine_duration" +
            "&timezone=auto&forecast_days=16"
        val forecast = getText(url)
        // Air quality is best-effort: never fail the whole report for it.
        val air = runCatching {
            getText("https://air-quality-api.open-meteo.com/v1/air-quality?latitude=$lat&longitude=$lon&current=us_aqi,european_aqi,pm2_5,pm10,carbon_monoxide,nitrogen_dioxide,sulphur_dioxide,ozone,dust")
        }.getOrNull()
        return RawWeather(forecast, air)
    }

    suspend fun fetchReport(city: City): WeatherReport =
        parseReport(city, fetchRaw(city), System.currentTimeMillis())

    /** Pure parse of previously fetched raw JSON (also used for the offline cache). */
    fun parseReport(city: City, raw: RawWeather, fetchedAtMillis: Long): WeatherReport {
        val root = JSONObject(raw.forecastJson)

        // Tolerance policy (AUD-015): only the current block's `time` stays strict — the
        // hourly window logic depends on it. Every other current field degrades to the
        // first hourly point (or a neutral default) instead of discarding a whole report.
        val c = root.optJSONObject("current") ?: JSONObject()
        val timeIso = c.getString("time")

        val h = root.optJSONObject("hourly") ?: JSONObject()
        val hTimes = h.optJSONArray("time") ?: JSONArray()
        val hourly = ArrayList<HourPoint>()
        val now = LocalDateTime.parse(timeIso, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        for (i in 0 until hTimes.length()) {
            val iso = hTimes.getString(i)
            val dt = runCatching { LocalDateTime.parse(iso, DateTimeFormatter.ISO_LOCAL_DATE_TIME) }.getOrNull() ?: continue
            if (dt.isBefore(now.withMinute(0).withSecond(0))) continue
            hourly += HourPoint(
                timeIso = iso,
                hourLabel = String.format(java.util.Locale.US, "%02d:00", dt.hour),
                temperature = h.dbl("temperature_2m", i),
                feelsLike = h.dbl("apparent_temperature", i),
                precipProbability = h.intg("precipitation_probability", i),
                precipitationMm = h.dbl("precipitation", i),
                weatherCode = h.intg("weather_code", i),
                isDay = h.intg("is_day", i, 1) == 1,
                humidity = h.intg("relative_humidity_2m", i),
                windSpeed = h.dbl("wind_speed_10m", i),
                windDirection = h.intg("wind_direction_10m", i),
                windGusts = h.dbl("wind_gusts_10m", i),
                uvIndex = h.dbl("uv_index", i),
                cloudCover = h.intg("cloud_cover", i),
                visibilityM = h.dbl("visibility", i),
                dewPoint = h.dbl("dew_point_2m", i),
                pressure = h.dbl("surface_pressure", i),
            )
            if (hourly.size >= 48) break
        }

        val firstHour = hourly.firstOrNull()
        val current = CurrentWeather(
            temperature = c.optDouble("temperature_2m", firstHour?.temperature ?: 0.0),
            feelsLike = c.optDouble("apparent_temperature", firstHour?.feelsLike ?: firstHour?.temperature ?: 0.0),
            humidity = c.optInt("relative_humidity_2m", firstHour?.humidity ?: 0),
            isDay = c.optInt("is_day", if (firstHour?.isDay == false) 0 else 1) == 1,
            weatherCode = c.optInt("weather_code", firstHour?.weatherCode ?: 0),
            windSpeed = c.optDouble("wind_speed_10m", firstHour?.windSpeed ?: 0.0),
            windDirection = c.optInt("wind_direction_10m", firstHour?.windDirection ?: 0),
            windGusts = c.optDouble("wind_gusts_10m", firstHour?.windGusts ?: 0.0),
            cloudCover = c.optInt("cloud_cover", firstHour?.cloudCover ?: 0),
            dewPoint = c.optDouble("dew_point_2m", firstHour?.dewPoint ?: 0.0),
            pressure = c.optDouble("surface_pressure", firstHour?.pressure ?: 0.0),
            precipitation = c.optDouble("precipitation", firstHour?.precipitationMm ?: 0.0),
            timeIso = timeIso,
        )

        val d = root.optJSONObject("daily") ?: JSONObject()
        val dTimes = d.optJSONArray("time") ?: JSONArray()
        val daily = ArrayList<DayForecast>()
        for (i in 0 until dTimes.length()) {
            daily += DayForecast(
                dateIso = dTimes.getString(i),
                weatherCode = d.intg("weather_code", i),
                tempMax = d.dbl("temperature_2m_max", i),
                tempMin = d.dbl("temperature_2m_min", i),
                sunriseIso = d.str("sunrise", i),
                sunsetIso = d.str("sunset", i),
                uvMax = d.dbl("uv_index_max", i),
                precipProbabilityMax = d.intg("precipitation_probability_max", i),
                precipSumMm = d.dbl("precipitation_sum", i),
                windSpeedMax = d.dbl("wind_speed_10m_max", i),
                windGustsMax = d.dbl("wind_gusts_10m_max", i),
                windDirectionDominant = d.intg("wind_direction_10m_dominant", i),
                daylightSeconds = d.dbl("daylight_duration", i).toInt(),
                sunshineSeconds = d.dbl("sunshine_duration", i).toInt(),
            )
        }

        val air = raw.airJson?.let { text ->
            runCatching {
                val a = JSONObject(text).getJSONObject("current")
                fun dv(name: String): Double? = if (a.isNull(name)) null else a.optDouble(name)
                fun iv(name: String): Int? = if (a.isNull(name)) null else a.optInt(name)
                AirQuality(
                    usAqi = iv("us_aqi"), euAqi = iv("european_aqi"),
                    pm25 = dv("pm2_5"), pm10 = dv("pm10"),
                    carbonMonoxide = dv("carbon_monoxide"),
                    nitrogenDioxide = dv("nitrogen_dioxide"),
                    sulphurDioxide = dv("sulphur_dioxide"),
                    ozone = dv("ozone"), dust = dv("dust"),
                ).takeIf { it.usAqi != null || it.pm25 != null || it.pm10 != null }
            }.getOrNull()
        }

        return WeatherReport(
            city = city,
            current = current,
            hourly = hourly,
            daily = daily,
            air = air,
            utcOffsetSec = root.optInt("utc_offset_seconds", 12_600),
            fetchedAtMillis = fetchedAtMillis,
        )
    }

    suspend fun searchCities(query: String): List<City> {
        if (query.isBlank()) return emptyList()
        val q = URLEncoder.encode(query.trim(), "UTF-8")
        val url = "https://geocoding-api.open-meteo.com/v1/search?name=$q&count=8&language=fa&format=json"
        val root = JSONObject(getText(url))
        if (!root.has("results")) return emptyList()
        val arr = root.getJSONArray("results")
        val out = ArrayList<City>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            out += City(
                name = o.optString("name"),
                country = o.optString("country"),
                admin1 = o.optString("admin1"),
                latitude = o.getDouble("latitude"),
                longitude = o.getDouble("longitude"),
            )
        }
        return out
    }

    // Null-safe array accessors: a variable missing for a region must never crash parsing.
    private fun JSONObject.arr(name: String): JSONArray? = if (has(name) && !isNull(name)) getJSONArray(name) else null
    private fun JSONObject.dbl(name: String, i: Int, def: Double = 0.0): Double {
        val a = arr(name) ?: return def
        return if (i < a.length() && !a.isNull(i)) a.optDouble(i, def) else def
    }
    private fun JSONObject.intg(name: String, i: Int, def: Int = 0): Int {
        val a = arr(name) ?: return def
        return if (i < a.length() && !a.isNull(i)) a.optInt(i, def) else def
    }
    private fun JSONObject.str(name: String, i: Int, def: String = ""): String {
        val a = arr(name) ?: return def
        return if (i < a.length() && !a.isNull(i)) a.optString(i, def) else def
    }
}

val POPULAR_IRAN_CITIES = listOf(
    City("تهران", "ایران", "تهران", 35.6892, 51.3890),
    City("مشهد", "ایران", "خراسان رضوی", 36.2972, 59.6067),
    City("اصفهان", "ایران", "اصفهان", 32.6539, 51.6660),
    City("شیراز", "ایران", "فارس", 29.5918, 52.5837),
    City("تبریز", "ایران", "آذربایجان شرقی", 38.0962, 46.2738),
    City("رشت", "ایران", "گیلان", 37.2761, 49.5886),
    City("کرج", "ایران", "البرز", 35.8327, 50.9915),
    City("اهواز", "ایران", "خوزستان", 31.3183, 48.6706),
    City("کیش", "ایران", "هرمزگان", 26.5380, 53.9800),
    City("یزد", "ایران", "یزد", 31.8974, 54.3569),
)
