package id.biojelan.app.ui.klien

import androidx.lifecycle.viewModelScope
import id.biojelan.app.core.AppConfig
import id.biojelan.app.core.parseIsoMillis
import id.biojelan.app.data.remote.AgenSummaryDto
import id.biojelan.app.data.remote.ApiResult
import id.biojelan.app.data.remote.TransactionDto
import id.biojelan.app.data.remote.UserDto
import id.biojelan.app.data.repository.SessionState
import id.biojelan.app.data.repository.PriceProvider
import id.biojelan.app.data.repository.SessionManager
import id.biojelan.app.data.repository.TransactionRepository
import id.biojelan.app.data.repository.TxStatus
import id.biojelan.app.data.repository.UserRepository
import id.biojelan.app.data.repository.txStatus
import id.biojelan.app.ui.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class KlienUiState(
    val agens: List<AgenSummaryDto> = emptyList(),
    val agensLoading: Boolean = true,
    val agensError: String? = null,
    val transactions: List<TransactionDto> = emptyList(),
    val txLoading: Boolean = true,
    val txError: String? = null,
    val price: Long = AppConfig.DEFAULT_PRICE_PER_LITER,
    /** transaction_id yang sedang diproses (terima/tolak/setujui batal/tolak batal). */
    val busyTxId: String? = null,
) {
    /** Transaksi terbaru yang menunggu diterima/ditolak Klien. */
    val pendingTx: TransactionDto?
        get() = transactions.firstOrNull { it.txStatus == TxStatus.Pending }

    /** Transaksi terbaru yang pembatalannya diajukan Agen dan menunggu jawaban Klien. */
    val cancelRequestTx: TransactionDto?
        get() = transactions.firstOrNull { it.txStatus == TxStatus.CancelRequested }

    fun agenName(agenId: String, fallback: String = ""): String =
        agens.firstOrNull { it.agenId == agenId }?.name ?: fallback.ifBlank { "Agen" }
}

class KlienViewModel(
    private val users: UserRepository,
    private val transactions: TransactionRepository,
    private val session: SessionManager,
    private val priceProvider: PriceProvider,
) : BaseViewModel() {
    private val _state = MutableStateFlow(KlienUiState())
    val state: StateFlow<KlienUiState> = _state.asStateFlow()

    /** Profil Klien aktif (nama, ID untuk ditunjukkan ke Agen). */
    val user: StateFlow<UserDto?> = session.state
        .map { (it as? SessionState.LoggedIn)?.user }
        .stateIn(viewModelScope, SharingStarted.Eagerly, session.currentUser)

    init {
        refreshAll()
    }

    fun refreshAll() {
        loadAgens()
        loadTransactions()
        viewModelScope.launch {
            val price = priceProvider.pricePerLiter()
            _state.update { it.copy(price = price) }
        }
    }

    fun loadAgens() {
        viewModelScope.launch {
            _state.update { it.copy(agensLoading = true, agensError = null) }
            when (val result = users.listAgen()) {
                is ApiResult.Success -> _state.update { it.copy(agens = result.data, agensLoading = false) }
                is ApiResult.Failure -> _state.update { it.copy(agensLoading = false, agensError = result.message) }
            }
        }
    }

    fun loadTransactions() {
        viewModelScope.launch {
            _state.update { it.copy(txLoading = true, txError = null) }
            // Server mengenali Klien dari token, jadi tidak perlu klien_id.
            when (val result = transactions.klienTransactions()) {
                is ApiResult.Success -> {
                    val sorted = result.data.sortedByDescending { tx -> parseIsoMillis(tx.createdAt) ?: 0L }
                    _state.update { it.copy(transactions = sorted, txLoading = false) }
                }
                is ApiResult.Failure -> _state.update { it.copy(txLoading = false, txError = result.message) }
            }
        }
    }

    fun accept(transactionId: String, onDone: () -> Unit = {}) = act(transactionId, "Transaksi diterima. Terima kasih!", onDone) {
        transactions.accept(transactionId)
    }

    fun reject(transactionId: String, onDone: () -> Unit = {}) = act(transactionId, "Transaksi ditolak.", onDone) {
        transactions.reject(transactionId)
    }

    fun acceptCancellation(transactionId: String, onDone: () -> Unit = {}) =
        act(transactionId, "Pembatalan disetujui.", onDone) {
            transactions.acceptCancellation(transactionId)
        }

    fun rejectCancellation(transactionId: String, onDone: () -> Unit = {}) =
        act(transactionId, "Pembatalan ditolak — transaksi tetap berlaku.", onDone) {
            transactions.rejectCancellation(transactionId)
        }

    private fun act(
        transactionId: String,
        successMessage: String,
        onDone: () -> Unit,
        call: suspend () -> ApiResult<*>,
    ) {
        if (_state.value.busyTxId != null) return
        viewModelScope.launch {
            _state.update { it.copy(busyTxId = transactionId) }
            when (val result = call()) {
                is ApiResult.Success -> {
                    toast(successMessage)
                    loadTransactions()
                    onDone()
                }
                is ApiResult.Failure -> toast(result.message)
            }
            _state.update { it.copy(busyTxId = null) }
        }
    }
}
