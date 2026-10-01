package id.biojelan.app.ui.auth

import androidx.lifecycle.viewModelScope
import id.biojelan.app.data.remote.ApiResult
import id.biojelan.app.data.repository.AuthRepository
import id.biojelan.app.ui.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val loading: Boolean = false,
    /** Error tingkat form (mis. "Kata sandi salah."). */
    val error: String? = null,
    val fieldErrors: Map<String, String> = emptyMap(),
    /** True setelah permintaan reset kata sandi berhasil. */
    val resetSent: Boolean = false,
)

class AuthViewModel(private val auth: AuthRepository) : BaseViewModel() {
    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    fun clearErrors() = _state.update { it.copy(error = null, fieldErrors = emptyMap(), resetSent = false) }

    /** "Lihat sebagai tamu" — langsung pindah ke SessionState.Guest, tanpa panggilan API. */
    fun enterGuestMode() = auth.enterGuestMode()

    fun login(email: String, password: String) {
        val errors = buildMap {
            if (!isEmail(email)) put("email", "Masukkan email yang valid.")
            if (password.isEmpty()) put("password", "Kata sandi wajib diisi.")
        }
        if (errors.isNotEmpty()) {
            _state.update { it.copy(fieldErrors = errors, error = null) }
            return
        }
        submit { auth.login(email, password) }
    }

    fun register(name: String, email: String, password: String, confirmation: String) {
        val errors = buildMap {
            if (name.isBlank()) put("name", "Nama wajib diisi.")
            if (!isEmail(email)) put("email", "Masukkan email yang valid.")
            if (password.length < 8) put("password", "Minimal 8 karakter.")
            if (confirmation != password) put("confirmation", "Konfirmasi kata sandi tidak cocok.")
        }
        if (errors.isNotEmpty()) {
            _state.update { it.copy(fieldErrors = errors, error = null) }
            return
        }
        submit { auth.register(name, email, password, confirmation) }
    }

    fun forgotPassword(email: String) {
        if (!isEmail(email)) {
            _state.update { it.copy(fieldErrors = mapOf("email" to "Masukkan email yang valid."), error = null) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null, fieldErrors = emptyMap()) }
            when (val result = auth.forgotPassword(email)) {
                is ApiResult.Success -> _state.update { it.copy(loading = false, resetSent = true) }
                is ApiResult.Failure -> _state.update { it.copy(loading = false, error = result.message) }
            }
        }
    }

    /** Sukses login/register memicu perubahan SessionState -> App berpindah layar sendiri. */
    private fun submit(block: suspend () -> ApiResult<Unit>) {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null, fieldErrors = emptyMap()) }
            when (val result = block()) {
                is ApiResult.Success -> _state.update { it.copy(loading = false) }
                is ApiResult.Failure -> _state.update { it.copy(loading = false, error = result.message) }
            }
        }
    }

    private fun isEmail(value: String): Boolean {
        val v = value.trim()
        val at = v.indexOf('@')
        return at > 0 && v.indexOf('.', at) > at + 1 && !v.endsWith(".") && !v.contains(' ')
    }
}
