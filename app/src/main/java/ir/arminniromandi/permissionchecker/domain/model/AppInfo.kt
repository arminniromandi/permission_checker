package ir.arminniromandi.permissionchecker.domain.model

data class AppInfo(
    val packageName: String,
    val appName: String,
    val versionName: String?,
    val versionCode: Long,
    val firstInstallTime: Long,
    val lastUpdateTime: Long,
    val isSystemApp: Boolean,
    val permissions: List<PermissionInfo>,
    val overallRisk: RiskLevel
)
