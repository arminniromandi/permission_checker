package ir.arminniromandi.permissionchecker.ui.detail

import ir.arminniromandi.permissionchecker.domain.model.AppInfo

sealed interface DetailUiState {
    data object Loading : DetailUiState
    data class Success(val app: AppInfo) : DetailUiState
    data class Error(val message: String) : DetailUiState
}

sealed interface DetailEvent {
    data class RequestUninstall(val packageName: String) : DetailEvent
    data object AppUninstalled : DetailEvent
}
