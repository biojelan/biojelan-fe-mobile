package id.biojelan.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.biojelan.app.core.AppConfig
import id.biojelan.app.core.AutoRefreshGate
import id.biojelan.app.core.DedupeWindow
import id.biojelan.app.core.nowEpochMillis
import id.biojelan.app.data.local.LiveAlerts
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** ViewModel dasar dengan kanal pesan sekali-pakai (toast) dan mesin auto-refresh berkala. */
abstract class BaseViewModel : ViewModel() {
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    // Pesan identik dalam 2 detik jadi satu toast (mis. satu refresh dengan beberapa request gagal karena sebab sama).
    private val toastDedupe = DedupeWindow(windowMs = 2_000L, now = { nowEpochMillis() })

    protected fun toast(text: String) {
        if (toastDedupe.shouldEmit(text)) _messages.tryEmit(text)
    }

    /** Pusat notifikasi. Diisi role yang punya polling; null = tidak ada notifikasi. */
    protected open val liveAlerts: LiveAlerts? get() = null

    /**
     * Kejadian yang datang dari luar (transaksi/permintaan/penugasan baru yang ditemukan polling). Selalu
     * dicatat ke kotak masuk (lonceng); toast-nya saja yang mengikuti toggle. Umpan balik atas aksi pengguna
     * sendiri (terima, tolak, gagal muat) tetap pakai [toast].
     */
    protected fun alert(text: String) {
        val alerts = liveAlerts
        alerts?.record(text)
        if (alerts == null || alerts.enabled.value) toast(text)
    }

    /** Umpan balik refresh manual (tombol muat ulang) yang berhasil tanpa error. */
    protected fun toastUpToDate() = toast(MSG_UP_TO_DATE)

    private val autoRefresher = AutoRefreshGate(
        scope = viewModelScope,
        minGapMs = AppConfig.AUTO_REFRESH_MIN_GAP_MS,
        now = { nowEpochMillis() },
        canRun = { canAutoRefresh() },
        work = { refreshSilently() },
    )

    /**
     * Muat ulang diam-diam untuk polling berkala: tanpa spinner, dan kalau gagal data lama dibiarkan
     * (tidak memunculkan error di layar yang sudah berisi). Diimplementasi tiap role.
     */
    protected open suspend fun refreshSilently() {}

    /** False saat ada aksi (terima/tolak/batal/buat transaksi) atau refresh manual berjalan — polling ditunda. */
    protected open fun canAutoRefresh(): Boolean = true

    /**
     * Dipanggil layar secara berkala dan saat app kembali ke depan. Aman dipanggil sering: aturan kapan
     * benar-benar jalan ada di [AutoRefreshGate] (teruji di commonTest).
     */
    fun autoRefresh() = autoRefresher.trigger()

    /**
     * Batalkan polling yang sedang di udara. Wajib dipanggil di awal aksi pengguna dan refresh manual:
     * respons polling yang lebih lama bisa tiba SETELAH hasil aksi dan menimpanya dengan data basi
     * (mis. transaksi yang baru diterima muncul lagi sebagai "menunggu").
     */
    protected fun cancelAutoRefresh() = autoRefresher.cancel()

    private companion object {
        const val MSG_UP_TO_DATE = "Data sudah sudah diperbarui."
    }
}