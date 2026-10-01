package id.biojelan.app.data.repository

import id.biojelan.app.data.remote.ApiClient
import id.biojelan.app.data.remote.ApiResult
import id.biojelan.app.data.remote.CreateDriverTransactionRequest
import id.biojelan.app.data.remote.DriverTransactionDto
import id.biojelan.app.data.remote.TransactionStatusDto
import io.ktor.http.HttpMethod
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement

val DriverTransactionDto.txStatus: TxStatus get() = TxStatus.from(status)

/**
 * Transaksi Driver → Agen (transaction_agen.md): Driver (mewakili Kilang) mengambil minyak dari Agen.
 * Alur status sama dengan transaksi Klien: PENDING → ACCEPTED | REJECTED (oleh Agen); Driver dapat
 * mengajukan batal saat PENDING/ACCEPTED → CANCEL_REQUESTED, lalu Agen menyetujui (CANCELLED) atau menolak
 * (kembali ACCEPTED). Identitas pemanggil diambil server dari token; `transaction_id` ada di path.
 *
 * Endpoint ini sudah live di backend (`AgentDriverTransactionController`, `DriverAgentTransactionController`)
 * dan field-nya sudah dicocokkan (lihat DTO di atas).
 */
class DriverTransactionRepository(
    private val api: ApiClient,
    private val json: Json,
) {
    // ------------------------------------------------------------------ Sisi Driver

    /** POST /api/driver/transaction. Isi salah satu dari [agenEmail] / [agenPhone]. Harga dihitung server. */
    suspend fun create(
        agenEmail: String? = null,
        agenPhone: String? = null,
        volumeLiter: Double,
        note: String? = null,
    ): ApiResult<DriverTransactionDto> {
        val body = json.encodeToString(
            CreateDriverTransactionRequest.serializer(),
            CreateDriverTransactionRequest(
                agenEmail = agenEmail?.trim()?.takeIf { it.isNotEmpty() },
                agenPhone = agenPhone?.trim()?.takeIf { it.isNotEmpty() },
                volumeLiter = kotlin.math.round(volumeLiter * 1000) / 1000,
                transactionNote = note?.trim()?.takeIf { it.isNotEmpty() },
            ),
        )
        return api.call(HttpMethod.Post, "/api/driver/transaction", bodyJson = body) { data ->
            json.decodeFromJsonElement(DriverTransactionDto.serializer(), requireNotNull(data))
        }
    }

    /** GET /api/driver/transactions — hanya transaksi milik Driver yang sedang login. */
    suspend fun driverTransactions(): ApiResult<List<DriverTransactionDto>> =
        api.call(HttpMethod.Get, "/api/driver/transactions") { data -> decodeList(data) }

    /** POST /api/driver/transaction/{id}/cancel — hanya saat PENDING/ACCEPTED. */
    suspend fun requestCancel(transactionId: String): ApiResult<TransactionStatusDto> =
        statusAction("/api/driver/transaction/${transactionId.trim()}/cancel")

    // ------------------------------------------------------------------ Sisi Agen

    /** GET /api/agent/driver/transactions — semua transaksi Driver → Agen milik Agen yang sedang login. */
    suspend fun agentDriverTransactions(): ApiResult<List<DriverTransactionDto>> =
        api.call(HttpMethod.Get, "/api/agent/driver/transactions") { data -> decodeList(data) }

    /** GET /api/agent/transactions[?status=] — versi terfilter status (kosong = semua). */
    suspend fun agentTransactions(status: TxStatus? = null): ApiResult<List<DriverTransactionDto>> {
        val query = if (status != null && status != TxStatus.Unknown) mapOf("status" to status.apiValue) else emptyMap()
        return api.call(HttpMethod.Get, "/api/agent/transactions", query = query) { data -> decodeList(data) }
    }

    /** POST /api/agent/transaction/{id}/accept — PENDING → ACCEPTED. */
    suspend fun accept(transactionId: String): ApiResult<TransactionStatusDto> = agentAction(transactionId, "accept")

    /** POST /api/agent/transaction/{id}/reject — PENDING → REJECTED. */
    suspend fun reject(transactionId: String): ApiResult<TransactionStatusDto> = agentAction(transactionId, "reject")

    /** POST /api/agent/transaction/{id}/cancel-accept — CANCEL_REQUESTED → CANCELLED. */
    suspend fun acceptCancellation(transactionId: String): ApiResult<TransactionStatusDto> =
        agentAction(transactionId, "cancel-accept")

    /** POST /api/agent/transaction/{id}/cancel-reject — CANCEL_REQUESTED → ACCEPTED. */
    suspend fun rejectCancellation(transactionId: String): ApiResult<TransactionStatusDto> =
        agentAction(transactionId, "cancel-reject")

    // ------------------------------------------------------------------ Helper

    private suspend fun agentAction(transactionId: String, action: String): ApiResult<TransactionStatusDto> =
        statusAction("/api/agent/transaction/${transactionId.trim()}/$action")

    private suspend fun statusAction(path: String): ApiResult<TransactionStatusDto> =
        api.call(HttpMethod.Post, path) { data ->
            json.decodeFromJsonElement(TransactionStatusDto.serializer(), requireNotNull(data))
        }

    private fun decodeList(data: JsonElement?): List<DriverTransactionDto> =
        if (data is JsonArray) json.decodeFromJsonElement(ListSerializer(DriverTransactionDto.serializer()), data)
        else emptyList()
}
