package id.biojelan.app.ui.agen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.biojelan.app.core.AppConfig
import id.biojelan.app.core.formatLiter
import id.biojelan.app.core.formatNumber
import id.biojelan.app.core.formatRelativeDateTime
import id.biojelan.app.core.initialsOf
import id.biojelan.app.data.remote.UserDto
import id.biojelan.app.data.repository.TxStatus
import id.biojelan.app.data.repository.txStatus
import id.biojelan.app.ui.Viewer
import id.biojelan.app.ui.components.BioChip
import id.biojelan.app.ui.components.ChipKind
import id.biojelan.app.ui.components.CircleIconButton
import id.biojelan.app.ui.components.JerrycanGauge
import id.biojelan.app.ui.components.StockHalo
import id.biojelan.app.ui.components.ErrorBlock
import id.biojelan.app.ui.components.NoteBox
import id.biojelan.app.ui.components.ScreenPad
import id.biojelan.app.ui.components.ScreenTopBar
import id.biojelan.app.ui.components.SectionHead
import id.biojelan.app.ui.components.TxRow
import id.biojelan.app.ui.counterpartName
import id.biojelan.app.ui.icons.BioIcons
import id.biojelan.app.ui.strings.BioText
import id.biojelan.app.ui.theme.BioTheme

/**
 * Stok Agen. Nilai `stock_liter` datang dari GET /api/user (dihitung server).
 * "Pergerakan stok" diturunkan dari daftar transaksi karena API-DOC belum punya endpoint riwayat stok.
 */
@Composable
fun AgenStockTab(state: AgenUiState, user: UserDto, vm: AgenViewModel) {
    val c = BioTheme.colors
    val s = BioText.current
    val stock = user.agen?.stockLiter ?: 0.0
    val threshold = AppConfig.STOCK_THRESHOLD_LITER
    val reached = stock >= threshold
    val ratio = (stock / threshold).toFloat().coerceIn(0f, 1f)

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
        ScreenTopBar(s.stockTitle, actions = {
            CircleIconButton(BioIcons.Refresh, onClick = vm::manualRefresh, contentDescription = s.reload, loading = state.loading)
        })
        Column(Modifier.padding(horizontal = ScreenPad), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(4.dp))
            StockHalo(Modifier.size(230.dp)) {
                JerrycanGauge(fill = ratio, modifier = Modifier.size(width = 132.dp, height = 164.dp))
            }
            Spacer(Modifier.height(4.dp))
            Text(formatLiter(stock), style = BioTheme.type.title.copy(fontSize = 34.sp), color = c.ink)
            Text(s.thresholdCaption(formatLiter(threshold)), style = BioTheme.type.body, color = c.muted)
            Spacer(Modifier.height(10.dp))
            BioChip(
                if (reached) s.aboveThresholdNote else s.remainingToThreshold(formatLiter(threshold - stock)),
                if (reached) ChipKind.Done else ChipKind.Pending,
            )
            Spacer(Modifier.height(16.dp))
            NoteBox(s.stockThresholdInfo, icon = BioIcons.Info)
        }

        Column(Modifier.padding(horizontal = ScreenPad)) {
            SectionHead(s.stockMovement)
            if (state.transactions.isEmpty()) {
                NoteBox(s.noStockMovement)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    state.transactions.take(20).forEach { tx ->
                        val name = tx.counterpartName(Viewer.Agen)
                        val (amountText, kind, status) = when (tx.txStatus) {
                            TxStatus.Accepted -> Triple("+" + formatNumber(tx.volumeLiter, 2) + " L", ChipKind.Done, s.stockIncoming)
                            TxStatus.Pending, TxStatus.CancelRequested ->
                                Triple("+" + formatNumber(tx.volumeLiter, 2) + " L", ChipKind.Pending, s.stockWaiting)
                            else -> Triple("0 L", ChipKind.Cancelled, s.stockCancelled)
                        }
                        TxRow(
                            avatar = initialsOf(name),
                            title = s.transactionDash(name),
                            subtitle = formatRelativeDateTime(tx.createdAt),
                            amount = amountText,
                            statusText = status,
                            statusKind = kind,
                        )
                    }
                }
            }
            if (state.driverTxError != null && state.driverTransactions.isEmpty()) {
                SectionHead(s.driverStockSection)
                ErrorBlock(state.driverTxError, onRetry = vm::refresh)
            }
            if (state.driverTransactions.isNotEmpty()) {
                SectionHead(s.driverStockSection)
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    state.driverTransactions.take(10).forEach { tx ->
                        val (amountText, kind, status) = when (tx.txStatus) {
                            TxStatus.Accepted -> Triple("−" + formatNumber(tx.volumeLiter, 2) + " L", ChipKind.Done, s.stockOutgoing)
                            TxStatus.Pending, TxStatus.CancelRequested ->
                                Triple("−" + formatNumber(tx.volumeLiter, 2) + " L", ChipKind.Pending, s.stockWaiting)
                            else -> Triple("0 L", ChipKind.Cancelled, s.stockCancelled)
                        }
                        TxRow(
                            avatar = "DR",
                            title = s.driverStockRow,
                            subtitle = formatRelativeDateTime(tx.createdAt),
                            amount = amountText,
                            statusText = status,
                            statusKind = kind,
                        )
                    }
                }
            }
        }
    }
}
