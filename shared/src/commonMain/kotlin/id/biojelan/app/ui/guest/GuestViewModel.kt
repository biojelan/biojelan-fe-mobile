package id.biojelan.app.ui.guest

import androidx.lifecycle.viewModelScope
import id.biojelan.app.core.AppConfig
import id.biojelan.app.data.remote.AgenSummaryDto
import id.biojelan.app.data.remote.ApiResult
import id.biojelan.app.data.repository.PriceProvider
import id.biojelan.app.data.repository.SessionManager
import id.biojelan.app.data.repository.UserRepository
import id.biojelan.app.ui.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Status yang boleh dilihat pengunjung tanpa akun (mode tamu): lokasi Agen + harga jelantah terkini.
 * Keduanya tidak butuh token — `GET /api/user/agen` publik (lihat catatan di `UserRepository.listAgen`),
 * dan [PriceProvider] juga tidak memanggil API berauth.
 */
data class GuestUiState(
    val agens: List<AgenSummaryDto> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null,
    val price: Long = AppConfig.DEFAULT_PRICE_PER_LITER,
)

class GuestViewModel(
    private val users: UserRepository,
    private val session: SessionManager,
    private val priceProvider: PriceProvider,
) : BaseViewModel() {
    private val _state = MutableStateFlow(GuestUiState())
    val state: StateFlow<GuestUiState> = _state.asStateFlow()

    init {
        loadAgens()
        viewModelScope.launch {
            val price = priceProvider.pricePerLiter()
            _state.update { it.copy(price = price) }
        }
    }

    fun loadAgens() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            when (val result = users.listAgen()) {
                is ApiResult.Success -> _state.update { it.copy(agens = result.data, loading = false) }
                is ApiResult.Failure -> _state.update { it.copy(loading = false, error = result.message) }
            }
        }
    }

    /** Keluar dari mode tamu, kembali ke layar autentikasi untuk mendaftar/masuk. */
    fun exitToRegister() = session.clear()
}
