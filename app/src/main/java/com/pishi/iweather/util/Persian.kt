package com.pishi.iweather.util

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

/** Persian digits + formatting helpers. */
object Fa {
    private val digits = charArrayOf('۰','۱','۲','۳','۴','۵','۶','۷','۸','۹')
    fun num(value: Any?): String {
        if (value == null) return "—"
        return value.toString().map { c -> if (c in '0'..'9') digits[c - '0'] else c }.joinToString("")
    }
    fun temp(celsius: Double): String = num(celsius.roundToInt()) + "°"
    fun percent(v: Int): String = num(v) + "٪"
}

data class Jalali(val year: Int, val month: Int, val day: Int) {
    val monthName: String get() = MONTHS[month - 1]
    fun format(): String = "${Fa.num(day)} $monthName ${Fa.num(year)}"
    companion object {
        val MONTHS = listOf(
            "فروردین","اردیبهشت","خرداد","تیر","مرداد","شهریور",
            "مهر","آبان","آذر","دی","بهمن","اسفند"
        )
    }
}

/**
 * Gregorian -> Jalali, anchor-based:
 * 2024-03-20 is exactly 1403-01-01, and Jalali leap years follow the
 * 33-year cycle (remainders 1,5,9,13,17,22,26,30 are leap).
 * Verified against 13 known dates (1379..1406, incl. leap boundaries).
 */
object JalaliCalendar {
    private val anchorGregorian: LocalDate = LocalDate.of(2024, 3, 20)
    private const val ANCHOR_JALALI_YEAR = 1403
    private val leapRemainders = setOf(1, 5, 9, 13, 17, 22, 26, 30)

    fun isLeapJalali(jy: Int): Boolean = Math.floorMod(jy, 33) in leapRemainders
    private fun yearLength(jy: Int): Int = if (isLeapJalali(jy)) 366 else 365

    fun fromGregorian(gy: Int, gm: Int, gd: Int): Jalali {
        var diff = (LocalDate.of(gy, gm, gd).toEpochDay() - anchorGregorian.toEpochDay()).toInt()
        var jy = ANCHOR_JALALI_YEAR
        if (diff >= 0) {
            while (diff >= yearLength(jy)) { diff -= yearLength(jy); jy++ }
        } else {
            while (diff < 0) { jy--; diff += yearLength(jy) }
        }
        val monthLengths = IntArray(12) { i ->
            if (i < 6) 31 else if (i < 11) 30 else if (isLeapJalali(jy)) 30 else 29
        }
        var m = 0
        while (diff >= monthLengths[m]) { diff -= monthLengths[m]; m++ }
        return Jalali(jy, m + 1, diff + 1)
    }

    fun today(): Jalali {
        val n = LocalDate.now()
        return fromGregorian(n.year, n.monthValue, n.dayOfMonth)
    }
    fun of(dateTimeIso: String): Jalali? = try {
        val dt = LocalDateTime.parse(dateTimeIso, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        fromGregorian(dt.year, dt.monthValue, dt.dayOfMonth)
    } catch (e: Exception) { null }

    fun weekdayFa(date: LocalDate): String = when (date.dayOfWeek.value) {
        1 -> "دوشنبه"; 2 -> "سه‌شنبه"; 3 -> "چهارشنبه"; 4 -> "پنجشنبه"
        5 -> "جمعه"; 6 -> "شنبه"; else -> "یکشنبه"
    }
}

/** Language-aware date presentation: FA -> Jalali + Persian weekday; EN -> Gregorian + English weekday. */
object Dates {
    private val EN_MONTHS = listOf(
        "January","February","March","April","May","June",
        "July","August","September","October","November","December"
    )

    fun weekday(date: LocalDate, lang: Lang): String = if (lang == Lang.FA) {
        JalaliCalendar.weekdayFa(date)
    } else when (date.dayOfWeek.value) {
        1 -> "Monday"; 2 -> "Tuesday"; 3 -> "Wednesday"; 4 -> "Thursday"
        5 -> "Friday"; 6 -> "Saturday"; else -> "Sunday"
    }

    /** Full date for headers: «شنبه ۲۴ آبان ۱۴۰۴» / "Saturday, November 15, 2025". */
    fun full(date: LocalDate, lang: Lang): String = if (lang == Lang.FA) {
        val j = JalaliCalendar.fromGregorian(date.year, date.monthValue, date.dayOfMonth)
        "${weekday(date, lang)} ${j.format()}"
    } else {
        "${weekday(date, lang)}, ${EN_MONTHS[date.monthValue - 1]} ${date.dayOfMonth}, ${date.year}"
    }

    /** Short date for day rows: «۲۴ آبان» / "Nov 15". */
    fun short(date: LocalDate, lang: Lang): String = if (lang == Lang.FA) {
        val j = JalaliCalendar.fromGregorian(date.year, date.monthValue, date.dayOfMonth)
        "${Fa.num(j.day)} ${j.monthName}"
    } else {
        "${EN_MONTHS[date.monthValue - 1].take(3)} ${date.dayOfMonth}"
    }
}
