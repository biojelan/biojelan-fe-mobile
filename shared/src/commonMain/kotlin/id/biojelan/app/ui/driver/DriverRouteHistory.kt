package id.biojelan.app.ui.driver

import id.biojelan.app.core.localEpochDayOf
import id.biojelan.app.core.parseIsoMillis
import id.biojelan.app.data.remote.DriverTransactionDto
import id.biojelan.app.data.repository.TxStatus
import id.biojelan.app.data.repository.txStatus

/**
 * Satu kartu di "Riwayat Rute".
 *
 * Backend tidak menyimpan riwayat penugasan pickup (hanya `GET /driver/pickup/status` untuk penugasan aktif),
 * jadi "rute" di sini = semua pengambilan Driver pada satu hari lokal. Angka dihitung dari transaksi yang
 * masih berlaku (bukan ditolak / dibatalkan).
 */
data class RouteHistoryEntry(
    val epochDay: Long,
    /** Jumlah Agen berbeda yang dikunjungi. */
    val agenCount: Int,
    val liters: Double,
    val value: Long,
    /** Transaksi yang masih berlaku, terlama dulu. */
    val transactions: List<DriverTransactionDto>,
    /** Selisih transaksi pertama dan terakhir hari itu; null kalau hanya ada satu transaksi. */
    val spanMinutes: Int?,
    /** Jumlah transaksi yang belum dikonfirmasi Agen (PENDING / minta batal). */
    val awaitingCount: Int,
)

private fun createdMillis(tx: DriverTransactionDto): Long = parseIsoMillis(tx.createdAt) ?: 0L

/**
 * Susun riwayat rute dari transaksi Driver: kelompokkan per hari lokal, terbaru di atas.
 * Hari yang tidak punya satu pun transaksi berlaku (semua ditolak / dibatalkan) dilewati; transaksi
 * dengan tanggal tak terbaca juga dilewati karena tidak bisa ditempatkan di hari mana pun.
 */
internal fun buildRouteHistory(transactions: List<DriverTransactionDto>): List<RouteHistoryEntry> =
    transactions
        .filter { it.txStatus != TxStatus.Rejected && it.txStatus != TxStatus.Cancelled }
        .mapNotNull { tx -> localEpochDayOf(tx.createdAt)?.let { day -> day to tx } }
        .groupBy({ it.first }, { it.second })
        .map { (day, list) ->
            val sorted = list.sortedBy { createdMillis(it) }
            val span = if (sorted.size < 2) null
            else ((createdMillis(sorted.last()) - createdMillis(sorted.first())) / 60_000L).toInt()
            RouteHistoryEntry(
                epochDay = day,
                agenCount = sorted.map { it.agenId.ifBlank { it.agenName } }.distinct().size,
                liters = sorted.sumOf { it.volumeLiter },
                value = sorted.sumOf { it.totalPrice },
                transactions = sorted,
                spanMinutes = span,
                awaitingCount = sorted.count { it.txStatus == TxStatus.Pending || it.txStatus == TxStatus.CancelRequested },
            )
        }
        .sortedByDescending { it.epochDay }

/** 130 -> "2j 10m", 85 -> "1j 25m", 45 -> "45m", 120 -> "2j 00m". Negatif dianggap 0. */
internal fun formatSpan(minutes: Int): String {
    val m = minutes.coerceAtLeast(0)
    val h = m / 60
    val r = m % 60
    return if (h == 0) "${r}m" else "${h}j ${r.toString().padStart(2, '0')}m"
}
