package id.biojelan.app.data.repository

import id.biojelan.app.data.remote.ApiClient
import id.biojelan.app.data.remote.ApiResult
import id.biojelan.app.data.remote.CheckClientEmailRequest
import id.biojelan.app.data.remote.CheckClientPhoneRequest
import id.biojelan.app.data.remote.ClientCheckDto
import id.biojelan.app.data.remote.CreateTransactionRequest
import id.biojelan.app.data.remote.TransactionDto
import id.biojelan.app.data.remote.TransactionStatusDto
import io.ktor.http.HttpMethod
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement

/** Status transaksi dari backend (`ClientTransactionStatus`). */
enum class TxStatus(val apiValue: String) {
    Pending("PENDING"),
    Accepted("ACCEPTED"),
    Rejected("REJECTED"),
    CancelRequested("CANCEL_REQUESTED"),
    Cancelled("CANCELLED"),
    Unknown("");

    companion object {
        fun from(raw: String): TxStatus = when (raw.trim().lowercase()) {
            "pending" -> Pending
            "accepted" -> Accepted
            "rejected" -> Rejected
            "cancel_requested" -> CancelRequested
            "cancelled", "canceled" -> Cancelled
            else -> Unknown
        }
    }
}

val TransactionDto.txStatus: TxStatus get() = TxStatus.from(status)

/**
 * Transaksi Agen → Klien (transaction_klien.md). Identitas pemanggil diambil server dari token,
 * jadi tidak ada lagi `agen_id`/`klien_id` di request; `transaction_id` ada di path, bukan di body.
 *
 * Alur status: PENDING → ACCEPTED | REJECTED (oleh Klien). Agen bisa mengajukan pembatalan saat
 * PENDING/ACCEPTED → CANCEL_REQUESTED, lalu Klien menyetujui (CANCELLED) atau menolak (kembali ACCEPTED).
 */
class TransactionRepository(
    private val api: ApiClient,
    private val json: Json,
) {
    // ------------------------------------------------------------------ Sisi Agen

    /**
     * POST /api/agent/transaction — Agen mencatat transaksi (status awal PENDING).
     * Isi salah satu dari [clientEmail] / [clientPhone]. Harga & total dihitung server.
     */
    suspend fun createAsAgen(
        clientEmail: String? = null,
        clientPhone: String? = null,
        volumeLiter: Double,
        note: String? = null,
    ): ApiResult<TransactionDto> {
        val body = json.encodeToString(
            CreateTransactionRequest.serializer(),
            CreateTransactionRequest(
                clientEmail = clientEmail?.trim()?.takeIf { it.isNotEmpty() },
                clientPhone = clientPhone?.trim()?.takeIf { it.isNotEmpty() },
                // Backend hanya menerima maksimal 3 desimal.
                volumeLiter = kotlin.math.round(volumeLiter * 1000) / 1000,
                transactionNote = note?.trim()?.takeIf { it.isNotEmpty() },
            ),
        )
        return api.call(HttpMethod.Post, "/api/agent/transaction", bodyJson = body) { data -> decodeTransaction(data) }
    }

    /** GET /api/agent/clients/transactions — semua transaksi milik Agen yang sedang login. */
    suspend fun agenTransactions(): ApiResult<List<TransactionDto>> =
        api.call(HttpMethod.Get, "/api/agent/clients/transactions") { data -> decodeList(data) }

    /** POST /api/agent/check-clients-email — cek apakah email milik Klien terdaftar. */
    suspend fun checkClientByEmail(email: String): ApiResult<ClientCheckDto> {
        val body = json.encodeToString(CheckClientEmailRequest.serializer(), CheckClientEmailRequest(email.trim()))
        return api.call(HttpMethod.Post, "/api/agent/check-clients-email", bodyJson = body) { data -> decodeClientCheck(data) }
    }

    /** POST /api/agent/check-clients-phone — cek apakah nomor telepon milik Klien terdaftar. */
    suspend fun checkClientByPhone(phone: String): ApiResult<ClientCheckDto> {
        val body = json.encodeToString(CheckClientPhoneRequest.serializer(), CheckClientPhoneRequest(phone.trim()))
        return api.call(HttpMethod.Post, "/api/agent/check-clients-phone", bodyJson = body) { data -> decodeClientCheck(data) }
    }

    /** POST /api/agent/transaction/{id}/cancel — Agen mengajukan pembatalan (hanya saat PENDING/ACCEPTED). */
    suspend fun requestCancel(transactionId: String): ApiResult<TransactionStatusDto> =
        statusAction("/api/agent/transaction/${transactionId.trim()}/cancel")

    // ------------------------------------------------------------------ Sisi Klien

    /**
     * GET /api/client/transactions[?status=] — transaksi milik Klien yang sedang login.
     * [status] opsional; kosong berarti semua status.
     */
    suspend fun klienTransactions(status: TxStatus? = null): ApiResult<List<TransactionDto>> {
        val query = if (status != null && status != TxStatus.Unknown) mapOf("status" to status.apiValue) else emptyMap()
        return api.call(HttpMethod.Get, "/api/client/transactions", query = query) { data -> decodeList(data) }
    }

    /** POST /api/client/transaction/{id}/accept — PENDING → ACCEPTED. */
    suspend fun accept(transactionId: String): ApiResult<TransactionStatusDto> = clientAction(transactionId, "accept")

    /** POST /api/client/transaction/{id}/reject — PENDING → REJECTED. */
    suspend fun reject(transactionId: String): ApiResult<TransactionStatusDto> = clientAction(transactionId, "reject")

    /** POST /api/client/transaction/{id}/cancel-accept — CANCEL_REQUESTED → CANCELLED. */
    suspend fun acceptCancellation(transactionId: String): ApiResult<TransactionStatusDto> =
        clientAction(transactionId, "cancel-accept")

    /** POST /api/client/transaction/{id}/cancel-reject — CANCEL_REQUESTED → ACCEPTED. */
    suspend fun rejectCancellation(transactionId: String): ApiResult<TransactionStatusDto> =
        clientAction(transactionId, "cancel-reject")

    // ------------------------------------------------------------------ Helper

    private suspend fun clientAction(transactionId: String, action: String): ApiResult<TransactionStatusDto> =
        statusAction("/api/client/transaction/${transactionId.trim()}/$action")

    private suspend fun statusAction(path: String): ApiResult<TransactionStatusDto> =
        api.call(HttpMethod.Post, path) { data ->
            json.decodeFromJsonElement(TransactionStatusDto.serializer(), requireNotNull(data))
        }

    private fun decodeTransaction(data: JsonElement?): TransactionDto =
        json.decodeFromJsonElement(TransactionDto.serializer(), requireNotNull(data))

    private fun decodeClientCheck(data: JsonElement?): ClientCheckDto =
        json.decodeFromJsonElement(ClientCheckDto.serializer(), requireNotNull(data))

    private fun decodeList(data: JsonElement?): List<TransactionDto> =
        if (data is JsonArray) json.decodeFromJsonElement(ListSerializer(TransactionDto.serializer()), data)
        else emptyList()
}
