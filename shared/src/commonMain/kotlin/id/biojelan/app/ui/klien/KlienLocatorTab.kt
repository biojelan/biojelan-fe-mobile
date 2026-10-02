package id.biojelan.app.ui.klien

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import id.biojelan.app.core.agenInitials
import id.biojelan.app.core.formatOperatingHours
import id.biojelan.app.data.remote.AgenSummaryDto
import id.biojelan.app.ui.components.AvatarBox
import id.biojelan.app.ui.components.BioChip
import id.biojelan.app.ui.components.ChipKind
import id.biojelan.app.ui.components.CircleIconButton
import id.biojelan.app.ui.components.EmptyBlock
import id.biojelan.app.ui.components.ErrorBlock
import id.biojelan.app.ui.components.LoadingBlock
import id.biojelan.app.ui.components.MapPreview
import id.biojelan.app.ui.components.ScreenPad
import id.biojelan.app.ui.components.ScreenTopBar
import id.biojelan.app.ui.components.SearchField
import id.biojelan.app.ui.icons.BioIcons
import id.biojelan.app.ui.strings.BioText
import id.biojelan.app.ui.theme.BioTheme
import kotlinx.coroutines.launch

private val MapHeight = 250.dp
private val SheetOverlap = 24.dp

/**
 * Cari Agen: peta di atas (pin bisa diketuk) dan daftar Agen sebagai sheet yang menimpa dasar peta.
 * Peta tetap diam, hanya daftar di dalam sheet yang bergulir.
 */
@Composable
fun KlienLocatorTab(
    state: KlienUiState,
    onRefresh: () -> Unit,
    onOpenAgen: (String) -> Unit,
) {
    val c = BioTheme.colors
    val s = BioText.current
    var query by rememberSaveable { mutableStateOf("") }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val filtered = remember(state.agens, query) {
        val q = query.trim().lowercase()
        state.agens
            .filter { q.isEmpty() || it.name.lowercase().contains(q) || it.address.lowercase().contains(q) }
            .sortedWith(compareBy<AgenSummaryDto> { it.name.lowercase() })
    }
    val highlight = filtered.indexOfFirst { it.agenId == selectedId }
    val openCount = filtered.count { it.isOpen }

    Column(Modifier.fillMaxSize()) {
        ScreenTopBar(s.tabFindAgent, actions = {
            CircleIconButton(BioIcons.Refresh, onClick = onRefresh, contentDescription = s.reload, loading = state.agensLoading)
        })
        SearchField(query, { query = it; selectedId = null }, "Cari nama atau alamat Agen", Modifier.padding(horizontal = ScreenPad))
        Spacer(Modifier.height(12.dp))

        Box(Modifier.weight(1f).fillMaxWidth()) {
            MapPreview(
                points = filtered.map { it.latitude to it.longitude },
                height = MapHeight,
                highlight = highlight,
                cornerRadius = 0.dp,
                onPinClick = { index ->
                    selectedId = filtered[index].agenId
                    scope.launch { listState.animateScrollToItem(index) }
                },
            )

            val sheetShape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(top = MapHeight - SheetOverlap)
                    .clip(sheetShape)
                    .background(c.paper, sheetShape)
                    .border(1.dp, c.line, sheetShape),
            ) {
                // pegangan + judul sheet
                Box(Modifier.fillMaxWidth().padding(top = 8.dp), contentAlignment = Alignment.Center) {
                    Box(Modifier.width(36.dp).height(4.dp).background(c.line, RoundedCornerShape(50)))
                }
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = ScreenPad).padding(top = 10.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(s.sectionAgenBioJelan, style = BioTheme.type.sectionTitle, color = c.ink, modifier = Modifier.weight(1f))
                    if (filtered.isNotEmpty()) {
                        BioChip("${filtered.size}", ChipKind.Neutral)
                        if (openCount > 0) BioChip("$openCount ${s.open}", ChipKind.Open)
                    }
                }

                when {
                    state.agensLoading && state.agens.isEmpty() -> LoadingBlock()
                    state.agensError != null && state.agens.isEmpty() -> ErrorBlock(state.agensError, onRefresh)
                    filtered.isEmpty() -> EmptyBlock(BioIcons.Search, "Agen tidak ditemukan", "Coba kata kunci lain.")
                    else -> LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = ScreenPad, end = ScreenPad, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        itemsIndexed(filtered, key = { _, agen -> agen.agenId }) { _, agen ->
                            AgenRow(
                                agen = agen,
                                selected = agen.agenId == selectedId,
                                onClick = { onOpenAgen(agen.agenId) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AgenRow(agen: AgenSummaryDto, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = BioTheme.colors
    val s = BioText.current
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (selected) c.primaryTint else c.surface, shape)
            .border(if (selected) 1.5.dp else 1.dp, if (selected) c.primary else c.line, shape)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AvatarBox(agenInitials(agen.name))
        Column(Modifier.weight(1f)) {
            Text(agen.name, style = BioTheme.type.cardTitle, color = c.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(agen.address, style = BioTheme.type.small, color = c.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                formatOperatingHours(agen.openAt, agen.closeAt, agen.openDays),
                style = BioTheme.type.caption,
                color = c.inkSoft,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        BioChip(if (agen.isOpen) s.open else s.closed, if (agen.isOpen) ChipKind.Open else ChipKind.Closed)
    }
}
