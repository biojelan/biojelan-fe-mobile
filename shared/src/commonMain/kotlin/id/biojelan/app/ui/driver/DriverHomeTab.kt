package id.biojelan.app.ui.driver

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import id.biojelan.app.core.firstNameOf
import id.biojelan.app.core.formatLiter
import id.biojelan.app.core.formatNumber
import id.biojelan.app.core.formatRupiahCompact
import id.biojelan.app.core.initialsOf
import id.biojelan.app.ui.components.BioButton
import id.biojelan.app.ui.components.BtnStyle
import id.biojelan.app.ui.components.ErrorBlock
import id.biojelan.app.ui.components.FadeInItem
import id.biojelan.app.ui.components.HomeHeader
import id.biojelan.app.ui.components.HomeScroll
import id.biojelan.app.ui.components.SectionHead
import id.biojelan.app.ui.components.bioCard
import id.biojelan.app.ui.components.rememberHeldLoading
import id.biojelan.app.ui.icons.BioIcons
import id.biojelan.app.ui.strings.BioText
import id.biojelan.app.ui.theme.BioTheme

/**
 * Beranda Driver mengikuti prototype "Rute": banner tugas baru, badge checkpoint, ringkasan rute,
 * peta bernomor, lalu "Urutan kunjungan". Ketuk satu stop untuk membuka detailnya (stepper + aksi).
 */
@Composable
fun DriverHomeTab(
    state: DriverUiState,
    name: String,
    vm: DriverViewModel,
    onGoTo: (Int) -> Unit,
    onNewTransaction: () -> Unit,
    onRecordFor: (agenContact: String) -> Unit,
) {
    val c = BioTheme.colors
    val s = BioText.current
    val firstName = firstNameOf(name, s.roleDriver)

    // Tanpa remember: nama Agen datang dari cache lookup yang bukan state, jadi dihitung ulang tiap recompose.
    val stops = buildRouteStops(state.pickup, state.transactions) { id -> vm.agenLocation(id)?.name }
    var openKey by rememberSaveable { mutableStateOf<String?>(null) }
    var dismissedBannerFor by rememberSaveable { mutableStateOf<String?>(null) }
    val openIndex = stops.indexOfFirst { it.key == openKey }

    if (openIndex >= 0) {
        val stop = stops[openIndex]
        DriverStopDetail(
            index = openIndex,
            stop = stop,
            agen = vm.agenLocation(stop.agenId),
            busy = state.pickupBusy,
            onBack = { openKey = null },
            onUpdate = vm::updatePickup,
            onRecord = onRecordFor,
        )
        return
    }

    HomeScroll {
        HomeHeader(name = firstName, initials = initialsOf(name), role = s.roleDriver)

        val pickup = state.pickup
        val currentStop = stops.lastOrNull { it.isPickup }
        if (pickup != null && currentStop != null &&
            currentStop.status == StopStatus.Assigned && dismissedBannerFor != pickup.pickupId
        ) {
            NewTaskBanner(
                pickupId = pickup.pickupId,
                agenName = currentStop.name.ifBlank { vm.agenLocation(currentStop.agenId)?.name.orEmpty() },
                onClose = { dismissedBannerFor = pickup.pickupId },
            )
        }

        GpsBadge(s.driverGpsBadge)

        when {
            stops.isNotEmpty() -> {
                Spacer(Modifier.height(16.dp))
                RouteSummaryCard(
                    pickupId = pickup?.pickupId,
                    stopCount = stops.size,
                    stats = listOf(
                        state.todayCount.toString() to s.todayLabel,
                        formatNumber(state.todayLiters, 1) + " L" to s.volumeToday,
                        formatRupiahCompact(state.todayValue) to s.valueToday,
                    ),
                )
                Spacer(Modifier.height(16.dp))
                RouteMapCard(stops)

                SectionHead(s.driverVisitOrder)
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    stops.forEachIndexed { idx, stop ->
                        FadeInItem(idx) {
                            val agen = vm.agenLocation(stop.agenId)
                            val subtitle = listOfNotNull(
                                stop.volumeLiter?.let { formatLiter(it) },
                                agen?.address?.takeIf { it.isNotBlank() },
                            ).joinToString(" · ")
                            StopRow(
                                idx,
                                stop.copy(name = stop.name.ifBlank { agen?.name.orEmpty() }),
                                subtitle,
                                onClick = { openKey = stop.key },
                            )
                        }
                    }
                }
            }
            state.pickupLoading || (state.loading && state.transactions.isEmpty()) -> {
                Spacer(Modifier.height(16.dp))
                Row(
                    Modifier.fillMaxWidth().bioCard(16.dp).padding(20.dp),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator(color = c.primary, strokeWidth = 3.dp, modifier = Modifier.size(20.dp))
                }
            }
            state.pickupError != null -> {
                Spacer(Modifier.height(16.dp))
                ErrorBlock(state.pickupError, onRetry = vm::refresh)
            }
            state.error != null && state.transactions.isEmpty() -> {
                Spacer(Modifier.height(16.dp))
                ErrorBlock(state.error, onRetry = vm::refresh)
            }
            else -> {
                SectionHead(s.driverPickupSection)
                DriverIdleState(onRecord = onNewTransaction, onHistory = { onGoTo(1) })
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

/** Kondisi "belum ada penugasan": adegan peta beranimasi + dua aksi cepat (tanpa blok teks panjang). */
@Composable
private fun DriverIdleState(onRecord: () -> Unit, onHistory: () -> Unit) {
    val s = BioText.current
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        DriverIdleScene(liveText = s.driverIdleLive)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BioButton(s.driverRecordButton, onRecord, Modifier.weight(1f), icon = BioIcons.Plus)
            BioButton(s.driverIdleHistory, onHistory, Modifier.weight(1f), style = BtnStyle.Outline, icon = BioIcons.Receipt)
        }
    }
}

/** GPS badge (prototype .gps-badge): titik berdenyut + keterangan checkpoint. */
@Composable
private fun GpsBadge(text: String) {
    val c = BioTheme.colors
    val infiniteTransition = rememberInfiniteTransition(label = "gps")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.8f,
        animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "gpsScale",
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "gpsAlpha",
    )
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
            .bioCard(14.dp)
            .padding(horizontal = 13.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(19.dp)) {
            Box(
                Modifier
                    .size(19.dp)
                    .graphicsLayer(scaleX = pulseScale, scaleY = pulseScale, alpha = pulseAlpha)
                    .border(2.dp, c.primary, RoundedCornerShape(50)),
            )
            Box(Modifier.size(9.dp).background(c.primary, RoundedCornerShape(50)))
        }
        Text(text, style = BioTheme.type.small, color = c.inkSoft)
    }
}
