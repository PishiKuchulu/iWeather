package com.pishi.iweather.data

data class City(
    val name: String,
    val country: String = "",
    val admin1: String = "",
    val latitude: Double,
    val longitude: Double,
) {
    val subtitle: String get() = listOf(admin1, country).filter { it.isNotBlank() }.joinToString("، ")
}

data class CurrentWeather(
    val temperature: Double,
    val feelsLike: Double,
    val humidity: Int,
    val isDay: Boolean,
    val weatherCode: Int,
    val windSpeed: Double,
    val windDirection: Int,
    val windGusts: Double = 0.0,
    val cloudCover: Int = 0,
    val dewPoint: Double = 0.0,
    val pressure: Double,
    val precipitation: Double,
    val timeIso: String,
)

data class HourPoint(
    val timeIso: String,
    val hourLabel: String,
    val temperature: Double,
    val feelsLike: Double = 0.0,
    val precipProbability: Int,
    val precipitationMm: Double = 0.0,
    val weatherCode: Int,
    val isDay: Boolean,
    val humidity: Int = 0,
    val windSpeed: Double = 0.0,
    val windDirection: Int = 0,
    val windGusts: Double = 0.0,
    val uvIndex: Double = 0.0,
    val cloudCover: Int = 0,
    val visibilityM: Double = 0.0,
    val dewPoint: Double = 0.0,
    val pressure: Double = 0.0,
)

data class DayForecast(
    val dateIso: String,
    val weatherCode: Int,
    val tempMax: Double,
    val tempMin: Double,
    val sunriseIso: String,
    val sunsetIso: String,
    val uvMax: Double,
    val precipProbabilityMax: Int,
    val precipSumMm: Double = 0.0,
    val windSpeedMax: Double = 0.0,
    val windGustsMax: Double = 0.0,
    val windDirectionDominant: Int = 0,
    val daylightSeconds: Int = 0,
    val sunshineSeconds: Int = 0,
)

/** CAMS air-quality snapshot (Open-Meteo air-quality API). Values in µg/m³ except CO (µg/m³ as reported). */
data class AirQuality(
    val usAqi: Int? = null,
    val euAqi: Int? = null,
    val pm25: Double? = null,
    val pm10: Double? = null,
    val carbonMonoxide: Double? = null,
    val nitrogenDioxide: Double? = null,
    val sulphurDioxide: Double? = null,
    val ozone: Double? = null,
    val dust: Double? = null,
)

data class WeatherReport(
    val city: City,
    val current: CurrentWeather,
    val hourly: List<HourPoint>,
    val daily: List<DayForecast>,
    val air: AirQuality? = null,
    val utcOffsetSec: Int = 12_600,
    val fetchedAtMillis: Long = System.currentTimeMillis(),
) {
    val aqiUs: Int? get() = air?.usAqi
    val pm25: Double? get() = air?.pm25
}

/** WMO weather-code -> FA/EN label, emoji and sky kind. */
enum class SkyKind { CLEAR_DAY, CLEAR_NIGHT, CLOUDY, RAIN, SNOW, FOG, STORM }

data class WmoInfo(val labelFa: String, val labelEn: String, val emoji: String, val kind: SkyKind)

fun wmoInfo(code: Int, isDay: Boolean = true): WmoInfo = when (code) {
    0 -> if (isDay) WmoInfo("آفتابی و صاف", "Sunny & clear", "☀️", SkyKind.CLEAR_DAY)
         else WmoInfo("صاف و پرستاره", "Clear & starry", "🌙", SkyKind.CLEAR_NIGHT)
    1 -> WmoInfo("عمدتاً صاف", "Mostly clear", if (isDay) "🌤️" else "🌙", if (isDay) SkyKind.CLEAR_DAY else SkyKind.CLEAR_NIGHT)
    2 -> WmoInfo("نیمه‌ابری", "Partly cloudy", "⛅", SkyKind.CLOUDY)
    3 -> WmoInfo("ابری", "Overcast", "☁️", SkyKind.CLOUDY)
    45, 48 -> WmoInfo("مه‌آلود", "Foggy", "🌫️", SkyKind.FOG)
    51, 53, 55 -> WmoInfo("نم‌نم باران", "Drizzle", "🌦️", SkyKind.RAIN)
    56, 57 -> WmoInfo("باران یخ‌زدهٔ خفیف", "Light freezing drizzle", "🌦️", SkyKind.RAIN)
    61 -> WmoInfo("باران خفیف", "Light rain", "🌧️", SkyKind.RAIN)
    63 -> WmoInfo("بارانی", "Rain", "🌧️", SkyKind.RAIN)
    65 -> WmoInfo("باران شدید", "Heavy rain", "🌧️", SkyKind.RAIN)
    66, 67 -> WmoInfo("باران یخ‌زده", "Freezing rain", "🌧️", SkyKind.RAIN)
    71 -> WmoInfo("برف خفیف", "Light snow", "🌨️", SkyKind.SNOW)
    73 -> WmoInfo("برفی", "Snow", "❄️", SkyKind.SNOW)
    75 -> WmoInfo("برف شدید", "Heavy snow", "❄️", SkyKind.SNOW)
    77 -> WmoInfo("دانه‌های برف", "Snow grains", "🌨️", SkyKind.SNOW)
    80, 81, 82 -> WmoInfo("رگبار باران", "Rain showers", "🌦️", SkyKind.RAIN)
    85, 86 -> WmoInfo("رگبار برف", "Snow showers", "🌨️", SkyKind.SNOW)
    95 -> WmoInfo("رعدوبرق", "Thunderstorm", "⛈️", SkyKind.STORM)
    96, 99 -> WmoInfo("رعدوبرق با تگرگ", "Thunderstorm with hail", "⛈️", SkyKind.STORM)
    else -> WmoInfo("نامشخص", "Unknown", "🌡️", SkyKind.CLOUDY)
}
