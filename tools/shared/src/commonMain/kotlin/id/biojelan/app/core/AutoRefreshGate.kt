package id.biojelan.app.core

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Penjaga polling berkala: [trigger] boleh dipanggil sesering apa pun; [work] hanya jalan kalau
 *  - tidak ada [work] sebelumnya yang masih berjalan,
 *  - [canRun] true (mis. tidak sedang ada aksi pengguna),
 *  - sudah ≥ [minGapMs] sejak [work] terakhir DIMULAI (atau sejak gate dibuat — pemilik biasanya baru saja
 *    memuat datanya sendiri). Trigger yang ditolak tidak menghabiskan jarak itu.
 *
 * Exception dari [work] ditelan di sini: dijalankan di scope ViewModel tanpa handler, exception yang bocor
 * akan mematikan app hanya karena polling latar belakang gagal.
 */
class AutoRefreshGate(
    private val scope: CoroutineScope,
    private val minGapMs: Long,
    private val now: () -> Long,
    private val canRun: () -> Boolean = { true },
    private val work: suspend () -> Unit,
) {
    private var job: Job? = null
    private var lastStartedAt: Long = now()

    fun trigger() {
        if (job?.isActive == true || !canRun()) return
        val t = now()
        val elapsed = t - lastStartedAt
        // elapsed negatif = jam perangkat diputar mundur; jangan sampai polling mati sampai jam "menyusul".
        if (elapsed in 0 until minGapMs) return
        lastStartedAt = t
        job = scope.launch {
            try {
                work()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Polling gagal diam-diam: data lama tetap tampil, putaran berikutnya mencoba lagi.
            }
        }
    }

    /** Batalkan [work] yang sedang berjalan (mis. pengguna baru menekan terima/tolak). */
    fun cancel() {
        job?.cancel()
        job = null
    }
}
