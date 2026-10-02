package id.biojelan.app.ui.components

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import id.biojelan.app.ui.theme.BioTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val OilLight = Color(0xFFEBC265)

private fun waveFill(w: Float, h: Float, baseY: Float, amp: Float, length: Float, phase: Float): Path = Path().apply {
    moveTo(0f, h)
    lineTo(0f, baseY)
    var x = 0f
    while (x <= w) {
        lineTo(x, baseY + amp * sin((x / length) * 2f * PI.toFloat() + phase))
        x += 6f
    }
    lineTo(w, h)
    close()
}

/**
 * Lingkaran cahaya lembut + cincin tipis + tetes kecil di belakang ilustrasi stok, supaya jerigen
 * tidak "melayang" di atas permukaan polos.
 */
@Composable
fun StockHalo(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val c = BioTheme.colors
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            val r = size.minDimension / 2f
            val center = Offset(size.width / 2f, size.height / 2f)
            drawCircle(
                brush = Brush.radialGradient(listOf(c.surface, c.surface.copy(alpha = 0f)), center = center, radius = r),
                radius = r,
                center = center,
            )
            drawCircle(c.primary.copy(alpha = 0.10f), radius = r - 1.dp.toPx(), center = center, style = Stroke(1.dp.toPx()))
            drawCircle(c.primary.copy(alpha = 0.07f), radius = r * 0.78f, center = center, style = Stroke(1.dp.toPx()))
            // tetes dekoratif di sekeliling cincin
            listOf(-0.9f to 3.5f, 0.35f to 2.5f, 2.3f to 3f, 3.6f to 2f).forEach { (angle, dot) ->
                val p = Offset(center.x + (r - 2.dp.toPx()) * cos(angle), center.y + (r - 2.dp.toPx()) * sin(angle))
                drawCircle(c.amber.copy(alpha = 0.55f), radius = dot.dp.toPx(), center = p)
            }
        }
        content()
    }
}

/**
 * Kartu stok Agen di Beranda: jerigen di dalam halo, latar gelombang minyak, dan lintasan progres
 * menuju ambang jemput (dengan penanda 25/50/75% dan target di ujung).
 */
@Composable
fun StockHeroCard(
    stock: Double,
    threshold: Double,
    eyebrow: String,
    valueText: String,
    thresholdText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    chip: @Composable () -> Unit,
) {
    val c = BioTheme.colors
    val ratio = if (threshold > 0) (stock / threshold).toFloat().coerceIn(0f, 1f) else 0f
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { started = true }
    val progress by animateFloatAsState(if (started) ratio else 0f, tween(900), label = "stockProgress")
    val shape = RoundedCornerShape(22.dp)

    Box(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.linearGradient(listOf(c.surface, c.primaryTint)), shape)
            .border(1.dp, c.line, shape)
            .clickable(onClick = onClick),
    ) {
        Canvas(Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height
            // busur kontur di pojok kanan-atas
            listOf(60f, 100f, 140f).forEach { r ->
                drawCircle(
                    c.primary.copy(alpha = 0.06f),
                    radius = r.dp.toPx(),
                    center = Offset(w * 0.96f, -h * 0.05f),
                    style = Stroke(1.5.dp.toPx()),
                )
            }
            // dua lapis gelombang minyak di dasar kartu
            drawPath(waveFill(w, h, h * 0.80f, 5.dp.toPx(), 120.dp.toPx(), 0.6f), c.amber.copy(alpha = 0.10f))
            drawPath(waveFill(w, h, h * 0.88f, 4.dp.toPx(), 90.dp.toPx(), 2.4f), c.amber.copy(alpha = 0.16f))
            // tetes kecil
            drawCircle(c.amber.copy(alpha = 0.35f), 3.dp.toPx(), Offset(w * 0.62f, h * 0.22f))
            drawCircle(c.amber.copy(alpha = 0.25f), 2.dp.toPx(), Offset(w * 0.70f, h * 0.34f))
            drawCircle(c.primary.copy(alpha = 0.18f), 2.5.dp.toPx(), Offset(w * 0.55f, h * 0.12f))
        }

        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StockHalo(Modifier.size(96.dp)) {
                    JerrycanGauge(fill = ratio, modifier = Modifier.size(width = 52.dp, height = 64.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(eyebrow, style = BioTheme.type.eyebrow, color = c.muted)
                    Text(valueText, style = BioTheme.type.display, color = c.ink)
                    Spacer(Modifier.height(6.dp))
                    chip()
                }
            }
            Spacer(Modifier.height(12.dp))
            ThresholdTrack(progress)
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth()) {
                Text("0", style = BioTheme.type.caption, color = c.muted)
                Spacer(Modifier.weight(1f))
                Text(thresholdText, style = BioTheme.type.caption, color = c.muted)
            }
        }
    }
}

@Composable
private fun ThresholdTrack(progress: Float) {
    val c = BioTheme.colors
    Canvas(Modifier.fillMaxWidth().height(14.dp)) {
        val h = size.height
        val r = h / 2f
        drawRoundRect(c.line, size = Size(size.width, h), cornerRadius = CornerRadius(r, r))
        val fillW = if (progress > 0f) (size.width * progress).coerceAtLeast(h) else 0f
        if (fillW > 0f) {
            drawRoundRect(
                brush = Brush.horizontalGradient(listOf(OilLight, c.amber, c.amberDeep), startX = 0f, endX = fillW),
                size = Size(fillW, h),
                cornerRadius = CornerRadius(r, r),
            )
        }
        listOf(0.25f, 0.5f, 0.75f).forEach { t ->
            val x = size.width * t
            drawLine(c.surface.copy(alpha = 0.85f), Offset(x, 3.dp.toPx()), Offset(x, h - 3.dp.toPx()), strokeWidth = 1.5.dp.toPx())
        }
        // target di ujung kanan
        val tc = Offset(size.width - r, r)
        drawCircle(c.surface, radius = r, center = tc)
        drawCircle(c.primary, radius = r - 1.dp.toPx(), center = tc, style = Stroke(2.dp.toPx()))
        drawCircle(c.primary, radius = 2.dp.toPx(), center = tc)
    }
}