package id.biojelan.app.data.remote

sealed interface ApiResult<out T> {
    data class Success<T>(val data: T, val message: String = "") : ApiResult<T>

    data class Failure(
        val message: String,
        val kind: Kind = Kind.Server,
        val httpStatus: Int? = null,
    ) : ApiResult<Nothing>

    enum class Kind {
        /** Tidak ada koneksi / timeout / DNS. */
        Network,

        /** Token kosong, kadaluarsa, atau ditolak server. */
        Unauthorized,

        /** Server membalas dengan error (validasi, salah password, dll.). */
        Server,
    }
}

inline fun <T, R> ApiResult<T>.map(transform: (T) -> R): ApiResult<R> = when (this) {
    is ApiResult.Success -> ApiResult.Success(transform(data), message)
    is ApiResult.Failure -> this
}

fun ApiResult<*>.errorMessageOrNull(): String? = (this as? ApiResult.Failure)?.message
