package id.biojelan.app.ui.agen

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import id.biojelan.app.core.formatLiter
import id.biojelan.app.core.formatRelativeDateTime
import id.biojelan.app.core.formatRupiah
import id.biojelan.app.data.remote.PickupStatusDto
import id.biojelan.app.ui.components.CircleIconButton
import id.biojelan.app.ui.components.ErrorBlock
import id.biojelan.app.ui.components.NoteBox
import id.biojelan.app.ui.components.NoteTone
import id.biojelan.app.ui.components.PickupTimeline
import id.biojelan.app.ui.components.ScreenPad
import id.biojelan.app.ui.components.ScreenTopBar
import id.biojelan.app.ui.components.bioCard
import id.biojelan.app.ui.icons.BioIcons
import id.biojelan.app.ui.strings.BioStrings
import id.biojelan.app.ui.strings.BioText
import id.biojelan.app.ui.theme.BioTheme

/**
 * Tab "Pickup" Agen: kartu status (ID + judul status), kartu terima/tolak bila Driver menunggu jawaban Agen,
 * timeline lima langkah yang sama dengan app Driver ([PickupTimeline]), lalu satu kartu info.
 *
 * Yang TIDAK ada karena API Agen hanya mengirim `pickup_id`, `status`, `updated_at`:
 * nama Driver & kendaraan, jadwal, dan estimasi tiba.
 */
@Composable
fun AgenPickupTab(state: AgenUiState, vm: AgenViewModel) {
    val c = BioTheme.colors
    val s = BioText.current
    val pickup = state.pickup

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
        ScreenTopBar(s.pickupTitle, actions = {
            CircleIconButton(BioIcons.Refresh, onClick = vm::manualRefresh, contentDescription = s.reload, loading = state.loading)
        })
        Column(Modifier.padding(horizontal = ScreenPad)) {
            if (pickup == null) {
                when {
                    state.pickupLoading -> Row(
                        Modifier.fillMaxWidth().bioCard(16.dp).padding(20.dp),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        CircularProgressIndicator(color = c.primary, strokeWidth = 3.dp, modifier = Modifier.size(20.dp))
                    }
                    // Gagal memuat ≠ tidak ada penjemputan: jangan tampilkan "belum dijadwalkan".
                    state.pickupError != null -> ErrorBlock(state.pickupError, vm::refresh)
                    else -> NoteBox(s.pickupNoneScheduled, icon = BioIcons.Truck)
                }
                return@Column
            }

            val step = agenPickupStep(pickup, state.driverTransactions)
            PickupHero(pickup, step, s)

            // Permintaan Driver langsung di bawah status: ini satu-satunya hal yang menunggu tindakan Agen.
            if (step == AgenPickupStep.AwaitingConfirm) {
                state.driverPendingTx?.let { tx ->
                    Spacer(Modifier.height(14.dp))
                    DriverRequestCard(
                        tx = tx,
                        busy = state.busyDriverTxId == tx.transactionId,
                        onAccept = { vm.acceptDriverTx(tx.transactionId) },
                        onReject = { vm.rejectDriverTx(tx.transactionId) },
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            if (step == AgenPickupStep.Cancelled) {
                NoteBox(s.pickupCancelledNote, tone = NoteTone.Rust, icon = BioIcons.Alert)
            } else if (step != AgenPickupStep.Unknown) {
                PickupTimeline(step.stepperIndex(), stepNote(step, s))
            }

            Spacer(Modifier.height(14.dp))
            val volume = requestedPickupVolume(pickup, state.driverTransactions)
            val total = if (volume != null) latestPickupTransaction(pickup, state.driverTransactions)?.totalPrice else null
            PickupInfoCard(
                buildList {
                    add(s.pickupInfoVolume to (volume?.let { formatLiter(it) + " " + s.pickupInfoVolumeValue } ?: s.driverStopVolumeNone))
                    if (total != null) add(s.total to formatRupiah(total))
                    add(s.pickupInfoUpdated to formatRelativeDateTime(pickup.updatedAt))
                },
            )
        }
    }
}

/** Penjelasan singkat di bawah langkah aktif timeline. */
private fun stepNote(step: AgenPickupStep, s: BioStrings): String? = when (step) {
    AgenPickupStep.Assigned -> s.pickupNoteAssigned
    AgenPickupStep.OnTheWay -> s.pickupNoteOtw
    AgenPickupStep.Arrived -> s.pickupNoteArrived
    AgenPickupStep.AwaitingConfirm -> s.pickupAwaitingYouNote
    AgenPickupStep.Completed -> s.pickupNoteCompleted
    AgenPickupStep.Cancelled, AgenPickupStep.Unknown -> null
}

/** Judul besar kartu status. */
private fun stepHeadline(step: AgenPickupStep, raw: String, s: BioStrings): String = when (step) {
    AgenPickupStep.Assigned -> s.pickupStatusAssigned
    AgenPickupStep.OnTheWay -> s.pickupStatusOtw
    AgenPickupStep.Arrived -> s.pickupStatusArrived
    AgenPickupStep.AwaitingConfirm -> s.stepAwaiting
    AgenPickupStep.Completed -> s.pickupStatusCompleted
    AgenPickupStep.Cancelled -> s.pickupStatusCancelled
    AgenPickupStep.Unknown -> raw
}

/** Kartu status: ID Pickup (kecil) di atas, judul status besar, waktu pembaruan. Selesai diberi lencana centang. */
@Composable
private fun PickupHero(pickup: PickupStatusDto, step: AgenPickupStep, s: BioStrings) {
    val c = BioTheme.colors
    Row(
        Modifier.fillMaxWidth().bioCard(22.dp).padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (step == AgenPickupStep.Completed) {
            Box(Modifier.size(46.dp).background(c.primary, CircleShape), contentAlignment = Alignment.Center) {
                Icon(BioIcons.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
            }
        }
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(s.pickupIdLabel, style = BioTheme.type.small, color = c.muted)
                Text(pickup.pickupId, style = BioTheme.type.monoSmall, color = c.ink, maxLines = 1)
            }
            Spacer(Modifier.height(6.dp))
            Text(stepHeadline(step, pickup.status, s), style = BioTheme.type.headline, color = c.ink)
            Spacer(Modifier.height(2.dp))
            Text(s.pickupUpdatedAt(formatRelativeDateTime(pickup.updatedAt)), style = BioTheme.type.small, color = c.muted)
        }
    }
}

/** Satu kartu info: baris label (kiri) + nilai (kanan), dipisah garis tipis. */
@Composable
private fun PickupInfoCard(rows: List<Pair<String, String>>) {
    val c = BioTheme.colors
    Column(Modifier.fillMaxWidth().bioCard(18.dp)) {
        rows.forEachIndexed { i, (label, value) ->
            if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(c.line))
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(label, style = BioTheme.type.small, color = c.muted, modifier = Modifier.weight(1f))
                Text(value, style = BioTheme.type.label, color = c.ink)
            }
        }
    }
}
