package id.biojelan.app.ui.driver

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.biojelan.app.core.formatLiter
import id.biojelan.app.core.initialsOf
import id.biojelan.app.core.googleMapsDirectionsUrl
import id.biojelan.app.data.remote.AgenSummaryDto
import id.biojelan.app.data.repository.PickupStatus
import id.biojelan.app.ui.components.AvatarBox
import id.biojelan.app.ui.components.BioButton
import id.biojelan.app.ui.components.BioSheet
import id.biojelan.app.ui.components.BtnStyle
import id.biojelan.app.ui.components.InfoItem
import id.biojelan.app.ui.components.NoteBox
import id.biojelan.app.ui.components.NoteTone
import id.biojelan.app.ui.components.ScreenPad
import id.biojelan.app.ui.components.SubHeader
import id.biojelan.app.ui.components.bioCard
import id.biojelan.app.ui.icons.BioIcons
import id.biojelan.app.ui.strings.BioText
import id.biojelan.app.ui.theme.BioTheme

/**
 * Detail satu stop (prototype #screen-driver-stop): hero Agen + tombol telepon, stepper, info, tombol aksi,
 * dan tautan "lewati stop". Hanya stop dari pickup aktif yang bisa diubah statusnya.
 *
 * Setelah Driver tiba, tombol utama membuka form catat pengambilan (transaksi ke Agen). Sesudahnya stop
 * berstatus "Menunggu Konfirmasi" sampai Agen menerima — tidak ada langkah "Checked" di app Driver.
 */
@Composable
internal fun DriverStopDetail(
    index: Int,
    stop: RouteStop,
    agen: AgenSummaryDto?,
    busy: Boolean,
    onBack: () -> Unit,
    onUpdate: (PickupStatus) -> Unit,
    onRecord: (agenContact: String) -> Unit,
) {
    val c = BioTheme.colors
    val s = BioText.current
    val uriHandler = LocalUriHandler.current
    var confirmSkip by remember { mutableStateOf(false) }
    val name = stop.name.ifBlank { agen?.name.orEmpty() }.ifBlank { s.driverStopUnknownAgen }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
        SubHeader(s.driverStopLabel(index + 1), onBack)

        // Hero Agen
        Row(
            Modifier.padding(horizontal = ScreenPad).padding(top = 6.dp).fillMaxWidth().bioCard(18.dp).padding(15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            AvatarBox(initialsOf(name), size = 46.dp, tone = NoteTone.Amber)
            Column(Modifier.weight(1f)) {
                Text(name, style = BioTheme.type.cardTitle, color = c.ink)
                if (!agen?.address.isNullOrBlank()) {
                    Text(agen?.address.orEmpty(), style = BioTheme.type.small, color = c.muted)
                }
            }
            if (!agen?.phone.isNullOrBlank()) {
                Box(
                    Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(50))
                        .background(c.primaryTint, RoundedCornerShape(50))
                        .clickable { uriHandler.openUri("tel:" + agen?.phone.orEmpty().filter { it.isDigit() || it == '+' }) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(BioIcons.Phone, contentDescription = s.driverCallAgen, tint = c.primary, modifier = Modifier.size(17.dp))
                }
            }
        }

        StopStepper(stop.status, Modifier.padding(horizontal = ScreenPad).padding(top = 18.dp, bottom = 4.dp))

        // Info
        Column(
            Modifier.padding(horizontal = ScreenPad).padding(top = 14.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            InfoItem(
                BioIcons.Drop,
                s.driverStopVolume,
                stop.volumeLiter?.let { formatLiter(it) } ?: s.driverStopVolumeNone,
                Modifier.bioCard(14.dp),
            )
            if (agen != null && (agen.address.isNotBlank() || agen.latitude != 0.0 || agen.longitude != 0.0)) {
                InfoItem(
                    BioIcons.Pin,
                    s.driverStopAddress,
                    agen.address.ifBlank { s.driverOpenMaps },
                    Modifier.bioCard(14.dp),
                    onClick = { uriHandler.openUri(googleMapsDirectionsUrl(agen.latitude, agen.longitude, agen.address)) },
                )
            }
            if (agen != null && agen.openAt.isNotBlank() && agen.closeAt.isNotBlank()) {
                InfoItem(BioIcons.Clock, s.driverStopHours, agen.openAt + " – " + agen.closeAt, Modifier.bioCard(14.dp))
            }
        }

        // Catatan status
        Column(Modifier.padding(horizontal = ScreenPad).padding(top = 14.dp)) {
            when (stop.status) {
                StopStatus.AwaitingConfirm -> NoteBox(s.driverAwaitingNote, tone = NoteTone.Amber, icon = BioIcons.Clock)
                StopStatus.Completed -> NoteBox(s.driverDoneNote, tone = NoteTone.Neutral, icon = BioIcons.Check)
                StopStatus.Rejected -> NoteBox(s.driverRejectedNote, tone = NoteTone.Rust, icon = BioIcons.Alert)
                else -> Unit
            }
        }

        // Aksi
        Column(Modifier.padding(horizontal = ScreenPad).padding(top = 18.dp)) {
            val actionable = stop.isPickup && (
                stop.status == StopStatus.Assigned || stop.status == StopStatus.OnTheWay || stop.status == StopStatus.Arrived
                )
            val label = when (stop.status) {
                StopStatus.Assigned -> s.driverActionStart
                StopStatus.OnTheWay -> s.driverActionArrive
                StopStatus.Arrived -> s.driverActionRecord
                StopStatus.AwaitingConfirm -> s.driverActionAwaiting
                StopStatus.Completed -> s.driverActionDone
                StopStatus.Skipped -> s.stopChipSkipped
                StopStatus.Rejected -> s.stopChipRejected
            }
            BioButton(
                label,
                onClick = {
                    when (stop.status) {
                        StopStatus.Assigned -> onUpdate(PickupStatus.OnTheWay)
                        StopStatus.OnTheWay -> onUpdate(PickupStatus.Arrived)
                        StopStatus.Arrived -> onRecord(agen?.phone.orEmpty())
                        else -> Unit
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = actionable,
                loading = busy && actionable && stop.status != StopStatus.Arrived,
                icon = when (stop.status) {
                    StopStatus.Assigned -> BioIcons.Truck
                    StopStatus.OnTheWay -> BioIcons.Pin
                    StopStatus.Arrived -> BioIcons.Plus
                    StopStatus.Completed -> BioIcons.Check
                    else -> null
                },
            )
            if (actionable) {
                Text(
                    s.driverSkipLink,
                    style = BioTheme.type.label,
                    color = c.rust,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { confirmSkip = true }
                        .padding(vertical = 12.dp),
                )
            }
        }
    }

    if (confirmSkip) {
        BioSheet(s.driverSkipTitle, onDismiss = { confirmSkip = false }) {
            Text(s.driverSkipBody, style = BioTheme.type.body, color = c.inkSoft)
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                BioButton(s.cancel, { confirmSkip = false }, Modifier.weight(1f), style = BtnStyle.Outline)
                BioButton(
                    s.driverSkipConfirm,
                    { confirmSkip = false; onUpdate(PickupStatus.Cancelled) },
                    Modifier.weight(1f),
                    style = BtnStyle.Rust,
                    loading = busy,
                )
            }
        }
    }
}

/**
 * Stepper lima langkah: Assigned → On the Way → Arrived → Menunggu Konfirmasi → Completed
 * (prototype .stepper, minus langkah "Checked"). Stop dilewati/ditolak tidak menyalakan langkah apa pun.
 */
@Composable
private fun StopStepper(status: StopStatus, modifier: Modifier = Modifier) {
    val c = BioTheme.colors
    val s = BioText.current
    val steps = listOf(s.stepAssigned, s.stepOnTheWay, s.stepArrived, s.stepAwaiting, s.stepCompleted)
    val idx = when (status) {
        StopStatus.Assigned -> 0
        StopStatus.OnTheWay -> 1
        StopStatus.Arrived -> 2
        StopStatus.AwaitingConfirm -> 3
        StopStatus.Completed -> steps.size // semua langkah sudah lewat
        StopStatus.Skipped, StopStatus.Rejected -> -1
    }
    Row(
        modifier
            .fillMaxWidth()
            .drawBehind {
                // Garis penghubung digambar sekali di belakang semua lingkaran (z-index seperti prototype).
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
            Column(
                Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
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
