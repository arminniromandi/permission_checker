package ir.arminniromandi.permissionchecker.domain.repository

import ir.arminniromandi.permissionchecker.domain.model.AppInfo
import ir.arminniromandi.permissionchecker.domain.model.ScanEvent
import ir.arminniromandi.permissionchecker.domain.model.ScanSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface AppScanRepository {
    fun scan(): Flow<ScanEvent>
    val lastResult: StateFlow<List<AppInfo>>
    val lastSummary: StateFlow<ScanSummary?>
    suspend fun getApp(packageName: String): AppInfo?
    suspend fun removeApp(packageName: String)
}
