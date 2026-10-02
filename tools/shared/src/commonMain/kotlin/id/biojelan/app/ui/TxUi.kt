package id.biojelan.app.ui

import id.biojelan.app.data.remote.TransactionDto
import id.biojelan.app.data.repository.TxStatus
import id.biojelan.app.ui.components.ChipKind

/** Sudut pandang pengguna, memengaruhi label status & nama pihak lawan. */
enum class Viewer { Klien, Agen }

fun TxStatus.chipKind(): ChipKind = when (this) {
    TxStatus.Pending, TxStatus.CancelRequested -> ChipKind.Pending
    TxStatus.Accepted -> ChipKind.Done
    TxStatus.Rejected, TxStatus.Cancelled -> ChipKind.Cancelled
    TxStatus.Unknown -> ChipKind.Neutral
}

fun TxStatus.label(viewer: Viewer): String = when (this) {
    TxStatus.Pending -> if (viewer == Viewer.Klien) "Menunggu Anda" else "Menunggu Klien"
    TxStatus.Accepted -> if (viewer == Viewer.Klien) "Selesai" else "Diterima"
    TxStatus.Rejected -> if (viewer == Viewer.Klien) "Ditolak" else "Ditolak Klien"
    TxStatus.CancelRequested -> if (viewer == Viewer.Klien) "Pembatalan diajukan" else "Menunggu pembatalan"
    TxStatus.Cancelled -> "Dibatalkan"
    TxStatus.Unknown -> "—"
}

/**
 * Label untuk transaksi Driver → Agen. [asAgen] true bila yang melihat adalah Agen (penerima permintaan),
 * false bila Driver (pembuat).
 */
fun TxStatus.driverLabel(asAgen: Boolean): String = when (this) {
    TxStatus.Pending -> if (asAgen) "Menunggu Anda" else "Menunggu Agen"
    TxStatus.Accepted -> "Diterima"
    TxStatus.Rejected -> if (asAgen) "Anda tolak" else "Ditolak Agen"
    TxStatus.CancelRequested -> if (asAgen) "Batal diajukan" else "Menunggu pembatalan"
    TxStatus.Cancelled -> "Dibatalkan"
    TxStatus.Unknown -> "—"
}

/** Nama pihak lawan: Agen bagi Klien (`agen_name`), Klien bagi Agen (`client_name`, null untuk tamu). */
fun TransactionDto.counterpartName(viewer: Viewer, agenNameLookup: (String) -> String? = { null }): String =
    when (viewer) {
        Viewer.Klien -> agenName.ifBlank { agenNameLookup(agenId).orEmpty() }.ifBlank { "Agen" }
        Viewer.Agen -> klienName.ifBlank { "Klien" }
    }
