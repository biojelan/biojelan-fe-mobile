package id.biojelan.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.biojelan.app.data.repository.AuthRepository
import id.biojelan.app.data.repository.SessionManager
import id.biojelan.app.data.repository.SessionState
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class RootViewModel(
    private val auth: AuthRepository,
    session: SessionManager,
) : ViewModel() {
    val state: StateFlow<SessionState> = session.state

    init {
        viewModelScope.launch { auth.bootstrap() }
    }

    fun retry() {
        viewModelScope.launch { auth.bootstrap() }
    }

    fun logout() {
        viewModelScope.launch { auth.logout() }
    }
}
