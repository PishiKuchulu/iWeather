package com.pishi.iweather

import com.pishi.iweather.data.City
import com.pishi.iweather.data.FavoritesStore
import com.pishi.iweather.data.UpdateManager
import com.pishi.iweather.data.WeatherStore
import com.pishi.iweather.notify.alertNotificationId
import com.pishi.iweather.notify.millisUntilNext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

/** AUD-001: identity/scheduling/updater invariants pinned by AUD-006, AUD-010 and the updater. */
class StoresAndWorkersTest {

    private val tehran = City("Tehran", "Iran", "Tehran", 35.6892, 51.3890)

    @Test
    fun nearbyTownsAreDistinctFavorites() {
        // ~1.7 km north of Tehran's center: a different locality, NOT the same favorite.
        val nearby = City("Tajrish", "Iran", "Tehran", 35.7042, 51.3890)
        assertFalse(FavoritesStore.same(tehran, nearby))
        assertFalse(WeatherStore.samePlace(tehran.latitude, tehran.longitude, nearby.latitude, nearby.longitude))
    }

    @Test
    fun samePlaceStillDedupes() {
        val regeocoded = City("Tehran", "Iran", "Tehran", 35.6892, 51.3890)
        assertTrue(FavoritesStore.same(tehran, regeocoded))
        assertEquals(WeatherStore.cityKey(tehran), WeatherStore.cityKey(regeocoded))
    }

    @Test
    fun cityKeysDifferForNearbyTowns() {
        val nearby = City("Tajrish", "Iran", "Tehran", 35.7042, 51.3890)
        assertNotEquals(WeatherStore.cityKey(tehran), WeatherStore.cityKey(nearby))
    }

    @Test
    fun notificationIdsAreStableAndDistinct() {
        val k1 = "2026-11-17|${WeatherStore.cityKey(tehran)}|RAIN"
        val k2 = "2026-11-17|35.704_51.389|RAIN"
        assertEquals(alertNotificationId(k1), alertNotificationId(k1))
        assertNotEquals(alertNotificationId(k1), alertNotificationId(k2))
        assertTrue(alertNotificationId(k1) >= 0)
    }

    @Test
    fun millisUntilNextHour() {
        val zone = ZoneId.of("Asia/Tehran")
        val at0600 = ZonedDateTime.of(2026, 11, 17, 6, 0, 0, 0, zone)
        assertEquals(60 * 60_000L, millisUntilNext(7, at0600))
        val at0800 = ZonedDateTime.of(2026, 11, 17, 8, 0, 0, 0, zone)
        assertEquals(23 * 60 * 60_000L, millisUntilNext(7, at0800))
    }

    @Test
    fun versionComparison() {
        assertTrue(UpdateManager.isNewer("1.6.0", "1.5.0"))
        assertTrue(UpdateManager.isNewer("1.10.0", "1.9.0"))
        assertTrue(UpdateManager.isNewer("2.0.0", "1.9.9"))
        assertFalse(UpdateManager.isNewer("1.5.0", "1.5.0"))
        assertFalse(UpdateManager.isNewer("1.5.0", "1.6.0"))
    }
}
