package com.pishi.iweather.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** The user's saved cities (max [MAX]) — plain JSON in the app's private preferences. */
object FavoritesStore {
    private const val PREFS = "iweather"
    private const val KEY = "favorites_json"
    const val MAX = 12

    fun load(context: Context): List<City> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.optJSONObject(i) ?: return@mapNotNull null
                val lat = o.optDouble("lat", Double.NaN)
                val lon = o.optDouble("lon", Double.NaN)
                if (lat.isNaN() || lon.isNaN()) null
                else City(
                    name = o.optString("name"),
                    country = o.optString("country"),
                    admin1 = o.optString("admin1"),
                    latitude = lat,
                    longitude = lon,
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun save(context: Context, cities: List<City>) {
        val arr = JSONArray()
        for (c in cities.take(MAX)) {
            arr.put(
                JSONObject()
                    .put("name", c.name).put("country", c.country).put("admin1", c.admin1)
                    .put("lat", c.latitude).put("lon", c.longitude),
            )
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, arr.toString()).apply()
    }

    /** Two cities are "the same place" within ~200 m (AUD-010) — nearby towns stay distinct favorites. */
    fun same(a: City, b: City): Boolean =
        WeatherStore.samePlace(a.latitude, a.longitude, b.latitude, b.longitude)
}
