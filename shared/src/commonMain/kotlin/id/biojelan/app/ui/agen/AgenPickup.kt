package id.biojelan.app.ui.agen

import id.biojelan.app.core.parseIsoMillis
import id.biojelan.app.data.remote.DriverTransactionDto
import id.biojelan.app.data.remote.PickupStatusDto
import id.biojelan.app.data.repository.PickupStatus
import id.biojelan.app.data.repository.TxStatus
import id.biojelan.app.data.repository.pickupStatus
import id.biojelan.app.data.repository.txStatus

/**
 * Langkah penjemputan dari sisi Agen, sama dengan stepper Driver:
 * Assigned → On the Way → Arrived → Menunggu Konfirmasi → Completed.
 *
 * "Menunggu Konfirmasi" tidak ada di backend; langkah ini diturunkan dari transaksi pengambilan Driver
 * yang masuk setelah Driver tiba dan belum dijawab Agen.
 */
enum class AgenPickupStep { Assigned, OnTheWay, Arrived, AwaitingConfirm, Completed, Cancelled, Unknown }

private const val ARRIVAL_TOLERANCE_MS = 5_000L

private fun createdMillis(tx: DriverTransactionDto): Long = parseIsoMillis(tx.createdAt) ?: 0L

/**
 * Transaksi pengambilan terbaru yang dibuat Driver SETELAH tiba. Transaksi lama dari penjemputan
 * sebelumnya tidak ikut dihitung. Daftar [driverTransactions] sudah khusus untuk Agen yang login.
 */
internal fun latestPickupTransaction(
    pickup: PickupStatusDto,
    driverTransactions: List<DriverTransactionDto>,
): DriverTransactionDto? {
    val arrivedAt = parseIsoMillis(pickup.updatedAt)
    return driverTransactions
        .filter { arrivedAt == null || createdMillis(it) >= arrivedAt - ARRIVAL_TOLERANCE_MS }
        .maxByOrNull { createdMillis(it) }
}

internal fun agenPickupStep(pickup: PickupStatusDto, driverTransactions: List<DriverTransactionDto>): AgenPickupStep =
    when (pickup.pickupStatus) {
        PickupStatus.Assigned -> AgenPickupStep.Assigned
        PickupStatus.OnTheWay -> AgenPickupStep.OnTheWay
        PickupStatus.Arrived -> when (latestPickupTransaction(pickup, driverTransactions)?.txStatus) {
            TxStatus.Pending, TxStatus.CancelRequested -> AgenPickupStep.AwaitingConfirm
            TxStatus.Accepted -> AgenPickupStep.Completed
            else -> AgenPickupStep.Arrived
        }
        PickupStatus.Completed -> AgenPickupStep.Completed
        PickupStatus.Cancelled -> AgenPickupStep.Cancelled
        PickupStatus.Unknown -> AgenPickupStep.Unknown
    }

/** Posisi di stepper 5 langkah: indeks langkah aktif, 5 = semua selesai, -1 = tidak ada yang menyala. */
internal fun AgenPickupStep.stepperIndex(): Int = when (this) {
    AgenPickupStep.Assigned -> 0
    AgenPickupStep.OnTheWay -> 1
    AgenPickupStep.Arrived -> 2
    AgenPickupStep.AwaitingConfirm -> 3
    AgenPickupStep.Completed -> 5
    AgenPickupStep.Cancelled, AgenPickupStep.Unknown -> -1
}

/**
 * Volume yang diminta Driver untuk penjemputan ini; null selama Driver belum mencatat apa pun setelah tiba
 * atau transaksinya ditolak / dibatalkan (tidak ada volume yang benar-benar berpindah).
 */
internal fun requestedPickupVolume(pickup: PickupStatusDto, driverTransactions: List<DriverTransactionDto>): Double? {
    if (pickup.pickupStatus != PickupStatus.Arrived && pickup.pickupStatus != PickupStatus.Completed) return null
    val tx = latestPickupTransaction(pickup, driverTransactions) ?: return null
    return when (tx.txStatus) {
        TxStatus.Pending, TxStatus.CancelRequested, TxStatus.Accepted -> tx.volumeLiter
        else -> null
    }
}
