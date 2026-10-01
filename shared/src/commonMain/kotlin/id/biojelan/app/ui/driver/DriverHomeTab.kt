package id.biojelan.app.ui.driver

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import id.biojelan.app.core.formatLiter
import id.biojelan.app.core.formatNumber
import id.biojelan.app.core.formatRelativeDateTime
import id.biojelan.app.core.formatRupiah
import id.biojelan.app.core.formatRupiahCompact
import id.biojelan.app.core.greeting
import id.biojelan.app.core.initialsOf
import id.biojelan.app.data.remote.PickupStatusDto
import id.biojelan.app.data.repository.PickupStatus
import id.biojelan.app.data.repository.pickupStatus
import id.biojelan.app.data.repository.txStatus
import id.biojelan.app.ui.chipKind
import id.biojelan.app.ui.components.BioButton
import id.biojelan.app.ui.components.BioChip
import id.biojelan.app.ui.components.BtnStyle
import id.biojelan.app.ui.components.ChipKind
import id.biojelan.app.ui.components.CircleIconButton
import id.biojelan.app.ui.components.ErrorBlock
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
    onNewTransaction: () -> Unit,
    onGoTo: (Int) -> Unit,
) {
    val c = BioTheme.colors
    val s = BioText.current
    val firstName = name.trim().substringBefore(' ').ifBlank { s.roleDriver }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = ScreenPad).padding(bottom = 24.dp),
    ) {
        Row(Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(greeting() + ",", style = BioTheme.type.body, color = c.muted)
                Text(firstName, style = BioTheme.type.headline, color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            CircleIconButton(BioIcons.Refresh, onClick = vm::refresh, contentDescription = s.reload)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard(state.todayCount.toString(), s.todayLabel, Modifier.weight(1f))
            StatCard(formatNumber(state.todayLiters, 1) + " L", s.volumeToday, Modifier.weight(1f))
            StatCard(formatRupiahCompact(state.todayValue), s.valueToday, Modifier.weight(1f))
        }

        SectionHead(s.driverPickupSection)
        DriverPickupCard(
            pickup = state.pickup,
            loading = state.pickupLoading,
            error = state.pickupError,
            busy = state.pickupBusy,
            onRetry = vm::refresh,
            onUpdate = vm::updatePickup,
        )

        Spacer(Modifier.height(16.dp))
        BioButton(s.driverNewTransactionTitle, onNewTransaction, Modifier.fillMaxWidth(), icon = BioIcons.Plus)

        SectionHead(s.recentActivity, action = s.seeAll, onAction = { onGoTo(1) })
        when {
            state.loading && state.transactions.isEmpty() -> Row(
                Modifier.fillMaxWidth().padding(24.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator(color = c.primary, strokeWidth = 3.dp, modifier = Modifier.size(24.dp))
            }
            state.error != null && state.transactions.isEmpty() -> ErrorBlock(state.error, onRetry = vm::refresh)
            state.transactions.isEmpty() -> NoteBox(s.driverNoTransactionsHint)
            else -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                state.transactions.take(3).forEach { tx ->
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
            else -> NoteBox(s.driverPickupNone, icon = BioIcons.Truck)
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
