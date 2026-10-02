package id.biojelan.app.ui.guest

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.biojelan.app.ui.components.BioButton
import id.biojelan.app.ui.components.BioSheet
import id.biojelan.app.ui.components.BioTabBar
import id.biojelan.app.ui.components.BtnStyle
import id.biojelan.app.ui.components.PriceBand
import id.biojelan.app.ui.components.ScreenPad
import id.biojelan.app.ui.components.TabItem
import id.biojelan.app.ui.icons.BioIcons
import id.biojelan.app.ui.klien.KlienLocatorTab
import id.biojelan.app.ui.klien.KlienUiState
import id.biojelan.app.ui.strings.BioText
import id.biojelan.app.ui.theme.BioTheme
import org.koin.compose.viewmodel.koinViewModel

private const val LOCATOR_TAB_INDEX = 1

/**
 * Alur untuk pengunjung yang memilih "Lihat sebagai tamu": boleh melihat lokasi Agen dan
 * harga jelantah terkini (tab Cari Agen). Tab lain dan baris Agen memicu modal ajakan daftar
 * (menyamai perilaku prototype HTML).
 */
@Composable
fun GuestScreen(vm: GuestViewModel = koinViewModel()) {
    val c = BioTheme.colors
    val s = BioText.current
    val guestTabs = listOf(
        TabItem(s.tabHome, BioIcons.Home),
        TabItem(s.tabFindAgent, BioIcons.Pin),
        TabItem(s.tabHistory, BioIcons.Receipt),
        TabItem(s.tabProfile, BioIcons.User),
    )
    val state by vm.state.collectAsStateWithLifecycle()
    var showUpsell by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().background(c.paper)) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f).statusBarsPadding()) {
                Column(Modifier.fillMaxSize()) {
                    GuestBanner(onClick = vm::exitToRegister)
                    PriceBand(
                        price = state.price,
                        caption = s.guestPriceCaption,
                        modifier = Modifier.padding(horizontal = ScreenPad, vertical = 4.dp),
                    )
                    Box(Modifier.weight(1f)) {
                        KlienLocatorTab(
                            state = KlienUiState(
                                agens = state.agens,
                                agensLoading = state.loading,
                                agensError = state.error,
                            ),
                            onRefresh = vm::loadAgens,
                            onOpenAgen = { showUpsell = true },
                        )
                    }
                }
            }
            BioTabBar(
                items = guestTabs,
                selected = LOCATOR_TAB_INDEX,
                onSelect = { index -> if (index != LOCATOR_TAB_INDEX) showUpsell = true },
            )
        }
    }

    if (showUpsell) {
        GuestUpsellSheet(
            onDismiss = { showUpsell = false },
            onRegister = {
                showUpsell = false
                vm.exitToRegister()
            },
        )
    }
}

@Composable
private fun GuestBanner(onClick: () -> Unit) {
    val c = BioTheme.colors
    val s = BioText.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ScreenPad, vertical = 10.dp)
            .background(c.primaryTint, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(BioIcons.Eye, contentDescription = null, tint = c.primary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(s.guestBannerTitle, style = BioTheme.type.bodyBold, color = c.primary)
            Text(s.guestBannerBody, style = BioTheme.type.small, color = c.primary)
        }
    }
}

@Composable
private fun GuestUpsellSheet(onDismiss: () -> Unit, onRegister: () -> Unit) {
    val c = BioTheme.colors
    val s = BioText.current
    BioSheet(title = s.guestUpsellTitle, onDismiss = onDismiss) {
        Text(s.guestUpsellBody, style = BioTheme.type.body, color = c.inkSoft)
        Spacer(Modifier.height(18.dp))
        BioButton(s.registerNow, onClick = onRegister, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp))
        BioButton(s.maybeLater, onClick = onDismiss, style = BtnStyle.Outline, modifier = Modifier.fillMaxWidth())
    }
}
