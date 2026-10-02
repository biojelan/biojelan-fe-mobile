package id.biojelan.app.data.remote

import id.biojelan.app.core.AppConfig
import id.biojelan.app.data.local.SessionStore
import io.ktor.client.HttpClient
import io.ktor.client.request.accept
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.parameter
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlin.coroutines.cancellation.CancellationException

/**
 * Lapisan HTTP tunggal untuk semua endpoint di API-DOC.
 *
 * Bentuk respons di dokumentasi:
 *   sukses -> { "success"?: true, "data": {...}|[...], "message": "Success ..." }
 *   gagal  -> { "data": [] | {}, "message": "Failed ..." }
 * Kode status HTTP TIDAK didokumentasikan, jadi kegagalan dideteksi dari kombinasi:
 * status non-2xx, `success: false`, atau pesan berawalan "Failed".
 */
class ApiClient(
    private val http: HttpClient,
    private val json: Json,
    private val store: SessionStore,
) {
    /** Dipanggil saat server menolak token (sesi berakhir). Di-set oleh SessionManager. */
    var onUnauthorized: (() -> Unit)? = null

    suspend fun <T> call(
        method: HttpMethod,
        path: String,
        authenticated: Boolean = true,
        query: Map<String, String> = emptyMap(),
        bodyJson: String? = null,
        parse: (JsonElement?) -> T,
    ): ApiResult<T> {
        val token = store.token
        if (authenticated && token.isNullOrBlank()) {
            return ApiResult.Failure(MSG_SESSION_EXPIRED, ApiResult.Kind.Unauthorized)
        }
        return try {
            val response = http.request(AppConfig.BASE_URL + path) {
                this.method = method
                query.forEach { (key, value) -> parameter(key, value) }
                if (authenticated && token != null) bearerAuth(token)
                accept(ContentType.Application.Json)
                if (bodyJson != null) {
                    contentType(ContentType.Application.Json)
                    setBody(bodyJson)
                }
            }
            val result = interpret(response.status.value, response.bodyAsText(), parse)
            if (authenticated && result is ApiResult.Failure && result.kind == ApiResult.Kind.Unauthorized) {
                onUnauthorized?.invoke()
            }
            result
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Timeout / tidak bisa connect: laporkan apa adanya, tidak pernah diganti data contoh.
            ApiResult.Failure(MSG_NETWORK, ApiResult.Kind.Network)
        }
    }

    private fun <T> interpret(status: Int, body: String, parse: (JsonElement?) -> T): ApiResult<T> {
        val root: JsonObject? = try {
            json.parseToJsonElement(body) as? JsonObject
        } catch (e: Exception) {
            null
        }
        val message = (root?.get("message") as? JsonPrimitive)?.contentOrNull
        val successFlag = (root?.get("success") as? JsonPrimitive)?.booleanOrNull
        val httpOk = status in 200..299
        val looksFailed = message?.trimStart()?.startsWith("Failed", ignoreCase = true) == true

        if (!httpOk || successFlag == false || looksFailed) {
            val unauthorized = status == 401 || message?.contains("unauthorized", ignoreCase = true) == true
            return ApiResult.Failure(
                message = translate(message) ?: if (unauthorized) MSG_SESSION_EXPIRED else "Terjadi kesalahan pada server ($status).",
                kind = if (unauthorized) ApiResult.Kind.Unauthorized else ApiResult.Kind.Server,
                httpStatus = status,
            )
        }
        return try {
            ApiResult.Success(parse(root?.get("data")), message.orEmpty())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            ApiResult.Failure("Format data dari server tidak sesuai.", ApiResult.Kind.Server, status)
        }
    }

    /**
     * Pesan error dari backend berbahasa Inggris ("Failed login! Wrong password.").
     * Yang sudah diketahui diterjemahkan; sisanya ditampilkan apa adanya tanpa awalan "Failed ...!".
     */
    private fun translate(message: String?): String? {
        if (message.isNullOrBlank()) return null
        KNOWN_MESSAGES.forEach { (needle, id) ->
            if (message.contains(needle, ignoreCase = true)) return id
        }
        // "Failed login! Wrong password." -> "Wrong password."
        val bang = message.indexOf('!')
        return if (message.startsWith("Failed", ignoreCase = true) && bang in 0 until message.lastIndex) {
            message.substring(bang + 1).trim()
        } else message
    }

    private companion object {
        const val MSG_NETWORK = "Tidak dapat terhubung ke server. Periksa koneksi internet Anda."
        const val MSG_SESSION_EXPIRED = "Sesi Anda berakhir. Silakan masuk kembali."

        val KNOWN_MESSAGES = listOf(
            "User not registered" to "Akun belum terdaftar.",
            "Wrong password" to "Kata sandi salah.",
            "Email already exists" to "Email sudah terdaftar.",
            "Invalid password" to "Kata sandi tidak valid.",
            "Invalid confirmed password" to "Konfirmasi kata sandi tidak cocok.",
            "Email not found" to "Email tidak ditemukan.",
            "User unauthorized" to MSG_SESSION_EXPIRED,
        )
    }
}
