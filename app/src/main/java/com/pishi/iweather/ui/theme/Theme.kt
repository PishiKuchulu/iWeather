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
 * iWeather "Luna" palette — deep indigo night, lavender glass, cyan/teal accents.
 * Every colour used by the screens lives here so the look can be tuned in one place.
 */
object Luna {
    val Bg = Color(0xFF090A22)
    val BgDeep = Color(0xFF05060F)

    val CardTop = Color(0xFF262063).copy(alpha = 0.72f)
    val CardBottom = Color(0xFF0F0D30).copy(alpha = 0.86f)
    val StrokeA = Color(0xFFCDBCFF).copy(alpha = 0.80f)
    val StrokeB = Color(0xFF6F5CD8).copy(alpha = 0.32f)
    val StrokeC = Color(0xFFB3A4FF).copy(alpha = 0.65f)
    val StrokeCyanA = Color(0xFF8FE3FF).copy(alpha = 0.85f)
    val StrokeCyanB = Color(0xFF4F8FE8).copy(alpha = 0.40f)
    val StrokeCyanC = Color(0xFF7FD6FF).copy(alpha = 0.75f)

    val Text = Color(0xFFF0EDFF)
    val TextSoft = Color(0xFFD9D3F5)
    val TextMuted = Color(0xFF9B95CD)
    val TextDim = Color(0xFFA9A3D6)
    val NavIdle = Color(0xFFB4ADDF)

    val Lavender = Color(0xFFB9A9F5)
    val Violet = Color(0xFF7D62F2)
    val VioletLight = Color(0xFFB992F6)
    val Teal = Color(0xFF2FE0C8)
    val Pink = Color(0xFFF472D0)
    val Lilac = Color(0xFFB39DFF)
    val Blue = Color(0xFF4DA3FF)
    val Cyan = Color(0xFF22E6D9)
    val Gold = Color(0xFFFFD76A)

    val RingA = Color(0xFFDBBFFF)
    val RingB = Color(0xFFA98CFF)
    val RingC = Color(0xFF6FA6FF)
    val RingD = Color(0xFF4FD0FF)

    val IconBoxA = Color(0xFF3A2FA0).copy(alpha = 0.90f)
    val IconBoxB = Color(0xFF1C1659).copy(alpha = 0.95f)
}

private val Scheme = darkColorScheme(
    primary = Luna.VioletLight,
    onPrimary = Color(0xFF1A1050),
    secondary = Luna.Cyan,
    background = Luna.Bg,
    surface = Color(0xFF14123A),
    surfaceVariant = Color(0xFF1C1858),
    secondaryContainer = Color(0xFF3A2FA0),
    onSecondaryContainer = Color.White,
    primaryContainer = Color(0xFF3A2FA0),
    onPrimaryContainer = Color.White,
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = Luna.TextDim,
    outline = Color(0x99B3A4FF),
    outlineVariant = Color(0x55B3A4FF),
)

@Composable
fun IWeatherTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, content = content)
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
        .border(1.2.dp, border, shape)
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
