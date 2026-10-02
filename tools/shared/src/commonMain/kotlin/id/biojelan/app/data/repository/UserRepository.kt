package id.biojelan.app.data.repository

import id.biojelan.app.data.remote.AgenSummaryDto
import id.biojelan.app.data.remote.ApiClient
import id.biojelan.app.data.remote.ApiResult
import id.biojelan.app.data.remote.UpdatePasswordRequest
import id.biojelan.app.data.remote.UpdateUserRequest
import id.biojelan.app.data.remote.UserDto
import io.ktor.http.HttpMethod
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray

/** Endpoint di user.md + PATCH /api/update-password dari authentication.md. */
class UserRepository(
    private val api: ApiClient,
    private val session: SessionManager,
    private val json: Json,
) {
    private val _agens = MutableStateFlow<List<AgenSummaryDto>>(emptyList())

    /** Cache daftar Agen terakhir (dipakai layar detail Agen supaya tidak perlu request ulang). */
    val agens: StateFlow<List<AgenSummaryDto>> = _agens.asStateFlow()

    fun agenById(agenId: String): AgenSummaryDto? = _agens.value.firstOrNull { it.agenId == agenId }

    /** GET /api/user — dan sinkronkan ke SessionManager. */
    suspend fun refreshUser(): ApiResult<UserDto> {
        val result = api.call(HttpMethod.Get, "/api/user") { data ->
            json.decodeFromJsonElement(UserDto.serializer(), requireNotNull(data))
        }
        if (result is ApiResult.Success) session.updateUser(result.data)
        return result
    }

    /** PATCH /api/user */
    suspend fun updateUser(request: UpdateUserRequest): ApiResult<UserDto> {
        val body = json.encodeToString(UpdateUserRequest.serializer(), request)
        val result = api.call(HttpMethod.Patch, "/api/user", bodyJson = body) { data ->
            json.decodeFromJsonElement(UserDto.serializer(), requireNotNull(data))
        }
        if (result is ApiResult.Success) session.updateUser(result.data)
        return result
    }

    /** DELETE /api/user — hapus akun permanen. */
    suspend fun deleteAccount(): ApiResult<Unit> {
        val result = api.call(HttpMethod.Delete, "/api/user") { }
        if (result is ApiResult.Success) session.clear()
        return result
    }

    /** PATCH /api/update-password */
    suspend fun changePassword(current: String, new: String, confirmation: String): ApiResult<Unit> {
        val body = json.encodeToString(
            UpdatePasswordRequest.serializer(),
            UpdatePasswordRequest(current, new, confirmation),
        )
        return api.call(HttpMethod.Patch, "/api/update-password", bodyJson = body) { }
    }

    /**
     * GET /api/user/agen — dicek ulang langsung ke `routes/api.php` & `bootstrap/app.php`: route ini
     * dideklarasikan DI LUAR grup `auth:sanctum` dan tidak ada middleware auth global, jadi endpoint ini
     * publik (berbeda dari asumsi lama di user.md yang mengira wajib Bearer token). Guest tanpa token pun
     * harus bisa memanggilnya.
     */
    suspend fun listAgen(): ApiResult<List<AgenSummaryDto>> {
        val result = api.call(HttpMethod.Get, "/api/user/agen", authenticated = false) { data ->
            if (data is JsonArray) json.decodeFromJsonElement(ListSerializer(AgenSummaryDto.serializer()), data)
            else emptyList()
        }
        if (result is ApiResult.Success) _agens.value = result.data
        return result
    }
}
