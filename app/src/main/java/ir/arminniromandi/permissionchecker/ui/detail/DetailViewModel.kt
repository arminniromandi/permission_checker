package ir.arminniromandi.permissionchecker.ui.detail

import android.content.pm.PackageManager
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import ir.arminniromandi.permissionchecker.domain.usecase.GetAppDetailUseCase
import ir.arminniromandi.permissionchecker.domain.usecase.RemoveAppUseCase
import ir.arminniromandi.permissionchecker.ui.navigation.DetailRoute
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getAppDetailUseCase: GetAppDetailUseCase,
    private val removeAppUseCase: RemoveAppUseCase,
    private val packageManager: PackageManager
) : ViewModel() {

    val packageName: String = savedStateHandle.get<String>("packageName")
        ?: runCatching { savedStateHandle.toRoute<DetailRoute>().packageName }.getOrDefault("")

    private val _uiState = MutableStateFlow<DetailUiState>(DetailUiState.Loading)
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    private val _events = Channel<DetailEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        loadApp()
    }

    fun loadApp() {
        viewModelScope.launch {
            _uiState.value = DetailUiState.Loading
            val app = getAppDetailUseCase(packageName)
            if (app != null) {
                _uiState.value = DetailUiState.Success(app)
            } else {
                _uiState.value = DetailUiState.Error("Application information not found")
            }
        }
    }

    fun onUninstallClicked() {
        viewModelScope.launch {
            _events.send(DetailEvent.RequestUninstall(packageName))
        }
    }

    fun onUninstallResultReturned() {
        // Check if package is still installed
        val isStillInstalled = try {
            packageManager.getPackageInfo(packageName, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        } catch (_: Exception) {
            false
        }

        if (!isStillInstalled) {
            viewModelScope.launch {
                removeAppUseCase(packageName)
                _events.send(DetailEvent.AppUninstalled)
            }
        }
    }
}
