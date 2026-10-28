package ir.arminniromandi.permissionchecker.domain.usecase

import ir.arminniromandi.permissionchecker.domain.model.AppInfo
import ir.arminniromandi.permissionchecker.domain.repository.AppScanRepository
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

class GetAppListUseCase @Inject constructor(
    private val repository: AppScanRepository
) {
    operator fun invoke(): StateFlow<List<AppInfo>> = repository.lastResult
}
