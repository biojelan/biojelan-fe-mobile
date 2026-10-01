package id.biojelan.app.ui.agen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import androidx.compose.ui.unit.dp
import id.biojelan.app.core.formatDateTime
import id.biojelan.app.core.formatLiter
import id.biojelan.app.core.formatNumber
import id.biojelan.app.core.formatRelativeDateTime
import id.biojelan.app.core.formatRupiah
import id.biojelan.app.core.formatRupiahCompact
import id.biojelan.app.core.initialsOf
import id.biojelan.app.data.remote.DriverTransactionDto
import id.biojelan.app.data.remote.TransactionDto
import id.biojelan.app.data.repository.TxStatus
import id.biojelan.app.data.repository.txStatus
import id.biojelan.app.ui.Viewer
import id.biojelan.app.ui.chipKind
import id.biojelan.app.ui.components.BioButton
import id.biojelan.app.ui.components.SegmentedTabs
import id.biojelan.app.ui.components.BtnStyle
import id.biojelan.app.ui.components.BioField
import id.biojelan.app.ui.components.BioSheet
import id.biojelan.app.ui.components.CircleIconButton
import id.biojelan.app.ui.components.DetailRow
import id.biojelan.app.ui.components.EmptyBlock
import id.biojelan.app.ui.components.ErrorBlock
import id.biojelan.app.ui.components.LoadingBlock
import id.biojelan.app.ui.components.NoteBox
import id.biojelan.app.ui.components.NoteTone
import id.biojelan.app.ui.components.ScreenPad
import id.biojelan.app.ui.components.ScreenTopBar
import id.biojelan.app.ui.components.StatCard
import id.biojelan.app.ui.components.TxRow
import id.biojelan.app.ui.counterpartName
import id.biojelan.app.ui.driverLabel
import id.biojelan.app.ui.icons.BioIcons
import id.biojelan.app.ui.label
import id.biojelan.app.ui.strings.BioText
import id.biojelan.app.ui.theme.BioTheme

@Composable
fun AgenTransactionsTab(state: AgenUiState, vm: AgenViewModel, onNewTransaction: () -> Unit) {
    val c = BioTheme.colors
    val s = BioText.current
    var segment by rememberSaveable { mutableStateOf(0) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var selectedDriverId by remember { mutableStateOf<String?>(null) }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                ScreenTopBar(s.transactionsTitle, actions = {
                    CircleIconButton(BioIcons.Refresh, onClick = vm::refresh, contentDescription = s.reload)
                })
            }
            item {
                SegmentedTabs(
                    options = listOf(s.segmentClient, s.segmentDriver),
                    selected = segment,
                    onSelect = { segment = it },
                    modifier = Modifier.padding(horizontal = ScreenPad),
                )
            }
            if (segment == 0) {
                item {
                    Row(Modifier.padding(horizontal = ScreenPad), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatCard(state.todayCount.toString(), s.todayLabel, Modifier.weight(1f))
                        StatCard(formatNumber(state.todayLiters, 1) + " L", s.volumeToday, Modifier.weight(1f))
                        StatCard(formatRupiahCompact(state.todayValue), s.valueToday, Modifier.weight(1f))
                    }
                }
                when {
                    state.loading && state.transactions.isEmpty() -> item { LoadingBlock() }
                    state.error != null && state.transactions.isEmpty() -> item { ErrorBlock(state.error, vm::refresh) }
                    state.transactions.isEmpty() -> item {
                        EmptyBlock(BioIcons.Receipt, s.noTransactionsTitle, s.noTransactionsHint)
                    }
                    else -> items(state.transactions) { tx ->
                        val name = tx.counterpartName(Viewer.Agen)
                        TxRow(
                            avatar = initialsOf(name),
                            title = name,
                            subtitle = formatRelativeDateTime(tx.createdAt) + " · " + formatLiter(tx.volumeLiter),
                            amount = formatRupiah(tx.totalPrice),
                            statusText = tx.txStatus.label(Viewer.Agen),
                            statusKind = tx.txStatus.chipKind(),
                            modifier = Modifier.padding(horizontal = ScreenPad),
                            onClick = { selectedId = tx.transactionId },
                        )
                    }
                }
            } else {
                item {
                    Row(Modifier.padding(horizontal = ScreenPad), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatCard(state.todayDriverCount.toString(), s.todayLabel, Modifier.weight(1f))
                        StatCard(formatNumber(state.todayDriverLiters, 1) + " L", s.volumeToday, Modifier.weight(1f))
                        StatCard(formatRupiahCompact(state.todayDriverValue), s.valueToday, Modifier.weight(1f))
                    }
                }
                when {
                    state.driverTransactions.isEmpty() -> item {
                        EmptyBlock(BioIcons.Truck, s.noTransactionsTitle, s.agenDriverNoTransactionsHint)
                    }
                    else -> items(state.driverTransactions) { tx ->
                        // DriverTransactionDto tidak punya nama Driver (hanya driver_id), jadi tampilkan label peran.
                        TxRow(
                            avatar = initialsOf(s.roleDriver),
                            title = s.roleDriver,
                            subtitle = formatRelativeDateTime(tx.createdAt) + " · " + formatLiter(tx.volumeLiter),
                            amount = formatRupiah(tx.totalPrice),
                            statusText = tx.txStatus.driverLabel(asAgen = true),
                            statusKind = tx.txStatus.chipKind(),
                            modifier = Modifier.padding(horizontal = ScreenPad),
                            onClick = { selectedDriverId = tx.transactionId },
                        )
                    }
                }
            }
        }

        if (segment == 0) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 18.dp, bottom = 16.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(c.primary, RoundedCornerShape(18.dp))
                    .clickable(onClick = onNewTransaction)
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(BioIcons.Plus, contentDescription = null, tint = c.onPrimary, modifier = Modifier.size(18.dp))
                Text(s.transactionButton, style = BioTheme.type.button, color = c.onPrimary)
            }
        }
    }

    val selected = state.transactions.firstOrNull { it.transactionId == selectedId }
    if (selected != null) {
        AgenTxDetailSheet(
            tx = selected,
            busy = state.busyTxId == selected.transactionId,
            onRequestCancel = { vm.requestCancel(selected.transactionId) { selectedId = null } },
            onDismiss = { selectedId = null },
        )
    }

    val selectedDriver = state.driverTransactions.firstOrNull { it.transactionId == selectedDriverId }
    if (selectedDriver != null) {
        AgenDriverTxDetailSheet(
            tx = selectedDriver,
            busy = state.busyDriverTxId == selectedDriver.transactionId,
            onAccept = { vm.acceptDriverTx(selectedDriver.transactionId); selectedDriverId = null },
            onReject = { vm.rejectDriverTx(selectedDriver.transactionId); selectedDriverId = null },
            onAcceptCancel = { vm.acceptDriverCancel(selectedDriver.transactionId); selectedDriverId = null },
            onRejectCancel = { vm.rejectDriverCancel(selectedDriver.transactionId); selectedDriverId = null },
            onDismiss = { selectedDriverId = null },
        )
    }
}

/** Detail transaksi Driver → Agen (transaction_agen.md), dilihat dari sisi Agen. */
@Composable
private fun AgenDriverTxDetailSheet(
    tx: DriverTransactionDto,
    busy: Boolean,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    onAcceptCancel: () -> Unit,
    onRejectCancel: () -> Unit,
    onDismiss: () -> Unit,
) {
    val c = BioTheme.colors
    val s = BioText.current
    var confirmReject by remember { mutableStateOf(false) }
    BioSheet(s.transactionDetailTitle, onDismiss) {
        DetailRow(s.labelTransactionId, tx.transactionId, mono = true)
        DetailRow(s.labelDate, formatDateTime(tx.createdAt))
        DetailRow(s.labelDriver, s.roleDriver)
        DetailRow(s.labelVolume, formatLiter(tx.volumeLiter))
        DetailRow(s.labelPricePerLiter, formatRupiah(tx.price))
        DetailRow(s.labelTotal, formatRupiah(tx.totalPrice), mono = true, valueColor = c.primary)
        if (tx.transactionNote.isNotBlank()) DetailRow(s.labelNote, tx.transactionNote)
        DetailRow(
            s.labelStatus, tx.txStatus.driverLabel(asAgen = true), last = true,
            valueColor = when (tx.txStatus) {
                TxStatus.Cancelled, TxStatus.Rejected -> c.rust
                TxStatus.Pending, TxStatus.CancelRequested -> c.amberDeep
                else -> c.primary
            },
        )
        Spacer(Modifier.height(14.dp))
        when (tx.txStatus) {
            TxStatus.Pending -> NoteBox(s.pendingDriverAgenNote, tone = NoteTone.Amber)
            TxStatus.CancelRequested -> NoteBox(s.cancelRequestedDriverAgenNote, tone = NoteTone.Amber)
            TxStatus.Rejected -> NoteBox(s.rejectedDriverAgenNote, tone = NoteTone.Rust, icon = BioIcons.Alert)
            TxStatus.Cancelled -> NoteBox(s.cancelledDriverAgenNote, tone = NoteTone.Rust, icon = BioIcons.Alert)
            else -> Unit
        }
        when (tx.txStatus) {
            TxStatus.Pending -> {
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    BioButton(
                        if (confirmReject) s.yesReject else s.reject,
                        onClick = { if (confirmReject) onReject() else confirmReject = true },
                        modifier = Modifier.weight(1f),
                        style = BtnStyle.Rust,
                        enabled = !busy,
                    )
                    BioButton(s.accept, onAccept, Modifier.weight(1f), loading = busy, icon = BioIcons.Check)
                }
                if (confirmReject) {
                    Spacer(Modifier.height(8.dp))
                    Text(s.rejectNotUndoable, style = BioTheme.type.small, color = c.rust)
                }
            }
            TxStatus.CancelRequested -> {
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    BioButton(s.rejectCancel, onRejectCancel, Modifier.weight(1f), style = BtnStyle.Outline, enabled = !busy)
                    BioButton(s.acceptCancel, onAcceptCancel, Modifier.weight(1f), style = BtnStyle.Rust, loading = busy, icon = BioIcons.Check)
                }
            }
            else -> Unit
        }
    }
}

@Composable
private fun AgenTxDetailSheet(
    tx: TransactionDto,
    busy: Boolean,
    onRequestCancel: () -> Unit,
    onDismiss: () -> Unit,
) {
    val c = BioTheme.colors
    val s = BioText.current
    var confirmCancel by remember { mutableStateOf(false) }
    BioSheet(s.transactionDetailTitle, onDismiss) {
        DetailRow(s.labelTransactionId, tx.transactionId, mono = true)
        DetailRow(s.labelDate, formatDateTime(tx.createdAt))
        DetailRow(s.labelClient, tx.counterpartName(Viewer.Agen))
        DetailRow(s.labelClientId, tx.klienId.ifBlank { "—" }, mono = true)
        DetailRow(s.labelVolume, formatLiter(tx.volumeLiter))
        DetailRow(s.labelPricePerLiter, formatRupiah(tx.price))
        DetailRow(s.labelTotal, formatRupiah(tx.totalPrice), mono = true, valueColor = c.primary)
        DetailRow(
            s.labelStatus, tx.txStatus.label(Viewer.Agen), last = true,
            valueColor = when (tx.txStatus) {
                TxStatus.Cancelled, TxStatus.Rejected -> c.rust
                TxStatus.Pending, TxStatus.CancelRequested -> c.amberDeep
                else -> c.primary
            },
        )
        Spacer(Modifier.height(14.dp))
        when (tx.txStatus) {
            TxStatus.Pending -> NoteBox(s.pendingAgenNote, tone = NoteTone.Amber)
            TxStatus.CancelRequested -> NoteBox(s.cancelRequestedAgenNote, tone = NoteTone.Amber)
            TxStatus.Rejected -> NoteBox(s.rejectedAgenNote, tone = NoteTone.Rust, icon = BioIcons.Alert)
            TxStatus.Cancelled -> NoteBox(s.cancelledAgenNote, tone = NoteTone.Rust, icon = BioIcons.Alert)
            else -> Unit
        }
        // Server hanya mengizinkan pengajuan batal saat PENDING atau ACCEPTED.
        if (tx.txStatus == TxStatus.Pending || tx.txStatus == TxStatus.Accepted) {
            Spacer(Modifier.height(14.dp))
            BioButton(
                if (confirmCancel) s.yesRequestCancel else s.requestCancel,
                onClick = { if (confirmCancel) onRequestCancel() else confirmCancel = true },
                modifier = Modifier.fillMaxWidth(),
                style = BtnStyle.Rust,
                loading = busy,
            )
        }
    }
}

@Composable
fun NewTransactionSheet(
    price: Long,
    creating: Boolean,
    vm: AgenViewModel,
    onSubmit: (contact: String, volumeLiter: Double) -> Unit,
    onDismiss: () -> Unit,
) {
    val c = BioTheme.colors
    val s = BioText.current
    var contact by remember { mutableStateOf("") }
    var volumeText by remember { mutableStateOf("") }
    var errors by remember { mutableStateOf(emptyMap<String, String>()) }
    val preview by vm.clientPreview.collectAsStateWithLifecycle()

    // Debounce: nunggu jeda ngetik sebelum nembak check-clients-email/phone, dibatalkan otomatis kalau
    // teksnya berubah lagi sebelum jeda selesai (LaunchedEffect restart tiap `contact` berubah).
    LaunchedEffect(contact) {
        delay(450)
        vm.checkClientContact(contact)
    }
    DisposableEffect(Unit) { onDispose { vm.resetClientPreview() } }

    val volume = volumeText.trim().replace(',', '.').toDoubleOrNull()
    val total = if (volume != null && volume > 0) kotlin.math.round(volume * price).toLong() else 0L

    fun submit() {
        val found = buildMap {
            if (contact.isBlank()) put("contact", s.errorClientContactRequired)
            if (volume == null || volume <= 0) put("volume", s.errorVolumeRequired)
            else if (volume > 1000) put("volume", s.errorVolumeTooLarge)
        }
        errors = found
        if (found.isEmpty() && volume != null) onSubmit(contact.trim(), volume)
    }

    BioSheet(s.newTransactionTitle, onDismiss) {
        NoteBox(
            s.newTransactionNote,
            icon = BioIcons.IdCard,
            modifier = Modifier.padding(bottom = 16.dp),
        )
        BioField(
            s.fieldClientContact, contact, { contact = it },
            placeholder = s.placeholderClientContact,
            keyboardType = KeyboardType.Email,
            error = errors["contact"],
        )
        ClientPreviewRow(preview, Modifier.padding(top = 4.dp, bottom = 4.dp))
        BioField(
            s.fieldVolume, volumeText, { volumeText = it },
            placeholder = s.placeholderVolume,
            keyboardType = KeyboardType.Decimal,
            imeAction = ImeAction.Done,
            onDone = { submit() },
            error = errors["volume"],
        )
        BioField(s.fieldPricePerLiter, formatRupiah(price), {}, readOnly = true, hint = s.hintReferencePriceKilang)

        Row(
            Modifier.fillMaxWidth().background(c.primaryTint, RoundedCornerShape(14.dp)).padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(s.total, style = BioTheme.type.bodyBold, color = c.primary)
            Text(formatRupiah(total), style = BioTheme.type.monoLarge, color = c.primary)
        }
        Spacer(Modifier.height(16.dp))
        BioButton(
            s.submitToClient,
            onClick = { submit() },
            loading = creating,
            // Tetap bisa ditekan meski NotFound: mungkin memang mau kirim ke email tamu (GUEST_CLIENT_EMAIL di
            // backend) atau preview gagal karena jaringan. Server yang menentukan valid/tidaknya secara final.
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Baris kecil di bawah field kontak Klien: status pengecekan check-clients-email/phone. */
@Composable
private fun ClientPreviewRow(preview: ClientPreview, modifier: Modifier = Modifier) {
    val c = BioTheme.colors
    val s = BioText.current
    when (preview) {
        ClientPreview.Idle -> Unit
        ClientPreview.Checking -> Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            CircularProgressIndicator(color = c.muted, strokeWidth = 2.dp, modifier = Modifier.size(12.dp))
            Text(s.checkingClient, style = BioTheme.type.small, color = c.muted)
        }
        is ClientPreview.Found -> Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(BioIcons.Check, contentDescription = null, tint = c.primary, modifier = Modifier.size(14.dp))
            Text(s.clientFound(preview.name), style = BioTheme.type.small, color = c.primary)
        }
        ClientPreview.NotFound -> Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(BioIcons.Alert, contentDescription = null, tint = c.rust, modifier = Modifier.size(14.dp))
            Text(s.clientNotFound, style = BioTheme.type.small, color = c.rust)
        }
    }
}
