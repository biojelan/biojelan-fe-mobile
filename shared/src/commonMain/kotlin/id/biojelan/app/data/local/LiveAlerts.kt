package id.biojelan.app.data.local

import id.biojelan.app.core.nowEpochMillis
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Pusat notifikasi transaksi masuk: toggle pop-up + kotak masuk riwayat.
 *
 *  - [enabled] hanya mengatur pop-up (toast). Default AKTIF.
 *  - [items] mencatat SETIAP kejadian baru yang ditemukan polling, termasuk saat pop-up dimatikan, supaya
 *    pengguna yang memilih tenang tetap bisa membuka lonceng dan melihat apa yang masuk.
 *  - Riwayat disimpan di [SessionStore] (maks [MAX_ALERT_HISTORY]) dan dihapus saat logout; toggle tidak.
 *
 * Catatan: hanya mencatat kejadian yang terlihat selagi app terbuka. Transaksi yang masuk saat app mati
 * baru bisa tercatat dengan push (FCM/APNs) + endpoint notifikasi dari backend.
 */
class LiveAlerts(
    private val store: SessionStore,
    private val notifier: SystemNotifier = NoopSystemNotifier,
) {
    private val _enabled = MutableStateFlow(store.liveAlerts ?: true)
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

    private val _items = MutableStateFlow(decodeAlerts(store.alertsJson))
    val items: StateFlow<List<AlertItem>> = _items.asStateFlow()

    fun setEnabled(value: Boolean) {
        store.liveAlerts = value
        _enabled.value = value
    }

    fun toggle() = setEnabled(!_enabled.value)

    /**
     * Catat kejadian baru ke riwayat. Kalau pop-up aktif, juga diteruskan ke [SystemNotifier] (notifikasi sistem
     * hanya benar-benar tampil bila izin diberikan dan app sedang tidak di depan; keputusan itu ada di platform).
     */
    fun record(text: String) {
        save(_items.value.withNewAlert(text, nowEpochMillis()))
        if (_enabled.value) notifier.show(text)
    }

    fun markAllRead() = save(_items.value.allMarkedRead())

    /** Dipanggil saat pengguna menghapus riwayat, atau logout. */
    fun clearInbox() = save(emptyList())

    private fun save(list: List<AlertItem>) {
        if (list == _items.value) return
        store.alertsJson = if (list.isEmpty()) null else encodeAlerts(list)
        _items.value = list
    }
}
