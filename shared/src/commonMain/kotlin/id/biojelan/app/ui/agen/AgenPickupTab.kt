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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.biojelan.app.core.formatLiter
import id.biojelan.app.core.formatRelativeDateTime
import id.biojelan.app.data.remote.PickupStatusDto
import id.biojelan.app.ui.components.BioChip
import id.biojelan.app.ui.components.ChipKind
import id.biojelan.app.ui.components.CircleIconButton
import id.biojelan.app.ui.components.ErrorBlock
import id.biojelan.app.ui.components.NoteBox
import id.biojelan.app.ui.components.NoteTone
import id.biojelan.app.ui.components.ScreenPad
import id.biojelan.app.ui.components.ScreenTopBar
import id.biojelan.app.ui.components.bioCard
import id.biojelan.app.ui.icons.BioIcons
import id.biojelan.app.ui.strings.BioStrings
import id.biojelan.app.ui.strings.BioText
import id.biojelan.app.ui.theme.BioTheme

/**
 * Tab "Pickup" Agen (prototype #screen-agen-pickup, "Info Pickup"): kartu ID + status dengan stepper lima
 * langkah, lalu info pickup. Kalau Driver sudah mencatat pengambilan dan menunggu Agen, kartu terima/tolak
 * ditampilkan di sini juga (sama dengan yang ada di Beranda).
 *
 * Yang TIDAK ada di prototype ini karena API Agen hanya mengirim `pickup_id`, `status`, `updated_at`:
 * nama Driver & kendaraan, jadwal, estimasi volume, dan daftar Agen dalam satu rute.
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

            Spacer(Modifier.height(14.dp))
            val (note, tone, icon) = stepNote(step, s)
            if (note.isNotBlank()) NoteBox(note, tone = tone, icon = icon)

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
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                PickupInfoItem(BioIcons.Clock, s.pickupInfoUpdated, formatRelativeDateTime(pickup.updatedAt))
                requestedPickupVolume(pickup, state.driverTransactions)?.let { liters ->
                    PickupInfoItem(BioIcons.Jerrycan, s.pickupInfoVolume, formatLiter(liters) + " " + s.pickupInfoVolumeValue)
                }
            }
        }
    }
}

private fun stepNote(step: AgenPickupStep, s: BioStrings): Triple<String, NoteTone, ImageVector> = when (step) {
    AgenPickupStep.Assigned -> Triple(s.pickupNoteAssigned, NoteTone.Amber, BioIcons.Truck)
    AgenPickupStep.OnTheWay -> Triple(s.pickupNoteOtw, NoteTone.Amber, BioIcons.Truck)
    AgenPickupStep.Arrived -> Triple(s.pickupNoteArrived, NoteTone.Amber, BioIcons.Truck)
    AgenPickupStep.AwaitingConfirm -> Triple(s.pickupAwaitingYouNote, NoteTone.Amber, BioIcons.Clock)
    AgenPickupStep.Completed -> Triple(s.pickupNoteCompleted, NoteTone.Neutral, BioIcons.Check)
    AgenPickupStep.Cancelled -> Triple(s.pickupCancelledNote, NoteTone.Rust, BioIcons.Alert)
    AgenPickupStep.Unknown -> Triple("", NoteTone.Amber, BioIcons.Info)
}

private fun stepChip(step: AgenPickupStep, raw: String, s: BioStrings): Pair<String, ChipKind> = when (step) {
    AgenPickupStep.Assigned -> s.pickupStatusAssigned to ChipKind.Pending
    AgenPickupStep.OnTheWay -> s.pickupStatusOtw to ChipKind.Pending
    AgenPickupStep.Arrived -> s.pickupStatusArrived to ChipKind.Pending
    AgenPickupStep.AwaitingConfirm -> s.stepAwaiting to ChipKind.Pending
    AgenPickupStep.Completed -> s.pickupStatusCompleted to ChipKind.Done
    AgenPickupStep.Cancelled -> s.pickupStatusCancelled to ChipKind.Cancelled
    AgenPickupStep.Unknown -> raw to ChipKind.Neutral
}

/** Kartu hero (prototype .pickup-hero): ID Pickup + chip status di atas, stepper di bawah. */
@Composable
private fun PickupHero(pickup: PickupStatusDto, step: AgenPickupStep, s: BioStrings) {
    val c = BioTheme.colors
    val (chipText, chipKind) = stepChip(step, pickup.status, s)
    Column(Modifier.fillMaxWidth().bioCard(20.dp).padding(18.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(s.pickupIdLabel, style = BioTheme.type.small, color = c.muted)
                Text(pickup.pickupId, style = BioTheme.type.monoSmall, color = c.ink, maxLines = 1)
            }
            BioChip(chipText, chipKind)
        }
        Spacer(Modifier.height(16.dp))
        PickupStepper(step.stepperIndex())
    }
}

/** Stepper lima langkah, tampilan sama dengan stepper stop di app Driver. */
@Composable
private fun PickupStepper(idx: Int) {
    val c = BioTheme.colors
    val s = BioText.current
    val steps = listOf(s.stepAssigned, s.stepOnTheWay, s.stepArrived, s.stepAwaiting, s.stepCompleted)
    Row(
        Modifier
            .fillMaxWidth()
            .drawBehind {
                val cell = size.width / steps.size
                for (i in 1 until steps.size) {
                    drawLine(
                        if (i < idx) c.primary else c.line,
                        Offset(cell * (i - 0.5f), 13.dp.toPx()),
                        Offset(cell * (i + 0.5f), 13.dp.toPx()),
                        strokeWidth = 2.dp.toPx(),
                    )
                }
            },
    ) {
        steps.forEachIndexed { i, label ->
            val done = i < idx
            val now = i == idx
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier
                        .size(26.dp)
                        .drawBehind { if (now) drawCircle(c.amberTint, radius = 17.dp.toPx()) }
                        .background(
                            when {
                                done -> c.primary
                                now -> c.amberDeep
                                else -> c.line
                            },
                            RoundedCornerShape(50),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (done) {
                        Icon(BioIcons.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                    } else {
                        Text(
                            (i + 1).toString(),
                            style = BioTheme.type.chip.copy(fontSize = 10.sp),
                            color = if (now) Color.White else c.muted,
                        )
                    }
                }
                Text(
                    label,
                    style = BioTheme.type.caption.copy(fontSize = 8.5.sp, lineHeight = 11.sp),
                    color = if (done || now) c.ink else c.muted,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    modifier = Modifier.padding(top = 6.dp, start = 2.dp, end = 2.dp),
                )
            }
        }
    }
}

/** Satu baris info (prototype .info-item): ikon hijau, label kecil, nilai tebal. */
@Composable
private fun PickupInfoItem(icon: ImageVector, label: String, value: String) {
    val c = BioTheme.colors
    Row(
        Modifier.fillMaxWidth().bioCard(14.dp).padding(horizontal = 13.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(icon, contentDescription = null, tint = c.primary, modifier = Modifier.padding(top = 1.dp).size(16.dp))
        Column {
            Text(label, style = BioTheme.type.caption, color = c.muted)
            Text(value, style = BioTheme.type.label, color = c.ink)
        }
    }
}
