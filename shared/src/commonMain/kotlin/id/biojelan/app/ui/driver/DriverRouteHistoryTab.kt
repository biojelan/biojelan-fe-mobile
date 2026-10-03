package id.biojelan.app.ui.driver

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import id.biojelan.app.core.formatEpochDay
import id.biojelan.app.core.formatLiter
import id.biojelan.app.core.formatNumber
import id.biojelan.app.core.formatRupiah
import id.biojelan.app.core.formatTimeOnly
import id.biojelan.app.core.initialsOf
import id.biojelan.app.core.todayEpochDay
import id.biojelan.app.ui.chipKind
import id.biojelan.app.ui.countsForVolume
import id.biojelan.app.ui.components.BioChip
import id.biojelan.app.ui.components.BioSheet
import id.biojelan.app.ui.components.ChipKind
import id.biojelan.app.ui.components.CircleIconButton
import id.biojelan.app.ui.components.EmptyBlock
import id.biojelan.app.ui.components.ErrorBlock
import id.biojelan.app.ui.components.LedgerHero
import id.biojelan.app.ui.components.LoadingBlock
import id.biojelan.app.ui.components.ScreenPad
import id.biojelan.app.ui.components.ScreenTopBar
import id.biojelan.app.ui.components.TxRow
import id.biojelan.app.ui.components.WeekBars
import id.biojelan.app.ui.components.weekVolumeBars
import id.biojelan.app.ui.components.bioCard
import id.biojelan.app.ui.driverLabel
import id.biojelan.app.ui.icons.BioIcons
import id.biojelan.app.ui.strings.BioStrings
import id.biojelan.app.ui.strings.BioText
import id.biojelan.app.data.repository.txStatus
import id.biojelan.app.ui.theme.BioTheme

/**
 * Tab "Riwayat Rute" (prototype #screen-driver-riwayat): kartu per rute berisi judul, tanggal, dan tiga
 * angka ringkas (Agen, volume terkumpul, rentang waktu). Ketuk kartu untuk melihat daftar kunjungannya.
 */
@Composable
fun DriverRouteHistoryTab(state: DriverUiState, vm: DriverViewModel) {
    val c = BioTheme.colors
    val s = BioText.current
    val history = remember(state.transactions) { buildRouteHistory(state.transactions) }
    val bars = remember(state.transactions) {
        weekVolumeBars(state.transactions.filter { it.txStatus.countsForVolume() }.map { it.createdAt to it.volumeLiter })
    }
    var openDay by remember { mutableStateOf<Long?>(null) }
    var selectedTxId by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ScreenTopBar(s.routeHistoryTitle, actions = {
                CircleIconButton(BioIcons.Refresh, onClick = vm::manualRefresh, contentDescription = s.reload, loading = state.loading)
            })
        }
        // Ringkasan hasil transaksi hari ini (dulu di tab Transaksi), ditaruh di atas daftar rute.
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
            history.isEmpty() -> item {
                EmptyBlock(BioIcons.Clock, s.routeHistoryEmptyTitle, s.routeHistoryEmptyHint)
            }
            else -> {
                item {
                    Text(
                        s.routeHistoryNote,
                        style = BioTheme.type.small,
                        color = c.muted,
                        modifier = Modifier.padding(horizontal = ScreenPad),
                    )
                }
                items(history, key = { it.epochDay }) { entry ->
                    RouteHistoryCard(entry, s, onClick = { openDay = entry.epochDay })
                }
            }
        }
    }

    val opened = history.firstOrNull { it.epochDay == openDay }
    if (opened != null) {
        BioSheet(s.routeHistoryItem(formatEpochDay(opened.epochDay)), onDismiss = { openDay = null }) {
            RouteHistorySummaryRow(opened, s)
            Spacer(Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Terbaru di atas, sama seperti daftar transaksi.
                opened.transactions.asReversed().forEach { tx ->
                    val name = tx.agenName.ifBlank { s.driverStopUnknownAgen }
                    TxRow(
                        avatar = initialsOf(name),
                        title = name,
                        subtitle = formatTimeOnly(tx.createdAt) + " · " + formatLiter(tx.volumeLiter),
                        amount = formatRupiah(tx.totalPrice),
                        statusText = tx.txStatus.driverLabel(asAgen = false),
                        statusKind = tx.txStatus.chipKind(),
                        onClick = { selectedTxId = tx.transactionId },
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }

    val selected = state.transactions.firstOrNull { it.transactionId == selectedTxId }
    if (selected != null) {
        DriverTxDetailSheet(
            tx = selected,
            busy = state.busyTxId == selected.transactionId,
            agenLocation = vm.agenLocation(selected.agenId),
            onRequestCancel = { vm.requestCancel(selected.transactionId) { selectedTxId = null } },
            onDismiss = { selectedTxId = null },
        )
    }
}

private fun routeDateLabel(epochDay: Long, s: BioStrings): String = when (epochDay) {
    todayEpochDay() -> s.todayLabel
    todayEpochDay() - 1 -> s.yesterdayLabel
    else -> formatEpochDay(epochDay)
}

/** Kartu satu rute (prototype .hist-card): judul + tanggal di atas, tiga angka kecil di bawah. */
@Composable
private fun RouteHistoryCard(entry: RouteHistoryEntry, s: BioStrings, onClick: () -> Unit) {
    val c = BioTheme.colors
    Column(
        Modifier
            .padding(horizontal = ScreenPad)
            .fillMaxWidth()
            .bioCard(16.dp)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                s.routeHistoryItem(formatEpochDay(entry.epochDay)),
                style = BioTheme.type.cardTitle,
                color = c.ink,
                modifier = Modifier.weight(1f),
            )
            if (entry.epochDay == todayEpochDay() || entry.epochDay == todayEpochDay() - 1) {
                Text(routeDateLabel(entry.epochDay, s), style = BioTheme.type.caption, color = c.muted)
            }
        }
        RouteHistorySummaryRow(entry, s)
        if (entry.awaitingCount > 0) {
            BioChip(s.routeHistoryAwaiting(entry.awaitingCount), ChipKind.Pending)
        }
    }
}

/** Baris tiga angka: "3 Agen · 1.480 L terkumpul · 2j 10m rentang". */
@Composable
private fun RouteHistorySummaryRow(entry: RouteHistoryEntry, s: BioStrings) {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        MiniStat(entry.agenCount.toString(), s.routeHistoryAgenLabel)
        MiniStat(formatNumber(entry.liters, 1) + " L", s.routeHistoryCollectedLabel)
        entry.spanMinutes?.let { MiniStat(formatSpan(it), s.routeHistorySpanLabel) }
    }
}

@Composable
private fun MiniStat(number: String, label: String) {
    val c = BioTheme.colors
    Text(
        buildAnnotatedString {
            withStyle(SpanStyle(fontWeight = FontWeight.ExtraBold, color = c.ink)) { append(number) }
            append(" ")
            append(label)
        },
        style = BioTheme.type.small,
        color = c.inkSoft,
        maxLines = 1,
    )
}
