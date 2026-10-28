package ir.arminniromandi.permissionchecker.domain.usecase

import ir.arminniromandi.permissionchecker.domain.model.AppInfo
import ir.arminniromandi.permissionchecker.domain.repository.AppScanRepository
import javax.inject.Inject

class GetAppDetailUseCase @Inject constructor(
    private val repository: AppScanRepository
) {
    suspend operator fun invoke(packageName: String): AppInfo? {
        return repository.getApp(packageName)
    }
}
