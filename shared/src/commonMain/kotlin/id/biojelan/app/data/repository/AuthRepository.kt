package id.biojelan.app.data.repository

import id.biojelan.app.data.remote.ApiClient
import id.biojelan.app.data.remote.ApiResult
import id.biojelan.app.data.remote.AuthPayload
import id.biojelan.app.data.remote.ForgotPasswordRequest
import id.biojelan.app.data.remote.LoginRequest
import id.biojelan.app.data.remote.RegisterRequest
import id.biojelan.app.data.remote.UserDto
import io.ktor.http.HttpMethod
import kotlinx.serialization.json.Json

/** Endpoint di authentication.md (kecuali `kilang-login` — itu untuk dashboard web Kilang, bukan app mobile). */
class AuthRepository(
    private val api: ApiClient,
    private val session: SessionManager,
    private val json: Json,
) {
    init {
        // Server menolak token -> keluarkan pengguna ke layar login.
        api.onUnauthorized = { session.clear() }
    }

    /** Dipanggil sekali saat app dibuka: validasi token tersimpan lewat GET /api/user. */
    suspend fun bootstrap() {
        if (session.token.isNullOrBlank()) {
            session.clear()
            return
        }
        session.setLoading()
        when (val result = fetchUser()) {
            is ApiResult.Success -> session.setLoggedIn(result.data)
            is ApiResult.Failure -> when (result.kind) {
                ApiResult.Kind.Unauthorized -> session.clear()
                else -> {
                    val cached = session.cachedUser()
                    if (cached != null) session.setLoggedIn(cached) else session.setStartupError(result.message)
                }
            }
        }
    }

    suspend fun login(email: String, password: String): ApiResult<Unit> {
        val body = json.encodeToString(LoginRequest.serializer(), LoginRequest(email.trim(), password))
        val auth = api.call(HttpMethod.Post, "/api/login", authenticated = false, bodyJson = body) { data ->
            json.decodeFromJsonElement(AuthPayload.serializer(), requireNotNull(data))
        }
        return completeAuth(auth)
    }

    suspend fun register(name: String, email: String, password: String, confirmation: String): ApiResult<Unit> {
        val body = json.encodeToString(
            RegisterRequest.serializer(),
            RegisterRequest(name.trim(), email.trim(), password, confirmation),
        )
        val auth = api.call(HttpMethod.Post, "/api/register", authenticated = false, bodyJson = body) { data ->
            json.decodeFromJsonElement(AuthPayload.serializer(), requireNotNull(data))
        }
        return completeAuth(auth)
    }

    /** Token dari login/register disimpan dulu, lalu profil lengkap (termasuk peran) diambil dari /api/user. */
    private suspend fun completeAuth(auth: ApiResult<AuthPayload>): ApiResult<Unit> {
        return when (auth) {
            is ApiResult.Failure -> auth
            is ApiResult.Success -> {
                session.saveToken(auth.data.token)
                when (val user = fetchUser()) {
                    is ApiResult.Success -> {
                        session.setLoggedIn(user.data)
                        ApiResult.Success(Unit)
                    }
                    is ApiResult.Failure -> {
                        session.clear()
                        user
                    }
                }
            }
        }
    }

    suspend fun forgotPassword(email: String): ApiResult<Unit> {
        val body = json.encodeToString(ForgotPasswordRequest.serializer(), ForgotPasswordRequest(email.trim()))
        return api.call(HttpMethod.Post, "/api/forgot-password", authenticated = false, bodyJson = body) { }
    }

    /** Logout selalu berhasil di sisi app, meski request ke server gagal (mis. offline). */
    suspend fun logout() {
        api.call(HttpMethod.Delete, "/api/logout") { }
        session.clear()
    }

    /** Lewati login: masuk ke SessionState.Guest (lihat GuestScreen untuk batasannya). */
    fun enterGuestMode() = session.setGuest()

    private suspend fun fetchUser(): ApiResult<UserDto> =
        api.call(HttpMethod.Get, "/api/user") { data ->
            json.decodeFromJsonElement(UserDto.serializer(), requireNotNull(data))
        }
}
