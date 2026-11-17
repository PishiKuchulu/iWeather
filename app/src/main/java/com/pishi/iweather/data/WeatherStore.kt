package com.pishi.iweather.data

import android.content.Context
import java.io.File

/**
 * Single source of truth for the saved city + the on-disk weather cache.
 * Used by the app UI, the home-screen widgets and the background alert worker,
 * so all three always show the same data.
 */
object WeatherStore {
    private const val PREFS = "iweather"

    fun savedCity(context: Context): City? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val name = prefs.getString("city_name", null) ?: return null
        val lat = prefs.getFloat("city_lat", Float.NaN)
        val lon = prefs.getFloat("city_lon", Float.NaN)
        if (lat.isNaN() || lon.isNaN()) return null
        return City(
            name = name,
            country = prefs.getString("city_country", "") ?: "",
            admin1 = prefs.getString("city_admin", "") ?: "",
            latitude = lat.toDouble(), longitude = lon.toDouble(),
        )
    }

    fun saveCity(context: Context, city: City) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("city_name", city.name)
            .putString("city_country", city.country)
            .putString("city_admin", city.admin1)
            .putFloat("city_lat", city.latitude.toFloat())
            .putFloat("city_lon", city.longitude.toFloat())
            .apply()
    }

    fun cachedReport(context: Context, api: WeatherApi = WeatherApi()): WeatherReport? {
        val city = savedCity(context) ?: POPULAR_IRAN_CITIES.first()
        return cachedReport(context, city, api)
    }

    /** Latest cached report for [city]: the "last viewed" cache first, then the per-city cache (favourites, switching back). */
    fun cachedReport(context: Context, city: City, api: WeatherApi = WeatherApi()): WeatherReport? =
        lastCached(context, city, api) ?: cachedCityReport(context, city, api)

    private fun lastCached(context: Context, city: City, api: WeatherApi): WeatherReport? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val fetchedAt = prefs.getLong("cache_fetched_at", 0L)
        val forecastFile = File(context.filesDir, "last_forecast.json")
        if (fetchedAt == 0L || !forecastFile.exists()) return null
        val cacheLat = prefs.getFloat("cache_lat", Float.NaN)
        val cacheLon = prefs.getFloat("cache_lon", Float.NaN)
        if (cacheLat.isNaN() || kotlin.math.abs(cacheLat - city.latitude.toFloat()) > 0.01f) return null
        if (cacheLon.isNaN() || kotlin.math.abs(cacheLon - city.longitude.toFloat()) > 0.01f) return null
        return try {
            val airFile = File(context.filesDir, "last_air.json")
            val air = if (airFile.exists()) airFile.readText() else null
            api.parseReport(city, WeatherApi.RawWeather(forecastFile.readText(), air), fetchedAt)
        } catch (e: Exception) {
            null
        }
    }

    fun saveCache(context: Context, city: City, raw: WeatherApi.RawWeather, fetchedAt: Long) {
        runCatching {
            File(context.filesDir, "last_forecast.json").writeText(raw.forecastJson)
            val airFile = File(context.filesDir, "last_air.json")
            if (raw.airJson != null) airFile.writeText(raw.airJson) else airFile.delete()
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putLong("cache_fetched_at", fetchedAt)
                .putFloat("cache_lat", city.latitude.toFloat())
                .putFloat("cache_lon", city.longitude.toFloat())
                .apply()
            saveCityCache(context, city, raw, fetchedAt)
        }
    }

    // ---- per-city cache: every city that was loaded keeps its last report (bounded, oldest evicted) ----

    private const val MAX_CITY_CACHES = 16

    private fun cityKey(city: City): String =
        String.format(java.util.Locale.US, "%.2f_%.2f", city.latitude, city.longitude)

    fun saveCityCache(context: Context, city: City, raw: WeatherApi.RawWeather, fetchedAt: Long) {
        val key = cityKey(city)
        File(context.filesDir, "cc_fc_$key.json").writeText(raw.forecastJson)
        val air = File(context.filesDir, "cc_air_$key.json")
        if (raw.airJson != null) air.writeText(raw.airJson) else air.delete()
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().putLong("cc_ts_$key", fetchedAt).apply()
        val stamps = prefs.all.filterKeys { it.startsWith("cc_ts_") }
            .mapValues { (it.value as? Long) ?: 0L }
        if (stamps.size > MAX_CITY_CACHES) {
            val edit = prefs.edit()
            for ((k, _) in stamps.entries.sortedBy { it.value }.take(stamps.size - MAX_CITY_CACHES)) {
                val old = k.removePrefix("cc_ts_")
                File(context.filesDir, "cc_fc_$old.json").delete()
                File(context.filesDir, "cc_air_$old.json").delete()
                edit.remove(k)
            }
            edit.apply()
        }
    }

    fun cachedCityReport(context: Context, city: City, api: WeatherApi = WeatherApi()): WeatherReport? {
        val key = cityKey(city)
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val fetchedAt = prefs.getLong("cc_ts_$key", 0L)
        val fc = File(context.filesDir, "cc_fc_$key.json")
        if (fetchedAt == 0L || !fc.exists()) return null
        return try {
            val airFile = File(context.filesDir, "cc_air_$key.json")
            val air = if (airFile.exists()) airFile.readText() else null
            api.parseReport(city, WeatherApi.RawWeather(fc.readText(), air), fetchedAt)
        } catch (e: Exception) {
            null
        }
    }

    fun clearCache(context: Context) {
        File(context.filesDir, "last_forecast.json").delete()
        File(context.filesDir, "last_air.json").delete()
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val edit = prefs.edit().remove("cache_fetched_at").remove("cache_lat").remove("cache_lon")
        for (k in prefs.all.keys.filter { it.startsWith("cc_ts_") }) {
            val key = k.removePrefix("cc_ts_")
            File(context.filesDir, "cc_fc_$key.json").delete()
            File(context.filesDir, "cc_air_$key.json").delete()
            edit.remove(k)
        }
        edit.apply()
    }

    fun lastFetchMillis(context: Context): Long =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong("cache_fetched_at", 0L)
}
