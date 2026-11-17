package com.pishi.iweather.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pishi.iweather.ui.theme.Luna
import com.pishi.iweather.ui.theme.lunaGlass

/** 24x24 line-icon path data (Lucide-style). Parsed once per icon and drawn with a stroke. */
object LunaPaths {
    const val DROPLET = "M12 22a7 7 0 0 0 7-7c0-2-1-3.9-3-5.5s-3.5-4-4-6.5c-.5 2.5-2 4.9-4 6.5C6 11.1 5 13 5 15a7 7 0 0 0 7 7z"
    const val WIND = "M17.7 7.7a2.5 2.5 0 1 1 1.8 4.3H2M9.6 4.6A2 2 0 1 1 11 8H2M12.6 19.4A2 2 0 1 0 14 16H2"
    const val GAUGE = "M12 14l4-4M3.34 19a10 10 0 1 1 17.32 0"
    const val EYE = "M2.062 12.348a1 1 0 0 1 0-.696 10.75 10.75 0 0 1 19.876 0 1 1 0 0 1 0 .696 10.75 10.75 0 0 1-19.876 0M15 12a3 3 0 1 1-6 0 3 3 0 0 1 6 0"
    const val BELL = "M10.268 21a2 2 0 0 0 3.464 0M3.262 15.326A1 1 0 0 0 4 17h16a1 1 0 0 0 .74-1.673C19.41 13.956 18 12.499 18 8A6 6 0 0 0 6 8c0 4.499-1.411 5.956-2.738 7.326"
    const val SHIRT = "M20.38 3.46L16 2a4 4 0 0 1-8 0L3.62 3.46a2 2 0 0 0-1.34 2.23l.58 3.47a1 1 0 0 0 .99.84H6v10c0 1.1.9 2 2 2h8a2 2 0 0 0 2-2V10h2.15a1 1 0 0 0 .99-.84l.58-3.47a2 2 0 0 0-1.34-2.23z"
    const val SPARKLES = "M12 3l1.9 5.8a2 2 0 0 0 1.3 1.3L21 12l-5.8 1.9a2 2 0 0 0-1.3 1.3L12 21l-1.9-5.8a2 2 0 0 0-1.3-1.3L3 12l5.8-1.9a2 2 0 0 0 1.3-1.3zM5 3v4M3 5h4M19 17v4M17 19h4"
    const val INSIGHTS = "M3 18.5L9 12.5l4 4 7-7.5M15.5 9H20v4.5"
    const val CHEVRON = "M9 18l6-6-6-6"
    const val EXPAND = "M15 3h6v6M21 3l-7 7M3 21l7-7M9 21H3v-6"
    const val CROWN = "M11.562 3.266a.5.5 0 0 1 .876 0L15.39 8.87a1 1 0 0 0 1.516.294L21.183 5.5a.5.5 0 0 1 .798.519l-2.834 10.246a1 1 0 0 1-.956.734H5.81a1 1 0 0 1-.957-.734L2.02 6.02a.5.5 0 0 1 .798-.519l4.276 3.664a1 1 0 0 0 1.516-.294z"
    const val PIN = "M20 10c0 6-8 12-8 12s-8-6-8-12a8 8 0 0 1 16 0zM15 10a3 3 0 1 1-6 0 3 3 0 0 1 6 0"
    const val STAR = "M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"
    const val UMBRELLA = "M22 12a10.06 10.06 1 0 0-20 0zM12 12v8a2 2 0 0 0 4 0M12 2v1"
}

/** Stroke icon from 24x24 path data. [fill] optionally paints the interior (e.g. heart/crown). */
@Composable
fun LIcon(
    path: String,
    tint: Color,
    modifier: Modifier = Modifier,
    iconSize: Dp = 24.dp,
    strokeWidth: Float = 1.7f,
    fill: Color? = null,
) {
    val parsed = remember(path) { PathParser().parsePathString(path).toPath() }
    Canvas(modifier.size(iconSize)) {
        val k = this.size.minDimension / 24f
        withTransform({ scale(k, k, Offset.Zero) }) {
            if (fill != null) drawPath(parsed, fill)
            drawPath(
                parsed, tint,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }
    }
}

/** Rounded dark-indigo tile that holds an icon (feature cards, wave card header). */
@Composable
fun LunaIconBox(modifier: Modifier = Modifier, boxSize: Dp = 46.dp, content: @Composable BoxScope.() -> Unit) {
    val shape = RoundedCornerShape(13.dp)
    Box(
        modifier = modifier
            .size(boxSize)
            .background(Brush.linearGradient(listOf(Luna.IconBoxA, Luna.IconBoxB)), shape)
            .border(1.dp, Luna.StrokeC.copy(alpha = 0.55f), shape),
        contentAlignment = Alignment.Center,
        content = content,
    )
}

/** Gradient score ring (lavender -> cyan). [progress] 0..1 animates when [animated]. */
@Composable
fun LunaRing(
    progress: Float,
    animated: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val p by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(if (animated) 1000 else 0),
        label = "lunaRing",
    )
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 9.dp.toPx()
            val inset = stroke / 2f
            val arc = Size(this.size.width - stroke, this.size.height - stroke)
            drawArc(
                color = Color.White.copy(alpha = 0.08f),
                startAngle = 0f, sweepAngle = 360f, useCenter = false,
                topLeft = Offset(inset, inset), size = arc,
                style = Stroke(width = stroke),
            )
            rotate(degrees = -90f) {
                drawArc(
                    brush = Brush.sweepGradient(listOf(Luna.RingA, Luna.RingB, Luna.RingC, Luna.RingD, Luna.RingA)),
                    startAngle = 0f, sweepAngle = 360f * p, useCenter = false,
                    topLeft = Offset(inset, inset), size = arc,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            }
        }
        content()
    }
}

/** Four-point sparkle with a soft glow — the star in the score card. */
@Composable
fun LunaStar(modifier: Modifier = Modifier, starSize: Dp = 34.dp) {
    Canvas(modifier.size(starSize)) {
        val c = center
        val r = this.size.minDimension / 2f
        val p = Path().apply {
            moveTo(c.x, c.y - r)
            cubicTo(c.x + r * 0.10f, c.y - r * 0.32f, c.x + r * 0.32f, c.y - r * 0.10f, c.x + r, c.y)
            cubicTo(c.x + r * 0.32f, c.y + r * 0.10f, c.x + r * 0.10f, c.y + r * 0.32f, c.x, c.y + r)
            cubicTo(c.x - r * 0.10f, c.y + r * 0.32f, c.x - r * 0.32f, c.y + r * 0.10f, c.x - r, c.y)
            cubicTo(c.x - r * 0.32f, c.y - r * 0.10f, c.x - r * 0.10f, c.y - r * 0.32f, c.x, c.y - r)
            close()
        }
        drawCircle(Luna.Blue.copy(alpha = 0.22f), radius = r * 0.85f, center = c)
        drawPath(
            p,
            Brush.linearGradient(
                listOf(Color(0xFF8FE0FF), Color(0xFF5B6DFF)),
                start = Offset(c.x - r, c.y - r), end = Offset(c.x + r, c.y + r),
            ),
        )
    }
}

/** Round glass header button (location / alerts). */
@Composable
fun LunaRoundButton(
    path: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badge: Boolean = false,
) {
    Box(
        modifier = modifier
            .size(44.dp)
            .background(Color(0xFF1A1745).copy(alpha = 0.85f), CircleShape)
            .border(1.3.dp, Luna.StrokeC, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        LIcon(path, Color(0xFFD8D0FF), iconSize = 22.dp)
        if (badge) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 9.dp, end = 10.dp)
                    .size(8.dp)
                    .background(Luna.Pink, CircleShape),
            )
        }
    }
}

/** Gradient pill call-to-action ("Start Sleep" in the original design). */
@Composable
fun LunaPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingPath: String? = null,
) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier = modifier
            .background(Brush.horizontalGradient(listOf(Luna.VioletLight, Luna.Violet)), shape)
            .border(1.4.dp, Color(0xFFE4D6FF).copy(alpha = 0.75f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (leadingPath != null) LIcon(leadingPath, Color.White, iconSize = 20.dp, strokeWidth = 1.9f)
        Text(text, color = Color.White, fontSize = 15.5.sp, fontWeight = FontWeight.Medium)
        LIcon(LunaPaths.CHEVRON, Color.White, iconSize = 18.dp, strokeWidth = 2.1f)
    }
}

/** Floating glass pill that hosts [LunaNavItem]s. */
@Composable
fun LunaNav(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .lunaGlass(radius = 34.dp)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

@Composable
fun RowScope.LunaNavItem(icon: ImageVector, label: String, active: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .weight(1f)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .then(
                    if (active) {
                        Modifier
                            .background(
                                Brush.radialGradient(listOf(Color(0xFF9A7DFF), Color(0xFF5B3FD8))),
                                CircleShape,
                            )
                            .border(1.3.dp, Color(0xFFCDBDFF).copy(alpha = 0.85f), CircleShape)
                    } else {
                        Modifier
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = label, tint = if (active) Color.White else Luna.NavIdle, modifier = Modifier.size(23.dp))
        }
        Text(
            label,
            color = if (active) Color.White else Luna.NavIdle,
            fontSize = 11.sp,
            fontWeight = if (active) FontWeight.Medium else FontWeight.Normal,
            maxLines = 1,
        )
    }
}
