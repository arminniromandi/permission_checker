package ir.arminniromandi.permissionchecker.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.arminniromandi.permissionchecker.domain.model.ScanEvent
import ir.arminniromandi.permissionchecker.domain.model.ScanSummary
import ir.arminniromandi.permissionchecker.domain.repository.AppScanRepository
import ir.arminniromandi.permissionchecker.domain.usecase.ScanAppsUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val scanAppsUseCase: ScanAppsUseCase,
    private val repository: AppScanRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Idle)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var scanJob: Job? = null

    init {
        // Observe repository lastSummary to restore Done state after return or app removal
        viewModelScope.launch {
            repository.lastSummary.collect { summary ->
                if (summary != null && _uiState.value !is HomeUiState.Scanning) {
                    _uiState.value = HomeUiState.Done(summary)
                }
            }
        }
    }

    fun startScan() {
        if (scanJob?.isActive == true) return // Prevent double click

        scanJob = viewModelScope.launch {
            scanAppsUseCase()
                .catch { e ->
                    if (e is CancellationException) throw e
                    _uiState.value = HomeUiState.Error(e.message ?: "An unexpected error occurred")
                }
                .collect { event ->
                    when (event) {
                        is ScanEvent.Progress -> {
                            _uiState.value = HomeUiState.Scanning(event.progress)
                        }
                        is ScanEvent.Completed -> {
                            _uiState.value = HomeUiState.Done(event.summary)
                        }
                        is ScanEvent.Error -> {
                            _uiState.value = HomeUiState.Error(event.message)
                        }
                    }
                }
        }
    }

    fun cancelScan() {
        scanJob?.cancel()
        scanJob = null
        val existingSummary = repository.lastSummary.value
        if (existingSummary != null) {
            _uiState.value = HomeUiState.Done(existingSummary)
        } else {
            _uiState.value = HomeUiState.Idle
        }
    }
}
