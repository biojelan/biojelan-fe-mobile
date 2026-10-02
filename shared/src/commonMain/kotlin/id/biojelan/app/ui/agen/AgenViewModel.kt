package id.biojelan.app.ui.agen

import androidx.lifecycle.viewModelScope
import id.biojelan.app.core.AppConfig
import id.biojelan.app.core.hasNewActionable
import id.biojelan.app.core.isToday
import id.biojelan.app.core.parseIsoMillis
import id.biojelan.app.data.remote.ApiResult
import id.biojelan.app.data.remote.DriverTransactionDto
import id.biojelan.app.data.remote.PickupStatusDto
import id.biojelan.app.data.remote.TransactionDto
import id.biojelan.app.data.remote.UserDto
import id.biojelan.app.data.repository.DriverTransactionRepository
import id.biojelan.app.data.repository.PickupRepository
import id.biojelan.app.data.repository.PriceProvider
import id.biojelan.app.data.repository.SessionManager
import id.biojelan.app.data.repository.SessionState
import id.biojelan.app.data.repository.TransactionRepository
import id.biojelan.app.data.repository.TxStatus
import id.biojelan.app.data.repository.UserRepository
import id.biojelan.app.data.repository.txStatus
import id.biojelan.app.ui.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AgenUiState(
    val transactions: List<TransactionDto> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null,
    val price: Long = AppConfig.DEFAULT_PRICE_PER_LITER,
    val creating: Boolean = false,
    /** transaction_id yang sedang diajukan pembatalannya. */
    val busyTxId: String? = null,
    val pickup: PickupStatusDto? = null,
    val pickupLoading: Boolean = true,
    /** Pesan error pemuatan status pickup terakhir; null = terakhir berhasil. Data [pickup] lama tetap dipertahankan. */
    val pickupError: String? = null,
    /** Pesan error pemuatan transaksi Driver terakhir; null = terakhir berhasil. */
    val driverTxError: String? = null,
    /** Transaksi Driver → Agen (Driver mengambil minyak dari Agen ini). */
    val driverTransactions: List<DriverTransactionDto> = emptyList(),
    /** transaction_id transaksi Driver yang sedang diproses (terima/tolak/setujui batal/tolak batal). */
    val busyDriverTxId: String? = null,
) {
    /** Permintaan Driver terbaru yang menunggu diterima/ditolak Agen. */
    val driverPendingTx: DriverTransactionDto?
        get() = driverTransactions.firstOrNull { it.txStatus == TxStatus.Pending }

    /** Permintaan pembatalan dari Driver terbaru yang menunggu jawaban Agen. */
    val driverCancelTx: DriverTransactionDto?
        get() = driverTransactions.firstOrNull { it.txStatus == TxStatus.CancelRequested }

    private val todays get() = transactions.filter {
        isToday(it.createdAt) && it.txStatus != TxStatus.Cancelled && it.txStatus != TxStatus.Rejected
    }
    val todayCount: Int get() = todays.size
    val todayLiters: Double get() = todays.sumOf { it.volumeLiter }

    private val todayDriverTx get() = driverTransactions.filter {
        isToday(it.createdAt) && it.txStatus != TxStatus.Cancelled && it.txStatus != TxStatus.Rejected
    }
    val todayDriverCount: Int get() = todayDriverTx.size
    val todayDriverLiters: Double get() = todayDriverTx.sumOf { it.volumeLiter }
    val todayDriverValue: Long get() = todayDriverTx.sumOf { it.totalPrice }
    val todayValue: Long get() = todays.sumOf { it.totalPrice }
}

/** Hasil pengecekan email/telepon Klien (check-clients-email/phone) sebelum submit transaksi. */
sealed interface ClientPreview {
    data object Idle : ClientPreview
    data object Checking : ClientPreview
    data class Found(val name: String) : ClientPreview
    data object NotFound : ClientPreview
}

class AgenViewModel(
    private val users: UserRepository,
    private val transactions: TransactionRepository,
    private val session: SessionManager,
    private val priceProvider: PriceProvider,
    private val pickups: PickupRepository,
    private val driverTx: DriverTransactionRepository,
) : BaseViewModel() {
    private val _clientPreview = MutableStateFlow<ClientPreview>(ClientPreview.Idle)
    val clientPreview: StateFlow<ClientPreview> = _clientPreview.asStateFlow()
    private var previewJob: Job? = null
    private val _state = MutableStateFlow(AgenUiState())
    val state: StateFlow<AgenUiState> = _state.asStateFlow()

    /** Profil Agen terbaru (stok, status buka/tutup, dst.) dari sesi. */
    val user: StateFlow<UserDto?> = session.state
        .map { (it as? SessionState.LoggedIn)?.user }
        .stateIn(viewModelScope, SharingStarted.Eagerly, session.currentUser)

    init {
        refresh()
    }

    override fun canAutoRefresh(): Boolean = _state.value.let {
        !it.loading && !it.creating && it.busyTxId == null && it.busyDriverTxId == null
    }

    /**
     * Polling berkala: transaksi Klien, permintaan Driver, status pickup, dan profil (stok). Harga tidak ikut
     * (jarang berubah). Semua gagal-diam: data lama tetap tampil, tidak ada spinner/error baru.
     */
    override suspend fun refreshSilently() {
        coroutineScope {
            launch { users.refreshUser() }
            launch {
                val result = pickups.agenStatus()
                if (result is ApiResult.Success) {
                    _state.update { it.copy(pickup = result.data, pickupLoading = false, pickupError = null) }
                }
            }
            launch { refreshDriverTx(silent = true) }
            val result = transactions.agenTransactions()
            if (result is ApiResult.Success) {
                val sorted = result.data.sortedByDescending { tx -> parseIsoMillis(tx.createdAt) ?: 0L }
                _state.update { it.copy(transactions = sorted, loading = false, error = null) }
            }
        }
    }

    fun refresh(): Job {
        cancelAutoRefresh()
        return viewModelScope.launch {
            val hadData = _state.value.transactions.isNotEmpty()
            _state.update { it.copy(loading = true, error = null) }
            // Stok (`stock_liter`) ikut di GET /api/user, jadi ambil ulang profil bersamaan dengan transaksi.
            launch { users.refreshUser() }
            launch {
                val price = priceProvider.pricePerLiter()
                _state.update { it.copy(price = price) }
            }
            launch { refreshPickup() }
            launch { refreshDriverTx() }
            // Server mengenali Agen dari token, jadi tidak perlu agen_id.
            when (val result = transactions.agenTransactions()) {
                is ApiResult.Success -> {
                    val sorted = result.data.sortedByDescending { tx -> parseIsoMillis(tx.createdAt) ?: 0L }
                    _state.update { it.copy(transactions = sorted, loading = false) }
                }
                is ApiResult.Failure -> {
                    _state.update { it.copy(loading = false, error = result.message) }
                    if (hadData) toast(result.message)
                }
            }
        }
    }

    /**
     * Dipanggil tombol muat ulang: sama seperti [refresh], lalu memberi tahu pengguna kalau semuanya
     * berhasil dimuat. Kalau ada yang gagal, pesan error yang sudah ada yang tampil (bukan notifikasi ini).
     */
    fun manualRefresh() {
        viewModelScope.launch {
            refresh().join()
            val st = _state.value
            if (st.error == null && st.pickupError == null && st.driverTxError == null) toastUpToDate()
        }
    }

    /**
     * check-clients-email/phone (transaction_klien.md) — dipanggil sambil mengetik di form transaksi baru,
     * supaya Agen tahu Klien-nya terdaftar sebelum submit (submit ke Klien yang tidak terdaftar akan
     * ditolak server dengan 422 "Client not registered").
     */
    fun checkClientContact(contact: String) {
        previewJob?.cancel()
        val value = contact.trim()
        if (value.isBlank()) {
            _clientPreview.update { ClientPreview.Idle }
            return
        }
        val isEmail = '@' in value
        // Hindari nembak API tiap huruf saat email/nomor jelas belum lengkap.
        if (isEmail && !value.substringAfter('@', "").contains('.')) {
            _clientPreview.update { ClientPreview.Idle }
            return
        }
        if (!isEmail && value.length < 8) {
            _clientPreview.update { ClientPreview.Idle }
            return
        }
        previewJob = viewModelScope.launch {
            _clientPreview.update { ClientPreview.Checking }
            val result = if (isEmail) transactions.checkClientByEmail(value) else transactions.checkClientByPhone(value)
            _clientPreview.update {
                when (result) {
                    is ApiResult.Success ->
                        if (result.data.isExist) ClientPreview.Found(result.data.clientName) else ClientPreview.NotFound
                    // Gagal cek (mis. jaringan) -> diam saja, jangan halangi submit; validasi tetap di server.
                    is ApiResult.Failure -> ClientPreview.Idle
                }
            }
        }
    }

    fun resetClientPreview() {
        previewJob?.cancel()
        _clientPreview.update { ClientPreview.Idle }
    }

    /**
     * POST /api/agent/transaction. [contact] berisi email ATAU nomor telepon Klien terdaftar; harga dihitung
     * server. Klien lalu menerima atau menolak lewat app-nya.
     */
    fun createTransaction(contact: String, volumeLiter: Double, onSuccess: () -> Unit) {
        if (_state.value.creating) return
        cancelAutoRefresh()
        val value = contact.trim()
        val isEmail = '@' in value
        viewModelScope.launch {
            _state.update { it.copy(creating = true) }
            val result = transactions.createAsAgen(
                clientEmail = if (isEmail) value else null,
                clientPhone = if (isEmail) null else value,
                volumeLiter = volumeLiter,
            )
            when (result) {
                is ApiResult.Success -> {
                    val who = result.data.klienName.ifBlank { value }
                    toast("Transaksi dikirim ke $who — menunggu konfirmasi Klien")
                    onSuccess()
                    refresh()
                }
                is ApiResult.Failure -> toast(result.message)
            }
            _state.update { it.copy(creating = false) }
        }
    }

    /**
     * POST /api/agent/transaction/{id}/cancel — hanya saat PENDING/ACCEPTED. Status jadi CANCEL_REQUESTED
     * sampai Klien menyetujui atau menolak.
     */
    fun requestCancel(transactionId: String, onDone: () -> Unit = {}) {
        if (_state.value.busyTxId != null) return
        cancelAutoRefresh()
        viewModelScope.launch {
            _state.update { it.copy(busyTxId = transactionId) }
            when (val result = transactions.requestCancel(transactionId)) {
                is ApiResult.Success -> {
                    toast("Pembatalan diajukan — menunggu persetujuan Klien")
                    refresh()
                    onDone()
                }
                is ApiResult.Failure -> toast(result.message)
            }
            _state.update { it.copy(busyTxId = null) }
        }
    }

    /**
     * GET /api/agent/driver/transactions. Bagian tambahan: kegagalan (mis. backend Driver belum ada) tidak
     * mengganggu tampilan transaksi Klien, daftar Driver cukup dibiarkan kosong.
     */
    /**
     * GET transaksi Driver → Agen. [silent] = dipanggil polling: sukses boleh memunculkan toast permintaan baru,
     * gagal tidak mengubah apa-apa. Kalau tidak silent, kegagalan dicatat di [AgenUiState.driverTxError] dan —
     * bila daftar sudah berisi — dikabari lewat toast.
     */
    private suspend fun refreshDriverTx(silent: Boolean = false) {
        when (val result = driverTx.agentDriverTransactions()) {
            is ApiResult.Success -> {
                val sorted = result.data.sortedByDescending { tx -> parseIsoMillis(tx.createdAt) ?: 0L }
                val before = _state.value.driverTransactions
                _state.update { it.copy(driverTransactions = sorted, driverTxError = null) }
                val hasNew = hasNewActionable(
                    before, sorted,
                    key = { "${it.transactionId}:${it.status}" },
                    actionable = { it.txStatus == TxStatus.Pending || it.txStatus == TxStatus.CancelRequested },
                )
                if (silent && hasNew) toast("Ada permintaan Driver baru yang menunggu persetujuan Anda")
            }
            is ApiResult.Failure -> if (!silent) {
                val hadData = _state.value.driverTransactions.isNotEmpty()
                _state.update { it.copy(driverTxError = result.message) }
                if (hadData) toast(result.message)
            }
        }
    }

    /** POST /api/agent/transaction/{id}/accept — Driver mengambil minyak; stok Agen berkurang di server. */
    fun acceptDriverTx(transactionId: String) = actDriver(transactionId, "Permintaan Driver diterima.") {
        driverTx.accept(transactionId)
    }

    /** POST /api/agent/transaction/{id}/reject */
    fun rejectDriverTx(transactionId: String) = actDriver(transactionId, "Permintaan Driver ditolak.") {
        driverTx.reject(transactionId)
    }

    /** POST /api/agent/transaction/{id}/cancel-accept */
    fun acceptDriverCancel(transactionId: String) = actDriver(transactionId, "Pembatalan disetujui.") {
        driverTx.acceptCancellation(transactionId)
    }

    /** POST /api/agent/transaction/{id}/cancel-reject */
    fun rejectDriverCancel(transactionId: String) =
        actDriver(transactionId, "Pembatalan ditolak — permintaan tetap berlaku.") {
            driverTx.rejectCancellation(transactionId)
        }

    private fun actDriver(transactionId: String, successMessage: String, call: suspend () -> ApiResult<*>) {
        if (_state.value.busyDriverTxId != null) return
        cancelAutoRefresh()
        viewModelScope.launch {
            _state.update { it.copy(busyDriverTxId = transactionId) }
            when (val result = call()) {
                is ApiResult.Success -> {
                    toast(successMessage)
                    refreshDriverTx()
                    users.refreshUser() // stok Agen ikut berubah di server
                }
                is ApiResult.Failure -> toast(result.message)
            }
            _state.update { it.copy(busyDriverTxId = null) }
        }
    }

    /**
     * GET /api/agen/pickup/status. Dipanggil bersamaan dengan [refresh]; kegagalan di sini tidak
     * memblokir tampilan transaksi/profil. Data [AgenUiState.pickup] lama DIPERTAHANKAN (jangan di-null-kan:
     * itu akan tampil sebagai "belum ada penjemputan" padahal cuma gagal memuat) dan errornya dicatat di
     * [AgenUiState.pickupError].
     */
    private suspend fun refreshPickup() {
        _state.update { it.copy(pickupLoading = true, pickupError = null) }
        when (val result = pickups.agenStatus()) {
            is ApiResult.Success -> _state.update { it.copy(pickup = result.data, pickupLoading = false, pickupError = null) }
            is ApiResult.Failure -> {
                val hadPickup = _state.value.pickup != null
                _state.update { it.copy(pickupLoading = false, pickupError = result.message) }
                if (hadPickup) toast(result.message)
            }
        }
    }
}