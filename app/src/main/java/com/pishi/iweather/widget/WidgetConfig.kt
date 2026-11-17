package com.pishi.iweather.widget

import android.content.Context
import com.pishi.iweather.data.City

enum class WidgetStyle { GLASS, TRANSPARENT, MINIMAL, MATERIAL, DYNAMIC }
enum class WidgetSize { SMALL, MEDIUM, LARGE }

/** Per-widget configuration, keyed by appWidgetId. Independent from the app's own city unless followAppCity. */
data class WidgetConfig(
    val followAppCity: Boolean = true,
    val cityName: String = "",
    val cityLat: Double = 0.0,
    val cityLon: Double = 0.0,
    val style: WidgetStyle = WidgetStyle.DYNAMIC,
    val showFeels: Boolean = true,
    val showHourly: Boolean = true,
    val showAqi: Boolean = true,
) {
    fun city(): City? = if (followAppCity || cityName.isBlank()) null
    else City(name = cityName, latitude = cityLat, longitude = cityLon)
}

object WidgetConfigStore {
    private const val PREFS = "iweather_widgets"

    fun load(context: Context, appWidgetId: Int): WidgetConfig {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val style = WidgetStyle.entries.firstOrNull { it.name == p.getString("w${appWidgetId}_style", null) }
            ?: WidgetStyle.DYNAMIC
        return WidgetConfig(
            followAppCity = p.getBoolean("w${appWidgetId}_follow", true),
            cityName = p.getString("w${appWidgetId}_city", "") ?: "",
            cityLat = p.getFloat("w${appWidgetId}_lat", 0f).toDouble(),
            cityLon = p.getFloat("w${appWidgetId}_lon", 0f).toDouble(),
            style = style,
            showFeels = p.getBoolean("w${appWidgetId}_feels", true),
            showHourly = p.getBoolean("w${appWidgetId}_hourly", true),
            showAqi = p.getBoolean("w${appWidgetId}_aqi", true),
        )
    }

    fun save(context: Context, appWidgetId: Int, cfg: WidgetConfig) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean("w${appWidgetId}_follow", cfg.followAppCity)
            .putString("w${appWidgetId}_city", cfg.cityName)
            .putFloat("w${appWidgetId}_lat", cfg.cityLat.toFloat())
            .putFloat("w${appWidgetId}_lon", cfg.cityLon.toFloat())
            .putString("w${appWidgetId}_style", cfg.style.name)
            .putBoolean("w${appWidgetId}_feels", cfg.showFeels)
            .putBoolean("w${appWidgetId}_hourly", cfg.showHourly)
            .putBoolean("w${appWidgetId}_aqi", cfg.showAqi)
            .apply()
    }

    fun delete(context: Context, appWidgetId: Int) {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val e = p.edit()
        listOf("follow", "city", "lat", "lon", "style", "feels", "hourly", "aqi")
            .forEach { e.remove("w${appWidgetId}_$it") }
        e.apply()
    }

    fun allIds(context: Context): Set<Int> {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return p.all.keys.mapNotNull { k ->
            k.removePrefix("w").substringBefore("_").toIntOrNull()
        }.toSet()
    }
}
