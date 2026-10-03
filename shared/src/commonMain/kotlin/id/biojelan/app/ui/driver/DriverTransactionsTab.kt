package id.biojelan.app.ui.driver

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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import id.biojelan.app.core.formatDateTime
import id.biojelan.app.core.googleMapsDirectionsUrl
import id.biojelan.app.core.formatLiter
import id.biojelan.app.core.formatNumber
import id.biojelan.app.core.formatTimeOnly
import id.biojelan.app.core.formatRupiah
import id.biojelan.app.core.initialsOf
import id.biojelan.app.data.remote.AgenSummaryDto
import id.biojelan.app.data.remote.DriverTransactionDto
import id.biojelan.app.data.repository.TxStatus
import id.biojelan.app.data.repository.txStatus
import id.biojelan.app.ui.chipKind
import id.biojelan.app.ui.components.BioButton
import id.biojelan.app.ui.components.BioField
import id.biojelan.app.ui.components.BioSheet
import id.biojelan.app.ui.components.BtnStyle
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
import id.biojelan.app.ui.driverLabel
import id.biojelan.app.ui.icons.BioIcons
import id.biojelan.app.ui.strings.BioText
import id.biojelan.app.ui.theme.BioTheme

@Composable
fun DriverTransactionsTab(state: DriverUiState, vm: DriverViewModel, onNewTransaction: () -> Unit) {
    val c = BioTheme.colors
    val s = BioText.current
    var selectedId by remember { mutableStateOf<String?>(null) }

    val groups = remember(state.transactions) { groupByDay(state.transactions) { it.createdAt } }
    val bars = remember(state.transactions) {
        weekVolumeBars(state.transactions.filter { it.txStatus.countsForVolume() }.map { it.createdAt to it.volumeLiter })
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
                LedgerHero(
                    eyebrow = s.volumeToday.uppercase(),
                    value = formatNumber(state.todayLiters, 1) + " L",
                    stats = listOf(
                        s.valueToday to formatRupiah(state.todayValue),
                        s.todayTransactions to state.todayCount.toString(),
                    ),
                    modifier = Modifier.padding(horizontal = ScreenPad),
                ) { WeekBars(bars) }
            }
            when {
                state.loading && state.transactions.isEmpty() -> item { LoadingBlock() }
                state.error != null && state.transactions.isEmpty() -> item { ErrorBlock(state.error, vm::refresh) }
                state.transactions.isEmpty() -> item {
                    EmptyBlock(BioIcons.Receipt, s.noTransactionsTitle, s.driverNoTransactionsHint)
                }
                else -> groups.forEach { group ->
                    item(key = "day-${group.epochDay}") { DayHeader(group.epochDay) }
                    items(group.items) { tx ->
                        val name = tx.agenName.ifBlank { "Agen" }
                        TxRow(
                            avatar = initialsOf(name),
                            title = name,
                            subtitle = formatTimeOnly(tx.createdAt) + " · " + formatLiter(tx.volumeLiter),
                            amount = formatRupiah(tx.totalPrice),
                            statusText = tx.txStatus.driverLabel(asAgen = false),
                            statusKind = tx.txStatus.chipKind(),
                            modifier = Modifier.padding(horizontal = ScreenPad),
                            onClick = { selectedId = tx.transactionId },
                        )
                    }
                }
            }
        }

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
            Text(s.driverRecordButton, style = BioTheme.type.button, color = c.onPrimary)
        }
    }

    val selected = state.transactions.firstOrNull { it.transactionId == selectedId }
    if (selected != null) {
        DriverTxDetailSheet(
            tx = selected,
            busy = state.busyTxId == selected.transactionId,
            agenLocation = vm.agenLocation(selected.agenId),
            onRequestCancel = { vm.requestCancel(selected.transactionId) { selectedId = null } },
            onDismiss = { selectedId = null },
        )
    }
}

@Composable
internal fun DriverTxDetailSheet(
    tx: DriverTransactionDto,
    busy: Boolean,
    agenLocation: AgenSummaryDto?,
    onRequestCancel: () -> Unit,
    onDismiss: () -> Unit,
) {
    val c = BioTheme.colors
    val s = BioText.current
    val uriHandler = LocalUriHandler.current
    var confirmCancel by remember { mutableStateOf(false) }
    BioSheet(s.transactionDetailTitle, onDismiss) {
        DetailRow(s.labelDate, formatDateTime(tx.createdAt))
        DetailRow(s.labelAgent, tx.agenName.ifBlank { "—" })
        DetailRow(s.labelVolume, formatLiter(tx.volumeLiter))
        DetailRow(s.labelPricePerLiter, formatRupiah(tx.price))
        DetailRow(s.labelTotal, formatRupiah(tx.totalPrice), mono = true, valueColor = c.primary)
        if (tx.transactionNote.isNotBlank()) DetailRow(s.labelNote, tx.transactionNote)
        DetailRow(
            s.labelStatus, tx.txStatus.driverLabel(asAgen = false), last = true,
            valueColor = when (tx.txStatus) {
                TxStatus.Cancelled, TxStatus.Rejected -> c.rust
                TxStatus.Pending, TxStatus.CancelRequested -> c.amberDeep
                else -> c.primary
            },
        )
        Spacer(Modifier.height(14.dp))
        when (tx.txStatus) {
            TxStatus.Pending -> NoteBox(s.pendingDriverNote, tone = NoteTone.Amber)
            TxStatus.CancelRequested -> NoteBox(s.cancelRequestedDriverNote, tone = NoteTone.Amber)
            TxStatus.Rejected -> NoteBox(s.rejectedDriverNote, tone = NoteTone.Rust, icon = BioIcons.Alert)
            TxStatus.Cancelled -> NoteBox(s.cancelledDriverNote, tone = NoteTone.Rust, icon = BioIcons.Alert)
            else -> Unit
        }
        if (agenLocation != null) {
            Spacer(Modifier.height(14.dp))
            BioButton(
                s.routeToAgen,
                onClick = {
                    uriHandler.openUri(
                        googleMapsDirectionsUrl(agenLocation.latitude, agenLocation.longitude, agenLocation.address),
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                style = BtnStyle.Outline,
                icon = BioIcons.Map,
            )
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
fun NewDriverTransactionSheet(
    creating: Boolean,
    initialContact: String = "",
    onSubmit: (contact: String, volumeLiter: Double, note: String) -> Unit,
    onDismiss: () -> Unit,
) {
    val s = BioText.current
    var contact by remember { mutableStateOf(initialContact) }
    var volumeText by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var errors by remember { mutableStateOf(emptyMap<String, String>()) }

    val volume = volumeText.trim().replace(',', '.').toDoubleOrNull()

    fun submit() {
        val found = buildMap {
            if (contact.isBlank()) put("contact", s.errorAgenContactRequired)
            if (volume == null || volume <= 0) put("volume", s.errorVolumeRequired)
            else if (volume > 10_000) put("volume", s.errorVolumeTooLarge)
        }
        errors = found
        if (found.isEmpty() && volume != null) onSubmit(contact.trim(), volume, note.trim())
    }

    BioSheet(s.driverNewTransactionTitle, onDismiss) {
        BioField(
            s.fieldAgenContact, contact, { contact = it },
            placeholder = s.placeholderAgenContact,
            keyboardType = KeyboardType.Email,
            error = errors["contact"],
        )
        BioField(
            s.fieldVolume, volumeText, { volumeText = it },
            placeholder = s.placeholderVolume,
            keyboardType = KeyboardType.Decimal,
            error = errors["volume"],
        )
        BioField(
            s.fieldTransactionNote, note, { note = it },
            placeholder = s.placeholderTransactionNote,
            imeAction = ImeAction.Done,
            onDone = { submit() },
        )
        Spacer(Modifier.height(16.dp))
        BioButton(s.submitToAgen, onClick = { submit() }, loading = creating, modifier = Modifier.fillMaxWidth())
    }
}