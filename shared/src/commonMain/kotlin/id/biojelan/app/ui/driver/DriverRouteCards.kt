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

// Posisi pin (pecahan lebar, tinggi) mengikuti prototype .rpin p1/p2/p3, lalu dilanjutkan untuk stop berikutnya.
private val PinSpots = listOf(
    0.10f to 0.47f, 0.43f to 0.24f, 0.72f to 0.51f, 0.88f to 0.22f, 0.28f to 0.78f, 0.58f to 0.80f,
)

/** Peta stilasi 150dp dengan lencana bernomor per stop (prototype .mapcard + .rpin). */
@Composable
internal fun RouteMapCard(stops: List<RouteStop>) {
    val c = BioTheme.colors
    val shape = RoundedCornerShape(20.dp)
    val transition = rememberInfiniteTransition(label = "routeMap")
    val pulse by transition.animateFloat(
        0f, 1f, infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Restart), label = "pulse",
    )
    val shown = stops.take(PinSpots.size)
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .height(150.dp)
            .clip(shape)
            .background(c.mapBase, shape)
            .border(1.dp, c.line, shape),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val step = 26.dp.toPx()
            val grid = 2.dp.toPx()
            var x = 0f
            while (x < size.width) { drawLine(c.mapGrid, Offset(x, 0f), Offset(x, size.height), strokeWidth = grid); x += step }
            var y = 0f
            while (y < size.height) { drawLine(c.mapGrid, Offset(0f, y), Offset(size.width, y), strokeWidth = grid); y += step }
            // garis putus-putus penghubung stop berurutan
            shown.zipWithNext().forEachIndexed { i, _ ->
                val a = PinSpots[i]
                val b = PinSpots[i + 1]
                drawLine(
                    color = c.primary.copy(alpha = 0.35f),
                    start = Offset(size.width * a.first, size.height * a.second),
                    end = Offset(size.width * b.first, size.height * b.second),
                    strokeWidth = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 7.dp.toPx())),
                )
            }
        }
        shown.forEachIndexed { i, stop ->
            val (fx, fy) = PinSpots[i]
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
