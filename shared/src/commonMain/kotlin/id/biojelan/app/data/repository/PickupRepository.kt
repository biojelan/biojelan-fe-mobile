package id.biojelan.app.data.repository

import id.biojelan.app.data.remote.ApiClient
import id.biojelan.app.data.remote.ApiResult
import id.biojelan.app.data.remote.PickupStatusDto
import id.biojelan.app.data.remote.UpdatePickupStatusRequest
import io.ktor.http.HttpMethod
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

enum class PickupStatus(val apiValue: String) {
    Assigned("ASSIGNED"), OnTheWay("OTW"), Arrived("ARRIVED"), Completed("COMPLETED"), Cancelled("CANCELLED"), Unknown("");

    companion object {
        fun from(raw: String): PickupStatus = when (raw.trim().uppercase()) {
            "ASSIGNED" -> Assigned
            "OTW" -> OnTheWay
            "ARRIVED" -> Arrived
            "COMPLETED" -> Completed
            "CANCELLED", "CANCELED" -> Cancelled
            else -> Unknown
        }
    }
}

val PickupStatusDto.pickupStatus: PickupStatus get() = PickupStatus.from(status)

/**
 * Endpoint pickup — sudah live di backend (`AgentPickupController`, `DriverPickupController`), dicocokkan
 * ke kode BE langsung (bukan cuma `pickup.md`, yang ternyata beda dari implementasi aslinya). Model `Pickup`
 * di DB terikat 1:1 ke satu `TransactionAgent` dan punya field `date`/`time`/`destination`, tapi field-field
 * itu TIDAK diekspos lewat API ini — respons cuma 3 field: `pickup_id` (format `"pkp-007"`), `status`, dan
 * `updated_at`. Kilang membuat penugasan dari dashboard web, jadi tidak ada endpoint create di sini.
 *
 * Status ada 5: ASSIGNED → OTW → ARRIVED → COMPLETED, atau CANCELLED kapan saja sebelum selesai. Backend
 * TIDAK memvalidasi urutan transisi (PATCH menerima status apa saja dari 5 itu) — urutan di atas murni
 * konvensi UI, bukan aturan server.
 *
 * GET mengembalikan HANYA pickup yang masih aktif (bukan COMPLETED/CANCELLED); kalau tidak ada, server
 * membalas 404 "Pickup not found" — itu kondisi NORMAL ("belum ada penugasan"), bukan error, makanya
 * dikonversi ke `Success(null)` di [getOrNullOn404] alih-alih diteruskan sebagai `Failure`.
 */
class PickupRepository(
    private val api: ApiClient,
    private val json: Json,
) {
    /** GET /api/agent/pickup/status — penugasan aktif milik Agen yang sedang login. */
    suspend fun agenStatus(): ApiResult<PickupStatusDto?> =
        getOrNullOn404(api.call(HttpMethod.Get, "/api/agent/pickup/status") { data -> decodeOptional(data) })

    /** GET /api/driver/pickup/status — penugasan aktif milik Driver yang sedang login. */
    suspend fun driverStatus(): ApiResult<PickupStatusDto?> =
        getOrNullOn404(api.call(HttpMethod.Get, "/api/driver/pickup/status") { data -> decodeOptional(data) })

    /**
     * PATCH /api/driver/pickup/status. [pickupId] harus persis string yang didapat dari GET (mis. "pkp-007")
     * — backend men-strip prefix "pkp-" lalu cocokkan sebagai ID, dan hanya mengizinkan Driver pemilik
     * pickup itu (Driver lain dapat 404, bukan 403).
     */
    suspend fun updateDriverStatus(pickupId: String, status: PickupStatus): ApiResult<PickupStatusDto> {
        val body = json.encodeToString(
            UpdatePickupStatusRequest.serializer(),
            UpdatePickupStatusRequest(pickupId = pickupId, status = status.apiValue),
        )
        return api.call(HttpMethod.Patch, "/api/driver/pickup/status", bodyJson = body) { data ->
            json.decodeFromJsonElement(PickupStatusDto.serializer(), requireNotNull(data))
        }
    }

    /** 404 dari endpoint show pickup berarti "tidak ada penugasan aktif", bukan kegagalan. */
    private fun getOrNullOn404(result: ApiResult<PickupStatusDto?>): ApiResult<PickupStatusDto?> =
        if (result is ApiResult.Failure && result.httpStatus == 404) ApiResult.Success(null) else result

    private fun decodeOptional(data: JsonElement?): PickupStatusDto? =
        if (data is JsonObject && data.isNotEmpty()) json.decodeFromJsonElement(PickupStatusDto.serializer(), data)
        else null
}
