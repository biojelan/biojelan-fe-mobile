package id.biojelan.app.ui.klien

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import id.biojelan.app.core.agenInitials
import id.biojelan.app.core.formatLiter
import id.biojelan.app.core.formatRelativeDateTime
import id.biojelan.app.core.formatDateTime
import id.biojelan.app.core.formatRupiah
import id.biojelan.app.core.formatRupiahCompact
import id.biojelan.app.core.formatNumber
import id.biojelan.app.data.remote.TransactionDto
import id.biojelan.app.data.repository.TxStatus
import id.biojelan.app.data.repository.txStatus
import id.biojelan.app.ui.Viewer
import id.biojelan.app.ui.chipKind
import id.biojelan.app.ui.components.BioButton
import id.biojelan.app.ui.components.BioChip
import id.biojelan.app.ui.components.BioSheet
import id.biojelan.app.ui.components.BtnStyle
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
import id.biojelan.app.ui.icons.BioIcons
import id.biojelan.app.ui.label
import id.biojelan.app.ui.strings.BioText
import id.biojelan.app.ui.theme.BioTheme

@Composable
fun KlienHistoryTab(state: KlienUiState, vm: KlienViewModel) {
    val s = BioText.current
    var selectedId by remember { mutableStateOf<String?>(null) }
    val agenName: (String) -> String? = { id -> state.agens.firstOrNull { it.agenId == id }?.name }

    val accepted = state.transactions.filter { it.txStatus == TxStatus.Accepted }
    val totalLiters = accepted.sumOf { it.volumeLiter }
    val totalValue = accepted.sumOf { it.totalPrice }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            ScreenTopBar(s.historyTitle, actions = {
                CircleIconButton(BioIcons.Refresh, onClick = vm::loadTransactions, contentDescription = s.reload)
            })
        }
        item {
            Row(Modifier.padding(horizontal = ScreenPad), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard(state.transactions.size.toString(), s.transactionsCount, Modifier.weight(1f))
                StatCard(formatNumber(totalLiters, 1) + " L", s.oilSold, Modifier.weight(1f))
                StatCard(formatRupiahCompact(totalValue), s.totalReceived, Modifier.weight(1f))
            }
        }
        when {
            state.txLoading && state.transactions.isEmpty() -> item { LoadingBlock() }
            state.txError != null && state.transactions.isEmpty() -> item { ErrorBlock(state.txError, vm::loadTransactions) }
            state.transactions.isEmpty() -> item {
                EmptyBlock(BioIcons.Receipt, s.noHistoryTitle, s.noHistoryHint)
            }
            else -> items(state.transactions) { tx ->
                val name = tx.counterpartName(Viewer.Klien, agenName)
                TxRow(
                    avatar = agenInitials(name),
                    title = name,
                    subtitle = formatRelativeDateTime(tx.createdAt) + " · " + formatLiter(tx.volumeLiter),
                    amount = formatRupiah(tx.totalPrice),
                    statusText = tx.txStatus.label(Viewer.Klien),
                    statusKind = tx.txStatus.chipKind(),
                    modifier = Modifier.padding(horizontal = ScreenPad),
                    onClick = { selectedId = tx.transactionId },
                )
            }
        }
    }

    val selected = state.transactions.firstOrNull { it.transactionId == selectedId }
    if (selected != null) {
        KlienTxDetailSheet(
            tx = selected,
            agenName = selected.counterpartName(Viewer.Klien, agenName),
            busy = state.busyTxId == selected.transactionId,
            onAccept = { vm.accept(selected.transactionId) { selectedId = null } },
            onReject = { vm.reject(selected.transactionId) { selectedId = null } },
            onAcceptCancel = { vm.acceptCancellation(selected.transactionId) { selectedId = null } },
            onRejectCancel = { vm.rejectCancellation(selected.transactionId) { selectedId = null } },
            onDismiss = { selectedId = null },
        )
    }
}

@Composable
private fun KlienTxDetailSheet(
    tx: TransactionDto,
    agenName: String,
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
        DetailRow(s.labelAgent, agenName)
        DetailRow(s.labelVolume, formatLiter(tx.volumeLiter))
        DetailRow(s.labelPricePerLiter, formatRupiah(tx.price))
        DetailRow(s.labelTotal, formatRupiah(tx.totalPrice), mono = true, valueColor = c.primary)
        DetailRow(s.labelStatus, tx.txStatus.label(Viewer.Klien), last = true, valueColor = when (tx.txStatus) {
            TxStatus.Cancelled, TxStatus.Rejected -> c.rust
            TxStatus.Pending, TxStatus.CancelRequested -> c.amberDeep
            else -> c.primary
        })

        if (tx.txStatus == TxStatus.Pending) {
            Spacer(Modifier.height(14.dp))
            NoteBox(
                s.pendingKlienNote,
                tone = NoteTone.Amber,
                icon = BioIcons.Info,
            )
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

        if (tx.txStatus == TxStatus.CancelRequested) {
            Spacer(Modifier.height(14.dp))
            NoteBox(
                s.cancelRequestedKlienNote,
                tone = NoteTone.Amber,
                icon = BioIcons.Alert,
            )
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                BioButton(s.rejectCancel, onRejectCancel, Modifier.weight(1f), style = BtnStyle.Outline, enabled = !busy)
                BioButton(s.acceptCancel, onAcceptCancel, Modifier.weight(1f), style = BtnStyle.Rust, loading = busy, icon = BioIcons.Check)
            }
        }
    }
}
