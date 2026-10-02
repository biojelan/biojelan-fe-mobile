package id.biojelan.app.ui.driver

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import id.biojelan.app.ui.icons.BioIcons
import id.biojelan.app.ui.theme.BioTheme

private const val PIN_PATH = "M12 2C8 2 5 5 5 9c0 5.2 7 13 7 13s7-7.8 7-13c0-4-3-7-7-7z"

// Blok kota dekoratif (x, y, lebar, tinggi) dalam pecahan ukuran kanvas.
private val Blocks = listOf(
    floatArrayOf(0.04f, 0.08f, 0.17f, 0.22f),
    floatArrayOf(0.27f, 0.05f, 0.12f, 0.14f),
    floatArrayOf(0.56f, 0.08f, 0.13f, 0.18f),
    floatArrayOf(0.80f, 0.58f, 0.16f, 0.24f),
    floatArrayOf(0.06f, 0.78f, 0.13f, 0.16f),
    floatArrayOf(0.62f, 0.72f, 0.10f, 0.20f),
)

/**
 * Adegan "menunggu penugasan" untuk Beranda Driver: peta stilasi dengan rute putus-putus yang berjalan,
 * truk yang menyusuri rute dari titik Driver (radar) ke tujuan (pin), tanpa paragraf teks.
 */
@Composable
internal fun DriverIdleScene(liveText: String, modifier: Modifier = Modifier) {
    val c = BioTheme.colors
    val truck = rememberVectorPainter(BioIcons.Truck)
    val pin = remember { PathParser().parsePathString(PIN_PATH).toPath() }
    val route = remember { Path() }
    val measure = remember { PathMeasure() }

    val transition = rememberInfiniteTransition(label = "idleScene")
    val travel by transition.animateFloat(
        0f, 1f, infiniteRepeatable(tween(7000, easing = LinearEasing), RepeatMode.Restart), label = "travel",
    )
    val dash by transition.animateFloat(
        0f, 1f, infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Restart), label = "dash",
    )
    val ripple by transition.animateFloat(
        0f, 1f, infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Restart), label = "ripple",
    )
    val blink by transition.animateFloat(
        1f, 0.25f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "blink",
    )

    val shape = RoundedCornerShape(20.dp)
    Box(modifier.fillMaxWidth().height(200.dp).clip(shape).background(c.mapBase, shape).border(1.dp, c.line, shape)) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val dp = 1.dp.toPx()

            // grid + blok kota
            val step = 26 * dp
            var gx = step
            while (gx < w) { drawLine(c.mapGrid, Offset(gx, 0f), Offset(gx, h), strokeWidth = dp); gx += step }
            var gy = step
            while (gy < h) { drawLine(c.mapGrid, Offset(0f, gy), Offset(w, gy), strokeWidth = dp); gy += step }
            Blocks.forEach { b ->
                drawRoundRect(
                    c.mapGrid,
                    topLeft = Offset(w * b[0], h * b[1]),
                    size = Size(w * b[2], h * b[3]),
                    cornerRadius = CornerRadius(6 * dp, 6 * dp),
                )
            }

            // sungai
            val river = Path().apply {
                moveTo(0f, h * 0.84f)
                cubicTo(w * 0.30f, h * 0.55f, w * 0.55f, h * 1.02f, w, h * 0.62f)
            }
            drawPath(river, Color(0xFFCFE0EA), style = Stroke(width = 12 * dp, cap = StrokeCap.Round))

            // rute
            val start = Offset(w * 0.14f, h * 0.70f)
            val end = Offset(w * 0.84f, h * 0.32f)
            route.reset()
            route.moveTo(start.x, start.y)
            route.cubicTo(w * 0.32f, h * 0.98f, w * 0.52f, h * 0.06f, end.x, end.y)
            drawPath(route, c.line, style = Stroke(width = 14 * dp, cap = StrokeCap.Round, join = StrokeJoin.Round))
            drawPath(route, c.surface, style = Stroke(width = 10 * dp, cap = StrokeCap.Round, join = StrokeJoin.Round))
            drawPath(
                route, c.primary,
                style = Stroke(
                    width = 2.5f * dp,
                    cap = StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(9 * dp, 9 * dp), phase = -dash * 18 * dp),
                ),
            )

            // radar di titik Driver
            for (i in 0..2) {
                val p = (ripple + i / 3f) % 1f
                val radius = (8 + 46 * p) * dp
                drawCircle(c.primary.copy(alpha = 0.10f * (1f - p)), radius, start)
                drawCircle(c.primary.copy(alpha = 0.40f * (1f - p)), radius, start, style = Stroke(2 * dp))
            }
            drawCircle(c.surface, 9 * dp, start)
            drawCircle(c.primary, 6 * dp, start)

            // tujuan: riak + pin
            drawCircle(c.amber.copy(alpha = 0.55f * (1f - ripple)), (5 + 24 * ripple) * dp, end, style = Stroke(2 * dp))
            val pinPx = 30 * dp
            val ps = pinPx / 24f
            translate(left = end.x - pinPx / 2f, top = end.y - pinPx) {
                scale(ps, ps, pivot = Offset.Zero) {
                    drawPath(pin, c.rust)
                    drawCircle(Color.White, radius = 3f, center = Offset(12f, 9f))
                }
            }

            // truk menyusuri rute
            measure.setPath(route, false)
            val pos = measure.getPosition(measure.length * travel)
            val fade = minOf(1f, travel / 0.08f, (1f - travel) / 0.08f).coerceIn(0f, 1f)
            drawCircle(c.primary.copy(alpha = 0.16f * fade), 24 * dp, pos)
            drawCircle(c.surface.copy(alpha = fade), 17 * dp, pos)
            drawCircle(c.primary.copy(alpha = fade), 14 * dp, pos)
            translate(left = pos.x - 9 * dp, top = pos.y - 9 * dp) {
                with(truck) { draw(Size(18 * dp, 18 * dp), alpha = fade, colorFilter = ColorFilter.tint(c.onPrimary)) }
            }
        }

        ScenePill(Modifier.align(Alignment.TopStart).padding(12.dp)) {
            Box(Modifier.size(7.dp).background(c.primary.copy(alpha = blink), RoundedCornerShape(50)))
            Text(liveText, style = BioTheme.type.chip, color = c.primary)
        }
    }
}

@Composable
private fun ScenePill(modifier: Modifier, content: @Composable () -> Unit) {
    val c = BioTheme.colors
    val pill = RoundedCornerShape(50)
    Row(
        modifier.background(c.surface.copy(alpha = 0.94f), pill).border(1.dp, c.line, pill).padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) { content() }
}
