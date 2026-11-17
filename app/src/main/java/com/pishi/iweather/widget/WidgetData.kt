package com.pishi.iweather.widget

import android.content.Context
import com.pishi.iweather.data.City
import com.pishi.iweather.data.WeatherApi
import com.pishi.iweather.data.WeatherReport
import com.pishi.iweather.data.WeatherStore

/**
 * Weather data for widgets. The app's own city reuses the shared app cache
 * (WeatherStore); a widget pinned to another city gets its own cache file,
 * refreshed by the same background worker — never a fake/default report.
 */
object WidgetData {
    /** Same canonical identity as the app caches (WeatherStore.cityKey). */
    private fun key(city: City): String = WeatherStore.cityKey(city)

    private fun forecastFile(context: Context, city: City) =
        WeatherStore.cacheFile(context, "widget_fc_${key(city)}.json")

    private fun airFile(context: Context, city: City) =
        WeatherStore.cacheFile(context, "widget_air_${key(city)}.json")

    fun save(context: Context, city: City, raw: WeatherApi.RawWeather) {
        runCatching {
            forecastFile(context, city).writeText(raw.forecastJson)
            val air = airFile(context, city)
            if (raw.airJson != null) air.writeText(raw.airJson) else air.delete()
        }
    }

    fun load(context: Context, city: City, api: WeatherApi = WeatherApi()): WeatherReport? {
        val f = forecastFile(context, city)
        if (f.exists()) {
            try {
                val air = airFile(context, city).let { if (it.exists()) it.readText() else null }
                return api.parseReport(city, WeatherApi.RawWeather(f.readText(), air), f.lastModified())
            } catch (e: Exception) {
                // fall through to the shared app cache
            }
        }
        val saved = WeatherStore.savedCity(context)
        if (saved != null &&
            WeatherStore.samePlace(saved.latitude, saved.longitude, city.latitude, city.longitude)
        ) {
            return WeatherStore.cachedReport(context, city, api)
        }
        return null
    }
}
