package com.maintenance.supervisor.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.maintenance.supervisor.domain.model.MaintenanceAnswer
import com.maintenance.supervisor.data.remote.NetworkMonitor
import com.maintenance.supervisor.domain.repository.*
import com.maintenance.supervisor.sync.SyncScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(val phone: String = "", val password: String = "", val loading: Boolean = false, val error: String? = null, val done: Boolean = false)
@HiltViewModel class LoginViewModel @Inject constructor(private val auth: AuthRepository, private val maintenance: MaintenanceRepository, private val scheduler: SyncScheduler) : ViewModel() {
    private val _state = MutableStateFlow(LoginUiState()); val state = _state.asStateFlow()
    fun phone(value: String) { _state.update { it.copy(phone = value, error = null) } }
    fun password(value: String) { _state.update { it.copy(password = value, error = null) } }
    fun submit() { if (_state.value.loading) return; viewModelScope.launch {
        val s = _state.value
        if (s.phone.isBlank() || s.password.isBlank()) { _state.update { it.copy(error = "أدخل رقم الهاتف وكلمة المرور.") }; return@launch }
        _state.update { it.copy(loading = true, error = null) }
        when (val result = auth.login(s.phone, s.password)) {
            is AppResult.Error -> _state.update { it.copy(loading = false, error = result.message) }
            is AppResult.Success -> { maintenance.bootstrap(); scheduler.enqueue(); _state.update { it.copy(loading = false, done = true, password = "") } }
        }
    } }
}

data class HomeUiState(val snapshot: HomeSnapshot = HomeSnapshot(null, null, null, null), val refreshing: Boolean = false, val message: String? = null, val connected: Boolean = false)
@HiltViewModel class HomeViewModel @Inject constructor(private val repository: MaintenanceRepository, private val auth: AuthRepository, private val scheduler: SyncScheduler, network: NetworkMonitor) : ViewModel() {
    private val transient = MutableStateFlow(Pair(false, null as String?))
    val state = combine(repository.observeHome(), transient, network.connected) { home, t, connected -> HomeUiState(home, t.first, t.second, connected) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())
    init { scheduler.enqueue() }
    fun refresh() = viewModelScope.launch { transient.value = true to null; val r = repository.sync(); transient.value = false to (r as? AppResult.Error)?.message }
    fun start(onReady: () -> Unit) = viewModelScope.launch { when (val r = repository.startOrLoadToday()) { is AppResult.Success -> onReady(); is AppResult.Error -> transient.value = false to r.message } }
    fun logout(onDone: () -> Unit) = viewModelScope.launch { auth.logout(); onDone() }
}

data class InspectionUiState(val home: HomeSnapshot? = null, val saving: Boolean = false, val error: String? = null)
@HiltViewModel class InspectionViewModel @Inject constructor(private val repository: MaintenanceRepository, private val scheduler: SyncScheduler) : ViewModel() {
    val state = repository.observeHome().map { InspectionUiState(it) }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InspectionUiState())
    fun update(itemId: Int, checked: Boolean, note: String) { val id = state.value.home?.daily?.report?.clientReportId ?: return; viewModelScope.launch { repository.saveAnswer(id, MaintenanceAnswer(itemId, checked, note)) } }
    fun complete(onDone: () -> Unit) { val id = state.value.home?.daily?.report?.clientReportId ?: return; viewModelScope.launch {
        if (repository.completeReport(id) is AppResult.Success) { scheduler.enqueue(); onDone() }
    } }
}
