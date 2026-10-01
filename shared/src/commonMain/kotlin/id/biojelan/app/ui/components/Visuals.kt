package id.biojelan.app.ui.components

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
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
 * Pratinjau peta bergaya prototype: grid + pin. Posisi pin diproyeksikan dari koordinat asli
 * (latitude/longitude) relatif terhadap kotak pembatas seluruh titik. Bukan peta interaktif —
 * tombol "Buka di peta" membuka aplikasi peta bawaan.
 */
@Composable
fun MapPreview(
    points: List<Pair<Double, Double>>,
    modifier: Modifier = Modifier,
    height: Dp = 150.dp,
    highlight: Int = -1,
) {
    val c = BioTheme.colors
    val pin = remember { PathParser().parsePathString(PIN_PATH).toPath() }
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier = modifier.fillMaxWidth().height(height).clip(shape).background(c.mapBase, shape).border(1.dp, c.line, shape),
    ) {
        Canvas(Modifier.fillMaxSize()) {
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

            val valid = points.filter { it.first != 0.0 || it.second != 0.0 }
            if (valid.isEmpty()) return@Canvas
            val minLat = valid.minOf { it.first }
            val maxLat = valid.maxOf { it.first }
            val minLng = valid.minOf { it.second }
            val maxLng = valid.maxOf { it.second }
            val padX = size.width * 0.14f
            val padY = size.height * 0.2f
            val pinPx = 26.dp.toPx()
            val s = pinPx / 24f

            points.forEachIndexed { index, (lat, lng) ->
                if (lat == 0.0 && lng == 0.0) return@forEachIndexed
                val fx = if (maxLng - minLng < 1e-9) 0.5f else ((lng - minLng) / (maxLng - minLng)).toFloat()
                val fy = if (maxLat - minLat < 1e-9) 0.5f else ((maxLat - lat) / (maxLat - minLat)).toFloat()
                val px = padX + fx * (size.width - 2 * padX)
                val py = padY + fy * (size.height - 2 * padY)
                val active = highlight < 0 || highlight == index
                translate(left = px - pinPx / 2f, top = py - pinPx) {
                    scale(s, s, pivot = Offset.Zero) {
                        drawPath(pin, color = if (active) c.primary else c.primary.copy(alpha = 0.45f))
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
        if (onClick != null) Icon(BioIcons.Chevron, contentDescription = null, tint = c.muted, modifier = Modifier.size(16.dp))
    }
}
