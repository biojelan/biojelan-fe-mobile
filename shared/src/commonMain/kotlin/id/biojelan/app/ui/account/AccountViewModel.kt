package id.biojelan.app.ui.account

import androidx.lifecycle.viewModelScope
import id.biojelan.app.data.remote.ApiResult
import id.biojelan.app.data.repository.AgenEdit
import id.biojelan.app.data.repository.AuthRepository
import id.biojelan.app.data.repository.SessionManager
import id.biojelan.app.data.repository.UserRepository
import id.biojelan.app.data.repository.toUpdateRequest
import id.biojelan.app.ui.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Operasi akun yang dipakai bersama oleh tab Profil Klien dan Agen. */
class AccountViewModel(
    private val users: UserRepository,
    private val auth: AuthRepository,
    private val session: SessionManager,
) : BaseViewModel() {
    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _formError = MutableStateFlow<String?>(null)

    /** Error terakhir dari form sheet (ditampilkan di dalam sheet). */
    val formError: StateFlow<String?> = _formError.asStateFlow()

    fun clearFormError() {
        _formError.value = null
    }

    fun updateProfile(name: String, phone: String, agenEdit: AgenEdit?, onSuccess: () -> Unit) {
        val user = session.currentUser ?: return
        perform(successMessage = "Profil diperbarui", onSuccess = onSuccess) {
            users.updateUser(user.toUpdateRequest(name = name.trim(), phone = phone.trim(), agenEdit = agenEdit))
        }
    }

    fun changePassword(current: String, new: String, confirmation: String, onSuccess: () -> Unit) {
        perform(successMessage = "Kata sandi diperbarui", onSuccess = onSuccess) {
            users.changePassword(current, new, confirmation)
        }
    }

    fun deleteAccount() {
        // Sukses -> SessionManager.clear() dipanggil repository dan App kembali ke layar login.
        perform(successMessage = "Akun dihapus", onSuccess = {}) { users.deleteAccount() }
    }

    fun logout() {
        viewModelScope.launch {
            _busy.value = true
            auth.logout()
            _busy.value = false
        }
    }

    private fun perform(successMessage: String, onSuccess: () -> Unit, call: suspend () -> ApiResult<*>) {
        if (_busy.value) return
        viewModelScope.launch {
            _busy.value = true
            _formError.value = null
            when (val result = call()) {
                is ApiResult.Success -> {
                    toast(successMessage)
                    onSuccess()
                }
                is ApiResult.Failure -> {
                    _formError.value = result.message
                    toast(result.message)
                }
            }
            _busy.update { false }
        }
    }
}
