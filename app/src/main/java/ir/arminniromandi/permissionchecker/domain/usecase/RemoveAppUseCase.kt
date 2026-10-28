package ir.arminniromandi.permissionchecker.domain.usecase

import ir.arminniromandi.permissionchecker.domain.repository.AppScanRepository
import javax.inject.Inject

class RemoveAppUseCase @Inject constructor(
    private val repository: AppScanRepository
) {
    suspend operator fun invoke(packageName: String) {
        repository.removeApp(packageName)
    }
}
