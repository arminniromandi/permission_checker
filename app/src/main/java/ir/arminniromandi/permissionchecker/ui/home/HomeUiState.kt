package ir.arminniromandi.permissionchecker.ui.home

import ir.arminniromandi.permissionchecker.domain.model.ScanProgress
import ir.arminniromandi.permissionchecker.domain.model.ScanSummary

sealed interface HomeUiState {
    data object Idle : HomeUiState
    data class Scanning(val progress: ScanProgress) : HomeUiState
    data class Done(val summary: ScanSummary) : HomeUiState
    data class Error(val message: String) : HomeUiState
}
