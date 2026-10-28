package ir.arminniromandi.permissionchecker.ui.applist

import ir.arminniromandi.permissionchecker.domain.model.AppInfo
import ir.arminniromandi.permissionchecker.domain.model.RiskLevel

data class AppListUiState(
    val apps: List<AppInfo> = emptyList(),
    val searchQuery: String = "",
    val selectedRiskFilter: RiskLevel? = null, // null means "All"
    val includeSystemApps: Boolean = false,
    val isLoading: Boolean = false
)
