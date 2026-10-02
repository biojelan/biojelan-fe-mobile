package id.biojelan.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import id.biojelan.app.core.AppConfig
import kotlinx.coroutines.delay

/**
 * Memanggil [onRefresh] segera saat layar tampil di depan, lalu tiap [intervalMs]. Berhenti otomatis saat
 * app ke background / layar tertutup layar lain, dan langsung jalan lagi begitu kembali (jadi approval yang
 * masuk selama app ditinggal langsung kelihatan). [onRefresh] sendiri yang menjaga agar tidak menembak
 * server berlebihan (lihat `BaseViewModel.autoRefresh`).
 */
@Composable
fun AutoRefresh(
    intervalMs: Long = AppConfig.AUTO_REFRESH_INTERVAL_MS,
    onRefresh: () -> Unit,
) {
    val owner = LocalLifecycleOwner.current
    val latest by rememberUpdatedState(onRefresh)
    LaunchedEffect(owner, intervalMs) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                latest()
                delay(intervalMs)
            }
        }
    }
}
