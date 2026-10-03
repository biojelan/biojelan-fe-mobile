package id.biojelan.app.ui.driver

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.biojelan.app.ui.components.BioChip
import id.biojelan.app.ui.components.ChipKind
import id.biojelan.app.ui.components.bioCard
import id.biojelan.app.ui.icons.BioIcons
import id.biojelan.app.ui.strings.BioStrings
import id.biojelan.app.ui.strings.BioText
import id.biojelan.app.ui.theme.BioTheme

/** Label chip stop: stop pickup yang sedang berjalan menampilkan langkah stepper-nya. */
internal fun chipFor(stop: RouteStop, s: BioStrings): Pair<String, ChipKind> = when (stop.status) {
    StopStatus.Completed -> s.stopChipDone to ChipKind.Done
    StopStatus.Skipped -> s.stopChipSkipped to ChipKind.Cancelled
    StopStatus.Rejected -> s.stopChipRejected to ChipKind.Cancelled
    StopStatus.AwaitingConfirm -> s.stopChipAwaitingAgen to ChipKind.Pending
    StopStatus.Assigned -> (if (stop.isPickup) s.stepAssigned else s.stopChipWaiting) to
        (if (stop.isPickup) ChipKind.Pending else ChipKind.Neutral)
    StopStatus.OnTheWay -> s.stepOnTheWay to ChipKind.Pending
    StopStatus.Arrived -> s.stepArrived to ChipKind.Pending
}

/** Banner "Tugas baru ditugaskan" (prototype .notif-banner). Bisa ditutup. */
@Composable
internal fun NewTaskBanner(pickupId: String, agenName: String, onClose: () -> Unit) {
    val c = BioTheme.colors
    val s = BioText.current
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 14.dp)
            .background(Brush.linearGradient(listOf(c.primaryTint, c.surface)), shape)
            .border(1.5.dp, c.primaryTint, shape)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box {
            Box(Modifier.size(36.dp).background(c.primary, RoundedCornerShape(11.dp)), contentAlignment = Alignment.Center) {
                Icon(BioIcons.Bell, contentDescription = null, tint = Color.White, modifier = Modifier.size(17.dp))
            }
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 2.dp, y = (-2).dp)
                    .size(9.dp)
                    .background(c.rust, RoundedCornerShape(50))
                    .border(2.dp, c.surface, RoundedCornerShape(50)),
            )
        }
        Column(Modifier.weight(1f)) {
            Text(s.driverNewTaskTitle, style = BioTheme.type.cardTitle, color = c.primary)
            Text(
                s.driverNewTaskBody(pickupId, agenName.ifBlank { s.driverStopUnknownAgen }),
                style = BioTheme.type.small,
                color = c.inkSoft,
            )
        }
        Box(
            Modifier.size(26.dp).clip(RoundedCornerShape(50)).clickable(onClick = onClose),
            contentAlignment = Alignment.Center,
        ) {
            Icon(BioIcons.Close, contentDescription = s.close, tint = c.muted, modifier = Modifier.size(13.dp))
        }
    }
}

/**
 * Kartu ringkasan rute (prototype .route-summary): gradien hijau, lingkaran amber di pojok kanan atas,
 * ID rute, judul, dan tiga angka. Angka diambil dari transaksi hari ini karena API tidak menyediakan
 * jarak maupun estimasi selesai.
 */
@Composable
internal fun RouteSummaryCard(
    pickupId: String?,
    stopCount: Int,
    stats: List<Pair<String, String>>,
) {
    val c = BioTheme.colors
    val s = BioText.current
    val shape = RoundedCornerShape(20.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.linearGradient(listOf(c.primary, c.primaryDeep)), shape)
            .drawBehind {
                drawCircle(
                    color = c.amber.copy(alpha = 0.18f),
                    radius = 75.dp.toPx(),
                    center = Offset(size.width - 45.dp.toPx(), 35.dp.toPx()),
                )
            }
            .padding(18.dp),
    ) {
        if (!pickupId.isNullOrBlank()) {
            Text("ID Rute " + pickupId, style = BioTheme.type.small, color = c.onPrimary.copy(alpha = 0.75f))
            Spacer(Modifier.height(6.dp))
        }
        Text(s.driverRouteHeadline(stopCount), style = BioTheme.type.subTitle, color = c.onPrimary)
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            stats.forEach { (number, label) ->
                Column {
                    Text(number, style = BioTheme.type.statNumber, color = c.onPrimary, maxLines = 1)
                    Text(
                        label.uppercase(),
                        style = BioTheme.type.caption,
                        color = c.onPrimary.copy(alpha = 0.7f),
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

// Posisi cadangan (pecahan lebar, tinggi) dipakai bila koordinat Agen belum tersedia (prototype .rpin).
private val FallbackSpots = listOf(
    0.10f to 0.47f, 0.43f to 0.24f, 0.72f to 0.51f, 0.88f to 0.22f, 0.28f to 0.78f, 0.58f to 0.80f,
)

internal const val MAX_MAP_STOPS = 8

/** Posisi simbolis Kilang bila koordinatnya belum ada. */
private val KilangFallbackSpot = 0.07f to 0.84f

/**
 * Peta skema 150dp: pin bernomor per stop di posisi relatif dari koordinat Agen ([spots], null = pakai
 * posisi cadangan), garis hijau untuk ruas yang sudah ditempuh dan putus-putus untuk ruas di depan.
 *
 * Titik amber menandai posisi driver berdasarkan status stop (bukan GPS): diam di checkpoint terakhir saat
 * ditugaskan, bergerak berulang ke tujuan saat dalam perjalanan, dan berhenti di tujuan saat tiba.
 */
@Composable
internal fun RouteMapCard(
    stops: List<RouteStop>,
    spots: List<MapSpot?>,
    kilangSpot: MapSpot?,
    onNavigate: (() -> Unit)? = null,
) {
    val c = BioTheme.colors
    val s = BioText.current
    val shape = RoundedCornerShape(20.dp)
    val transition = rememberInfiniteTransition(label = "routeMap")
    val pulse by transition.animateFloat(
        0f, 1f, infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Restart), label = "pulse",
    )
    val travel by transition.animateFloat(
        0f, 1f, infiniteRepeatable(tween(2600, easing = FastOutSlowInEasing), RepeatMode.Restart), label = "travel",
    )
    // Stop aktif ada di ujung daftar, jadi bila lebih dari batas yang dipotong adalah stop tertua.
    val shown = stops.takeLast(MAX_MAP_STOPS)
    val pos = shown.indices.map { i ->
        spots.getOrNull(i)?.let { it.x to it.y } ?: FallbackSpots[i % FallbackSpots.size]
    }
    // Kilang: titik awal dan akhir rute. Tanpa koordinat dipakai posisi cadangan di sudut kiri bawah.
    val kilangPos = kilangSpot?.let { it.x to it.y } ?: KilangFallbackSpot
    val progress = routeProgress(shown)
    val ci = progress.currentIndex
    // Titik driver: (pecahan x, pecahan y) atau null bila tidak ada yang perlu digambar.
    fun lerp(from: Pair<Float, Float>, to: Pair<Float, Float>) =
        (from.first + (to.first - from.first) * travel) to (from.second + (to.second - from.second) * travel)
    val origin = if (ci <= 0) kilangPos else pos[ci - 1]
    val driverAt: Pair<Float, Float>? = when (progress.phase) {
        RoutePhase.Ready -> origin
        RoutePhase.EnRoute -> lerp(origin, pos[ci])
        RoutePhase.Arrived, RoutePhase.Awaiting -> pos[ci]
        RoutePhase.Finished -> lerp(pos.last(), kilangPos)
        RoutePhase.Idle -> null
    }
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .height(150.dp)
            .clip(shape)
            .background(c.mapBase, shape)
            .border(1.dp, c.line, shape)
            .let { if (onNavigate != null) it.clickable(onClick = onNavigate) else it },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val step = 26.dp.toPx()
            val grid = 2.dp.toPx()
            var x = 0f
            while (x < size.width) { drawLine(c.mapGrid, Offset(x, 0f), Offset(x, size.height), strokeWidth = grid); x += step }
            var y = 0f
            while (y < size.height) { drawLine(c.mapGrid, Offset(0f, y), Offset(size.width, y), strokeWidth = grid); y += step }
            // Rantai Kilang -> stop 1..n -> Kilang. Ruas solid bila driver sudah sampai di tujuan ruas itu.
            val chain = listOf(kilangPos) + pos + kilangPos
            for (i in 0 until chain.size - 1) {
                val a = chain[i]
                val b = chain[i + 1]
                val reached = i < shown.size && shown[i].status.let {
                    it == StopStatus.Completed || it == StopStatus.Rejected ||
                        it == StopStatus.Arrived || it == StopStatus.AwaitingConfirm
                }
                drawLine(
                    color = if (reached) c.primary else c.primary.copy(alpha = 0.35f),
                    start = Offset(size.width * a.first, size.height * a.second),
                    end = Offset(size.width * b.first, size.height * b.second),
                    strokeWidth = if (reached) 3.dp.toPx() else 2.dp.toPx(),
                    pathEffect = if (reached) null else PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 7.dp.toPx())),
                )
            }
        }
        // Titik Kilang (kotak bertanda K).
        Box(
            Modifier.offset(x = maxWidth * kilangPos.first - 12.dp, y = maxHeight * kilangPos.second - 12.dp).size(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.size(24.dp).background(c.primaryDeep, RoundedCornerShape(7.dp)), contentAlignment = Alignment.Center) {
                Text("K", style = BioTheme.type.chip.copy(fontSize = 10.sp), color = Color.White)
            }
        }
        shown.forEachIndexed { i, stop ->
            val (fx, fy) = pos[i]
            val current = stop.isPickup && !stop.isFinal
            val bg = when {
                stop.status == StopStatus.Completed -> c.primary
                stop.status == StopStatus.Skipped || stop.status == StopStatus.Rejected -> c.rust
                current -> c.amberDeep
                else -> c.muted
            }
            Box(
                Modifier.offset(x = maxWidth * fx - 12.dp, y = maxHeight * fy - 12.dp).size(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (current) {
                    Box(
                        Modifier
                            .size(24.dp)
                            .graphicsLayer(scaleX = 0.8f + pulse, scaleY = 0.8f + pulse, alpha = 0.55f * (1f - pulse))
                            .border(2.dp, c.amberDeep, RoundedCornerShape(50)),
                    )
                }
                Box(Modifier.size(24.dp).background(bg, RoundedCornerShape(50)), contentAlignment = Alignment.Center) {
                    Text((i + 1).toString(), style = BioTheme.type.chip.copy(fontSize = 10.sp), color = Color.White)
                }
            }
        }
        // Titik driver di atas pin supaya tetap terlihat saat berhenti di stop.
        driverAt?.let { (fx, fy) ->
            Box(
                Modifier
                    .offset(x = maxWidth * fx - 7.dp, y = maxHeight * fy - 7.dp)
                    .size(14.dp)
                    .background(c.amber, RoundedCornerShape(50))
                    .border(2.dp, Color.White, RoundedCornerShape(50)),
            )
        }
        // Pil ajakan: menandakan peta bisa diketuk untuk membuka rute di Google Maps.
        if (onNavigate != null) {
            Row(
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(10.dp)
                    .background(c.primary, RoundedCornerShape(50))
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(BioIcons.Navigate, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                Text(s.driverMapTapHint, style = BioTheme.type.chip, color = Color.White, maxLines = 1)
            }
        }
    }
}

/**
 * Kartu "Posisi rute": checkpoint terakhir yang sudah dilewati → tujuan yang sedang berjalan, plus chip fase.
 * Semua diturunkan dari status stop; API tidak mengirim posisi GPS maupun daftar stop berikutnya.
 */
@Composable
internal fun RouteProgressCard(progress: RouteProgress, modifier: Modifier = Modifier) {
    val c = BioTheme.colors
    val s = BioText.current
    val (phaseText, phaseKind) = when (progress.phase) {
        RoutePhase.Ready -> s.stepAssigned to ChipKind.Pending
        RoutePhase.EnRoute -> s.stepOnTheWay to ChipKind.Pending
        RoutePhase.Arrived -> s.stepArrived to ChipKind.Pending
        RoutePhase.Awaiting -> s.stepAwaiting to ChipKind.Pending
        RoutePhase.Finished -> s.driverProgressReturn to ChipKind.Done
        RoutePhase.Idle -> s.stopChipDone to ChipKind.Done
    }
    Column(modifier.fillMaxWidth().bioCard(16.dp).padding(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(s.driverProgressTitle, style = BioTheme.type.cardTitle, color = c.ink, modifier = Modifier.weight(1f))
            BioChip(phaseText, phaseKind)
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ProgressEnd(
                label = s.driverProgressFrom,
                value = progress.from?.name?.ifBlank { s.driverStopUnknownAgen } ?: s.driverKilang,
                modifier = Modifier.weight(1f),
            )
            Icon(BioIcons.Chevron, contentDescription = null, tint = c.amberDeep, modifier = Modifier.size(18.dp))
            ProgressEnd(
                label = s.driverProgressTo,
                value = progress.to?.name?.ifBlank { s.driverStopUnknownAgen }
                    ?: if (progress.phase == RoutePhase.Finished) s.driverKilang else s.driverProgressNone,
                modifier = Modifier.weight(1f),
                highlight = progress.to != null || progress.phase == RoutePhase.Finished,
            )
        }
    }
}

@Composable
private fun ProgressEnd(label: String, value: String, modifier: Modifier = Modifier, highlight: Boolean = false) {
    val c = BioTheme.colors
    Column(modifier) {
        Text(label, style = BioTheme.type.caption, color = c.muted)
        Text(
            value,
            style = BioTheme.type.bodyBold,
            color = if (highlight) c.amberDeep else c.ink,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Hint box di bawah peta rute: menjelaskan bahwa peta/tombol Navigasi membuka Google Maps. Bisa ditutup. */
@Composable
internal fun RouteHintBox(onClose: () -> Unit, modifier: Modifier = Modifier) {
    val c = BioTheme.colors
    val s = BioText.current
    Row(
        modifier
            .fillMaxWidth()
            .background(c.amberTint, RoundedCornerShape(14.dp))
            .padding(start = 12.dp, top = 10.dp, bottom = 10.dp, end = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(BioIcons.Info, contentDescription = null, tint = c.amberText, modifier = Modifier.padding(top = 1.dp).size(18.dp))
        Text(s.driverMapHintBody, style = BioTheme.type.small, color = c.amberText, modifier = Modifier.weight(1f))
        Box(
            Modifier.size(28.dp).clip(RoundedCornerShape(50)).clickable(onClick = onClose),
            contentAlignment = Alignment.Center,
        ) {
            Icon(BioIcons.Close, contentDescription = s.close, tint = c.amberText, modifier = Modifier.size(13.dp))
        }
    }
}

/** Kartu aksi "Navigasi ke Agen": tombol sekaligus hint — satu ketukan membuka Google Maps dengan auto-direction. */
@Composable
internal fun NavigateCard(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = BioTheme.colors
    val s = BioText.current
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(c.primaryTint, shape)
            .border(1.5.dp, c.primary.copy(alpha = 0.35f), shape)
            .clickable(onClick = onClick)
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(42.dp).background(c.primary, RoundedCornerShape(13.dp)), contentAlignment = Alignment.Center) {
            Icon(BioIcons.Navigate, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(s.driverNavigate, style = BioTheme.type.cardTitle, color = c.primary)
            Text(s.driverNavigateSub, style = BioTheme.type.small, color = c.inkSoft)
        }
        Icon(BioIcons.Chevron, contentDescription = null, tint = c.primary, modifier = Modifier.size(16.dp))
    }
}

/** Baris "Urutan kunjungan" (prototype .stop-row). Stop berjalan diberi bingkai amber. */
@Composable
internal fun StopRow(index: Int, stop: RouteStop, subtitle: String, onClick: () -> Unit) {
    val c = BioTheme.colors
    val s = BioText.current
    val current = stop.isPickup && !stop.isFinal
    val (chipText, chipKind) = chipFor(stop, s)
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(c.surface, shape)
            .border(if (current) 1.5.dp else 1.dp, if (current) c.amberDeep else c.line, shape)
            .clickable(onClick = onClick)
            .padding(13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val (numBg, numFg) = when {
            stop.status == StopStatus.Completed -> c.primary to Color.White
            current -> c.amberDeep to Color.White
            stop.status == StopStatus.Skipped || stop.status == StopStatus.Rejected -> c.rustTint to c.rust
            else -> c.primaryTint to c.primary
        }
        Box(Modifier.size(30.dp).background(numBg, RoundedCornerShape(50)), contentAlignment = Alignment.Center) {
            if (stop.status == StopStatus.Completed) {
                Icon(BioIcons.Check, contentDescription = null, tint = numFg, modifier = Modifier.size(15.dp))
            } else {
                Text((index + 1).toString(), style = BioTheme.type.label, color = numFg)
            }
        }
        Column(Modifier.weight(1f)) {
            Text(
                stop.name.ifBlank { s.driverStopUnknownAgen },
                style = BioTheme.type.cardTitle, color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            if (subtitle.isNotBlank()) {
                Text(subtitle, style = BioTheme.type.small, color = c.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        BioChip(chipText, chipKind)
        Icon(BioIcons.Chevron, contentDescription = null, tint = c.muted, modifier = Modifier.size(16.dp))
    }
}
