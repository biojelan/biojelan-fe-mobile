package id.biojelan.app.data.repository

import id.biojelan.app.core.AppConfig
import id.biojelan.app.data.remote.ApiClient
import id.biojelan.app.data.remote.ApiResult
import id.biojelan.app.data.remote.PriceDto
import io.ktor.http.HttpMethod
import kotlinx.serialization.json.Json

/**
 * Sumber harga jelantah per liter (harga Klien → Agen) untuk TAMPILAN (banner harga, perkiraan total di
 * form). `POST /api/agent/transaction` sendiri tidak menerima harga dari app — server yang menghitung
 * `total_price` dari harga berlaku di database, jadi nilai dari provider ini murni estimasi di layar.
 */
interface PriceProvider {
    suspend fun pricePerLiter(): Long
}

/**
 * GET /api/price — endpoint publik (di luar `auth:sanctum`, sama seperti `/api/user/agen`), hanya
 * mengembalikan harga tipe Klien (`PriceType::Client`). Tidak ada endpoint publik untuk harga tipe Agen
 * (dipakai server saat menghitung transaksi Driver→Agen) — harga transaksi Driver dihitung server.
 *
 * Kalau harga belum ada hari ini, server membalas 404 "Price is unavailable" — itu kondisi normal, bukan
 * error, jadi fallback ke [AppConfig.DEFAULT_PRICE_PER_LITER] alih-alih menampilkan error ke pengguna untuk
 * sekadar banner harga. Kegagalan jaringan juga fallback ke nilai yang sama lewat alasan serupa.
 */
class ApiPriceProvider(
    private val api: ApiClient,
    private val json: Json,
) : PriceProvider {
    override suspend fun pricePerLiter(): Long {
        val result = api.call(HttpMethod.Get, "/api/price", authenticated = false) { data ->
            json.decodeFromJsonElement(PriceDto.serializer(), requireNotNull(data))
        }
        return when (result) {
            is ApiResult.Success -> result.data.pricePerLiter.takeIf { it > 0 } ?: AppConfig.DEFAULT_PRICE_PER_LITER
            is ApiResult.Failure -> AppConfig.DEFAULT_PRICE_PER_LITER
        }
    }
}
