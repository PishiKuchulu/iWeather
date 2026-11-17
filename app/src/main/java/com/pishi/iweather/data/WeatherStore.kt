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
        // Coordinates persist at full Double precision (raw bits in a Long). The legacy
        // Float keys are still read as a fallback for installs from before this change.
        val lat = if (prefs.contains("city_lat_bits")) {
            Double.fromBits(prefs.getLong("city_lat_bits", 0L))
        } else {
            prefs.getFloat("city_lat", Float.NaN).toDouble()
        }
        val lon = if (prefs.contains("city_lon_bits")) {
            Double.fromBits(prefs.getLong("city_lon_bits", 0L))
        } else {
            prefs.getFloat("city_lon", Float.NaN).toDouble()
        }
        if (lat.isNaN() || lon.isNaN()) return null
        return City(
            name = name,
            country = prefs.getString("city_country", "") ?: "",
            admin1 = prefs.getString("city_admin", "") ?: "",
            latitude = lat, longitude = lon,
        )
    }

    fun saveCity(context: Context, city: City) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("city_name", city.name)
            .putString("city_country", city.country)
            .putString("city_admin", city.admin1)
            .putLong("city_lat_bits", city.latitude.toRawBits())
            .putLong("city_lon_bits", city.longitude.toRawBits())
            .apply()
    }

    /**
     * The one canonical city-identity key (AUD-010/AUD-014): a ~110 m coordinate grid.
     * Two places farther apart than ~110 m never share a cache, a favorite slot, or a
     * notification identity; the same place re-geocoded returns identical coordinates.
     */
    fun cityKey(city: City): String =
        String.format(java.util.Locale.US, "%.3f_%.3f", city.latitude, city.longitude)

    /** Coordinate identity tolerance (~200 m): same place, allowing minor geocoder jitter. */
    fun samePlace(aLat: Double, aLon: Double, bLat: Double, bLon: Double): Boolean =
        kotlin.math.abs(aLat - bLat) < 0.002 && kotlin.math.abs(aLon - bLon) < 0.002

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
        val cacheLat = if (prefs.contains("cache_lat_bits")) {
            Double.fromBits(prefs.getLong("cache_lat_bits", 0L))
        } else {
            prefs.getFloat("cache_lat", Float.NaN).toDouble()
        }
        val cacheLon = if (prefs.contains("cache_lon_bits")) {
            Double.fromBits(prefs.getLong("cache_lon_bits", 0L))
        } else {
            prefs.getFloat("cache_lon", Float.NaN).toDouble()
        }
        if (cacheLat.isNaN() || cacheLon.isNaN()) return null
        if (!samePlace(cacheLat, cacheLon, city.latitude, city.longitude)) return null
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
                .putLong("cache_lat_bits", city.latitude.toRawBits())
                .putLong("cache_lon_bits", city.longitude.toRawBits())
                .apply()
            saveCityCache(context, city, raw, fetchedAt)
        }
    }

    // ---- per-city cache: every city that was loaded keeps its last report (bounded, oldest evicted) ----

    private const val MAX_CITY_CACHES = 16

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
            .remove("cache_lat_bits").remove("cache_lon_bits")
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
