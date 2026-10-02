package id.biojelan.app.data.repository

import id.biojelan.app.data.remote.UpdateAgenRequest
import id.biojelan.app.data.remote.UpdateUserRequest
import id.biojelan.app.data.remote.UserDto

/** Field Agen yang bisa diubah dari app. */
data class AgenEdit(
    val address: String,
    val bankName: String,
    val accountNumber: String,
    val openAt: String,
    val closeAt: String,
    val openDays: List<String>,
)

/**
 * Bentuk body PATCH /api/user dari data user saat ini + perubahan.
 *
 * API-DOC tidak menjelaskan apakah PATCH bersifat parsial, jadi app SELALU mengirim set lengkap field yang
 * bisa diubah dari app (nama, telepon, dan data Agen) dengan perubahan diterapkan. Sengaja TIDAK dikirim:
 * `password` (punya endpoint sendiri), `email` (tidak bisa diubah dari app; mengirim ulang email yang sama
 * berisiko kena validasi unik di backend), dan `stock_liter` (dimiliki server, berubah lewat transaksi,
 * sehingga menulisnya dari app berisiko menimpa data terbaru).
 */
fun UserDto.toUpdateRequest(
    name: String = this.name,
    phone: String = this.phone,
    agenEdit: AgenEdit? = null,
): UpdateUserRequest {
    val a = agen
    val agenRequest = if (a == null) null else UpdateAgenRequest(
        address = agenEdit?.address ?: a.address,
        latitude = a.latitude.takeIf { it != 0.0 },
        longitude = a.longitude.takeIf { it != 0.0 },
        bankName = agenEdit?.bankName ?: a.bankName,
        accountNumber = agenEdit?.accountNumber ?: a.accountNumber,
        openAt = agenEdit?.openAt ?: a.openAt,
        closeAt = agenEdit?.closeAt ?: a.closeAt,
        openDays = agenEdit?.openDays ?: a.openDays,
    )
    return UpdateUserRequest(name = name, phone = phone, agen = agenRequest)
}