package com.pishi.iweather.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pishi.iweather.data.SkyKind

/**
 * iWeather "Luna" palettes — one hue family, two moods.
 * Night: deep indigo, lavender glass, cyan/teal accents (the original look).
 * Day: airy sky-lavender glass with deep indigo ink.
 * Every colour used by the screens is read through [Luna], whose getters delegate to the
 * palette [IWeatherTheme] activates, so a theme switch re-skins the whole UI in one place.
 * [Luna.Content] is the foreground for text/icons drawn on glass surfaces: white at night,
 * deep indigo by day — never hardcode Color.White on a card.
 */
data class LunaPalette(
    val Bg: Color, val BgDeep: Color,
    val CardTop: Color, val CardBottom: Color,
    val StrokeA: Color, val StrokeB: Color, val StrokeC: Color,
    val StrokeCyanA: Color, val StrokeCyanB: Color, val StrokeCyanC: Color,
    val Text: Color, val TextSoft: Color, val TextMuted: Color, val TextDim: Color,
    val NavIdle: Color, val Content: Color,
    val Lavender: Color, val Violet: Color, val VioletLight: Color,
    val Teal: Color, val Pink: Color, val Lilac: Color, val Blue: Color,
    val Cyan: Color, val Gold: Color,
    val RingA: Color, val RingB: Color, val RingC: Color, val RingD: Color,
    val IconBoxA: Color, val IconBoxB: Color,
)

val NightPalette = LunaPalette(
    Bg = Color(0xFF090A22), BgDeep = Color(0xFF05060F),
    CardTop = Color(0xFF262063).copy(alpha = 0.72f), CardBottom = Color(0xFF0F0D30).copy(alpha = 0.86f),
    StrokeA = Color(0xFFCDBCFF).copy(alpha = 0.72f), StrokeB = Color(0xFF6F5CD8).copy(alpha = 0.30f), StrokeC = Color(0xFFB3A4FF).copy(alpha = 0.58f),
    StrokeCyanA = Color(0xFF8FE3FF).copy(alpha = 0.78f), StrokeCyanB = Color(0xFF4F8FE8).copy(alpha = 0.36f), StrokeCyanC = Color(0xFF7FD6FF).copy(alpha = 0.68f),
    Text = Color(0xFFF0EDFF), TextSoft = Color(0xFFD9D3F5), TextMuted = Color(0xFF9B95CD), TextDim = Color(0xFFA9A3D6),
    NavIdle = Color(0xFFB4ADDF), Content = Color.White,
    Lavender = Color(0xFFB9A9F5), Violet = Color(0xFF7D62F2), VioletLight = Color(0xFFB992F6),
    Teal = Color(0xFF2FE0C8), Pink = Color(0xFFF472D0), Lilac = Color(0xFFB39DFF), Blue = Color(0xFF4DA3FF),
    Cyan = Color(0xFF22E6D9), Gold = Color(0xFFFFD76A),
    RingA = Color(0xFFDBBFFF), RingB = Color(0xFFA98CFF), RingC = Color(0xFF6FA6FF), RingD = Color(0xFF4FD0FF),
    IconBoxA = Color(0xFF3A2FA0).copy(alpha = 0.90f), IconBoxB = Color(0xFF1C1659).copy(alpha = 0.95f),
)

val DayPalette = LunaPalette(
    Bg = Color(0xFFDCE9F8), BgDeep = Color(0xFFC7DCF3),
    CardTop = Color(0xFFFFFFFF).copy(alpha = 0.78f), CardBottom = Color(0xFFE9F1FD).copy(alpha = 0.90f),
    StrokeA = Color(0xFF7D62F2).copy(alpha = 0.50f), StrokeB = Color(0xFF7D62F2).copy(alpha = 0.26f), StrokeC = Color(0xFF9B7EDE).copy(alpha = 0.45f),
    StrokeCyanA = Color(0xFF1D7FB8).copy(alpha = 0.55f), StrokeCyanB = Color(0xFF1D7FB8).copy(alpha = 0.28f), StrokeCyanC = Color(0xFF1D7FB8).copy(alpha = 0.45f),
    Text = Color(0xFF191938), TextSoft = Color(0xFF2A2A52), TextMuted = Color(0xFF5B5E8C), TextDim = Color(0xFF4C4F78),
    NavIdle = Color(0xFF4C4F78), Content = Color(0xFF191938),
    Lavender = Color(0xFF6A4FD0), Violet = Color(0xFF7D62F2), VioletLight = Color(0xFF8A63E8),
    Teal = Color(0xFF0DA58F), Pink = Color(0xFFD63FA8), Lilac = Color(0xFF7A5CFF), Blue = Color(0xFF2B7CD3),
    Cyan = Color(0xFF0FA3A3), Gold = Color(0xFFD9930D),
    RingA = Color(0xFFB07FE8), RingB = Color(0xFF8A63E8), RingC = Color(0xFF4A86E8), RingD = Color(0xFF1FA8D8),
    IconBoxA = Color(0xFF7D62F2).copy(alpha = 0.26f), IconBoxB = Color(0xFF4A3FB8).copy(alpha = 0.16f),
)

/** Active-palette facade: existing `Luna.X` call sites keep working; values follow the theme. */
object Luna {
    internal var palette: LunaPalette = NightPalette
    val Bg: Color get() = palette.Bg
    val BgDeep: Color get() = palette.BgDeep
    val CardTop: Color get() = palette.CardTop
    val CardBottom: Color get() = palette.CardBottom
    val StrokeA: Color get() = palette.StrokeA
    val StrokeB: Color get() = palette.StrokeB
    val StrokeC: Color get() = palette.StrokeC
    val StrokeCyanA: Color get() = palette.StrokeCyanA
    val StrokeCyanB: Color get() = palette.StrokeCyanB
    val StrokeCyanC: Color get() = palette.StrokeCyanC
    val Text: Color get() = palette.Text
    val TextSoft: Color get() = palette.TextSoft
    val TextMuted: Color get() = palette.TextMuted
    val TextDim: Color get() = palette.TextDim
    val NavIdle: Color get() = palette.NavIdle
    val Content: Color get() = palette.Content
    val Lavender: Color get() = palette.Lavender
    val Violet: Color get() = palette.Violet
    val VioletLight: Color get() = palette.VioletLight
    val Teal: Color get() = palette.Teal
    val Pink: Color get() = palette.Pink
    val Lilac: Color get() = palette.Lilac
    val Blue: Color get() = palette.Blue
    val Cyan: Color get() = palette.Cyan
    val Gold: Color get() = palette.Gold
    val RingA: Color get() = palette.RingA
    val RingB: Color get() = palette.RingB
    val RingC: Color get() = palette.RingC
    val RingD: Color get() = palette.RingD
    val IconBoxA: Color get() = palette.IconBoxA
    val IconBoxB: Color get() = palette.IconBoxB
}

/** True when the effective UI mood is the dark (night) palette. */
fun themeIsDark(mode: com.pishi.iweather.util.ThemeMode, isDay: Boolean): Boolean = when (mode) {
    com.pishi.iweather.util.ThemeMode.DAY -> false
    com.pishi.iweather.util.ThemeMode.NIGHT -> true
    com.pishi.iweather.util.ThemeMode.AUTO -> !isDay
}

private val DarkScheme = darkColorScheme(
    primary = NightPalette.VioletLight,
    onPrimary = Color(0xFF1A1050),
    secondary = NightPalette.Cyan,
    background = NightPalette.Bg,
    surface = Color(0xFF14123A),
    surfaceVariant = Color(0xFF1C1858),
    secondaryContainer = Color(0xFF3A2FA0),
    onSecondaryContainer = Color.White,
    primaryContainer = Color(0xFF3A2FA0),
    onPrimaryContainer = Color.White,
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = NightPalette.TextDim,
    outline = Color(0x99B3A4FF),
    outlineVariant = Color(0x55B3A4FF),
)

private val LightScheme = androidx.compose.material3.lightColorScheme(
    primary = DayPalette.Violet,
    onPrimary = Color.White,
    secondary = DayPalette.Cyan,
    background = DayPalette.Bg,
    surface = Color(0xFFF4F8FE),
    surfaceVariant = Color(0xFFE2ECFA),
    secondaryContainer = Color(0xFFD9CFFB),
    onSecondaryContainer = Color(0xFF2A1B6B),
    primaryContainer = Color(0xFFD9CFFB),
    onPrimaryContainer = Color(0xFF2A1B6B),
    onBackground = DayPalette.Text,
    onSurface = DayPalette.Text,
    onSurfaceVariant = DayPalette.TextDim,
    outline = Color(0x8A7D62F2),
    outlineVariant = Color(0x4D7D62F2),
)

@Composable
fun IWeatherTheme(
    mode: com.pishi.iweather.util.ThemeMode = com.pishi.iweather.util.ThemeMode.AUTO,
    isDay: Boolean = false,
    content: @Composable () -> Unit,
) {
    val dark = themeIsDark(mode, isDay)
    // Set before MaterialTheme composes its children: every Luna.* read below follows.
    Luna.palette = if (dark) NightPalette else DayPalette
    MaterialTheme(colorScheme = if (dark) DarkScheme else LightScheme, content = content)
}

/** Glass surface: translucent indigo gradient + luminous lavender border (cyan variant for emphasis). */
fun Modifier.lunaGlass(radius: Dp = 26.dp, cyan: Boolean = false): Modifier {
    val shape = RoundedCornerShape(radius)
    val border = if (cyan) {
        Brush.linearGradient(listOf(Luna.StrokeCyanA, Luna.StrokeCyanB, Luna.StrokeCyanC))
    } else {
        Brush.linearGradient(listOf(Luna.StrokeA, Luna.StrokeB, Luna.StrokeC))
    }
    return this
        .background(Brush.linearGradient(listOf(Luna.CardTop, Luna.CardBottom)), shape)
        .border(0.9.dp, border, shape)
}

// ---- Legacy sky-kind helpers (still used for widget colours / insight logic) ----

fun skyGradient(kind: SkyKind): Brush = Brush.verticalGradient(skyColors(kind))

fun skyColors(kind: SkyKind): List<Color> = when (kind) {
    SkyKind.CLEAR_DAY -> listOf(Color(0xFF0B2672), Color(0xFF2C63D2), Color(0xFF86B8F6))
    SkyKind.CLEAR_NIGHT -> listOf(Color(0xFF04050D), Color(0xFF0B0C2E), Color(0xFF2A1D70))
    SkyKind.CLOUDY -> listOf(Color(0xFF1B2F72), Color(0xFF47629F), Color(0xFF8B9CC9))
    SkyKind.RAIN -> listOf(Color(0xFF1F2744), Color(0xFF3E4A76), Color(0xFF6F7CA6))
    SkyKind.SNOW -> listOf(Color(0xFF2B4A8C), Color(0xFF7396CA), Color(0xFFC6D7F0))
    SkyKind.FOG -> listOf(Color(0xFF3A4A78), Color(0xFF8693BB), Color(0xFFC8CFE3))
    SkyKind.STORM -> listOf(Color(0xFF0D1124), Color(0xFF222949), Color(0xFF444D7A))
}
