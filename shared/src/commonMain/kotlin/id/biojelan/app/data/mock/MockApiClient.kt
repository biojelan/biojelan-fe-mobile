package id.biojelan.app.data.mock

import id.biojelan.app.data.remote.AgenSummaryDto
import id.biojelan.app.data.remote.ApiResult
import id.biojelan.app.data.remote.AuthPayload
import id.biojelan.app.data.remote.ClientCheckDto
import id.biojelan.app.data.remote.CreateDriverTransactionRequest
import id.biojelan.app.data.remote.CreateTransactionRequest
import id.biojelan.app.data.remote.DriverTransactionDto
import id.biojelan.app.data.remote.LoginRequest
import id.biojelan.app.data.remote.UpdatePickupStatusRequest
import id.biojelan.app.data.remote.PickupStatusDto
import id.biojelan.app.data.remote.RegisterRequest
import id.biojelan.app.data.remote.TransactionDto
import id.biojelan.app.data.remote.TransactionStatusDto
import id.biojelan.app.data.remote.UserDto
import io.ktor.http.HttpMethod
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

/**
 * Fallback API client: meniru respons backend dengan data dummy dari [MockData].
 *
 * Dipanggil oleh `ApiClient` saat request ke server gagal (network error) dan
 * `AppConfig.ENABLE_FALLBACK` aktif. Semua method mengembalikan [ApiResult] supaya
 * bisa langsung dipakai sebagai pengganti respons asli.
 */
class MockApiClient(private val json: Json) {

    /**
     * Token yang sedang "aktif" di mock session. Di-set saat login/register mock berhasil,
     * dan dipakai untuk menentukan user mana yang sedang login.
     */
    private var activeToken: String? = null

    /** Transaksi Driver → Agen yang bisa berubah selama sesi, supaya alur Driver ↔ Agen terasa utuh. */
    private val driverTxs = MockData.driverTransactions.toMutableList()

    /** Status penjemputan bersama: diubah Driver (PATCH), dibaca Driver dan Agen (GET). */
    private var pickup: PickupStatusDto = MockData.agenPickupStatus

    /**
     * True untuk endpoint pickup. Pickup sudah live di backend, jadi ini hanya dipakai kalau
     * `AppConfig.MOCK_PICKUP_API` dinyalakan manual (saklar darurat) — dispatch di bawah tetap ada juga
     * sebagai fallback [ENABLE_FALLBACK] biasa kalau server pickup tidak terjangkau.
     */
    fun handlesPickupFlow(method: HttpMethod, path: String): Boolean =
        path == "/api/driver/pickup/status" || path == "/api/agent/pickup/status"

    private fun isAgentDriverAction(path: String): Boolean =
        path.endsWith("/accept") || path.endsWith("/reject") ||
            path.endsWith("/cancel-accept") || path.endsWith("/cancel-reject")

    private fun setDriverStatus(transactionId: String, status: String): TransactionStatusDto {
        val index = driverTxs.indexOfFirst { it.transactionId == transactionId }
        if (index < 0) throw MockAuthException("Transaksi tidak ditemukan.")
        driverTxs[index] = driverTxs[index].copy(status = status)
        return TransactionStatusDto(transactionId = transactionId, status = status)
    }

    /** Handle request berdasarkan method + path, return ApiResult yang sesuai. */
    fun <T> handle(
        method: HttpMethod,
        path: String,
        token: String?,
        bodyJson: String?,
        parse: (JsonElement?) -> T,
    ): ApiResult<T> {
        // Simpan token terakhir yang dipakai untuk lookup user
        if (token != null) activeToken = token

        return try {
            val result = route(method, path, bodyJson)
            @Suppress("UNCHECKED_CAST")
            ApiResult.Success(result as T, "Mock data (offline fallback)")
        } catch (e: MockAuthException) {
            ApiResult.Failure(e.message ?: "Autentikasi gagal.", ApiResult.Kind.Server)
        } catch (e: Exception) {
            ApiResult.Failure("Mock handler error: ${e.message}", ApiResult.Kind.Server)
        }
    }

    private fun route(method: HttpMethod, path: String, bodyJson: String?): Any? = when {

        // ============ Authentication ============

        method == HttpMethod.Post && path == "/api/login" -> {
            val req = json.decodeFromString(LoginRequest.serializer(), bodyJson!!)
            val payload = MockData.authPayloadForEmail(req.email)
                ?: throw MockAuthException("Akun belum terdaftar.")
            if (req.password != MockData.MOCK_PASSWORD) throw MockAuthException("Kata sandi salah.")
            activeToken = MockData.credentials[req.email]
            payload
        }

        method == HttpMethod.Post && path == "/api/register" -> {
            val req = json.decodeFromString(RegisterRequest.serializer(), bodyJson!!)
            activeToken = MockData.TOKEN_KLIEN
            MockData.registerPayload(req.name, req.email)
        }

        method == HttpMethod.Delete && path == "/api/logout" -> Unit

        method == HttpMethod.Post && path == "/api/forgot-password" -> Unit

        method == HttpMethod.Patch && path == "/api/update-password" -> Unit

        // ============ User ============

        method == HttpMethod.Get && path == "/api/user" -> {
            currentMockUser()
        }

        method == HttpMethod.Patch && path == "/api/user" -> {
            // Return user saat ini (anggap update berhasil)
            currentMockUser()
        }

        method == HttpMethod.Delete && path == "/api/user" -> Unit

        // ============ Agen List ============

        method == HttpMethod.Get && path == "/api/user/agen" -> {
            MockData.agenList
        }

        // ============ Transactions (Agen → Klien) ============

        method == HttpMethod.Post && path == "/api/agent/transaction" -> {
            val req = json.decodeFromString(CreateTransactionRequest.serializer(), bodyJson!!)
            MockData.agenTransactions.first().copy(
                transactionId = "TXN-MOCK-${System.currentTimeMillis()}",
                klienName = "Klien Mock",
                volumeLiter = req.volumeLiter,
                status = "PENDING",
            )
        }

        method == HttpMethod.Get && path == "/api/agent/clients/transactions" -> {
            MockData.agenTransactions
        }

        method == HttpMethod.Post && path.startsWith("/api/agent/check-clients-") -> {
            ClientCheckDto(isExist = true, clientId = "1", clientName = "Budi Santoso")
        }

        method == HttpMethod.Post && path.startsWith("/api/agent/transaction/") && path.endsWith("/cancel") -> {
            TransactionStatusDto(
                transactionId = path.removePrefix("/api/agent/transaction/").substringBefore("/"),
                status = "CANCEL_REQUESTED",
            )
        }

        method == HttpMethod.Get && path == "/api/client/transactions" -> {
            MockData.klienTransactions
        }

        method == HttpMethod.Post && path.startsWith("/api/client/transaction/") -> {
            val rest = path.removePrefix("/api/client/transaction/")
            val status = when (rest.substringAfter("/")) {
                "accept", "cancel-reject" -> "ACCEPTED"
                "reject" -> "REJECTED"
                "cancel-accept" -> "CANCELLED"
                else -> throw MockAuthException("Endpoint mock tidak dikenal: $path")
            }
            TransactionStatusDto(transactionId = rest.substringBefore("/"), status = status)
        }

        // ============ Driver ↔ Agen (transaction_agen.md) ============

        method == HttpMethod.Post && path == "/api/driver/transaction" -> {
            val req = json.decodeFromString(CreateDriverTransactionRequest.serializer(), bodyJson!!)
            val agen = MockData.agenList.firstOrNull { it.phone == req.agenPhone } ?: MockData.agenList.first()
            val now = "2026-09-28 09:00:00"
            val tx = DriverTransactionDto(
                transactionId = "TXN-DRV-${System.currentTimeMillis()}",
                driverId = MockData.driverUser.userId,
                agenId = agen.agenId,
                agenName = agen.name,
                volumeLiter = req.volumeLiter,
                price = MockData.DRIVER_PRICE_PER_LITER,
                totalPrice = kotlin.math.round(req.volumeLiter * MockData.DRIVER_PRICE_PER_LITER).toLong(),
                status = "PENDING",
                transactionNote = req.transactionNote.orEmpty(),
                createdAt = now,
                updatedAt = now,
            )
            driverTxs.add(0, tx)
            tx
        }

        method == HttpMethod.Get && path == "/api/driver/transactions" -> {
            driverTxs.toList()
        }

        method == HttpMethod.Post && path.startsWith("/api/driver/transaction/") && path.endsWith("/cancel") -> {
            setDriverStatus(path.removePrefix("/api/driver/transaction/").substringBefore("/"), "CANCEL_REQUESTED")
        }

        method == HttpMethod.Get && (path == "/api/agent/driver/transactions" || path == "/api/agent/transactions") -> {
            driverTxs.toList()
        }

        method == HttpMethod.Post && path.startsWith("/api/agent/transaction/") && isAgentDriverAction(path) -> {
            val rest = path.removePrefix("/api/agent/transaction/")
            val status = when (rest.substringAfter("/")) {
                "accept", "cancel-reject" -> "ACCEPTED"
                "reject" -> "REJECTED"
                else -> "CANCELLED" // cancel-accept
            }
            setDriverStatus(rest.substringBefore("/"), status)
        }

        // ============ Pickup (pickup.md) ============

        method == HttpMethod.Get && (path == "/api/agent/pickup/status" || path == "/api/driver/pickup/status") -> {
            pickup
        }

        method == HttpMethod.Patch && path == "/api/driver/pickup/status" -> {
            val req = json.decodeFromString(UpdatePickupStatusRequest.serializer(), bodyJson!!)
            if (req.pickupId != pickup.pickupId) throw MockAuthException("Penugasan jemput tidak ditemukan.")
            pickup = pickup.copy(status = req.status, updatedAt = "2026-09-28 10:00:00")
            // Respons asli hanya memuat pickup_id, status, updated_at.
            PickupStatusDto(pickupId = pickup.pickupId, status = pickup.status, updatedAt = pickup.updatedAt)
        }

        // ============ Fallback ============
        else -> {
            // Endpoint tidak dikenal, return empty supaya tidak crash
            null
        }
    }

    private fun currentMockUser(): UserDto =
        activeToken?.let { MockData.userByToken[it] } ?: MockData.klienUser

    private class MockAuthException(message: String) : Exception(message)

    private object System {
        // Simple counter for mock transaction IDs (KMP-safe, no java.lang.System)
        private var counter = 0L
        fun currentTimeMillis(): Long = ++counter
    }
}
