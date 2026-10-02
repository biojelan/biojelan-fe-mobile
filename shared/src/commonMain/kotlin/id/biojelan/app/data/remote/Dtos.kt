package id.biojelan.app.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/*
 * DTO 1:1 dengan API-DOC (authentication.md, user.md, transaction_klien.md, transaction_agen.md) dan backend.
 * Semua field diberi default supaya respons yang kurang lengkap tidak membuat parsing gagal.
 */

// ============================================================ Authentication

@Serializable
data class AuthPayload(
    val token: String,
    val name: String = "",
    val email: String = "",
)

@Serializable
data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String,
    @SerialName("password_confirmation") val passwordConfirmation: String,
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
)

@Serializable
data class UpdatePasswordRequest(
    val password: String,
    @SerialName("new_password") val newPassword: String,
    // Backend memvalidasi `password_confirmation` (same:new_password); `new_password_confirmed` tidak dikenal.
    @SerialName("password_confirmation") val passwordConfirmation: String,
)

@Serializable
data class ForgotPasswordRequest(
    val email: String,
)

// ============================================================ User

/** Objek `agen` di GET/PATCH /api/user (hanya ada untuk akun Agen). */
@Serializable
data class AgenDto(
    @SerialName("agen_id") @Serializable(with = FlexibleStringSerializer::class)
    val agenId: String = "",
    val address: String = "",
    @Serializable(with = FlexibleDoubleSerializer::class) val latitude: Double = 0.0,
    @Serializable(with = FlexibleDoubleSerializer::class) val longitude: Double = 0.0,
    @SerialName("bank_name") val bankName: String = "",
    @SerialName("account_number") @Serializable(with = FlexibleStringSerializer::class)
    val accountNumber: String = "",
    @SerialName("open_at") val openAt: String = "",
    @SerialName("close_at") val closeAt: String = "",
    @SerialName("open_days") val openDays: List<String> = emptyList(),
    @SerialName("is_open") @Serializable(with = FlexibleBooleanSerializer::class)
    val isOpen: Boolean = false,
    @SerialName("stock_liter") @Serializable(with = FlexibleDoubleSerializer::class)
    val stockLiter: Double = 0.0,
)

/** GET /api/user. Field `password` dari respons sengaja TIDAK dipetakan (jangan pernah disimpan di app). */
@Serializable
data class UserDto(
    @SerialName("user_id") @Serializable(with = FlexibleStringSerializer::class)
    val userId: String = "",
    @SerialName("role_id") val roleId: Int = 0,
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    @SerialName("is_verified") @Serializable(with = FlexibleBooleanSerializer::class)
    val isVerified: Boolean = false,
    @SerialName("is_active") @Serializable(with = FlexibleBooleanSerializer::class)
    val isActive: Boolean = true,
    val agen: AgenDto? = null,
)

/** Item di GET /api/user/agen. */
@Serializable
data class AgenSummaryDto(
    @SerialName("agen_id") @Serializable(with = FlexibleStringSerializer::class)
    val agenId: String = "",
    @SerialName("role_id") val roleId: Int = 0,
    val name: String = "",
    val phone: String = "",
    val address: String = "",
    @Serializable(with = FlexibleDoubleSerializer::class) val latitude: Double = 0.0,
    @Serializable(with = FlexibleDoubleSerializer::class) val longitude: Double = 0.0,
    @SerialName("open_at") val openAt: String = "",
    @SerialName("close_at") val closeAt: String = "",
    @SerialName("is_open") @Serializable(with = FlexibleBooleanSerializer::class)
    val isOpen: Boolean = false,
    @SerialName("open_days") val openDays: List<String> = emptyList(),
)

/** Body PATCH /api/user. Field null tidak dikirim. `password` sengaja tidak ada — pakai /api/update-password. */
@Serializable
data class UpdateUserRequest(
    val name: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val agen: UpdateAgenRequest? = null,
)

/** latitude/longitude pada REQUEST berupa angka (user.md terbaru; backend memvalidasi `numeric`). */
@Serializable
data class UpdateAgenRequest(
    val address: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    @SerialName("bank_name") val bankName: String? = null,
    @SerialName("account_number") val accountNumber: String? = null,
    @SerialName("open_at") val openAt: String? = null,
    @SerialName("close_at") val closeAt: String? = null,
    @SerialName("open_days") val openDays: List<String>? = null,
)

// ============================================================ Transaction (Agen → Klien)

/**
 * POST /api/agent/transaction. Klien dikenali lewat `client_email` ATAU `client_phone` (salah satu wajib).
 * Harga & total dihitung server, jadi TIDAK dikirim dari app. `volume_liter` maksimal 3 desimal.
 */
@Serializable
data class CreateTransactionRequest(
    @SerialName("client_email") val clientEmail: String? = null,
    @SerialName("client_phone") val clientPhone: String? = null,
    @SerialName("volume_liter") val volumeLiter: Double,
    @SerialName("transaction_note") val transactionNote: String? = null,
)

/** POST /api/agent/check-clients-email */
@Serializable
data class CheckClientEmailRequest(
    @SerialName("client_email") val clientEmail: String,
)

/** POST /api/agent/check-clients-phone */
@Serializable
data class CheckClientPhoneRequest(
    @SerialName("client_phone") val clientPhone: String,
)

/** Hasil check-clients-email/phone. Bila Klien tidak ditemukan, `is_exist` false dan sisanya null. */
@Serializable
data class ClientCheckDto(
    @SerialName("is_exist") @Serializable(with = FlexibleBooleanSerializer::class)
    val isExist: Boolean = false,
    @SerialName("client_id") @Serializable(with = FlexibleStringSerializer::class)
    val clientId: String = "",
    @SerialName("client_name") val clientName: String = "",
    @SerialName("client_email") val clientEmail: String = "",
    @SerialName("client_phone") val clientPhone: String = "",
)

/**
 * Satu model untuk semua respons transaksi. Nama pihak lawan beda per sisi:
 * daftar Agen -> `client_name`, daftar Klien -> `agen_name`. `client_id` bernilai null untuk transaksi tamu.
 */
@Serializable
data class TransactionDto(
    @SerialName("transaction_id") @Serializable(with = FlexibleStringSerializer::class)
    val transactionId: String = "",
    @SerialName("agen_id") @Serializable(with = FlexibleStringSerializer::class)
    val agenId: String = "",
    @SerialName("client_id") @Serializable(with = FlexibleStringSerializer::class)
    val klienId: String = "",
    @SerialName("client_name") val klienName: String = "",
    @SerialName("agen_name") val agenName: String = "",
    @SerialName("volume_liter") @Serializable(with = FlexibleDoubleSerializer::class)
    val volumeLiter: Double = 0.0,
    @Serializable(with = FlexibleLongSerializer::class) val price: Long = 0L,
    @SerialName("total_price") @Serializable(with = FlexibleLongSerializer::class)
    val totalPrice: Long = 0L,
    val status: String = "",
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = "",
)

/** Respons accept/reject/cancel/cancel-accept/cancel-reject: hanya id + status baru. */
@Serializable
data class TransactionStatusDto(
    @SerialName("transaction_id") @Serializable(with = FlexibleStringSerializer::class)
    val transactionId: String = "",
    val status: String = "",
)

// ============================================================ Transaction (Driver → Agen)

/**
 * POST /api/driver/transaction (transaction_agen.md). Driver mewakili Kilang dan mengambil minyak dari Agen.
 * Agen dikenali lewat `agen_email` ATAU `agen_phone`. Harga & total dihitung server.
 */
@Serializable
data class CreateDriverTransactionRequest(
    @SerialName("agen_email") val agenEmail: String? = null,
    @SerialName("agen_phone") val agenPhone: String? = null,
    @SerialName("volume_liter") val volumeLiter: Double,
    @SerialName("transaction_note") val transactionNote: String? = null,
)

/** Respons transaksi Driver → Agen (dipakai sisi Driver maupun sisi Agen penerima). */
@Serializable
data class DriverTransactionDto(
    @SerialName("transaction_id") @Serializable(with = FlexibleStringSerializer::class)
    val transactionId: String = "",
    @SerialName("driver_id") @Serializable(with = FlexibleStringSerializer::class)
    val driverId: String = "",
    @SerialName("agen_id") @Serializable(with = FlexibleStringSerializer::class)
    val agenId: String = "",
    @SerialName("agen_name") val agenName: String = "",
    @SerialName("volume_liter") @Serializable(with = FlexibleDoubleSerializer::class)
    val volumeLiter: Double = 0.0,
    @Serializable(with = FlexibleLongSerializer::class) val price: Long = 0L,
    @SerialName("total_price") @Serializable(with = FlexibleLongSerializer::class)
    val totalPrice: Long = 0L,
    val status: String = "",
    @SerialName("transaction_note") val transactionNote: String = "",
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = "",
)

// ============================================================ Price

/** GET /api/price — harga Klien berlaku hari ini (`price_type` selalu "KLIEN" untuk endpoint publik ini). */
@Serializable
data class PriceDto(
    @SerialName("price_id") val priceId: String = "",
    @SerialName("price_per_liter") @Serializable(with = FlexibleLongSerializer::class)
    val pricePerLiter: Long = 0L,
    @SerialName("price_type") val priceType: String = "",
    @SerialName("start_date") val startDate: String = "",
    @SerialName("end_date") val endDate: String? = null,
)

// ============================================================ Pickup (Driver)

/**
 * Status penjemputan (pickup.md): GET /api/agen/pickup/status (Agen), GET & PATCH /api/driver/pickup/status
 * (Driver). Respons Driver hanya memuat `pickup_id`, `status`, `updated_at`, sisanya berisi default.
 * Sisi Kilang (create) ada di dashboard web.
 */
@Serializable
data class PickupStatusDto(
    @SerialName("pickup_id") @Serializable(with = FlexibleStringSerializer::class)
    val pickupId: String = "",
    @SerialName("driver_id") @Serializable(with = FlexibleStringSerializer::class)
    val driverId: String = "",
    @SerialName("agen_id") @Serializable(with = FlexibleStringSerializer::class)
    val agenId: String = "",
    val date: String = "",
    /** ASSIGNED, OTW, COMPLETED, CANCELLED. */
    val status: String = "",
    @SerialName("updated_at") val updatedAt: String = "",
)

/** Body PATCH /api/driver/pickup/status. `status`: ASSIGNED, OTW, COMPLETED, CANCELLED. */
@Serializable
data class UpdatePickupStatusRequest(
    @SerialName("pickup_id") val pickupId: String,
    val status: String,
)