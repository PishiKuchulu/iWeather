package com.pishi.iweather.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.LayoutDirection
import com.pishi.iweather.R
import com.pishi.iweather.data.WeatherReport
import kotlin.math.sin
import kotlin.random.Random

/**
 * The 16 hero scenes of iWeather. Each one has a hand-built day and night illustration
 * (res/drawable-nodpi/scene_<kind>_<day|night>.webp); [SceneArt] lays the live particles
 * (rain, snow, hail, stars, wind streaks, lightning flash) on top of it.
 */
enum class SceneKind {
    CLEAR, PARTLY_CLOUDY, CLOUDY, OVERCAST, FOG, DRIZZLE, RAIN, HEAVY_RAIN,
    THUNDERSTORM, HAIL, SLEET, SNOW, BLIZZARD, WINDY, DUST, HOT,
}

/** WMO code (+ wind / heat / dust / cloud cover) -> scene. Mirrors the mapping documented in docs/WEATHER-SCENES.md. */
fun sceneFor(report: WeatherReport): SceneKind {
    val c = report.current
    val dust = report.air?.dust ?: 0.0
    val base = when (c.weatherCode) {
        0, 1 -> SceneKind.CLEAR
        2 -> SceneKind.PARTLY_CLOUDY
        3 -> if (c.cloudCover < 80) SceneKind.CLOUDY else SceneKind.OVERCAST
        45, 48 -> SceneKind.FOG
        51, 53, 55 -> SceneKind.DRIZZLE
        56, 57, 66, 67 -> SceneKind.SLEET
        61 -> SceneKind.DRIZZLE
        63, 80, 81 -> SceneKind.RAIN
        65, 82 -> SceneKind.HEAVY_RAIN
        71, 73, 77, 85 -> SceneKind.SNOW
        75, 86 -> SceneKind.BLIZZARD
        95 -> SceneKind.THUNDERSTORM
        96, 99 -> SceneKind.HAIL
        else -> if (c.cloudCover >= 80) SceneKind.OVERCAST else SceneKind.CLOUDY
    }
    val dry = base == SceneKind.CLEAR || base == SceneKind.PARTLY_CLOUDY || base == SceneKind.CLOUDY
    return when {
        dry && dust >= 120.0 -> SceneKind.DUST
        dry && c.windSpeed >= 35.0 -> SceneKind.WINDY
        base == SceneKind.CLEAR && c.isDay && c.temperature >= 38.0 -> SceneKind.HOT
        else -> base
    }
}

@DrawableRes
fun sceneRes(kind: SceneKind, night: Boolean): Int = when (kind) {
        SceneKind.CLEAR -> if (night) R.drawable.scene_clear_night else R.drawable.scene_clear_day
        SceneKind.PARTLY_CLOUDY -> if (night) R.drawable.scene_partly_cloudy_night else R.drawable.scene_partly_cloudy_day
        SceneKind.CLOUDY -> if (night) R.drawable.scene_cloudy_night else R.drawable.scene_cloudy_day
        SceneKind.OVERCAST -> if (night) R.drawable.scene_overcast_night else R.drawable.scene_overcast_day
        SceneKind.FOG -> if (night) R.drawable.scene_fog_night else R.drawable.scene_fog_day
        SceneKind.DRIZZLE -> if (night) R.drawable.scene_drizzle_night else R.drawable.scene_drizzle_day
        SceneKind.RAIN -> if (night) R.drawable.scene_rain_night else R.drawable.scene_rain_day
        SceneKind.HEAVY_RAIN -> if (night) R.drawable.scene_heavy_rain_night else R.drawable.scene_heavy_rain_day
        SceneKind.THUNDERSTORM -> if (night) R.drawable.scene_thunderstorm_night else R.drawable.scene_thunderstorm_day
        SceneKind.HAIL -> if (night) R.drawable.scene_hail_night else R.drawable.scene_hail_day
        SceneKind.SLEET -> if (night) R.drawable.scene_sleet_night else R.drawable.scene_sleet_day
        SceneKind.SNOW -> if (night) R.drawable.scene_snow_night else R.drawable.scene_snow_day
        SceneKind.BLIZZARD -> if (night) R.drawable.scene_blizzard_night else R.drawable.scene_blizzard_day
        SceneKind.WINDY -> if (night) R.drawable.scene_windy_night else R.drawable.scene_windy_day
        SceneKind.DUST -> if (night) R.drawable.scene_dust_night else R.drawable.scene_dust_day
        SceneKind.HOT -> if (night) R.drawable.scene_clear_night else R.drawable.scene_hot_day
}

/** Scene illustration + animated particles. Mirrored in RTL so the sun/moon stays opposite the text column. */
@Composable
fun SceneArt(
    scene: SceneKind,
    night: Boolean,
    animated: Boolean,
    thunder: Boolean,
    modifier: Modifier = Modifier,
) {
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Box(modifier.aspectRatio(1104f / 660f)) {
        Image(
            painter = painterResource(sceneRes(scene, night)),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { scaleX = if (rtl) -1f else 1f },
        )
        SceneParticles(scene, night, animated, thunder, rtl, Modifier.fillMaxSize())
    }
}

private data class Mote(val x: Float, val y: Float, val v: Float)

private data class ParticleSpec(
    val rain: Int = 0,
    val snow: Int = 0,
    val hail: Int = 0,
    val stars: Int = 0,
    val streaks: Int = 0,
    val dust: Int = 0,
    val flash: Boolean = false,
    val slant: Float = 0.2f,
) {
    val any: Boolean get() = rain + snow + hail + stars + streaks + dust > 0 || flash
}

private fun specFor(scene: SceneKind, night: Boolean, thunder: Boolean): ParticleSpec = when (scene) {
    SceneKind.CLEAR -> ParticleSpec(stars = if (night) 40 else 0)
    SceneKind.PARTLY_CLOUDY -> ParticleSpec(stars = if (night) 16 else 0)
    SceneKind.CLOUDY -> ParticleSpec(stars = if (night) 6 else 0)
    SceneKind.DRIZZLE -> ParticleSpec(rain = 45, slant = 0.12f)
    SceneKind.RAIN -> ParticleSpec(rain = 90, slant = 0.2f)
    SceneKind.HEAVY_RAIN -> ParticleSpec(rain = 170, slant = 0.3f)
    SceneKind.THUNDERSTORM -> ParticleSpec(rain = 130, slant = 0.28f, flash = true)
    SceneKind.HAIL -> ParticleSpec(hail = 38, flash = thunder)
    SceneKind.SLEET -> ParticleSpec(rain = 60, snow = 35, slant = 0.18f)
    SceneKind.SNOW -> ParticleSpec(snow = 70)
    SceneKind.BLIZZARD -> ParticleSpec(snow = 140, streaks = 26)
    SceneKind.WINDY -> ParticleSpec(streaks = 22, stars = if (night) 24 else 0)
    SceneKind.DUST -> ParticleSpec(dust = 70, streaks = 18)
    SceneKind.FOG, SceneKind.OVERCAST, SceneKind.HOT -> ParticleSpec()
}

@Composable
private fun SceneParticles(
    scene: SceneKind,
    night: Boolean,
    animated: Boolean,
    thunder: Boolean,
    rtl: Boolean,
    modifier: Modifier,
) {
    val spec = remember(scene, night, thunder) { specFor(scene, night, thunder) }
    if (!spec.any) return

    // The frame clock is only READ inside the draw lambda, so a new frame invalidates drawing
    // but never triggers recomposition of this composable.
    var frameMs by remember { mutableLongStateOf(0L) }
    LaunchedEffect(animated, spec) {
        if (animated) {
            while (true) {
                withFrameMillis { frameMs = it }
            }
        }
    }
    val motes = remember(scene, night) {
        val r = Random(scene.ordinal * 31 + if (night) 7 else 3)
        fun list(n: Int) = List(n) { Mote(r.nextFloat(), r.nextFloat(), 0.6f + r.nextFloat() * 0.8f) }
        MoteSet(list(spec.rain), list(spec.snow), list(spec.hail), list(spec.stars), list(spec.streaks), list(spec.dust))
    }
    val rainColor = if (night) Color(0xFF8FA6FF) else Color(0xFFC4D8FF)

    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val t = frameMs / 1000f
        val dir = if (rtl) 1f else -1f // the art leans its rain toward the sun's opposite side; mirror in RTL

        for (d in motes.rain) {
            val prog = (d.y + t * d.v * 0.9f) % 1f
            val y = prog * h
            val len = h * (0.045f + 0.035f * d.v)
            val x = (((d.x * w + dir * spec.slant * y) % w) + w) % w
            drawLine(
                color = rainColor.copy(alpha = 0.35f + 0.3f * d.v / 1.4f),
                start = Offset(x, y),
                end = Offset(x + dir * spec.slant * len, y + len),
                strokeWidth = (0.7f + d.v) * density,
                cap = StrokeCap.Round,
            )
        }
        for (d in motes.hail) {
            val prog = (d.y + t * d.v * 1.1f) % 1f
            val y = prog * h
            val x = (((d.x * w + dir * 0.25f * y) % w) + w) % w
            drawLine(
                Color(0xFFCFE0FF).copy(alpha = 0.25f),
                Offset(x, y), Offset(x - dir * 0.3f * 22f * density, y - 22f * density),
                strokeWidth = 2f * density, cap = StrokeCap.Round,
            )
            drawCircle(Color(0xFFF2F7FF), radius = (2.2f + d.v * 1.6f) * density, center = Offset(x, y))
        }
        for (d in motes.snow) {
            val prog = (d.y + t * d.v * 0.07f) % 1f
            val y = prog * h
            val x = d.x * w + sin(t * 0.9f + d.y * 20f) * 9f * density
            drawCircle(Color.White.copy(alpha = 0.55f + 0.35f * (d.v / 1.4f)), radius = (1.1f + d.v * 1.5f) * density, center = Offset(x, y))
        }
        for (s in motes.stars) {
            val twinkle = 0.3f + 0.7f * (0.5f + 0.5f * sin(t * (1f + s.v * 1.6f) + s.x * 40f))
            drawCircle(
                Color(0xFFE8E2FF).copy(alpha = 0.85f * twinkle),
                radius = (0.6f + s.v * 0.9f) * density,
                center = Offset(s.x * w, s.y * h * 0.58f),
            )
        }
        for (d in motes.streaks) {
            val travel = ((d.x + t * 0.09f * d.v) % 1.3f) - 0.15f
            val x = (if (rtl) 1f - travel else travel) * w
            val y = d.y * h * 0.85f
            val len = w * 0.07f * d.v * (if (rtl) -1f else 1f)
            drawLine(Color.White.copy(alpha = 0.22f), Offset(x, y), Offset(x + len, y + h * 0.004f), strokeWidth = 1.2f * density, cap = StrokeCap.Round)
        }
        for (d in motes.dust) {
            val x = ((d.x + t * 0.05f * d.v) % 1f) * w
            val y = (d.y + sin(t * 0.4f + d.x * 9f) * 0.015f) * h
            drawCircle(Color(0xFFF2CF98).copy(alpha = 0.55f), radius = (0.7f + d.v * 0.9f) * density, center = Offset(x, y))
        }
        if (spec.flash) {
            val cycle = t % 6f
            if (cycle < 0.12f || (cycle > 0.26f && cycle < 0.34f)) {
                drawRect(Color.White.copy(alpha = 0.16f))
            }
        }
    }
}

private class MoteSet(
    val rain: List<Mote>,
    val snow: List<Mote>,
    val hail: List<Mote>,
    val stars: List<Mote>,
    val streaks: List<Mote>,
    val dust: List<Mote>,
)
