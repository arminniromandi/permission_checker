package ir.arminniromandi.permissionchecker.ui.applist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.arminniromandi.permissionchecker.domain.model.AppInfo
import ir.arminniromandi.permissionchecker.domain.model.RiskLevel
import ir.arminniromandi.permissionchecker.domain.usecase.GetAppListUseCase
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class AppListViewModel @Inject constructor(
    getAppListUseCase: GetAppListUseCase
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _selectedRiskFilter = MutableStateFlow<RiskLevel?>(null)
    val selectedRiskFilter: StateFlow<RiskLevel?> = _selectedRiskFilter

    private val _includeSystemApps = MutableStateFlow(false)
    val includeSystemApps: StateFlow<Boolean> = _includeSystemApps

    private val debouncedSearch = _searchQuery.debounce(200L)

    val uiState: StateFlow<AppListUiState> = combine(
        getAppListUseCase(),
        debouncedSearch,
        _selectedRiskFilter,
        _includeSystemApps
    ) { allApps, query, riskFilter, includeSystem ->
        val filtered = allApps.filter { app ->
            // System app filter (default: exclude system apps unless toggled)
            if (!includeSystem && app.isSystemApp) {
                return@filter false
            }

            // Risk filter
            if (riskFilter != null && app.overallRisk != riskFilter) {
                return@filter false
            }

            // Search query filter
            if (query.isNotBlank()) {
                val matchesName = app.appName.contains(query, ignoreCase = true)
                val matchesPackage = app.packageName.contains(query, ignoreCase = true)
                if (!matchesName && !matchesPackage) {
                    return@filter false
                }
            }

            true
        }.sortedWith(
            compareBy<AppInfo> { it.overallRisk.ordinal } // HIGH (0) first, then MEDIUM (1), LOW (2), NONE (3)
                .thenBy { it.appName.lowercase() }
        )

        AppListUiState(
            apps = filtered,
            searchQuery = _searchQuery.value,
            selectedRiskFilter = riskFilter,
            includeSystemApps = includeSystem
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AppListUiState()
    )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onRiskFilterChanged(risk: RiskLevel?) {
        _selectedRiskFilter.value = risk
    }

    fun onIncludeSystemAppsToggled(include: Boolean) {
        _includeSystemApps.value = include
    }
}
