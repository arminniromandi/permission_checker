package ir.arminniromandi.permissionchecker.domain.usecase

import ir.arminniromandi.permissionchecker.domain.model.ScanEvent
import ir.arminniromandi.permissionchecker.domain.repository.AppScanRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ScanAppsUseCase @Inject constructor(
    private val repository: AppScanRepository
) {
    operator fun invoke(): Flow<ScanEvent> = repository.scan()
}
