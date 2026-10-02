package id.biojelan.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.biojelan.app.core.formatEpochDay
import id.biojelan.app.core.formatMonthKey
import id.biojelan.app.core.localEpochDayOf
import id.biojelan.app.core.monthKeyOf
import id.biojelan.app.core.parseIsoMillis
import id.biojelan.app.core.todayEpochDay
import id.biojelan.app.ui.strings.BioText
import id.biojelan.app.ui.theme.BioTheme

// ------------------------------------------------------------------ pengelompokan

/** Satu kelompok transaksi pada hari yang sama. [epochDay] = [UNKNOWN_GROUP] kalau tanggal gagal di-parse. */
data class DayGroup<T>(val epochDay: Long, val items: List<T>)

/** Satu kelompok transaksi pada bulan yang sama. [key] = tahun * 12 + (bulan - 1), atau [UNKNOWN_MONTH]. */
data class MonthGroup<T>(val key: Int, val items: List<T>)

const val UNKNOWN_GROUP = Long.MIN_VALUE
const val UNKNOWN_MONTH = Int.MIN_VALUE

/** Urut terbaru dulu, lalu kelompokkan per hari lokal. */
fun <T> groupByDay(items: List<T>, createdAt: (T) -> String): List<DayGroup<T>> =
    items
        .sortedByDescending { parseIsoMillis(createdAt(it)) ?: Long.MIN_VALUE }
        .groupBy { localEpochDayOf(createdAt(it)) ?: UNKNOWN_GROUP }
        .map { (day, list) -> DayGroup(day, list) }

/** Urut terbaru dulu, lalu kelompokkan per bulan lokal. */
fun <T> groupByMonth(items: List<T>, createdAt: (T) -> String): List<MonthGroup<T>> =
    items
        .sortedByDescending { parseIsoMillis(createdAt(it)) ?: Long.MIN_VALUE }
        .groupBy { monthKeyOf(createdAt(it)) ?: UNKNOWN_MONTH }
        .map { (key, list) -> MonthGroup(key, list) }

/**
 * Volume 7 hari terakhir (indeks 6 = hari ini), dinormalisasi 0..1 terhadap hari tersibuk.
 * Semua nol bila belum ada data.
 */
fun weekVolumeBars(entries: List<Pair<String, Double>>): List<Float> {
    val today = todayEpochDay()
    val sums = DoubleArray(7)
    entries.forEach { (iso, volume) ->
        val day = localEpochDayOf(iso) ?: return@forEach
        val diff = (today - day).toInt()
        if (diff in 0..6) sums[6 - diff] += volume
    }
    val max = sums.max()
    return sums.map { if (max <= 0.0) 0f else (it / max).toFloat() }
}

// ------------------------------------------------------------------ header kelompok

@Composable
fun DayHeader(epochDay: Long, modifier: Modifier = Modifier) {
    val c = BioTheme.colors
    val s = BioText.current
    val today = todayEpochDay()
    val label = when (epochDay) {
        UNKNOWN_GROUP -> "—"
        today -> s.todayLabel
        today - 1 -> s.yesterdayLabel
        else -> formatEpochDay(epochDay)
    }
    Text(
        label,
        style = BioTheme.type.label,
        color = c.muted,
        modifier = modifier.fillMaxWidth().padding(horizontal = ScreenPad).padding(top = 8.dp),
    )
}

/** Header bulan dengan subtotal di kanan (mis. "Rp 336rb"). */
@Composable
fun MonthHeader(monthKey: Int, trailing: String?, modifier: Modifier = Modifier) {
    val c = BioTheme.colors
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = ScreenPad).padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            if (monthKey == UNKNOWN_MONTH) "—" else formatMonthKey(monthKey),
            style = BioTheme.type.label,
            color = c.muted,
            modifier = Modifier.weight(1f),
        )
        if (trailing != null) Text(trailing, style = BioTheme.type.monoSmall, color = c.muted)
    }
}

// ------------------------------------------------------------------ hero

/**
 * Kartu ringkasan hijau di atas daftar (Transaksi & Riwayat). Angka utama di kiri, [visual] kecil
 * di kanan (bar mingguan atau donut), dan deretan [stats] di bawah.
 */
@Composable
fun LedgerHero(
    eyebrow: String,
    value: String,
    stats: List<Pair<String, String>>,
    modifier: Modifier = Modifier,
    visual: @Composable () -> Unit,
) {
    val c = BioTheme.colors
    val shape = RoundedCornerShape(22.dp)
    Box(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .drawBehind {
                drawRect(Brush.linearGradient(listOf(c.primaryDeep, c.primary)))
                val w = size.width
                val h = size.height
                listOf(60f, 100f, 140f).forEach { r ->
                    drawCircle(
                        c.onPrimary.copy(alpha = 0.06f),
                        radius = r.dp.toPx(),
                        center = Offset(w * 0.96f, -h * 0.05f),
                        style = Stroke(1.5.dp.toPx()),
                    )
                }
                drawPath(waveFill(w, h, h * 0.86f, 4.dp.toPx(), 110.dp.toPx(), 0.6f), c.amber.copy(alpha = 0.14f))
            },
    ) {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    Text(eyebrow, style = BioTheme.type.eyebrow, color = c.onPrimary.copy(alpha = 0.7f), maxLines = 1)
                    Spacer(Modifier.height(4.dp))
                    Text(value, style = BioTheme.type.display.copy(fontSize = 30.sp), color = c.onPrimary, maxLines = 1)
                }
                Spacer(Modifier.width(12.dp))
                visual()
            }
            if (stats.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    stats.forEach { (label, number) ->
                        Column {
                            Text(label, style = BioTheme.type.caption, color = c.onPrimary.copy(alpha = 0.7f), maxLines = 1)
                            Spacer(Modifier.height(2.dp))
                            Text(number, style = BioTheme.type.mono, color = c.onPrimary, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

/** Tujuh bar volume harian untuk [LedgerHero]; bar terakhir (hari ini) berwarna amber. */
@Composable
fun WeekBars(values: List<Float>, modifier: Modifier = Modifier) {
    val c = BioTheme.colors
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { started = true }
    val grow by animateFloatAsState(if (started) 1f else 0f, tween(700), label = "weekBars")
    Canvas(modifier.size(width = 92.dp, height = 52.dp)) {
        val n = values.size.coerceAtLeast(1)
        val gap = 5.dp.toPx()
        val barW = (size.width - gap * (n - 1)) / n
        val minH = 4.dp.toPx()
        values.forEachIndexed { i, v ->
            val barH = (minH + (size.height - minH) * v.coerceIn(0f, 1f) * grow)
            drawRoundRect(
                color = if (i == values.lastIndex) c.amber else c.onPrimary.copy(alpha = 0.32f),
                topLeft = Offset(i * (barW + gap), size.height - barH),
                size = Size(barW, barH),
                cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx()),
            )
        }
    }
}

/** Cincin progres kecil untuk [LedgerHero]: [ratio] 0..1, teks persen di tengah, [caption] di bawahnya. */
@Composable
fun HeroDonut(ratio: Float, caption: String, modifier: Modifier = Modifier) {
    val c = BioTheme.colors
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { started = true }
    val sweep by animateFloatAsState(if (started) ratio.coerceIn(0f, 1f) else 0f, tween(800), label = "heroDonut")
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(58.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(58.dp)) {
                val stroke = 7.dp.toPx()
                val inset = stroke / 2f
                val arcSize = Size(size.width - stroke, size.height - stroke)
                drawArc(
                    color = c.onPrimary.copy(alpha = 0.25f),
                    startAngle = 0f, sweepAngle = 360f, useCenter = false,
                    topLeft = Offset(inset, inset), size = arcSize, style = Stroke(stroke),
                )
                if (sweep > 0f) {
                    drawArc(
                        color = c.amber,
                        startAngle = -90f, sweepAngle = 360f * sweep, useCenter = false,
                        topLeft = Offset(inset, inset), size = arcSize,
                        style = Stroke(stroke, cap = StrokeCap.Round),
                    )
                }
            }
            Text(
                "${(ratio.coerceIn(0f, 1f) * 100f).toInt()}%",
                style = BioTheme.type.caption,
                color = c.onPrimary,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height(3.dp))
        Text(caption, style = BioTheme.type.caption, color = c.onPrimary.copy(alpha = 0.7f), maxLines = 1)
    }
}
