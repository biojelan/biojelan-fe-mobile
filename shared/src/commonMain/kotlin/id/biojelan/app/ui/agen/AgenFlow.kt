package id.biojelan.app.ui.agen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
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
import id.biojelan.app.ui.components.AutoRefresh
import id.biojelan.app.ui.components.BioTabBar
import id.biojelan.app.ui.components.CollectMessages
import id.biojelan.app.ui.components.TabItem
import id.biojelan.app.ui.components.ToastHost
import id.biojelan.app.ui.components.rememberToastState
import id.biojelan.app.ui.icons.BioIcons
import id.biojelan.app.ui.strings.BioText
import id.biojelan.app.ui.theme.BioTheme
import org.koin.compose.viewmodel.koinViewModel

/** Alur pengguna berperan Agen: satu layar dengan 4 tab. */
@Composable
fun AgenFlow(vm: AgenViewModel = koinViewModel(), account: AccountViewModel = koinViewModel()) {
    val c = BioTheme.colors
    val s = BioText.current
    val tabs = listOf(
        TabItem(s.tabHome, BioIcons.Home),
        TabItem(s.tabTransactions, BioIcons.Receipt),
        TabItem(s.tabStock, BioIcons.Drop),
        TabItem(s.tabProfile, BioIcons.User),
    )
    val state by vm.state.collectAsStateWithLifecycle()
    val user by vm.user.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(0) }
    var showNew by remember { mutableStateOf(false) }
    val toast = rememberToastState()

    CollectMessages(vm.messages, toast)
    CollectMessages(account.messages, toast)
    AutoRefresh(onRefresh = vm::autoRefresh)

    Box(Modifier.fillMaxSize().background(c.paper)) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f).statusBarsPadding()) {
                val current = user
                if (current != null) {
                    when (tab) {
                        0 -> AgenHomeTab(state, current, vm, onNewTransaction = { showNew = true }, onGoTo = { tab = it })
                        1 -> AgenTransactionsTab(state, vm, onNewTransaction = { showNew = true })
                        2 -> AgenStockTab(state, current, vm)
                        else -> ProfileTab(current, account)
                    }
                }
            }
            BioTabBar(tabs, selected = tab, onSelect = { tab = it })
        }
        ToastHost(toast, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 84.dp))
    }

    if (showNew) {
        NewTransactionSheet(
            price = state.price,
            creating = state.creating,
            vm = vm,
            onSubmit = { contact, volume ->
                vm.createTransaction(contact, volume) {
                    showNew = false
                    tab = 1
                }
            },
            onDismiss = { showNew = false },
        )
    }
}
