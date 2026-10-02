package id.biojelan.app.ui.klien

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import id.biojelan.app.ui.account.AccountViewModel
import id.biojelan.app.ui.account.ProfileTab
import id.biojelan.app.ui.components.BioSheet
import id.biojelan.app.ui.components.AutoRefresh
import id.biojelan.app.ui.components.BioTabBar
import id.biojelan.app.ui.components.CollectMessages
import id.biojelan.app.ui.components.NoteBox
import id.biojelan.app.ui.components.TabItem
import id.biojelan.app.ui.components.ToastHost
import id.biojelan.app.ui.components.rememberToastState
import id.biojelan.app.ui.icons.BioIcons
import id.biojelan.app.ui.strings.BioText
import id.biojelan.app.ui.theme.BioTheme
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun KlienMainScreen(
    onOpenAgen: (String) -> Unit,
    vm: KlienViewModel = koinViewModel(),
    account: AccountViewModel = koinViewModel(),
) {
    val c = BioTheme.colors
    val s = BioText.current
    val tabs = listOf(
        TabItem(s.tabHome, BioIcons.Home),
        TabItem(s.tabFindAgent, BioIcons.Pin),
        TabItem(s.tabHistory, BioIcons.Receipt),
        TabItem(s.tabProfile, BioIcons.User),
    )
    val state by vm.state.collectAsStateWithLifecycle()
    val user by vm.user.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(0) }
    var showId by remember { mutableStateOf(false) }
    val toast = rememberToastState()

    CollectMessages(vm.messages, toast)
    CollectMessages(account.messages, toast)
    AutoRefresh(onRefresh = vm::autoRefresh)

    Box(Modifier.fillMaxSize().background(c.paper)) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f).statusBarsPadding()) {
                when (tab) {
                    0 -> KlienHomeTab(
                        state = state,
                        name = user?.name.orEmpty(),
                        vm = vm,
                        onOpenAgen = onOpenAgen,
                        onGoTo = { tab = it },
                        onShowId = { showId = true },
                    )
                    1 -> KlienLocatorTab(state = state, onRefresh = vm::refreshAll, onOpenAgen = onOpenAgen)
                    2 -> KlienHistoryTab(state = state, vm = vm)
                    else -> user?.let { ProfileTab(it, account) }
                }
            }
            BioTabBar(tabs, selected = tab, onSelect = { tab = it })
        }
        ToastHost(toast, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 84.dp))
    }

    if (showId) {
        BioSheet(s.myIdTitle, onDismiss = { showId = false }) {
            Text(
                "Tunjukkan atau bacakan email atau nomor telepon ini ke Agen saat menjual minyak jelantah. Agen memakainya untuk mencatat transaksi atas nama Anda.",
                style = BioTheme.type.body,
                color = c.muted,
            )
            listOf(user?.email.orEmpty(), user?.phone.orEmpty()).filter { it.isNotBlank() }.forEach { value ->
                Spacer(Modifier.height(12.dp))
                SelectionContainer {
                    Text(
                        value,
                        style = BioTheme.type.monoLarge,
                        color = c.primary,
                        modifier = Modifier
                            .background(c.primaryTint, androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
                            .padding(16.dp),
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            NoteBox("Tekan lama pada teks untuk menyalin.")
        }
    }
}
