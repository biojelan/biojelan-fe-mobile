package id.biojelan.app.ui.agen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import id.biojelan.app.core.formatTimeOnly
import id.biojelan.app.core.formatRupiah
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
import id.biojelan.app.ui.components.DayHeader
import id.biojelan.app.ui.components.DetailRow
import id.biojelan.app.ui.components.LedgerHero
import id.biojelan.app.ui.components.WeekBars
import id.biojelan.app.ui.components.groupByDay
import id.biojelan.app.ui.components.weekVolumeBars
import id.biojelan.app.ui.countsForVolume
import id.biojelan.app.ui.components.EmptyBlock
import id.biojelan.app.ui.components.ErrorBlock
import id.biojelan.app.ui.components.LoadingBlock
import id.biojelan.app.ui.components.NoteBox
import id.biojelan.app.ui.components.NoteTone
import id.biojelan.app.ui.components.ScreenPad
import id.biojelan.app.ui.components.ScreenTopBar
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

    val clientGroups = remember(state.transactions) { groupByDay(state.transactions) { it.createdAt } }
    val driverGroups = remember(state.driverTransactions) { groupByDay(state.driverTransactions) { it.createdAt } }
    val clientBars = remember(state.transactions) {
        weekVolumeBars(state.transactions.filter { it.txStatus.countsForVolume() }.map { it.createdAt to it.volumeLiter })
    }
    val driverBars = remember(state.driverTransactions) {
        weekVolumeBars(state.driverTransactions.filter { it.txStatus.countsForVolume() }.map { it.createdAt to it.volumeLiter })
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                ScreenTopBar(s.transactionsTitle, actions = {
                    CircleIconButton(BioIcons.Refresh, onClick = vm::manualRefresh, contentDescription = s.reload, loading = state.loading)
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
                    LedgerHero(
                        eyebrow = s.volumeToday.uppercase(),
                        value = formatNumber(state.todayLiters, 1) + " L",
                        stats = listOf(
                            s.valueToday to formatRupiah(state.todayValue),
                            s.todayTransactions to state.todayCount.toString(),
                        ),
                        modifier = Modifier.padding(horizontal = ScreenPad),
                    ) { WeekBars(clientBars) }
                }
                when {
                    state.loading && state.transactions.isEmpty() -> item { LoadingBlock() }
                    state.error != null && state.transactions.isEmpty() -> item { ErrorBlock(state.error, vm::refresh) }
                    state.transactions.isEmpty() -> item {
                        EmptyBlock(BioIcons.Receipt, s.noTransactionsTitle, s.noTransactionsHint)
                    }
                    else -> clientGroups.forEach { group ->
                        item(key = "c-day-${group.epochDay}") { DayHeader(group.epochDay) }
                        items(group.items) { tx ->
                            val name = tx.counterpartName(Viewer.Agen)
                            TxRow(
                                avatar = initialsOf(name),
                                title = name,
                                subtitle = formatTimeOnly(tx.createdAt) + " · " + formatLiter(tx.volumeLiter),
                                amount = formatRupiah(tx.totalPrice),
                                statusText = tx.txStatus.label(Viewer.Agen),
                                statusKind = tx.txStatus.chipKind(),
                                modifier = Modifier.padding(horizontal = ScreenPad),
                                onClick = { selectedId = tx.transactionId },
                            )
                        }
                    }
                }
            } else {
                item {
                    LedgerHero(
                        eyebrow = s.volumeToday.uppercase(),
                        value = formatNumber(state.todayDriverLiters, 1) + " L",
                        stats = listOf(
                            s.valueToday to formatRupiah(state.todayDriverValue),
                            s.todayTransactions to state.todayDriverCount.toString(),
                        ),
                        modifier = Modifier.padding(horizontal = ScreenPad),
                    ) { WeekBars(driverBars) }
                }
                when {
                    state.driverTxError != null && state.driverTransactions.isEmpty() ->
                        item { ErrorBlock(state.driverTxError, vm::refresh) }
                    state.driverTransactions.isEmpty() -> item {
                        EmptyBlock(BioIcons.Truck, s.noTransactionsTitle, s.agenDriverNoTransactionsHint)
                    }
                    else -> driverGroups.forEach { group ->
                        item(key = "d-day-${group.epochDay}") { DayHeader(group.epochDay) }
                        items(group.items) { tx ->
                            // DriverTransactionDto tidak punya nama Driver (hanya driver_id), jadi tampilkan label peran.
                            TxRow(
                                avatar = initialsOf(s.roleDriver),
                                title = s.roleDriver,
                                subtitle = formatTimeOnly(tx.createdAt) + " · " + formatLiter(tx.volumeLiter),
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
            TxStatus.Pending -> if (tx.klienId.isNotBlank()) NoteBox(s.pendingAgenNote, tone = NoteTone.Amber)
            TxStatus.CancelRequested -> NoteBox(s.cancelRequestedAgenNote, tone = NoteTone.Amber)
            TxStatus.Rejected -> NoteBox(s.rejectedAgenNote, tone = NoteTone.Rust, icon = BioIcons.Alert)
            TxStatus.Cancelled -> NoteBox(s.cancelledAgenNote, tone = NoteTone.Rust, icon = BioIcons.Alert)
            else -> Unit
        }
        // Transaksi tamu tidak punya Klien yang bisa menyetujui pembatalan (status akan macet), jadi tidak dibatalkan dari app.
        val isGuestTx = tx.klienId.isBlank()
        if (isGuestTx) {
            Spacer(Modifier.height(14.dp))
            NoteBox(s.guestTxAgenNote, tone = NoteTone.Amber)
        }
        // Server hanya mengizinkan pengajuan batal saat PENDING atau ACCEPTED.
        if (!isGuestTx && (tx.txStatus == TxStatus.Pending || tx.txStatus == TxStatus.Accepted)) {
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

/** Mode input transaksi: Klien terdaftar vs Tamu. */
private enum class TxInputMode { Registered, Guest }

/** Tipe kontak yang dipilih lewat dropdown. */
private enum class ContactType { Email, Phone }

@Composable
fun NewTransactionSheet(
    price: Long,
    creating: Boolean,
    vm: AgenViewModel,
    onSubmit: (contact: String, volumeLiter: Double, guestName: String?) -> Unit,
    onDismiss: () -> Unit,
) {
    val c = BioTheme.colors
    val s = BioText.current
    var mode by remember { mutableStateOf(TxInputMode.Registered) }
    var contactType by remember { mutableStateOf(ContactType.Email) }
    var contact by remember { mutableStateOf("") }
    var guestName by remember { mutableStateOf("") }
    var guestPhone by remember { mutableStateOf("") }
    var volumeText by remember { mutableStateOf("") }
    var errors by remember { mutableStateOf(emptyMap<String, String>()) }
    val preview by vm.clientPreview.collectAsStateWithLifecycle()

    // Reset contact saat ganti mode atau tipe kontak
    LaunchedEffect(mode) { contact = ""; guestName = ""; guestPhone = ""; errors = emptyMap(); vm.resetClientPreview() }
    LaunchedEffect(contactType) { contact = ""; errors = emptyMap(); vm.resetClientPreview() }

    // Debounce check-clients-email/phone
    LaunchedEffect(contact) {
        delay(450)
        vm.checkClientContact(contact)
    }
    DisposableEffect(Unit) { onDispose { vm.resetClientPreview() } }

    val volume = volumeText.trim().replace(',', '.').toDoubleOrNull()
    val total = if (volume != null && volume > 0) kotlin.math.round(volume * price).toLong() else 0L

    fun submit() {
        val found = buildMap {
            if (mode == TxInputMode.Registered) {
                if (contact.isBlank()) put("contact", s.errorClientContactRequired)
            } else {
                if (guestName.isBlank()) put("guestName", s.errorClientNameRequired)
                if (guestPhone.isBlank()) put("guestPhone", s.errorClientContactRequired)
            }
            if (volume == null || volume <= 0) put("volume", s.errorVolumeRequired)
            else if (volume > 1000) put("volume", s.errorVolumeTooLarge)
        }
        errors = found
        if (found.isEmpty() && volume != null) {
            val resolvedContact = if (mode == TxInputMode.Registered) contact.trim() else guestPhone.trim()
            onSubmit(resolvedContact, volume, if (mode == TxInputMode.Guest) guestName.trim() else null)
        }
    }

    BioSheet(s.newTransactionTitle, onDismiss) {
        SegmentedTabs(
            options = listOf(s.txTabRegistered, s.txTabGuest),
            selected = if (mode == TxInputMode.Registered) 0 else 1,
            onSelect = { mode = if (it == 0) TxInputMode.Registered else TxInputMode.Guest },
            modifier = Modifier.padding(bottom = 16.dp),
        )

        if (mode == TxInputMode.Registered) {
            // Dropdown pilih tipe kontak
            ContactTypeSelector(contactType) { contactType = it }
            BioField(
                if (contactType == ContactType.Email) s.fieldEmail else s.fieldPhone,
                contact, { contact = it },
                placeholder = if (contactType == ContactType.Email) s.placeholderEmail else s.placeholderPhone,
                keyboardType = if (contactType == ContactType.Email) KeyboardType.Email else KeyboardType.Phone,
                error = errors["contact"],
            )
            ClientPreviewRow(preview, Modifier.padding(top = 4.dp, bottom = 4.dp))
        } else {
            BioField(
                s.fieldClientName, guestName, { guestName = it },
                placeholder = s.placeholderClientName,
                error = errors["guestName"],
            )
            BioField(
                s.fieldPhone, guestPhone, { guestPhone = it },
                placeholder = s.placeholderPhone,
                keyboardType = KeyboardType.Phone,
                error = errors["guestPhone"],
            )
        }

        BioField(
            s.fieldVolume, volumeText, { volumeText = it },
            placeholder = s.placeholderVolume,
            keyboardType = KeyboardType.Decimal,
            imeAction = ImeAction.Done,
            onDone = { submit() },
            error = errors["volume"],
        )
        BioField(s.fieldPricePerLiter, formatRupiah(price), {}, readOnly = true)

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
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Selector tipe kontak: Email atau No. Telepon. */
@Composable
private fun ContactTypeSelector(selected: ContactType, onChange: (ContactType) -> Unit) {
    val c = BioTheme.colors
    val s = BioText.current
    Row(
        Modifier.fillMaxWidth().padding(bottom = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ContactType.entries.forEach { type ->
            val on = type == selected
            val label = when (type) {
                ContactType.Email -> s.fieldEmail
                ContactType.Phone -> s.labelPhone
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (on) c.primaryTint else c.surface, RoundedCornerShape(12.dp))
                    .border(1.5.dp, if (on) c.primary else c.line, RoundedCornerShape(12.dp))
                    .clickable { onChange(type) }
                    .padding(vertical = 11.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(label, style = BioTheme.type.bodyBold, color = if (on) c.primary else c.inkSoft)
            }
        }
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