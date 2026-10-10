package id.biojelan.app.data.repository

import id.biojelan.app.core.KilangPoint
import id.biojelan.app.data.remote.ApiClient
import id.biojelan.app.data.remote.ApiResult
import id.biojelan.app.data.remote.FactoryDto
import io.ktor.http.HttpMethod
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

fun FactoryDto.toKilangPoint(): KilangPoint = KilangPoint(latitude = latitude, longitude = longitude, address = address)

/**
 * GET /api/user/factory — lokasi Kilang (titik awal & akhir rute Driver). Butuh token dengan peran
 * agent/driver/admin/superadmin. Kalau belum ada data Kilang, server membalas 404 "Factory not found";
 * itu kondisi normal (peta pakai posisi cadangan), jadi dikonversi ke `Success(null)`, bukan error.
 */
class FactoryRepository(
    private val api: ApiClient,
    private val json: Json,
) {
    suspend fun location(): ApiResult<KilangPoint?> {
        val result = api.call(HttpMethod.Get, "/api/user/factory") { data ->
            if (data is JsonObject && data.isNotEmpty()) {
                json.decodeFromJsonElement(FactoryDto.serializer(), data).toKilangPoint()
            } else null
        }
        return if (result is ApiResult.Failure && result.httpStatus == 404) ApiResult.Success(null) else result
    }
}
