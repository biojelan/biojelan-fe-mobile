package id.biojelan.app.ui.driver

import id.biojelan.app.core.isToday
import id.biojelan.app.core.parseIsoMillis
import id.biojelan.app.data.remote.DriverTransactionDto
import id.biojelan.app.data.remote.PickupStatusDto
import id.biojelan.app.data.repository.PickupStatus
import id.biojelan.app.data.repository.TxStatus
import id.biojelan.app.data.repository.pickupStatus
import id.biojelan.app.data.repository.txStatus

/**
 * Status satu stop di rute Driver, mengikuti stepper prototype:
 * Assigned → On the Way → Arrived → Menunggu Konfirmasi → Completed.
 *
 * Tidak ada status "Checked": backend memang tidak punya, dan hitung dirigen / cek kualitas tidak
 * dilakukan di app Driver. Setelah tiba, Driver cukup mencatat pengambilan lalu menunggu Agen
 * mengonfirmasi (transaksi PENDING → ACCEPTED).
 */
enum class StopStatus { Assigned, OnTheWay, Arrived, AwaitingConfirm, Completed, Skipped, Rejected }

/**
 * Satu baris di "Urutan kunjungan".
 *
 * API tidak punya konsep rute multi-Agen (pickup terikat 1:1 ke satu Agen), jadi daftar ini disusun dari
 * dua sumber yang memang ada: penugasan pickup aktif (stop yang sedang berjalan) dan transaksi Driver
 * hari ini (stop yang sudah dikunjungi, dikelompokkan per Agen).
 */
data class RouteStop(
    val agenId: String,
    val name: String,
    /** Volume dari transaksi terakhir ke Agen ini; null kalau belum ada transaksi. */
    val volumeLiter: Double?,
    val status: StopStatus,
    /** True untuk stop yang berasal dari penugasan pickup aktif (satu-satunya yang bisa diubah statusnya). */
    val isPickup: Boolean,
    val pickupId: String? = null,
) {
    val key: String get() = agenId.ifBlank { "pickup-" + pickupId.orEmpty() }
    val isFinal: Boolean get() = status == StopStatus.Completed || status == StopStatus.Skipped || status == StopStatus.Rejected
}

private fun TxStatus.toStopStatus(): StopStatus = when (this) {
    TxStatus.Accepted -> StopStatus.Completed
    TxStatus.Rejected -> StopStatus.Rejected
    TxStatus.Cancelled -> StopStatus.Skipped
    TxStatus.Pending, TxStatus.CancelRequested, TxStatus.Unknown -> StopStatus.AwaitingConfirm
}

private fun createdMillis(tx: DriverTransactionDto): Long = parseIsoMillis(tx.createdAt) ?: 0L

/**
 * Status stop untuk penugasan pickup aktif. Saat ARRIVED, status ditentukan oleh transaksi pengambilan
 * yang dibuat Driver SETELAH tiba (supaya transaksi lama ke Agen yang sama tidak ikut dihitung):
 * PENDING → menunggu Agen, ACCEPTED → selesai, REJECTED → tetap Arrived agar bisa dicatat ulang.
 */
internal fun pickupStopStatus(pickup: PickupStatusDto, transactions: List<DriverTransactionDto>): StopStatus =
    when (pickup.pickupStatus) {
        PickupStatus.OnTheWay -> StopStatus.OnTheWay
        PickupStatus.Arrived -> {
            val arrivedAt = parseIsoMillis(pickup.updatedAt)
            val latest = transactions
                .filter { it.agenId == pickup.agenId && it.agenId.isNotBlank() }
                .filter { arrivedAt == null || createdMillis(it) >= arrivedAt - ARRIVAL_TOLERANCE_MS }
                .maxByOrNull { createdMillis(it) }
            val st = latest?.txStatus?.toStopStatus()
            if (st == null || st == StopStatus.Rejected || st == StopStatus.Skipped) StopStatus.Arrived else st
        }
        PickupStatus.Completed -> StopStatus.Completed
        PickupStatus.Cancelled -> StopStatus.Skipped
        PickupStatus.Assigned, PickupStatus.Unknown -> StopStatus.Assigned
    }

private const val ARRIVAL_TOLERANCE_MS = 5_000L

/** True bila pickup masih ARRIVED tapi Agen sudah menerima transaksinya — saatnya menutup penugasan. */
internal fun shouldAutoComplete(pickup: PickupStatusDto?, transactions: List<DriverTransactionDto>): Boolean =
    pickup != null &&
        pickup.pickupStatus == PickupStatus.Arrived &&
        pickupStopStatus(pickup, transactions) == StopStatus.Completed

/**
 * Susun urutan kunjungan: stop yang sudah lewat (urut waktu) lalu stop pickup aktif di paling bawah.
 * [agenName] dipakai bila nama tidak ada di transaksi (mis. pickup baru, belum ada transaksi).
 */
internal fun buildRouteStops(
    pickup: PickupStatusDto?,
    transactions: List<DriverTransactionDto>,
    agenName: (String) -> String?,
): List<RouteStop> {
    val today = transactions
        .filter { isToday(it.createdAt) }
        .sortedBy { createdMillis(it) }

    val visited = today
        .groupBy { it.agenId }
        .filterKeys { it.isNotBlank() && it != pickup?.agenId }
        .map { (agenId, group) ->
            val latest = group.maxByOrNull { createdMillis(it) } ?: group.last()
            RouteStop(
                agenId = agenId,
                name = latest.agenName.ifBlank { agenName(agenId).orEmpty() },
                volumeLiter = latest.volumeLiter,
                status = latest.txStatus.toStopStatus(),
                isPickup = false,
            ) to createdMillis(latest)
        }
        .sortedBy { it.second }
        .map { it.first }

    if (pickup == null) return visited

    val forAgen = today.filter { it.agenId == pickup.agenId && it.agenId.isNotBlank() }
    val status = pickupStopStatus(pickup, transactions)
    val latestTx = if (status == StopStatus.Assigned || status == StopStatus.OnTheWay) null
    else forAgen.maxByOrNull { createdMillis(it) }
    val current = RouteStop(
        agenId = pickup.agenId,
        name = latestTx?.agenName?.ifBlank { null } ?: agenName(pickup.agenId).orEmpty(),
        volumeLiter = latestTx?.volumeLiter,
        status = status,
        isPickup = true,
        pickupId = pickup.pickupId,
    )
    return visited + current
}
