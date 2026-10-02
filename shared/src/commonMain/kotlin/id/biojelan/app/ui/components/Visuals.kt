package id.biojelan.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.biojelan.app.core.formatRupiah
import id.biojelan.app.ui.icons.BioIcons
import id.biojelan.app.ui.strings.BioText
import id.biojelan.app.ui.theme.BioTheme

// ------------------------------------------------------------------ tetesan minyak

private const val DROP_PATH = "M50 4 C24 40 10 64 10 82 C10 105 28 120 50 120 C72 120 90 105 90 82 C90 64 76 40 50 4 Z"

/**
 * Tetesan minyak. [fill] 0..1 = tinggi isi (untuk gauge stok). [outline] menggambar garis tepi.
 * Viewbox 100×124, jadi beri modifier dengan rasio kira-kira 0,8.
 */
@Composable
fun DropGauge(
    fill: Float,
    modifier: Modifier = Modifier,
    outline: Boolean = true,
) {
    val c = BioTheme.colors
    val path = remember { PathParser().parsePathString(DROP_PATH).toPath() }
    val level = fill.coerceIn(0f, 1f)
    Canvas(modifier) {
        scale(size.width / 100f, size.height / 124f, pivot = Offset.Zero) {
            if (outline) {
                drawPath(path, color = c.primaryTint)
            }
            if (level > 0f) {
                val top = 120f - 116f * level
                clipRect(left = 0f, top = top, right = 100f, bottom = 124f) {
                    drawPath(
                        path,
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFFE9BA55), c.amberDeep),
                            startY = 4f,
                            endY = 120f,
                        ),
                    )
                }
            }
            if (outline) {
                drawPath(path, color = c.primary.copy(alpha = 0.35f), style = Stroke(width = 2.5f))
            }
        }
    }
}

// ------------------------------------------------------------------ jerigen jelantah

private const val JERRYCAN_BODY = "M14 38 Q14 30 22 30 H58 L86 44 V108 Q86 118 76 118 H24 Q14 118 14 108 Z"
private const val JERRYCAN_HANDLE = "M24 30 V20 Q24 14 30 14 H44 Q50 14 50 20 V30"
private const val WAVE_PERIOD = 66f

// Tinggi isi minimum (proporsi badan jerigen) supaya stok kecil tetap terlihat sebagai cairan.
private const val MIN_VISIBLE_LEVEL = 0.14f

private fun wavePath(surfaceY: Float, amplitude: Float, shift: Float): Path = Path().apply {
    var x = -WAVE_PERIOD + shift
    moveTo(x, surfaceY)
    repeat(6) {
        quadraticTo(x + WAVE_PERIOD / 4f, surfaceY - amplitude, x + WAVE_PERIOD / 2f, surfaceY)
        quadraticTo(x + WAVE_PERIOD * 3f / 4f, surfaceY + amplitude, x + WAVE_PERIOD, surfaceY)
        x += WAVE_PERIOD
    }
    lineTo(x, 124f)
    lineTo(-WAVE_PERIOD + shift, 124f)
    close()
}

/**
 * Jerigen jelantah. [fill] 0..1 = rasio stok terhadap ambang (stok / ambang), jadi tingginya
 * mengikuti data asli. Stok > 0 selalu tampil minimal [MIN_VISIBLE_LEVEL] agar tidak terlihat kosong;
 * angka sebenarnya tetap ditampilkan sebagai teks di luar gambar. [animate] menggeser gelombang pelan.
 * Viewbox 100×124, jadi beri modifier dengan rasio kira-kira 0,8.
 */
@Composable
fun JerrycanGauge(
    fill: Float,
    modifier: Modifier = Modifier,
    animate: Boolean = true,
) {
    val c = BioTheme.colors
    val body = remember { PathParser().parsePathString(JERRYCAN_BODY).toPath() }
    val handle = remember { PathParser().parsePathString(JERRYCAN_HANDLE).toPath() }
    val level = fill.coerceIn(0f, 1f)
    val shift by if (animate && level > 0f) {
        rememberInfiniteTransition(label = "jerrycanWave").animateFloat(
            initialValue = 0f,
            targetValue = WAVE_PERIOD,
            animationSpec = infiniteRepeatable(tween(3200, easing = LinearEasing), RepeatMode.Restart),
            label = "waveShift",
        )
    } else {
        remember { mutableStateOf(0f) }
    }
    val edge = c.primary.copy(alpha = 0.35f)

    Canvas(modifier) {
        scale(size.width / 100f, size.height / 124f, pivot = Offset.Zero) {
            // tutup + pegangan
            drawRoundRect(c.primary, topLeft = Offset(62f, 12f), size = Size(16f, 22f), cornerRadius = CornerRadius(3f, 3f))
            drawPath(handle, color = edge, style = Stroke(width = 4f, cap = StrokeCap.Round))

            // badan
            drawPath(body, color = c.primaryTint)

            if (level > 0f) {
                val shown = level.coerceAtLeast(MIN_VISIBLE_LEVEL)
                val surface = 118f - 88f * shown
                clipPath(body) {
                    drawPath(wavePath(surface - 2f, amplitude = 3.5f, shift = shift), color = Color(0xFFEBC265))
                    drawPath(wavePath(surface + 2f, amplitude = 3.5f, shift = WAVE_PERIOD - shift), color = c.amber)
                }
            }

            drawPath(body, color = edge, style = Stroke(width = 2.5f))

            // skala liter
            listOf(54f, 68f, 82f).forEach { y ->
                drawLine(edge, Offset(66f, y), Offset(78f, y), strokeWidth = 1.5f, cap = StrokeCap.Round)
            }
            // kilau kaca
            drawLine(Color.White.copy(alpha = 0.55f), Offset(22f, 50f), Offset(22f, 92f), strokeWidth = 4f, cap = StrokeCap.Round)
        }
    }
}

// ------------------------------------------------------------------ band harga

@Composable
fun PriceBand(price: Long, caption: String, modifier: Modifier = Modifier) {
    val c = BioTheme.colors
    val shape = RoundedCornerShape(22.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Brush.linearGradient(listOf(c.primaryDeep, c.primary)), shape)
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("HARGA JELANTAH", style = BioTheme.type.eyebrow, color = c.onPrimary.copy(alpha = 0.7f))
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(formatRupiah(price), style = BioTheme.type.display.copy(fontSize = 28.sp), color = Color.White)
                Text(" / liter", style = BioTheme.type.bodyBold, color = c.onPrimary.copy(alpha = 0.75f), modifier = Modifier.padding(bottom = 4.dp))
            }
            Spacer(Modifier.height(4.dp))
            Text(caption, style = BioTheme.type.small, color = c.onPrimary.copy(alpha = 0.7f))
        }
        DropGauge(fill = 1f, outline = false, modifier = Modifier.size(width = 44.dp, height = 55.dp))
    }
}

// ------------------------------------------------------------------ peta sederhana

private const val PIN_PATH = "M12 2C8 2 5 5 5 9c0 5.2 7 13 7 13s7-7.8 7-13c0-4-3-7-7-7z"

/**
 * Posisi ujung bawah pin (titik koordinat) untuk tiap [points] pada kanvas berukuran [w] x [h] px.
 * Elemen null = titik tanpa koordinat (0,0). Dipakai bersama oleh gambar dan hit-test supaya tak pernah beda.
 */
private fun pinTips(points: List<Pair<Double, Double>>, w: Float, h: Float): List<Offset?> {
    val valid = points.filter { it.first != 0.0 || it.second != 0.0 }
    if (valid.isEmpty()) return points.map { null }
    val minLat = valid.minOf { it.first }
    val maxLat = valid.maxOf { it.first }
    val minLng = valid.minOf { it.second }
    val maxLng = valid.maxOf { it.second }
    val padX = w * 0.14f
    val padY = h * 0.2f
    return points.map { (lat, lng) ->
        if (lat == 0.0 && lng == 0.0) return@map null
        val fx = if (maxLng - minLng < 1e-9) 0.5f else ((lng - minLng) / (maxLng - minLng)).toFloat()
        val fy = if (maxLat - minLat < 1e-9) 0.5f else ((maxLat - lat) / (maxLat - minLat)).toFloat()
        Offset(padX + fx * (w - 2 * padX), padY + fy * (h - 2 * padY))
    }
}

/**
 * Pratinjau peta bergaya prototype: grid + pin. Posisi pin diproyeksikan dari koordinat asli
 * (latitude/longitude) relatif terhadap kotak pembatas seluruh titik. Bukan peta interaktif —
 * tombol "Buka di peta" membuka aplikasi peta bawaan.
 *
 * [highlight] >= 0 menebalkan satu pin dan meredupkan sisanya. Bila [onPinClick] diisi, ketukan di
 * dekat sebuah pin memanggilnya dengan indeks pin tersebut. [cornerRadius] 0 = tanpa sudut (full-bleed).
 */
@Composable
fun MapPreview(
    points: List<Pair<Double, Double>>,
    modifier: Modifier = Modifier,
    height: Dp = 150.dp,
    highlight: Int = -1,
    cornerRadius: Dp = 20.dp,
    onPinClick: ((Int) -> Unit)? = null,
) {
    val c = BioTheme.colors
    val pin = remember { PathParser().parsePathString(PIN_PATH).toPath() }
    val shape = RoundedCornerShape(cornerRadius)
    Box(
        modifier = modifier.fillMaxWidth().height(height).clip(shape).background(c.mapBase, shape).border(1.dp, c.line, shape),
    ) {
        Canvas(
            Modifier
                .fillMaxSize()
                .then(
                    if (onPinClick == null) Modifier else Modifier.pointerInput(points) {
                        detectTapGestures { tap ->
                            val pinPx = 26.dp.toPx()
                            val reach = 28.dp.toPx()
                            val tips = pinTips(points, size.width.toFloat(), size.height.toFloat())
                            var best = -1
                            var bestDist = Float.MAX_VALUE
                            tips.forEachIndexed { index, tip ->
                                if (tip == null) return@forEachIndexed
                                // pusat tubuh pin berada setengah tinggi pin di atas ujungnya
                                val dist = (tap - Offset(tip.x, tip.y - pinPx / 2f)).getDistance()
                                if (dist < bestDist) { bestDist = dist; best = index }
                            }
                            if (best >= 0 && bestDist <= reach) onPinClick(best)
                        }
                    },
                ),
        ) {
            val step = 26.dp.toPx()
            var x = step
            while (x < size.width) {
                drawLine(c.mapGrid, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1.dp.toPx())
                x += step
            }
            var y = step
            while (y < size.height) {
                drawLine(c.mapGrid, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
                y += step
            }
            // "sungai" dekoratif
            drawLine(Color(0xFFCFE0EA), Offset(0f, size.height * 0.72f), Offset(size.width, size.height * 0.38f), strokeWidth = 9.dp.toPx())

            val tips = pinTips(points, size.width, size.height)
            val basePx = 26.dp.toPx()
            // pin yang disorot digambar terakhir supaya tidak tertutup pin lain
            val order = tips.indices.sortedBy { if (it == highlight) 1 else 0 }
            order.forEach { index ->
                val tip = tips[index] ?: return@forEach
                val active = highlight < 0 || highlight == index
                val pinPx = if (index == highlight) basePx * 1.3f else basePx
                val s = pinPx / 24f
                translate(left = tip.x - pinPx / 2f, top = tip.y - pinPx) {
                    scale(s, s, pivot = Offset.Zero) {
                        drawPath(pin, color = if (index == highlight) c.amberDeep else if (active) c.primary else c.primary.copy(alpha = 0.45f))
                        drawCircle(Color.White, radius = 3f, center = Offset(12f, 9f))
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------ tab bar

data class TabItem(val label: String, val icon: ImageVector)

@Composable
fun BioTabBar(items: List<TabItem>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val c = BioTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(c.surface)
            .drawBehind {
                drawLine(c.line, Offset(0f, 0f), Offset(size.width, 0f), strokeWidth = 1.dp.toPx())
            }
            .navigationBarsPadding()
            .padding(top = 8.dp, bottom = 6.dp),
    ) {
        items.forEachIndexed { index, item ->
            val on = index == selected
            Column(
                modifier = Modifier.weight(1f).clickable { onSelect(index) }.padding(vertical = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(item.icon, contentDescription = item.label, tint = if (on) c.primary else c.muted, modifier = Modifier.size(22.dp))
                Spacer(Modifier.height(3.dp))
                Text(item.label, style = BioTheme.type.tab, color = if (on) c.primary else c.muted, maxLines = 1)
                Spacer(Modifier.height(3.dp))
                Box(Modifier.size(4.dp).background(if (on) c.amber else Color.Transparent, RoundedCornerShape(50)))
            }
        }
    }
}

// ------------------------------------------------------------------ baris list

@Composable
fun TxRow(
    avatar: String,
    title: String,
    subtitle: String,
    amount: String,
    statusText: String,
    statusKind: ChipKind,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val c = BioTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .bioCard(16.dp)
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AvatarBox(
            avatar,
            tone = when (statusKind) {
                ChipKind.Cancelled -> NoteTone.Rust
                ChipKind.Pending -> NoteTone.Amber
                else -> NoteTone.Neutral
            },
        )
        Column(Modifier.weight(1f)) {
            Text(title, style = BioTheme.type.cardTitle, color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = BioTheme.type.small, color = c.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(amount, style = BioTheme.type.mono, color = c.ink)
            Spacer(Modifier.height(4.dp))
            BioChip(statusText, statusKind)
        }
    }
}

@Composable
fun ProfileRow(
    icon: ImageVector,
    label: String,
    value: String? = null,
    modifier: Modifier = Modifier,
    tint: Color = BioTheme.colors.primary,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val c = BioTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        Column(Modifier.weight(1f)) {
            Text(label, style = BioTheme.type.bodyBold, color = if (tint == c.rust) c.rust else c.ink)
            if (value != null) Text(value, style = BioTheme.type.small, color = c.muted)
        }
        if (trailing != null) trailing()
        else if (onClick != null) Icon(BioIcons.Chevron, contentDescription = null, tint = c.muted, modifier = Modifier.size(16.dp))
    }
}

/**
 * Staggered fade-in + slide-up wrapper for list items.
 * Each item fades in with a delay based on its [index] for a polished appearance.
 */
@Composable
fun FadeInItem(index: Int, content: @Composable () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(index * 50L)
        visible = true
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 4 },
    ) {
        content()
    }
}