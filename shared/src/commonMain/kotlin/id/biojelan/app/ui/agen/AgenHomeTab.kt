package id.biojelan.app.ui.agen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import id.biojelan.app.core.AppConfig
import id.biojelan.app.core.formatLiter
import id.biojelan.app.core.formatNumber
import id.biojelan.app.core.formatRelativeDateTime
import id.biojelan.app.core.formatRupiah
import id.biojelan.app.core.firstNameOf
import id.biojelan.app.core.formatRupiahCompact
import id.biojelan.app.core.agenInitials
import id.biojelan.app.core.initialsOf
import id.biojelan.app.data.remote.UserDto
import id.biojelan.app.data.repository.PickupStatus
import id.biojelan.app.data.repository.pickupStatus
import id.biojelan.app.data.repository.txStatus
import id.biojelan.app.ui.Viewer
import id.biojelan.app.ui.chipKind
import id.biojelan.app.ui.components.BioButton
import id.biojelan.app.ui.components.BtnStyle
import id.biojelan.app.ui.components.rememberHeldLoading
import id.biojelan.app.ui.components.BioChip
import id.biojelan.app.ui.components.ChipKind
import id.biojelan.app.ui.components.ErrorBlock
import id.biojelan.app.ui.components.FadeInItem
import id.biojelan.app.ui.components.HomeHeader
import id.biojelan.app.ui.components.NoteBox
import id.biojelan.app.ui.components.NoteTone
import id.biojelan.app.ui.components.PriceBand
import id.biojelan.app.ui.components.ScreenPad
import id.biojelan.app.ui.components.SectionHead
import id.biojelan.app.ui.components.StatCard
import id.biojelan.app.ui.components.StockHeroCard
import id.biojelan.app.ui.components.TxRow
import id.biojelan.app.ui.counterpartName
import id.biojelan.app.ui.icons.BioIcons
import id.biojelan.app.ui.label
import id.biojelan.app.ui.strings.BioText
import id.biojelan.app.ui.theme.BioTheme

@Composable
fun AgenHomeTab(
    state: AgenUiState,
    user: UserDto,
    vm: AgenViewModel,
    onNewTransaction: () -> Unit,
    onGoTo: (Int) -> Unit,
) {
    val c = BioTheme.colors
    val s = BioText.current
    val agen = user.agen
    val firstName = firstNameOf(user.name, s.roleAgent)
    val stock = agen?.stockLiter ?: 0.0
    val threshold = AppConfig.STOCK_THRESHOLD_LITER
    val reached = stock >= threshold

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = ScreenPad).padding(bottom = 24.dp),
    ) {
        HomeHeader(name = firstName, initials = agenInitials(user.name), role = s.roleAgent)

        PriceBand(state.price, s.priceCaptionAgen)

        state.driverTxError?.let { message ->
            Spacer(Modifier.height(16.dp))
            NoteBox(s.driverRequestsLoadFailed + " " + message, tone = NoteTone.Rust, icon = BioIcons.Alert)
        }

        state.driverPendingTx?.let { tx ->
            Spacer(Modifier.height(16.dp))
            DriverRequestCard(
                tx = tx,
                busy = state.busyDriverTxId == tx.transactionId,
                onAccept = { vm.acceptDriverTx(tx.transactionId) },
                onReject = { vm.rejectDriverTx(tx.transactionId) },
            )
        }

        state.driverCancelTx?.let { tx ->
            Spacer(Modifier.height(16.dp))
            DriverCancelCard(
                tx = tx,
                busy = state.busyDriverTxId == tx.transactionId,
                onAccept = { vm.acceptDriverCancel(tx.transactionId) },
                onReject = { vm.rejectDriverCancel(tx.transactionId) },
            )
        }

        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard(state.todayCount.toString(), s.todayTransactions, Modifier.weight(1f))
            StatCard(formatNumber(state.todayLiters, 1) + " L", s.collectedToday, Modifier.weight(1f))
            StatCard(formatRupiahCompact(state.todayValue), s.valueToday, Modifier.weight(1f))
        }

        Spacer(Modifier.height(16.dp))
        StockHeroCard(
            stock = stock,
            threshold = threshold,
            eyebrow = s.currentStockLabel,
            valueText = formatLiter(stock),
            thresholdText = formatLiter(threshold),
            onClick = { onGoTo(2) },
        ) {
            // Kalau Kilang sudah menugaskan Driver, tampilkan status itu — lebih akurat
            // daripada sekadar "siap dijemput" begitu stok lewat ambang.
            when (state.pickup?.pickupStatus) {
                PickupStatus.Assigned -> BioChip(s.pickupStatusAssigned, ChipKind.Pending)
                PickupStatus.OnTheWay -> BioChip(s.pickupStatusOtw, ChipKind.Pending)
                PickupStatus.Arrived -> BioChip(s.pickupStatusArrived, ChipKind.Pending)
                else -> BioChip(
                    if (reached) s.readyForPickup else s.remainingToThreshold(formatLiter(threshold - stock)),
                    if (reached) ChipKind.Done else ChipKind.Pending,
                )
            }
        }

        Spacer(Modifier.height(14.dp))
        BioButton(s.newTransaction, onNewTransaction, Modifier.fillMaxWidth(), icon = BioIcons.Plus)

        SectionHead(s.recentActivity, action = s.seeAll, onAction = { onGoTo(1) })
        when {
            state.loading && state.transactions.isEmpty() -> Row(Modifier.fillMaxWidth().padding(24.dp), horizontalArrangement = Arrangement.Center) {
                CircularProgressIndicator(color = c.primary, strokeWidth = 3.dp, modifier = Modifier.size(24.dp))
            }
            state.error != null && state.transactions.isEmpty() -> ErrorBlock(state.error, onRetry = vm::refresh)
            state.transactions.isEmpty() -> NoteBox(s.noTransactionsAgenHint)
            else -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                state.transactions.take(3).forEachIndexed { idx, tx ->
                    FadeInItem(idx) {
                        val name = tx.counterpartName(Viewer.Agen)
                        TxRow(
                            avatar = initialsOf(name),
                            title = name,
                            subtitle = formatRelativeDateTime(tx.createdAt) + " · " + formatLiter(tx.volumeLiter),
                            amount = formatRupiah(tx.totalPrice),
                            statusText = tx.txStatus.label(Viewer.Agen),
                            statusKind = tx.txStatus.chipKind(),
                            onClick = { onGoTo(1) },
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