package id.biojelan.app.ui.agen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import id.biojelan.app.core.AppConfig
import id.biojelan.app.core.agenInitials
import id.biojelan.app.core.formatLiter
import id.biojelan.app.core.formatNumber
import id.biojelan.app.core.formatRelativeDateTime
import id.biojelan.app.core.formatRupiah
import id.biojelan.app.core.formatRupiahCompact
import id.biojelan.app.core.greeting
import id.biojelan.app.core.initialsOf
import id.biojelan.app.data.remote.UserDto
import id.biojelan.app.data.repository.PickupStatus
import id.biojelan.app.data.repository.pickupStatus
import id.biojelan.app.data.repository.txStatus
import id.biojelan.app.ui.Viewer
import id.biojelan.app.ui.chipKind
import id.biojelan.app.ui.components.BioButton
import id.biojelan.app.ui.components.BtnStyle
import id.biojelan.app.ui.components.BioChip
import id.biojelan.app.ui.components.ChipKind
import id.biojelan.app.ui.components.Drip
import id.biojelan.app.ui.components.DropGauge
import id.biojelan.app.ui.components.ErrorBlock
import id.biojelan.app.ui.components.NoteBox
import id.biojelan.app.ui.components.NoteTone
import id.biojelan.app.ui.components.PriceBand
import id.biojelan.app.ui.components.ScreenPad
import id.biojelan.app.ui.components.SectionHead
import id.biojelan.app.ui.components.StatCard
import id.biojelan.app.ui.components.TxRow
import id.biojelan.app.ui.components.bioCard
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
    val stock = agen?.stockLiter ?: 0.0
    val threshold = AppConfig.STOCK_THRESHOLD_LITER
    val reached = stock >= threshold

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = ScreenPad).padding(bottom = 24.dp),
    ) {
        Row(Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(greeting() + ",", style = BioTheme.type.body, color = c.muted)
                Text(user.name, style = BioTheme.type.headline, color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            OpenTogglePill(isOpen = agen?.isOpen == true, busy = state.togglingOpen, onClick = vm::toggleOpen)
        }

        PriceBand(state.price, s.priceCaptionAgen)

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
        Row(
            Modifier.fillMaxWidth().bioCard(20.dp).clickable { onGoTo(2) }.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            DropGauge(fill = (stock / threshold).toFloat(), modifier = Modifier.size(width = 46.dp, height = 57.dp))
            Column(Modifier.weight(1f)) {
                Text(s.currentStockLabel, style = BioTheme.type.eyebrow, color = c.muted)
                Text(formatLiter(stock), style = BioTheme.type.display, color = c.ink)
                Spacer(Modifier.height(6.dp))
                // Kalau Kilang sudah menugaskan Driver, tampilkan status itu — lebih akurat
                // daripada sekadar "siap dijemput" begitu stok lewat ambang.
                val pickupStatus = state.pickup?.pickupStatus
                when (pickupStatus) {
                    PickupStatus.Assigned -> BioChip(s.pickupStatusAssigned, ChipKind.Pending)
                    PickupStatus.OnTheWay -> BioChip(s.pickupStatusOtw, ChipKind.Pending)
                    PickupStatus.Arrived -> BioChip(s.pickupStatusArrived, ChipKind.Pending)
                    else -> BioChip(
                        if (reached) s.readyForPickup else s.remainingToThreshold(formatLiter(threshold - stock)),
                        if (reached) ChipKind.Done else ChipKind.Pending,
                    )
                }
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
                state.transactions.take(3).forEach { tx ->
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

        Spacer(Modifier.height(18.dp))
        BioButton(
            s.reload, vm::refresh, Modifier.fillMaxWidth(),
            style = BtnStyle.Outline,
            icon = BioIcons.Refresh,
        )
    }
}

@Composable
private fun OpenTogglePill(isOpen: Boolean, busy: Boolean, onClick: () -> Unit) {
    val c = BioTheme.colors
    val s = BioText.current
    val bg = if (isOpen) c.primaryTint else c.rustTint
    val fg = if (isOpen) c.primary else c.rust
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(bg, RoundedCornerShape(50))
            .clickable(enabled = !busy, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Drip(fg)
        Text(if (isOpen) s.open else s.closed, style = BioTheme.type.chip, color = fg)
    }
}
