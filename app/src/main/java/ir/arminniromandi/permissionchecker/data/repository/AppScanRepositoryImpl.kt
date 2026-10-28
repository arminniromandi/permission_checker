package ir.arminniromandi.permissionchecker.data.repository

import ir.arminniromandi.permissionchecker.data.source.AppIconLoader
import ir.arminniromandi.permissionchecker.data.source.PackageScanner
import ir.arminniromandi.permissionchecker.domain.model.AppInfo
import ir.arminniromandi.permissionchecker.domain.model.RiskLevel
import ir.arminniromandi.permissionchecker.domain.model.ScanEvent
import ir.arminniromandi.permissionchecker.domain.model.ScanProgress
import ir.arminniromandi.permissionchecker.domain.model.ScanSummary
import ir.arminniromandi.permissionchecker.domain.repository.AppScanRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppScanRepositoryImpl @Inject constructor(
    private val packageScanner: PackageScanner,
    private val iconLoader: AppIconLoader
) : AppScanRepository {

    private val _lastResult = MutableStateFlow<List<AppInfo>>(emptyList())
    override val lastResult: StateFlow<List<AppInfo>> = _lastResult.asStateFlow()

    private val _lastSummary = MutableStateFlow<ScanSummary?>(null)
    override val lastSummary: StateFlow<ScanSummary?> = _lastSummary.asStateFlow()

    private val scanMutex = Mutex()

    override fun scan(): Flow<ScanEvent> = flow {
        val startTime = System.currentTimeMillis()

        val packageNames = packageScanner.getInstalledPackageNames()
        val total = packageNames.size

        if (total == 0) {
            val emptySummary = ScanSummary(0, 0, 0, 0, 0, 0, 0, 0)
            _lastResult.value = emptyList()
            _lastSummary.value = emptySummary
            emit(ScanEvent.Completed(emptyList(), emptySummary))
            return@flow
        }

        // Emit initial progress
        emit(ScanEvent.Progress(ScanProgress(scanned = 0, total = total)))

        val scannedApps = ArrayList<AppInfo>(total)

        // Live counters (counting each app once)
        var highCount = 0
        var mediumCount = 0
        var lowCount = 0
        var scanned = 0

        // Summary breakdown counters
        var userHigh = 0
        var userMedium = 0
        var userLow = 0
        var userTotal = 0

        var systemHigh = 0
        var systemMedium = 0
        var systemLow = 0
        var systemTotal = 0

        // 2. Sequential scanning: strictly one app at a time
        for (pkg in packageNames) {
            currentCoroutineContext().ensureActive()

            val appInfo = try {
                packageScanner.scanPackage(pkg)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                null
            }

            scanned++

            if (appInfo != null) {
                scannedApps.add(appInfo)

                when (appInfo.overallRisk) {
                    RiskLevel.HIGH -> highCount++
                    RiskLevel.MEDIUM -> mediumCount++
                    RiskLevel.LOW, RiskLevel.NONE -> lowCount++
                }

                if (appInfo.isSystemApp) {
                    systemTotal++
                    when (appInfo.overallRisk) {
                        RiskLevel.HIGH -> systemHigh++
                        RiskLevel.MEDIUM -> systemMedium++
                        RiskLevel.LOW, RiskLevel.NONE -> systemLow++
                    }
                } else {
                    userTotal++
                    when (appInfo.overallRisk) {
                        RiskLevel.HIGH -> userHigh++
                        RiskLevel.MEDIUM -> userMedium++
                        RiskLevel.LOW, RiskLevel.NONE -> userLow++
                    }
                }
            }

            emit(
                ScanEvent.Progress(
                    ScanProgress(
                        scanned = scanned,
                        total = total,
                        highCount = highCount,
                        mediumCount = mediumCount,
                        lowCount = lowCount
                    )
                )
            )
        }

        // Prevent UI flash if scan completed too quickly (< 400ms)
        val elapsed = System.currentTimeMillis() - startTime
        if (elapsed < 400) {
            delay(400 - elapsed)
        }

        val summary = ScanSummary(
            userHigh = userHigh,
            userMedium = userMedium,
            userLow = userLow,
            userTotal = userTotal,
            systemHigh = systemHigh,
            systemMedium = systemMedium,
            systemLow = systemLow,
            systemTotal = systemTotal
        )

        _lastResult.value = scannedApps
        _lastSummary.value = summary

        emit(ScanEvent.Completed(apps = scannedApps, summary = summary))
    }.flowOn(Dispatchers.IO)

    override suspend fun getApp(packageName: String): AppInfo? = withContext(Dispatchers.IO) {
        _lastResult.value.find { it.packageName == packageName }
            ?: packageScanner.scanPackage(packageName)
    }

    override suspend fun removeApp(packageName: String): Unit = withContext(Dispatchers.IO) {
        scanMutex.withLock {
            val currentList = _lastResult.value
            val updatedList = currentList.filterNot { it.packageName == packageName }
            _lastResult.value = updatedList
            iconLoader.evict(packageName)

            // Recalculate summary
            var userHigh = 0
            var userMedium = 0
            var userLow = 0
            var userTotal = 0

            var systemHigh = 0
            var systemMedium = 0
            var systemLow = 0
            var systemTotal = 0

            for (app in updatedList) {
                if (app.isSystemApp) {
                    systemTotal++
                    when (app.overallRisk) {
                        RiskLevel.HIGH -> systemHigh++
                        RiskLevel.MEDIUM -> systemMedium++
                        RiskLevel.LOW, RiskLevel.NONE -> systemLow++
                    }
                } else {
                    userTotal++
                    when (app.overallRisk) {
                        RiskLevel.HIGH -> userHigh++
                        RiskLevel.MEDIUM -> userMedium++
                        RiskLevel.LOW, RiskLevel.NONE -> userLow++
                    }
                }
            }

            _lastSummary.value = ScanSummary(
                userHigh = userHigh,
                userMedium = userMedium,
                userLow = userLow,
                userTotal = userTotal,
                systemHigh = systemHigh,
                systemMedium = systemMedium,
                systemLow = systemLow,
                systemTotal = systemTotal
            )
        }
    }
}
