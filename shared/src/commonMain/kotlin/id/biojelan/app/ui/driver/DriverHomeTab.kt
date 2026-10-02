package id.biojelan.app.ui.driver

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import id.biojelan.app.core.firstNameOf
import id.biojelan.app.core.formatLiter
import id.biojelan.app.core.formatNumber
import id.biojelan.app.core.formatRelativeDateTime
import id.biojelan.app.core.formatRupiah
import id.biojelan.app.core.formatRupiahCompact
import id.biojelan.app.core.initialsOf
import id.biojelan.app.data.remote.PickupStatusDto
import id.biojelan.app.data.repository.PickupStatus
import id.biojelan.app.data.repository.pickupStatus
import id.biojelan.app.data.repository.txStatus
import id.biojelan.app.ui.chipKind
import id.biojelan.app.ui.components.BioButton
import id.biojelan.app.ui.components.BioChip
import id.biojelan.app.ui.components.BtnStyle
import id.biojelan.app.ui.components.rememberHeldLoading
import id.biojelan.app.ui.components.ChipKind
import id.biojelan.app.ui.components.ErrorBlock
import id.biojelan.app.ui.components.FadeInItem
import id.biojelan.app.ui.components.HomeHeader
import id.biojelan.app.ui.components.HomeScroll
import id.biojelan.app.ui.components.NoteBox
import id.biojelan.app.ui.components.ScreenPad
import id.biojelan.app.ui.components.SectionHead
import id.biojelan.app.ui.components.StatCard
import id.biojelan.app.ui.components.TxRow
import id.biojelan.app.ui.components.bioCard
import id.biojelan.app.ui.driverLabel
import id.biojelan.app.ui.icons.BioIcons
import id.biojelan.app.ui.strings.BioText
import id.biojelan.app.ui.theme.BioTheme

/** Beranda Driver: penugasan jemput dari Kilang (pickup.md), statistik hari ini, dan transaksi terbaru. */
@Composable
fun DriverHomeTab(
    state: DriverUiState,
    name: String,
    vm: DriverViewModel,
    onGoTo: (Int) -> Unit,
    onNewTransaction: () -> Unit,
) {
    val c = BioTheme.colors
    val s = BioText.current
    val firstName = firstNameOf(name, s.roleDriver)

    HomeScroll {
        HomeHeader(name = firstName, initials = initialsOf(name), role = s.roleDriver)

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard(state.todayCount.toString(), s.todayLabel, Modifier.weight(1f))
            StatCard(formatNumber(state.todayLiters, 1) + " L", s.volumeToday, Modifier.weight(1f))
            StatCard(formatRupiahCompact(state.todayValue), s.valueToday, Modifier.weight(1f))
        }

        // GPS badge — matches HTML .gps-badge
        GpsBadge(s.driverGpsBadge)

        // Route summary card — matches HTML .route-summary (gradient card)
        val pickup = state.pickup
        if (pickup != null) {
            Spacer(Modifier.height(14.dp))
            RouteSummaryCard(
                pickupId = pickup.pickupId,
                estVolume = formatNumber(state.todayLiters, 1) + " L",
            )
        }

        SectionHead(s.driverPickupSection)
        DriverPickupCard(
            pickup = state.pickup,
            loading = state.pickupLoading,
            error = state.pickupError,
            busy = state.pickupBusy,
            onRetry = vm::refresh,
            onUpdate = vm::updatePickup,
            onRecord = onNewTransaction,
            onHistory = { onGoTo(1) },
        )

        SectionHead(s.recentActivity, action = s.seeAll, onAction = { onGoTo(1) })
        when {
            state.loading && state.transactions.isEmpty() -> Row(
                Modifier.fillMaxWidth().padding(24.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator(color = c.primary, strokeWidth = 3.dp, modifier = Modifier.size(24.dp))
            }
            state.error != null && state.transactions.isEmpty() -> ErrorBlock(state.error, onRetry = vm::refresh)
            state.transactions.isEmpty() -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                GhostRows()
                Text(s.driverNoTransactionsHint, style = BioTheme.type.small, color = c.muted)
            }
            else -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                state.transactions.take(3).forEachIndexed { idx, tx ->
                    FadeInItem(idx) {
                        val agen = tx.agenName.ifBlank { "Agen" }
                        TxRow(
                            avatar = initialsOf(agen),
                            title = agen,
                            subtitle = formatRelativeDateTime(tx.createdAt) + " · " + formatLiter(tx.volumeLiter),
                            amount = formatRupiah(tx.totalPrice),
                            statusText = tx.txStatus.driverLabel(asAgen = false),
                            statusKind = tx.txStatus.chipKind(),
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(18.dp))
        BioButton(
            s.reload, vm::manualRefresh, Modifier.fillMaxWidth(),
            style = BtnStyle.Outline,
            loading = rememberHeldLoading(state.loading),
            icon = BioIcons.Refresh,
        )
    }
}

/** Penugasan jemput: tampilkan status dan tombol ubah status (ASSIGNED → OTW → COMPLETED, atau CANCELLED). */
@Composable
private fun DriverPickupCard(
    pickup: PickupStatusDto?,
    loading: Boolean,
    error: String?,
    busy: Boolean,
    onRetry: () -> Unit,
    onUpdate: (PickupStatus) -> Unit,
    onRecord: () -> Unit,
    onHistory: () -> Unit,
) {
    val c = BioTheme.colors
    val s = BioText.current
    var confirmCancel by remember { mutableStateOf(false) }

    if (pickup == null) {
        when {
            loading -> Row(
                Modifier.fillMaxWidth().bioCard(16.dp).padding(20.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator(color = c.primary, strokeWidth = 3.dp, modifier = Modifier.size(20.dp))
            }
            error != null -> ErrorBlock(error, onRetry)
            else -> DriverIdleState(onRecord = onRecord, onHistory = onHistory)
        }
        return
    }

    val status = pickup.pickupStatus
    val (title, note, kind) = when (status) {
        PickupStatus.Assigned -> Triple(s.driverPickupAssigned, s.driverPickupNoteAssigned, ChipKind.Pending)
        PickupStatus.OnTheWay -> Triple(s.driverPickupOtw, s.driverPickupNoteOtw, ChipKind.Pending)
        PickupStatus.Arrived -> Triple(s.driverPickupArrived, s.driverPickupNoteArrived, ChipKind.Pending)
        PickupStatus.Completed -> Triple(s.driverPickupCompleted, s.driverPickupNoteCompleted, ChipKind.Done)
        PickupStatus.Cancelled -> Triple(s.driverPickupCancelled, "", ChipKind.Cancelled)
        PickupStatus.Unknown -> Triple(pickup.status, "", ChipKind.Neutral)
    }
    val (iconBg, iconFg) = when (kind) {
        ChipKind.Done -> c.primaryTint to c.primary
        ChipKind.Cancelled -> c.rustTint to c.rust
        ChipKind.Pending -> c.amberTint to c.amberText
        else -> c.line to c.inkSoft
    }

    Column(Modifier.fillMaxWidth().bioCard(16.dp).padding(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(40.dp).background(iconBg, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                Icon(BioIcons.Truck, contentDescription = null, tint = iconFg, modifier = Modifier.size(20.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = BioTheme.type.cardTitle, color = c.ink)
                if (note.isNotBlank()) Text(note, style = BioTheme.type.small, color = c.muted)
            }
        }
        Spacer(Modifier.height(10.dp))
        BioChip(s.pickupUpdatedAt(formatRelativeDateTime(pickup.updatedAt)), ChipKind.Neutral)

        // Status akhir (COMPLETED/CANCELLED) tidak bisa diubah lagi. Backend tidak memvalidasi urutan
        // transisi — urutan ASSIGNED → OTW → ARRIVED → COMPLETED di bawah murni konvensi UI.
        val isActive = status == PickupStatus.Assigned || status == PickupStatus.OnTheWay || status == PickupStatus.Arrived
        if (isActive) {
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                BioButton(
                    if (confirmCancel) s.yesCancel else s.cancel,
                    onClick = {
                        if (confirmCancel) {
                            confirmCancel = false
                            onUpdate(PickupStatus.Cancelled)
                        } else {
                            confirmCancel = true
                        }
                    },
                    modifier = Modifier.weight(1f),
                    style = BtnStyle.Rust,
                    enabled = !busy,
                )
                when (status) {
                    PickupStatus.Assigned -> BioButton(
                        s.driverPickupStart, { onUpdate(PickupStatus.OnTheWay) }, Modifier.weight(1f),
                        loading = busy, icon = BioIcons.Truck,
                    )
                    PickupStatus.OnTheWay -> BioButton(
                        s.driverPickupArrive, { onUpdate(PickupStatus.Arrived) }, Modifier.weight(1f),
                        loading = busy, icon = BioIcons.Pin,
                    )
                    else -> BioButton(
                        s.driverPickupFinish, { onUpdate(PickupStatus.Completed) }, Modifier.weight(1f),
                        loading = busy, icon = BioIcons.Check,
                    )
                }
            }
        }
    }
}

/** Kondisi "belum ada penugasan": adegan peta beranimasi + dua aksi cepat (tanpa blok teks panjang). */
@Composable
private fun DriverIdleState(onRecord: () -> Unit, onHistory: () -> Unit) {
    val s = BioText.current
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        DriverIdleScene(title = s.driverIdleTitle, liveText = s.driverIdleLive)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BioButton(s.driverRecordButton, onRecord, Modifier.weight(1f), icon = BioIcons.Plus)
            BioButton(s.driverIdleHistory, onHistory, Modifier.weight(1f), style = BtnStyle.Outline, icon = BioIcons.Receipt)
        }
    }
}

/** Placeholder baris transaksi (kerangka berkedip) saat belum ada riwayat, supaya daftar tidak kosong melompong. */
@Composable
private fun GhostRows(count: Int = 2) {
    val c = BioTheme.colors
    val transition = rememberInfiniteTransition(label = "ghost")
    val alpha by transition.animateFloat(
        0.45f, 1f, infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "ghostAlpha",
    )
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        repeat(count) {
            Row(
                Modifier.fillMaxWidth().bioCard(16.dp).padding(14.dp).graphicsLayer(alpha = alpha),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(Modifier.size(44.dp).background(c.line, RoundedCornerShape(14.dp)))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.fillMaxWidth(0.45f).height(11.dp).background(c.line, RoundedCornerShape(6.dp)))
                    Box(Modifier.fillMaxWidth(0.7f).height(9.dp).background(c.line.copy(alpha = 0.7f), RoundedCornerShape(6.dp)))
                }
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.size(width = 58.dp, height = 11.dp).background(c.line, RoundedCornerShape(6.dp)))
                    Box(Modifier.size(width = 44.dp, height = 16.dp).background(c.primaryTint, RoundedCornerShape(8.dp)))
                }
            }
        }
    }
}

/** GPS badge — matches HTML .gps-badge: pulsing dot + checkpoint text. */
@Composable
private fun GpsBadge(text: String) {
    val c = BioTheme.colors
    // Pulse animation matching HTML @keyframes pulse: scale 0.6→1.8, opacity 0.6→0
    val infiniteTransition = rememberInfiniteTransition()
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
    )
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
            .bioCard(14.dp)
            .padding(horizontal = 13.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // GPS dot with pulsing ring
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(19.dp)) {
            // Pulsing ring behind the dot
            Box(
                Modifier
                    .size(19.dp)
                    .graphicsLayer(scaleX = pulseScale, scaleY = pulseScale, alpha = pulseAlpha)
                    .border(2.dp, c.primary, RoundedCornerShape(50)),
            )
            // Static dot
            Box(Modifier.size(9.dp).background(c.primary, RoundedCornerShape(50)))
        }
        Text(text, style = BioTheme.type.small, color = c.inkSoft)
    }
}

/**
 * Route summary card — matches HTML .route-summary: dark gradient background with pickup ID and
 * estimated volume, mimicking the prototype's visual style.
 */
@Composable
private fun RouteSummaryCard(pickupId: String, estVolume: String) {
    val c = BioTheme.colors
    val s = BioText.current
    Column(
        Modifier
            .fillMaxWidth()
            .background(
                brush = androidx.compose.ui.graphics.Brush.linearGradient(
                    colors = listOf(c.primary, c.primaryDeep),
                ),
                shape = RoundedCornerShape(20.dp),
            )
            .padding(18.dp),
    ) {
        Text(
            "ID " + pickupId,
            style = BioTheme.type.small,
            color = c.onPrimary.copy(alpha = .75f),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            s.driverRouteToday,
            style = BioTheme.type.headline,
            color = c.onPrimary,
        )
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            Column {
                Text(estVolume, style = BioTheme.type.display, color = c.onPrimary)
                Text(
                    s.driverEstVolume,
                    style = BioTheme.type.small,
                    color = c.onPrimary.copy(alpha = .7f),
                )
            }
        }
    }
}