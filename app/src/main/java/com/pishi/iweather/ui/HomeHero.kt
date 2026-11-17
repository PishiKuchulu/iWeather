package com.pishi.iweather.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pishi.iweather.data.City
import com.pishi.iweather.data.FavoritesStore
import com.pishi.iweather.data.HourPoint
import com.pishi.iweather.data.POPULAR_IRAN_CITIES
import com.pishi.iweather.data.WeatherReport
import com.pishi.iweather.data.wmoInfo
import com.pishi.iweather.ui.theme.Luna
import com.pishi.iweather.ui.theme.lunaGlass
import com.pishi.iweather.util.AppSettings
import com.pishi.iweather.util.AppStrings
import com.pishi.iweather.util.Dates
import com.pishi.iweather.util.Fmt
import com.pishi.iweather.util.Insights
import com.pishi.iweather.util.Lang
import com.pishi.iweather.util.strings
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

// ============================ Luna sections ============================

@Composable
internal fun LunaHero(
    report: WeatherReport,
    scene: SceneKind,
    st: AppSettings,
    s: AppStrings,
    cityLocal: java.time.LocalDateTime,
    mood: String,
    isFav: Boolean,
    hasAlerts: Boolean,
    stale: Boolean,
    loading: Boolean,
    onMyLocation: () -> Unit,
    onToggleFavorite: () -> Unit,
    onAlertsClick: () -> Unit,
) {
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val serif = FontFamily.Serif
    Box(Modifier.fillMaxWidth()) {
        if (st.dynamicSky) {
            SceneArt(
                scene = scene,
                night = !report.current.isDay,
                animated = st.animations,
                thunder = report.current.weatherCode >= 95,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1104f / 660f)
                    .background(Brush.verticalGradient(listOf(Luna.BgDeep, Luna.Bg))),
            )
        }
        // Soft scrim on the text side keeps the copy readable over bright day scenes.
        Box(
            Modifier
                .matchParentSize()
                .background(
                    Brush.horizontalGradient(
                        if (rtl) listOf(Color.Transparent, Luna.Bg.copy(alpha = 0.55f))
                        else listOf(Luna.Bg.copy(alpha = 0.55f), Color.Transparent),
                    ),
                ),
        )
        Column(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 6.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "iWeather",
                        color = Luna.TextSoft, fontSize = 30.sp, fontWeight = FontWeight.ExtraLight,
                        letterSpacing = 2.sp,
                    )
                    Text(
                        Dates.full(cityLocal.toLocalDate(), st.lang),
                        color = Luna.TextDim, fontSize = 12.sp,
                    )
                }
                LunaRoundButton(LunaPaths.BELL, s.alertsTitle, onAlertsClick, badge = hasAlerts)
                Spacer(Modifier.width(8.dp))
                LunaRoundButton(LunaPaths.PIN, s.myLocation, onMyLocation)
            }
            Spacer(Modifier.height(18.dp))
            Column(Modifier.fillMaxWidth(0.6f)) {
                Text(
                    greetingFor(cityLocal.hour, s),
                    color = Luna.TextSoft, fontFamily = serif, fontSize = 26.sp, lineHeight = 30.sp,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        report.city.name,
                        color = Luna.TextSoft, fontFamily = serif, fontSize = 34.sp, lineHeight = 38.sp,
                        maxLines = 2, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Box(
                        Modifier
                            .size(48.dp)
                            .semantics { contentDescription = if (isFav) s.favActionRemove else s.favActionAdd }
                            .clickable(onClick = onToggleFavorite),
                        contentAlignment = Alignment.Center,
                    ) {
                        LIcon(
                            LunaPaths.STAR,
                            tint = if (isFav) Luna.Gold else Luna.Lavender,
                            iconSize = 22.dp, strokeWidth = 1.6f,
                            fill = if (isFav) Luna.Gold else null,
                        )
                    }
                }
                val subtitle = listOf(report.city.admin1, report.city.country)
                    .filter { it.isNotBlank() }
                    .joinToString(if (st.isFa) "، " else ", ")
                if (subtitle.isNotBlank()) {
                    Text(subtitle, color = Luna.TextDim, fontSize = 12.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Box(
                    Modifier
                        .padding(vertical = 12.dp)
                        .width(28.dp)
                        .height(1.dp)
                        .background(Luna.Violet),
                )
                Text(
                    mood,
                    color = Luna.Lavender, fontSize = 14.sp, lineHeight = 20.sp,
                    maxLines = 4, overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                LunaPillButton(
                    text = s.myLocation.removePrefix("📍").trim(),
                    onClick = onMyLocation,
                    leadingPath = LunaPaths.PIN,
                )
                if (loading) {
                    Spacer(Modifier.width(12.dp))
                    CircularProgressIndicator(color = Luna.VioletLight, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                }
            }
            if (stale) {
                Spacer(Modifier.height(10.dp))
                Chip(s.staleBanner, Color(0xFFFFC107))
            }
        }
    }
}

@Composable
internal fun LunaScoreCard(report: WeatherReport, st: AppSettings, s: AppStrings, comfort: Int, label: String) {
    val cur = report.current
    val today = report.daily.firstOrNull()
    Row(
        modifier = Modifier.fillMaxWidth().lunaGlass(30.dp).padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LunaRing(progress = comfort / 100f, animated = st.animations, modifier = Modifier.size(116.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(Fmt.temp(cur.temperature, st), color = Luna.Content, fontSize = 34.sp, fontWeight = FontWeight.Light)
                Text("${s.feelsLike} ${Fmt.temp(cur.feelsLike, st)}", color = Luna.TextSoft, fontSize = 11.sp)
                if (comfort >= 85) {
                    Spacer(Modifier.height(2.dp))
                    LIcon(LunaPaths.CROWN, Luna.Gold, iconSize = 16.dp, strokeWidth = 1.5f, fill = Luna.Gold.copy(alpha = 0.55f))
                }
            }
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(label, color = Luna.Text, fontSize = 21.sp, fontWeight = FontWeight.Medium, lineHeight = 25.sp)
            Spacer(Modifier.height(6.dp))
            Text(
                "${s.comfortTitle}: ${Insights.comfortLabel(comfort, st.lang)} (${Fmt.percent(comfort, st.lang)})",
                color = Luna.TextMuted, fontSize = 14.sp, lineHeight = 20.sp,
            )
            if (today != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "${Fmt.temp(today.tempMin, st)} / ${Fmt.temp(today.tempMax, st)}",
                    color = Luna.TextDim, fontSize = 13.sp,
                )
            }
        }
        LunaStar(Modifier.align(Alignment.Top))
    }
}

@Composable
internal fun LunaStatRow(report: WeatherReport, st: AppSettings, s: AppStrings) {
    val cur = report.current
    val first = report.hourly.firstOrNull()
    val pressureDiff = if (report.hourly.size >= 7) report.hourly[6].pressure - report.hourly[0].pressure else 0.0
    val trendArrow = when {
        pressureDiff >= 1.5 -> "↗"
        pressureDiff <= -1.5 -> "↘"
        else -> "→"
    }
    val dewLabel = if (st.isFa) "شبنم" else "Dew"
    Row(
        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        LunaStat(
            LunaPaths.DROPLET, s.dHumidity, Fmt.percent(cur.humidity, st.lang),
            "$dewLabel ${Fmt.temp(cur.dewPoint, st)}", Luna.Teal, Modifier.weight(1f),
        )
        LunaStat(
            LunaPaths.WIND, s.dWind, Fmt.wind(cur.windSpeed, st),
            Insights.windDirection(cur.windDirection, st.lang), Luna.Lilac, Modifier.weight(1f),
        )
        LunaStat(
            LunaPaths.GAUGE, s.dPressure, Fmt.pressure(cur.pressure, st),
            trendArrow, if (pressureDiff <= -1.5) Luna.Pink else Luna.Teal, Modifier.weight(1f), highlight = true,
        )
        LunaStat(
            LunaPaths.EYE, s.dVisibility, first?.let { Fmt.visibility(it.visibilityM, st) } ?: "—",
            "☁ ${Fmt.percent(cur.cloudCover, st.lang)}", Luna.Lilac, Modifier.weight(1f),
        )
    }
}

@Composable
internal fun LunaStat(
    path: String,
    label: String,
    value: String,
    sub: String,
    subColor: Color,
    modifier: Modifier,
    highlight: Boolean = false,
) {
    Column(
        modifier = modifier.fillMaxHeight().lunaGlass(22.dp, cyan = highlight).padding(horizontal = 10.dp, vertical = 14.dp),
    ) {
        LIcon(path, Luna.Lavender, iconSize = 26.dp, strokeWidth = 1.5f)
        Spacer(Modifier.height(10.dp))
        Text(label, color = Luna.TextDim, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(value, color = Luna.Content, fontSize = 14.sp, fontWeight = FontWeight.Medium, lineHeight = 17.sp)
        Spacer(Modifier.height(4.dp))
        Text(sub, color = subColor, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

internal val WAVE_GUIDES = listOf(0.125f, 0.375f, 0.625f, 0.875f)

@Composable
internal fun LunaWaveCard(report: WeatherReport, st: AppSettings, s: AppStrings) {
    var long by remember { mutableStateOf(false) }
    val hours = remember(report, long) { report.hourly.take(if (long) 48 else 24) }
    if (hours.size < 2) return
    val temps = hours.map { it.temperature }
    val lo = temps.min()
    val hi = temps.max()
    val maxRain = hours.maxOf { it.precipProbability }

    Column(Modifier.fillMaxWidth().lunaGlass(30.dp).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LunaIconBox(boxSize = 44.dp) {
                LIcon(LunaPaths.SPARKLES, Luna.Lavender, iconSize = 24.dp, strokeWidth = 1.5f, fill = Luna.Lavender.copy(alpha = 0.25f))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(s.waveTitle, color = Luna.Text, fontSize = 19.sp, fontWeight = FontWeight.Medium)
                Text(if (long) s.waveNext48 else s.waveNext24, color = Luna.TextDim, fontSize = 12.sp)
            }
            Text(
                "${Fmt.temp(lo, st)} – ${Fmt.temp(hi, st)}",
                color = Luna.Content, fontSize = 19.sp, fontWeight = FontWeight.Medium,
            )
            Box(
                Modifier
                    .size(48.dp)
                    .semantics { contentDescription = if (long) s.waveNext24 else s.waveNext48 }
                    .clickable { long = !long },
                contentAlignment = Alignment.Center,
            ) {
                LIcon(LunaPaths.EXPAND, Luna.Lavender, iconSize = 20.dp, strokeWidth = 1.8f)
            }
        }
        Spacer(Modifier.height(10.dp))
        WaveCanvas(hours, Modifier.fillMaxWidth().height(150.dp))
        Row(Modifier.fillMaxWidth()) {
            for (f in WAVE_GUIDES) {
                val idx = (f * (hours.size - 1)).roundToInt().coerceIn(0, hours.size - 1)
                Text(
                    Fmt.clock(hours[idx].hourLabel, st),
                    color = Luna.TextDim, fontSize = 11.5.sp, textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f), maxLines = 1,
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            WaveLegend(Luna.Pink, s.chartMax, Fmt.temp(hi, st), Modifier.weight(1f))
            WaveLegend(Luna.Cyan, s.chartMin, Fmt.temp(lo, st), Modifier.weight(1f))
            WaveLegend(Luna.Lilac, s.legendFeels, Fmt.temp(hours.first().feelsLike, st), Modifier.weight(1f))
            WaveLegend(Luna.Blue, s.legendRain, Fmt.percent(maxRain, st.lang), Modifier.weight(1f))
        }
    }
}

@Composable
internal fun WaveLegend(color: Color, label: String, value: String, modifier: Modifier) {
    Row(modifier, verticalAlignment = Alignment.Top) {
        Box(Modifier.padding(top = 4.dp).size(10.dp).background(color, CircleShape))
        Spacer(Modifier.width(6.dp))
        Column {
            Text(label, color = Color(0xFFDDD8F7), fontSize = 12.sp, maxLines = 1)
            Text(value, color = Color(0xFFC2BCE8), fontSize = 12.sp, maxLines = 1)
        }
    }
}

/** Smooth temperature curve with gradient stroke, feels-like dashes, rain bars and time guides. */
@Composable
internal fun WaveCanvas(hours: List<HourPoint>, modifier: Modifier) {
    val temps = hours.map { it.temperature }
    val feels = hours.map { it.feelsLike }
    val lo = minOf(temps.min(), feels.min())
    val hi = maxOf(temps.max(), feels.max())
    val range = (hi - lo).coerceAtLeast(1.0)
    val n = hours.size
    Canvas(modifier) {
        val mirror = layoutDirection == LayoutDirection.Rtl
        val w = size.width
        val h = size.height
        val topPad = 16.dp.toPx()
        val botPad = 8.dp.toPx()
        val ch = h - topPad - botPad
        fun px(i: Int): Float {
            val x = w * i / (n - 1)
            return if (mirror) w - x else x
        }
        fun py(v: Double): Float = topPad + ((hi - v) / range * ch).toFloat()
        fun curve(values: List<Double>, closeToBottom: Boolean): Path {
            val p = Path()
            p.moveTo(px(0), py(values[0]))
            for (i in 1 until n) {
                val mx = (px(i - 1) + px(i)) / 2f
                p.cubicTo(mx, py(values[i - 1]), mx, py(values[i]), px(i), py(values[i]))
            }
            if (closeToBottom) {
                p.lineTo(px(n - 1), h)
                p.lineTo(px(0), h)
                p.close()
            }
            return p
        }

        // time guides
        for (f in WAVE_GUIDES) {
            drawLine(
                Luna.Content.copy(alpha = 0.18f),
                Offset(w * f, 0f), Offset(w * f, h),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(), 5.dp.toPx())),
            )
        }
        // rain-chance bars
        val barW = (w / n) * 0.5f
        hours.forEachIndexed { i, hp ->
            val bh = hp.precipProbability / 100f * (ch * 0.4f)
            if (bh > 1f) {
                drawLine(
                    Luna.Blue.copy(alpha = 0.35f),
                    Offset(px(i), h), Offset(px(i), h - bh),
                    strokeWidth = barW, cap = StrokeCap.Butt,
                )
            }
        }
        // filled area
        drawPath(
            curve(temps, closeToBottom = true),
            Brush.verticalGradient(listOf(Color(0xFF8E6BFF).copy(alpha = 0.45f), Color.Transparent)),
        )
        val line = curve(temps, closeToBottom = false)
        val strokeBrush = Brush.horizontalGradient(listOf(Color(0xFFBF93FF), Color(0xFF6F7FFF), Color(0xFF3FDCFF)))
        drawPath(line, strokeBrush, alpha = 0.28f, style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round))
        drawPath(line, strokeBrush, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
        // feels-like (dashed)
        drawPath(
            curve(feels, closeToBottom = false),
            Luna.Pink.copy(alpha = 0.75f),
            style = Stroke(
                width = 1.6.dp.toPx(), cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(9.dp.toPx(), 7.dp.toPx())),
            ),
        )
        // extremes + "now" marker
        val maxIdx = temps.indexOf(temps.max())
        val minIdx = temps.indexOf(temps.min())
        drawCircle(Luna.Pink, 4.dp.toPx(), Offset(px(maxIdx), py(temps[maxIdx])))
        drawCircle(Luna.Cyan, 4.dp.toPx(), Offset(px(minIdx), py(temps[minIdx])))
        drawCircle(Luna.Content, 5.dp.toPx(), Offset(px(0), py(temps[0])))
        drawCircle(Luna.Violet, 5.dp.toPx(), Offset(px(0), py(temps[0])), style = Stroke(width = 2.dp.toPx()))
    }
}

@Composable
internal fun LunaFeatureRow(
    s: AppStrings,
    clothing: String,
    insight: String,
    onClothing: () -> Unit,
    onInsight: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        LunaFeature(LunaPaths.SHIRT, s.featClothTitle, clothing, onClothing, Modifier.weight(1f))
        LunaFeature(LunaPaths.INSIGHTS, s.featInsightTitle, insight, onInsight, Modifier.weight(1f))
    }
}

@Composable
internal fun LunaFeature(path: String, title: String, desc: String, onClick: () -> Unit, modifier: Modifier) {
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Column(
        modifier = modifier
            .fillMaxHeight()
            .lunaGlass(26.dp)
            .clickable(onClick = onClick)
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LunaIconBox(boxSize = 42.dp) { LIcon(path, Color(0xFFD6CDFF), iconSize = 22.dp, strokeWidth = 1.5f) }
            Spacer(Modifier.width(10.dp))
            Text(title, color = Luna.Text, fontSize = 15.5.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f), maxLines = 2)
            LIcon(
                LunaPaths.CHEVRON, Color(0xFFC9C1F5),
                modifier = Modifier.graphicsLayer { scaleX = if (rtl) -1f else 1f },
                iconSize = 18.dp, strokeWidth = 2f,
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(desc, color = Luna.TextMuted, fontSize = 12.5.sp, lineHeight = 18.sp, maxLines = 4, overflow = TextOverflow.Ellipsis)
    }
}
