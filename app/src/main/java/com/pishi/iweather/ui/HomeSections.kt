package com.pishi.iweather.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pishi.iweather.data.DayForecast
import com.pishi.iweather.data.WeatherReport
import com.pishi.iweather.data.wmoInfo
import com.pishi.iweather.util.AppSettings
import com.pishi.iweather.util.AppStrings
import com.pishi.iweather.util.Astronomy
import com.pishi.iweather.util.Dates
import com.pishi.iweather.util.Fmt
import com.pishi.iweather.util.Insights
import com.pishi.iweather.util.strings
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.math.roundToInt

// Shared building blocks of the home screen (air quality, sun & moon, 16-day list, score rings…).
// Extracted from the original single-file HomeScreen; layout/logic unchanged except where noted.

/** Staggered fade/slide entrance for cards; instant when animations are off. */
@Composable
internal fun AnimatedCard(index: Int, enabled: Boolean, content: @Composable () -> Unit) {
    var shown by remember { mutableStateOf(!enabled) }
    LaunchedEffect(Unit) {
        if (enabled) {
            kotlinx.coroutines.delay(index * 70L)
            shown = true
        }
    }
    val progress by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = tween(450),
        label = "cardIn",
    )
    Box(
        modifier = Modifier.graphicsLayer {
            alpha = if (enabled) progress else 1f
            translationY = if (enabled) (1f - progress) * 36f else 0f
        },
    ) { content() }
}

/** Time-of-day greeting in the city's local time (Eclipse hero). */
internal fun greetingFor(hour: Int, s: AppStrings): String = when (hour) {
    in 5..10 -> s.greetMorning
    in 11..14 -> s.greetNoon
    in 15..18 -> s.greetAfternoon
    else -> s.greetNight
}

@Composable
internal fun AirCard(report: WeatherReport, st: AppSettings) {
    val s = strings(st.lang)
    val air = report.air ?: return
    val usPrimary = st.aqiStandard == com.pishi.iweather.util.AqiStandard.US
    val primaryValue = if (usPrimary) air.usAqi else air.euAqi
    val primaryLabel = if (usPrimary) s.aqiUs else s.aqiEu
    val primaryBand = primaryValue?.let {
        if (usPrimary) Insights.aqiLabel(it, st.lang) else Insights.aqiEuLabel(it, st.lang)
    }
    if (primaryValue != null) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(Fmt.num(primaryValue, st.lang), color = aqiColor(primaryValue, usPrimary), fontSize = 40.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(10.dp))
            Column {
                Text(primaryLabel, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Text(primaryBand ?: "", color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp)
            }
            Spacer(Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.End) {
                air.usAqi?.let { Text("${s.aqiUs}: ${Fmt.num(it, st.lang)}", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp) }
                air.euAqi?.let { Text("${s.aqiEu}: ${Fmt.num(it, st.lang)}", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp) }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
    val pollutants = listOfNotNull(
        air.pm25?.let { "PM2.5" to it },
        air.pm10?.let { "PM10" to it },
        air.ozone?.let { "O₃" to it },
        air.nitrogenDioxide?.let { "NO₂" to it },
        air.sulphurDioxide?.let { "SO₂" to it },
        air.carbonMonoxide?.let { "CO" to it },
        air.dust?.let { s.dust to it },
    )
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        for (row in pollutants.chunked(3)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for ((name, v) in row) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color.White.copy(alpha = 0.10f), RoundedCornerShape(10.dp))
                            .padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(name, color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
                        Text(Fmt.num(v.roundToInt(), st.lang), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
    if (st.healthAdvice) {
        val advice = Insights.healthAdvice(report, st.lang)
        if (advice.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            for (line in advice) {
                Text("🩺 $line", color = Color.White.copy(alpha = 0.95f), fontSize = 12.5.sp, modifier = Modifier.padding(vertical = 1.dp))
            }
        }
    }
    Spacer(Modifier.height(6.dp))
    Text(s.camsNote, color = Color.White.copy(alpha = 0.65f), fontSize = 11.sp)
}

@Composable
internal fun SunMoonCard(report: WeatherReport, st: AppSettings) {
    val s = strings(st.lang)
    val today = report.daily.firstOrNull() ?: return
    // Polar day/night or missing data gives an empty string; never substring blindly.
    val rise = Fmt.isoHm(today.sunriseIso)?.let { Fmt.clock(it, st) } ?: "—"
    val set = Fmt.isoHm(today.sunsetIso)?.let { Fmt.clock(it, st) } ?: "—"
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        SunCell("🌅", s.sunrise, rise)
        SunCell("🌇", s.sunset, set)
        SunCell("🕐", s.dayLength, Fmt.duration(today.daylightSeconds, st))
        SunCell("🔆", s.sunshine, Fmt.duration(today.sunshineSeconds, st))
    }
    val localDate = runCatching { LocalDate.parse(today.dateIso) }.getOrNull()
    if (localDate != null) {
        val twilight = Astronomy.civilTwilight(localDate, report.city.latitude, report.city.longitude, report.utcOffsetSec)
        val moon = Astronomy.moonPhase(localDate)
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            SunCell("🌄", s.twilightDawn, twilight.dawn?.let { Fmt.clock(it.hour * 60 + it.minute, st) } ?: "—")
            SunCell("🌆", s.twilightDusk, twilight.dusk?.let { Fmt.clock(it.hour * 60 + it.minute, st) } ?: "—")
            SunCell(
                moon.emoji,
                "${s.moon}: ${if (st.isFa) moon.nameFa else moon.nameEn}",
                "${s.moonIllum} ${Fmt.percent(moon.illuminationPercent, st.lang)}",
            )
            SunCell("📆", Fmt.num(String.format(java.util.Locale.US, s.moonAge, moon.ageDays.toInt()), st.lang), "")
        }
    }
}

@Composable
internal fun SunCell(emoji: String, label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, fontSize = 20.sp)
        Text(label, color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp, textAlign = TextAlign.Center)
        if (value.isNotBlank()) {
            Text(value, color = Color.White, fontSize = 12.5.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
internal fun DailyList(report: WeatherReport, st: AppSettings) {
    val s = strings(st.lang)
    var expanded by remember { mutableStateOf<String?>(null) }
    Column {
        report.daily.forEachIndexed { idx, day ->
            val di = wmoInfo(day.weatherCode)
            val date = runCatching { LocalDate.parse(day.dateIso) }.getOrNull()
            val dayName = when (idx) {
                0 -> s.today
                1 -> s.tomorrow
                else -> date?.let { Dates.weekday(it, st.lang) } ?: ""
            }
            val dateText = date?.let { Dates.short(it, st.lang) } ?: ""
            val isOpen = expanded == day.dateIso
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = if (isOpen) null else day.dateIso }
                    .padding(vertical = 7.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.width(86.dp)) {
                        Text(dayName, color = Color.White, fontWeight = FontWeight.Medium, fontSize = 13.5.sp)
                        Text(dateText, color = Color.White.copy(alpha = 0.75f), fontSize = 11.5.sp)
                    }
                    Text(di.emoji, fontSize = 20.sp)
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(if (st.isFa) di.labelFa else di.labelEn, color = Color.White, fontSize = 13.sp)
                        Text(
                            "💧 ${Fmt.percent(day.precipProbabilityMax, st.lang)}",
                            color = Color(0xFFB3E5FC), fontSize = 11.5.sp,
                        )
                    }
                    Text(
                        "${Fmt.temp(day.tempMin, st)}  ",
                        color = Color.White.copy(alpha = 0.8f), fontSize = 13.5.sp,
                    )
                    TempBar(day.tempMin, day.tempMax, report.daily.minOf { it.tempMin }, report.daily.maxOf { it.tempMax })
                    Text(
                        "  ${Fmt.temp(day.tempMax, st)}",
                        color = Color.White, fontSize = 13.5.sp, fontWeight = FontWeight.Medium,
                    )
                    Text(if (isOpen) " ▲" else " ▼", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                }
                if (isOpen) {
                    DayDetail(day, st)
                }
            }
            if (idx < report.daily.lastIndex) {
                Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.14f)))
            }
        }
    }
}

@Composable
internal fun DayDetail(day: DayForecast, st: AppSettings) {
    val s = strings(st.lang)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp)
            .background(Color.White.copy(alpha = 0.10f), RoundedCornerShape(12.dp))
            .padding(12.dp),
    ) {
        Text(Insights.daySummary(day, st), color = Color.White, fontSize = 13.sp)
        Spacer(Modifier.height(8.dp))
        val rows = listOf(
            "🌧️ ${s.ddPrecip}" to Fmt.precip(day.precipSumMm, st),
            "💨 ${s.ddWindMax}" to Fmt.wind(day.windSpeedMax, st),
            "🌪️ ${s.ddGusts}" to Fmt.wind(day.windGustsMax, st),
            "☀️ ${s.ddUv}" to "${Fmt.num(day.uvMax.roundToInt(), st.lang)} · ${Insights.uvLabel(day.uvMax, st.lang)}",
            "🌅 ${s.sunrise}" to (Fmt.isoHm(day.sunriseIso)?.let { Fmt.clock(it, st) } ?: "—"),
            "🌇 ${s.sunset}" to (Fmt.isoHm(day.sunsetIso)?.let { Fmt.clock(it, st) } ?: "—"),
            "🕐 ${s.ddDayLength}" to Fmt.duration(day.daylightSeconds, st),
            "🧭 ${s.dWind}" to Insights.windDirection(day.windDirectionDominant, st.lang),
        )
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            for (row in rows.chunked(2)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    for ((k, v) in row) {
                        Text("$k: $v", color = Color.White.copy(alpha = 0.95f), fontSize = 12.5.sp, modifier = Modifier.weight(1f))
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
internal fun ScoreRing(score: Int, emoji: String, animated: Boolean) {
    val sweep by animateFloatAsState(
        targetValue = score / 100f,
        animationSpec = tween(if (animated) 900 else 0),
        label = "ring",
    )
    val color = when {
        score >= 70 -> Color(0xFF69F0AE)
        score >= 40 -> Color(0xFFFFD54F)
        else -> Color(0xFFFF8A80)
    }
    Box(Modifier.size(64.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            drawArc(
                color = Color.White.copy(alpha = 0.18f),
                startAngle = -90f, sweepAngle = 360f, useCenter = false,
                style = Stroke(width = 7f, cap = StrokeCap.Round),
            )
            drawArc(
                color = color,
                startAngle = -90f, sweepAngle = 360f * sweep, useCenter = false,
                style = Stroke(width = 7f, cap = StrokeCap.Round),
            )
        }
        Text(emoji, fontSize = 21.sp)
    }
}

@Composable
internal fun SectionTitle(text: String) {
    Text(text, color = Color.White, fontSize = 15.5.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(8.dp))
}

@Composable
internal fun Chip(text: String, color: Color) {
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.18f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(text, color = color, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
internal fun TempBar(min: Double, max: Double, globalMin: Double, globalMax: Double) {
    val range = (globalMax - globalMin).coerceAtLeast(1.0)
    val start = ((min - globalMin) / range).toFloat().coerceIn(0f, 1f)
    val end = ((max - globalMin) / range).toFloat().coerceIn(0f, 1f)
    Canvas(
        modifier = Modifier
            .width(64.dp)
            .height(6.dp),
    ) {
        drawLine(Color.White.copy(alpha = 0.2f), Offset(0f, size.height / 2), Offset(size.width, size.height / 2), strokeWidth = 6f, cap = StrokeCap.Round)
        drawLine(
            brush = Brush.horizontalGradient(listOf(Color(0xFF4FC3F7), Color(0xFFFFB74D))),
            start = Offset(size.width * start, size.height / 2),
            end = Offset(size.width * end, size.height / 2),
            strokeWidth = 6f, cap = StrokeCap.Round,
        )
    }
}

internal fun aqiColor(aqi: Int, us: Boolean): Color {
    if (us) {
        return when {
            aqi <= 50 -> Color(0xFF69F0AE)
            aqi <= 100 -> Color(0xFFFFEE58)
            aqi <= 150 -> Color(0xFFFFB74D)
            aqi <= 200 -> Color(0xFFFF8A80)
            aqi <= 300 -> Color(0xFFCE93D8)
            else -> Color(0xFFEF9A9A)
        }
    }
    return when {
        aqi <= 20 -> Color(0xFF69F0AE)
        aqi <= 40 -> Color(0xFFFFEE58)
        aqi <= 60 -> Color(0xFFFFB74D)
        aqi <= 80 -> Color(0xFFFF8A80)
        aqi <= 100 -> Color(0xFFCE93D8)
        else -> Color(0xFFEF9A9A)
    }
}

internal fun cityLocalNow(report: WeatherReport): LocalDateTime =
    LocalDateTime.now(ZoneOffset.ofTotalSeconds(report.utcOffsetSec))

internal fun cityLocalFromEpoch(epochMillis: Long, utcOffsetSec: Int): LocalDateTime =
    LocalDateTime.ofEpochSecond(epochMillis / 1000, 0, ZoneOffset.ofTotalSeconds(utcOffsetSec))
