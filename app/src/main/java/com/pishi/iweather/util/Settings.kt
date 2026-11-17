package com.pishi.iweather.util

import android.content.Context
import kotlin.math.roundToInt

enum class Lang { FA, EN }
/** UI theme: AUTO follows the current location's day/night; DAY/NIGHT force a palette. */
enum class ThemeMode { AUTO, DAY, NIGHT }
enum class TempUnit { C, F }
enum class WindUnit { KMH, MS, MPH, KN }
enum class PressureUnit { HPA, MBAR, MMHG, INHG }
enum class PrecipUnit { MM, IN }
enum class VisUnit { KM, MI }
enum class AqiStandard { US, EU }
enum class AlertType { RAIN, SNOW, STORM, HEAT, COLD, WIND, DUST, AQI, UV }

data class AlertThresholds(
    val tempHigh: Int = 38,
    val tempLow: Int = -5,
    val windGust: Int = 60,
    val aqi: Int = 150,
)

data class AppSettings(
    val lang: Lang = Lang.FA,
    val langChosen: Boolean = false,
    val tempUnit: TempUnit = TempUnit.C,
    val windUnit: WindUnit = WindUnit.KMH,
    val pressureUnit: PressureUnit = PressureUnit.HPA,
    val precipUnit: PrecipUnit = PrecipUnit.MM,
    val visUnit: VisUnit = VisUnit.KM,
    val time24: Boolean = true,
    val dynamicSky: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.AUTO,
    val animations: Boolean = true,
    val compact: Boolean = false,
    val aqiStandard: AqiStandard = AqiStandard.US,
    val healthAdvice: Boolean = true,
    val notifEnabled: Boolean = false,
    val notifTypes: Set<AlertType> = AlertType.entries.toSet(),
    val thTempHigh: Int = 38,
    val thTempLow: Int = -5,
    val thWind: Int = 60,
    val thAqi: Int = 150,
    val notifIntervalH: Int = 3,
    val quietEnabled: Boolean = false,
    val quietStart: Int = 23,
    val quietEnd: Int = 7,
    val briefEnabled: Boolean = false,
    val briefHour: Int = 7,
) {
    val thresholds: AlertThresholds get() = AlertThresholds(thTempHigh, thTempLow, thWind, thAqi)
    val isFa: Boolean get() = lang == Lang.FA
}

/** Persists AppSettings in the app's SharedPreferences. Every value round-trips; nothing decorative. */
class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("iweather", Context.MODE_PRIVATE)

    fun load(): AppSettings {
        fun <T : Enum<T>> enumVal(key: String, def: T, values: Array<T>): T =
            values.firstOrNull { it.name == prefs.getString(key, def.name) } ?: def
        val types = prefs.getString("set_notif_types", null)
            ?.split(",")?.mapNotNull { name -> AlertType.entries.firstOrNull { it.name == name } }?.toSet()
            ?: AlertType.entries.toSet()
        return AppSettings(
            lang = enumVal("set_lang", Lang.FA, Lang.entries.toTypedArray()),
            langChosen = prefs.getBoolean("set_lang_chosen", false),
            tempUnit = enumVal("set_temp_unit", TempUnit.C, TempUnit.entries.toTypedArray()),
            windUnit = enumVal("set_wind_unit", WindUnit.KMH, WindUnit.entries.toTypedArray()),
            pressureUnit = enumVal("set_pressure_unit", PressureUnit.HPA, PressureUnit.entries.toTypedArray()),
            precipUnit = enumVal("set_precip_unit", PrecipUnit.MM, PrecipUnit.entries.toTypedArray()),
            visUnit = enumVal("set_vis_unit", VisUnit.KM, VisUnit.entries.toTypedArray()),
            time24 = prefs.getBoolean("set_time24", true),
            dynamicSky = prefs.getBoolean("set_dynamic_sky", true),
            themeMode = enumVal("set_theme_mode", ThemeMode.AUTO, ThemeMode.entries.toTypedArray()),
            animations = prefs.getBoolean("set_animations", true),
            compact = prefs.getBoolean("set_compact", false),
            aqiStandard = enumVal("set_aqi_std", AqiStandard.US, AqiStandard.entries.toTypedArray()),
            healthAdvice = prefs.getBoolean("set_health_advice", true),
            notifEnabled = prefs.getBoolean("set_notif_enabled", false),
            notifTypes = types,
            thTempHigh = prefs.getInt("set_th_high", 38),
            thTempLow = prefs.getInt("set_th_low", -5),
            thWind = prefs.getInt("set_th_wind", 60),
            thAqi = prefs.getInt("set_th_aqi", 150),
            notifIntervalH = prefs.getInt("set_notif_interval", 3),
            quietEnabled = prefs.getBoolean("set_quiet_enabled", false),
            quietStart = prefs.getInt("set_quiet_start", 23),
            quietEnd = prefs.getInt("set_quiet_end", 7),
            briefEnabled = prefs.getBoolean("set_brief_enabled", false),
            briefHour = prefs.getInt("set_brief_hour", 7),
        )
    }

    fun save(s: AppSettings) {
        prefs.edit()
            .putString("set_lang", s.lang.name)
            .putBoolean("set_lang_chosen", s.langChosen)
            .putString("set_temp_unit", s.tempUnit.name)
            .putString("set_wind_unit", s.windUnit.name)
            .putString("set_pressure_unit", s.pressureUnit.name)
            .putString("set_precip_unit", s.precipUnit.name)
            .putString("set_vis_unit", s.visUnit.name)
            .putBoolean("set_time24", s.time24)
            .putBoolean("set_dynamic_sky", s.dynamicSky)
            .putString("set_theme_mode", s.themeMode.name)
            .putBoolean("set_animations", s.animations)
            .putBoolean("set_compact", s.compact)
            .putString("set_aqi_std", s.aqiStandard.name)
            .putBoolean("set_health_advice", s.healthAdvice)
            .putBoolean("set_notif_enabled", s.notifEnabled)
            .putString("set_notif_types", s.notifTypes.joinToString(",") { it.name })
            .putInt("set_th_high", s.thTempHigh)
            .putInt("set_th_low", s.thTempLow)
            .putInt("set_th_wind", s.thWind)
            .putInt("set_th_aqi", s.thAqi)
            .putInt("set_notif_interval", s.notifIntervalH)
            .putBoolean("set_quiet_enabled", s.quietEnabled)
            .putInt("set_quiet_start", s.quietStart)
            .putInt("set_quiet_end", s.quietEnd)
            .putBoolean("set_brief_enabled", s.briefEnabled)
            .putInt("set_brief_hour", s.briefHour)
            .apply()
    }
}

/** All unit conversion + localized number/clock formatting in one place. */
object Fmt {
    fun num(v: Any?, lang: Lang): String {
        if (v == null) return "—"
        val s = v.toString()
        return if (lang == Lang.FA) Fa.num(s) else s
    }

    fun percent(v: Int, lang: Lang): String =
        if (lang == Lang.FA) Fa.percent(v) else "$v%"

    private fun oneDecimal(v: Double): String {
        val r = (v * 10).roundToInt() / 10.0
        return if (r == r.toInt().toDouble()) r.toInt().toString() else r.toString()
    }

    fun temp(celsius: Double, st: AppSettings): String {
        val v = if (st.tempUnit == TempUnit.F) celsius * 9 / 5 + 32 else celsius
        val label = when {
            st.tempUnit == TempUnit.F -> "°F"
            st.lang == Lang.EN -> "°C"
            else -> "°"
        }
        return num(v.roundToInt(), st.lang) + label
    }

    fun tempDelta(celsius: Double, st: AppSettings): String {
        val v = if (st.tempUnit == TempUnit.F) celsius * 9 / 5 else celsius
        return num(v.roundToInt(), st.lang) + "°"
    }

    fun wind(kmh: Double, st: AppSettings): String {
        val (v, unit) = when (st.windUnit) {
            WindUnit.KMH -> kmh to "km/h"
            WindUnit.MS -> kmh / 3.6 to "m/s"
            WindUnit.MPH -> kmh * 0.621371 to "mph"
            WindUnit.KN -> kmh * 0.539957 to "kn"
        }
        return "${num(v.roundToInt(), st.lang)} $unit"
    }

    fun pressure(hpa: Double, st: AppSettings): String = when (st.pressureUnit) {
        PressureUnit.HPA -> "${num(hpa.roundToInt(), st.lang)} hPa"
        PressureUnit.MBAR -> "${num(hpa.roundToInt(), st.lang)} mbar"
        PressureUnit.MMHG -> "${num((hpa * 0.750062).roundToInt(), st.lang)} mmHg"
        PressureUnit.INHG -> {
            val v = (hpa * 0.0295301 * 100).roundToInt() / 100.0
            "${num(v, st.lang)} inHg"
        }
    }

    fun precip(mm: Double, st: AppSettings): String = when (st.precipUnit) {
        PrecipUnit.MM -> "${num(oneDecimal(mm), st.lang)} mm"
        PrecipUnit.IN -> {
            val v = (mm / 25.4 * 100).roundToInt() / 100.0
            "${num(v, st.lang)} in"
        }
    }

    fun visibility(meters: Double, st: AppSettings): String {
        if (meters <= 0) return "—"
        return when (st.visUnit) {
            VisUnit.KM -> {
                if (meters < 1000) "${num(meters.roundToInt(), st.lang)} ${if (st.isFa) "متر" else "m"}"
                else "${num(oneDecimal(meters / 1000), st.lang)} ${if (st.isFa) "کیلومتر" else "km"}"
            }
            VisUnit.MI -> {
                val mi = meters / 1609.344
                if (mi < 0.1) "${num((meters * 1.0936).roundToInt(), st.lang)} yd"
                else "${num(oneDecimal(mi), st.lang)} mi"
            }
        }
    }

    /** "2026-11-15T06:41" -> "06:41"; null when the API gave no value (polar day/night, missing data). */
    fun isoHm(iso: String): String? = if (iso.length >= 16 && iso[13] == ':') iso.substring(11, 16) else null

    /** "HH:mm" (24h ISO fragment) -> localized clock per the 12/24h setting. */
    fun clock(hhmm: String, st: AppSettings): String {
        val parts = hhmm.split(":")
        val h = parts.getOrNull(0)?.toIntOrNull() ?: return hhmm
        val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
        return clock(h * 60 + m, st)
    }

    fun clock(minutesOfDay: Int, st: AppSettings): String {
        val h24 = (minutesOfDay / 60) % 24
        val m = minutesOfDay % 60
        return if (st.time24) {
            num(String.format(java.util.Locale.US, "%02d:%02d", h24, m), st.lang)
        } else {
            val suffix = when {
                st.isFa -> if (h24 < 12) "ق.ظ" else "ب.ظ"
                else -> if (h24 < 12) "AM" else "PM"
            }
            val h12 = if (h24 % 12 == 0) 12 else h24 % 12
            "${num(h12, st.lang)}:${num(String.format(java.util.Locale.US, "%02d", m), st.lang)} $suffix"
        }
    }

    fun duration(totalSeconds: Int, st: AppSettings): String {
        val h = totalSeconds / 3600
        val m = (totalSeconds % 3600) / 60
        return if (st.isFa) {
            when {
                h > 0 && m > 0 -> "${num(h, st.lang)} ساعت و ${num(m, st.lang)} دقیقه"
                h > 0 -> "${num(h, st.lang)} ساعت"
                else -> "${num(m, st.lang)} دقیقه"
            }
        } else {
            when {
                h > 0 && m > 0 -> "${h}h ${m}m"
                h > 0 -> "${h}h"
                else -> "${m}m"
            }
        }
    }
}
