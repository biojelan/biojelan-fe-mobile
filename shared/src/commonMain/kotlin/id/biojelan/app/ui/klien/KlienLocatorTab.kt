package id.biojelan.app.ui.klien

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import id.biojelan.app.ui.components.FilterPill
import id.biojelan.app.ui.components.LoadingBlock
import id.biojelan.app.ui.components.MapPreview
import id.biojelan.app.ui.components.ScreenPad
import id.biojelan.app.ui.components.ScreenTopBar
import id.biojelan.app.ui.components.SearchField
import id.biojelan.app.ui.components.bioCard
import id.biojelan.app.ui.icons.BioIcons
import id.biojelan.app.ui.strings.BioText
import id.biojelan.app.ui.theme.BioTheme

@Composable
fun KlienLocatorTab(
    state: KlienUiState,
    onRefresh: () -> Unit,
    onOpenAgen: (String) -> Unit,
) {
    val s = BioText.current
    var query by rememberSaveable { mutableStateOf("") }
    var onlyOpen by rememberSaveable { mutableStateOf(false) }

    val filtered = remember(state.agens, query, onlyOpen) {
        val q = query.trim().lowercase()
        state.agens
            .filter { !onlyOpen || it.isOpen }
            .filter { q.isEmpty() || it.name.lowercase().contains(q) || it.address.lowercase().contains(q) }
            .sortedWith(compareByDescending<AgenSummaryDto> { it.isOpen }.thenBy { it.name.lowercase() })
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            ScreenTopBar(s.tabFindAgent, actions = {
                CircleIconButton(BioIcons.Refresh, onClick = onRefresh, contentDescription = s.reload)
            })
        }
        item { SearchField(query, { query = it }, "Cari nama atau alamat Agen", Modifier.padding(horizontal = ScreenPad)) }
        item {
            Row(Modifier.padding(horizontal = ScreenPad), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterPill("Semua", selected = !onlyOpen, onClick = { onlyOpen = false })
                FilterPill(s.open, selected = onlyOpen, onClick = { onlyOpen = true })
            }
        }
        if (filtered.isNotEmpty()) {
            item {
                MapPreview(
                    points = filtered.map { it.latitude to it.longitude },
                    modifier = Modifier.padding(horizontal = ScreenPad),
                )
            }
        }
        when {
            state.agensLoading && state.agens.isEmpty() -> item { LoadingBlock() }
            state.agensError != null && state.agens.isEmpty() -> item { ErrorBlock(state.agensError, onRefresh) }
            filtered.isEmpty() -> item {
                EmptyBlock(BioIcons.Search, "Agen tidak ditemukan", "Coba kata kunci lain atau hapus filter.")
            }
            else -> items(filtered) { agen ->
                AgenRow(agen, Modifier.padding(horizontal = ScreenPad)) { onOpenAgen(agen.agenId) }
            }
        }
    }
}

@Composable
private fun AgenRow(agen: AgenSummaryDto, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val c = BioTheme.colors
    val s = BioText.current
    Row(
        modifier = modifier.fillMaxWidth().bioCard(16.dp).clickable(onClick = onClick).padding(14.dp),
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
