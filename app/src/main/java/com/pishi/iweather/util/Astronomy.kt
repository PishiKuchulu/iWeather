package com.pishi.iweather.util

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.round
import kotlin.math.sin

data class MoonInfo(val nameFa: String, val nameEn: String, val emoji: String, val illuminationPercent: Int, val ageDays: Double)
data class Twilight(val dawn: LocalDateTime?, val dusk: LocalDateTime?)

/**
 * On-device astronomy, computed from well-known solar/lunar math (SunCalc-style
 * solar position + synodic lunar cycle). No network, no fake data: twilight
 * times are accurate to a few minutes; moon phase/illumination are standard
 * approximations used by calendar apps.
 */
object Astronomy {

    // ---------- Moon ----------
    private const val SYNODIC = 29.530588853
    private val NEW_MOON_EPOCH_DAY: Long = LocalDate.of(2000, 1, 6).toEpochDay() // new moon 2000-01-06 18:14 UTC

    fun moonPhase(date: LocalDate): MoonInfo {
        val daysSince = (date.toEpochDay() - NEW_MOON_EPOCH_DAY).toDouble() - 0.26 // noon of `date` vs 18:14 UTC reference
        var age = daysSince % SYNODIC
        if (age < 0) age += SYNODIC
        val frac = age / SYNODIC
        val illumination = ((1 - cos(2 * PI * frac)) / 2 * 100).toInt()
        val sector = floor(frac * 8 + 0.5).toInt() % 8
        val (name, nameEn, emoji) = when (sector) {
            0 -> Triple("ماه نو", "New moon", "🌑")
            1 -> Triple("هلال افزاینده", "Waxing crescent", "🌒")
            2 -> Triple("تربیع اول", "First quarter", "🌓")
            3 -> Triple("کوژ افزاینده", "Waxing gibbous", "🌔")
            4 -> Triple("ماه کامل", "Full moon", "🌕")
            5 -> Triple("کوژ کاهنده", "Waning gibbous", "🌖")
            6 -> Triple("تربیع آخر", "Last quarter", "🌗")
            else -> Triple("هلال کاهنده", "Waning crescent", "🌘")
        }
        return MoonInfo(name, nameEn, emoji, illumination, (age * 10).toInt() / 10.0)
    }

    // ---------- Sun (SunCalc algorithm) ----------
    private val RAD = PI / 180.0
    private const val DAY_MS = 86_400_000.0
    private const val J1970 = 2440588.0
    private const val J2000 = 2451545.0
    private val OBLIQUITY = RAD * 23.4397
    private const val J0 = 0.0009

    private fun toJulian(ms: Double) = ms / DAY_MS - 0.5 + J1970
    private fun fromJulian(j: Double) = (j + 0.5 - J1970) * DAY_MS
    private fun toDays(ms: Double) = toJulian(ms) - J2000
    private fun solarMeanAnomaly(d: Double) = RAD * (357.5291 + 0.98560028 * d)
    private fun eclipticLongitude(m: Double): Double {
        val c = RAD * (1.9148 * sin(m) + 0.02 * sin(2 * m) + 0.0003 * sin(3 * m))
        val p = RAD * 102.9372
        return m + c + p + PI
    }
    private fun declination(l: Double) = asin(sin(OBLIQUITY) * sin(l))
    private fun julianCycle(d: Double, lw: Double) = round(d - J0 - lw / (2 * PI))
    private fun approxTransit(ht: Double, lw: Double, n: Double) = J0 + (ht + lw) / (2 * PI) + n
    private fun solarTransitJ(ds: Double, m: Double, l: Double) =
        J2000 + ds + 0.0053 * sin(m) - 0.0069 * sin(2 * l)
    private fun hourAngle(h: Double, phi: Double, dec: Double): Double? {
        val v = (sin(h) - sin(phi) * sin(dec)) / (cos(phi) * cos(dec))
        if (v < -1 || v > 1) return null // polar day/night: event does not occur
        return acos(v)
    }
    private fun getSetJ(h: Double, lw: Double, phi: Double, dec: Double, n: Double, m: Double, l: Double): Double? {
        val w = hourAngle(h, phi, dec) ?: return null
        val ds = approxTransit(w, lw, n)
        return solarTransitJ(ds, m, l)
    }

    /** Civil twilight (sun 6° below horizon) start/end for [date] at the location, in location-local time. */
    fun civilTwilight(date: LocalDate, lat: Double, lon: Double, utcOffsetSec: Int): Twilight {
        val noonUtcMs = date.atTime(12, 0).toEpochSecond(ZoneOffset.UTC) * 1000.0
        val lw = RAD * (-lon)
        val phi = RAD * lat
        val d = toDays(noonUtcMs)
        val n = julianCycle(d, lw)
        val ds = approxTransit(0.0, lw, n)
        val m = solarMeanAnomaly(ds)
        val l = eclipticLongitude(m)
        val dec = declination(l)
        val jNoon = solarTransitJ(ds, m, l)
        val jDusk = getSetJ(-6.0 * RAD, lw, phi, dec, n, m, l)
        val offset = ZoneOffset.ofTotalSeconds(utcOffsetSec)
        fun toLocal(j: Double?) = j?.let { LocalDateTime.ofInstant(Instant.ofEpochMilli(fromJulian(it).toLong()), offset) }
        val dusk = toLocal(jDusk)
        val dawn = if (jDusk != null) toLocal(jNoon - (jDusk - jNoon)) else null
        return Twilight(dawn, dusk)
    }
}
