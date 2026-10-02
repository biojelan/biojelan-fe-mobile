package id.biojelan.app.ui.driver

import androidx.lifecycle.viewModelScope
import id.biojelan.app.core.changedStatus
import id.biojelan.app.core.isToday
import id.biojelan.app.core.parseIsoMillis
import id.biojelan.app.core.shouldAnnounceNewAssignment
import id.biojelan.app.data.remote.ApiResult
import id.biojelan.app.data.remote.DriverTransactionDto
import id.biojelan.app.data.remote.PickupStatusDto
import id.biojelan.app.data.remote.UserDto
import id.biojelan.app.data.remote.AgenSummaryDto
import id.biojelan.app.data.repository.DriverTransactionRepository
import id.biojelan.app.data.repository.PickupRepository
import id.biojelan.app.data.repository.PickupStatus
import id.biojelan.app.data.repository.SessionManager
import id.biojelan.app.data.repository.SessionState
import id.biojelan.app.data.repository.UserRepository
import id.biojelan.app.data.repository.TxStatus
import id.biojelan.app.data.repository.txStatus
import id.biojelan.app.ui.BaseViewModel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DriverUiState(
    val transactions: List<DriverTransactionDto> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null,
    val creating: Boolean = false,
    /** transaction_id yang sedang diajukan pembatalannya. */
    val busyTxId: String? = null,
    /** Penugasan jemput dari Kilang (pickup.md). */
    val pickup: PickupStatusDto? = null,
    val pickupLoading: Boolean = true,
    val pickupError: String? = null,
    val pickupBusy: Boolean = false,
) {
    private val todays
        get() = transactions.filter {
            isToday(it.createdAt) && it.txStatus != TxStatus.Cancelled && it.txStatus != TxStatus.Rejected
        }
    val todayCount: Int get() = todays.size
    val todayLiters: Double get() = todays.sumOf { it.volumeLiter }
    val todayValue: Long get() = todays.sumOf { it.totalPrice }
}

/** Sisi Driver dari transaction_agen.md. Endpoint transaksi sudah live di backend. */
class DriverViewModel(
    private val transactions: DriverTransactionRepository,
    private val pickups: PickupRepository,
    private val users: UserRepository,
    session: SessionManager,
) : BaseViewModel() {
    private val _state = MutableStateFlow(DriverUiState())
    val state: StateFlow<DriverUiState> = _state.asStateFlow()

    /** Profil Driver aktif (untuk tab Profil). */
    val user: StateFlow<UserDto?> = session.state
        .map { (it as? SessionState.LoggedIn)?.user }
        .stateIn(viewModelScope, SharingStarted.Eagerly, session.currentUser)

    init {
        refresh()
        viewModelScope.launch { users.listAgen() }
    }

    /**
     * Lokasi Agen tujuan (untuk tombol "Rute ke Agen") — dari cache `GET /api/user/agen` yang sama dengan
     * yang dipakai Klien untuk Cari Agen, jadi tidak perlu endpoint baru. Bisa null kalau daftar Agen belum
     * selesai dimuat atau Agen tidak ditemukan (mis. sudah nonaktif).
     */
    fun agenLocation(agenId: String): AgenSummaryDto? = users.agenById(agenId)

    override fun canAutoRefresh(): Boolean = _state.value.let {
        !it.loading && !it.creating && !it.pickupBusy && it.busyTxId == null
    }

    /**
     * Polling berkala: transaksi + penugasan pickup. Gagal-diam (data lama tetap tampil). Toast muncul kalau
     * Agen baru saja menjawab permintaan Driver, atau ada penugasan jemput baru dari Kilang.
     */
    override suspend fun refreshSilently() {
        coroutineScope {
            launch {
                val result = pickups.driverStatus()
                if (result is ApiResult.Success) {
                    val previous = _state.value
                    val incoming = result.data
                    _state.update { it.copy(pickup = incoming, pickupLoading = false, pickupError = null) }
                    val announce = shouldAnnounceNewAssignment(
                        previousLoadedOk = !previous.pickupLoading && previous.pickupError == null,
                        previousId = previous.pickup?.pickupId,
                        incomingId = incoming?.pickupId,
                    )
                    if (announce) toast("Ada penugasan jemput baru dari Kilang")
                }
            }
            val result = transactions.driverTransactions()
            if (result is ApiResult.Success) {
                val sorted = result.data.sortedByDescending { tx -> parseIsoMillis(tx.createdAt) ?: 0L }
                val before = _state.value.transactions
                _state.update { it.copy(transactions = sorted, loading = false, error = null) }
                val changed = changedStatus(before, sorted, id = { it.transactionId }, status = { it.status })
                if (changed.isNotEmpty()) {
                    toast(
                        if (changed.size > 1) "Status ${changed.size} permintaan Anda diperbarui"
                        else when (changed.first().txStatus) {
                            TxStatus.Accepted -> "Agen menerima permintaan Anda"
                            TxStatus.Rejected -> "Agen menolak permintaan Anda"
                            TxStatus.Cancelled -> "Agen menyetujui pembatalan"
                            else -> "Status permintaan Anda diperbarui"
                        },
                    )
                }
            }
        }
    }

    /** GET /api/driver/transactions dan GET /api/driver/pickup/status. */
    fun refresh() {
        cancelAutoRefresh()
        viewModelScope.launch {
            val hadData = _state.value.transactions.isNotEmpty()
            _state.update { it.copy(loading = true, error = null) }
            launch { refreshPickup() }
            when (val result = transactions.driverTransactions()) {
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

    private suspend fun refreshPickup() {
        _state.update { it.copy(pickupLoading = true, pickupError = null) }
        when (val result = pickups.driverStatus()) {
            is ApiResult.Success -> _state.update { it.copy(pickup = result.data, pickupLoading = false) }
            is ApiResult.Failure -> _state.update { it.copy(pickupLoading = false, pickupError = result.message) }
        }
    }

    /**
     * PATCH /api/driver/pickup/status — ASSIGNED → OTW → COMPLETED, atau CANCELLED. Respons hanya memuat
     * pickup_id, status, updated_at, jadi hasilnya digabung ke penugasan yang sedang tampil.
     */
    fun updatePickup(status: PickupStatus) {
        val current = _state.value.pickup ?: return
        if (_state.value.pickupBusy) return
        cancelAutoRefresh()
        viewModelScope.launch {
            _state.update { it.copy(pickupBusy = true) }
            when (val result = pickups.updateDriverStatus(current.pickupId, status)) {
                is ApiResult.Success -> {
                    toast(
                        when (status) {
                            PickupStatus.OnTheWay -> "Status diubah: dalam perjalanan"
                            PickupStatus.Completed -> "Penjemputan ditandai selesai"
                            PickupStatus.Cancelled -> "Penjemputan dibatalkan"
                            else -> "Status penjemputan diperbarui"
                        },
                    )
                    _state.update { st ->
                        st.copy(
                            pickup = st.pickup?.copy(
                                status = result.data.status.ifBlank { status.apiValue },
                                updatedAt = result.data.updatedAt.ifBlank { current.updatedAt },
                            ),
                        )
                    }
                }
                is ApiResult.Failure -> toast(result.message)
            }
            _state.update { it.copy(pickupBusy = false) }
        }
    }

    /**
     * POST /api/driver/transaction. [contact] berisi email ATAU nomor telepon Agen terdaftar; harga dihitung
     * server. Agen lalu menerima atau menolak di app-nya.
     */
    fun createTransaction(contact: String, volumeLiter: Double, note: String, onSuccess: () -> Unit) {
        if (_state.value.creating) return
        cancelAutoRefresh()
        val value = contact.trim()
        val isEmail = '@' in value
        viewModelScope.launch {
            _state.update { it.copy(creating = true) }
            val result = transactions.create(
                agenEmail = if (isEmail) value else null,
                agenPhone = if (isEmail) null else value,
                volumeLiter = volumeLiter,
                note = note,
            )
            when (result) {
                is ApiResult.Success -> {
                    val who = result.data.agenName.ifBlank { value }
                    toast("Permintaan dikirim ke $who — menunggu konfirmasi Agen")
                    onSuccess()
                    refresh()
                }
                is ApiResult.Failure -> toast(result.message)
            }
            _state.update { it.copy(creating = false) }
        }
    }

    /** POST /api/driver/transaction/{id}/cancel — hanya saat PENDING/ACCEPTED; Agen lalu menyetujui atau menolak. */
    fun requestCancel(transactionId: String, onDone: () -> Unit = {}) {
        if (_state.value.busyTxId != null) return
        cancelAutoRefresh()
        viewModelScope.launch {
            _state.update { it.copy(busyTxId = transactionId) }
            when (val result = transactions.requestCancel(transactionId)) {
                is ApiResult.Success -> {
                    toast("Pembatalan diajukan — menunggu persetujuan Agen")
                    refresh()
                    onDone()
                }
                is ApiResult.Failure -> toast(result.message)
            }
            _state.update { it.copy(busyTxId = null) }
        }
    }
}
