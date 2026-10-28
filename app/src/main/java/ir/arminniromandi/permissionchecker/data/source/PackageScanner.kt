package ir.arminniromandi.permissionchecker.data.source

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import ir.arminniromandi.permissionchecker.domain.model.AppInfo
import ir.arminniromandi.permissionchecker.domain.model.PermissionInfo
import ir.arminniromandi.permissionchecker.domain.risk.RiskClassifier
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PackageScanner @Inject constructor(
    private val packageManager: PackageManager
) {

    //گرفتن برنامه های نصب شده صرفا
    fun getInstalledPackageNames(): List<String> {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                packageManager.getInstalledApplications(0)
            }.map { it.packageName }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /*
    * شروغ اسکن و دریافت مجوز ها و دسته بندی بر اساس ریسک
    * سپس  برگرداندن به صورت
    * */
    fun scanPackage(packageName: String): AppInfo? {
        val packageInfo = getPackageInfoSafe(packageName) ?: return null
        val appInfo = packageInfo.applicationInfo ?: return null

        val appName = try {
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (_: Exception) {
            packageName
        }

        val isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0) &&
                (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP == 0)

        val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            packageInfo.versionCode.toLong()
        }

        val permissions = extractPermissions(packageInfo)
        val overallRisk = RiskClassifier.calculateOverallRisk(permissions)

        return AppInfo(
            packageName = packageName,
            appName = appName,
            versionName = packageInfo.versionName,
            versionCode = versionCode,
            firstInstallTime = packageInfo.firstInstallTime,
            lastUpdateTime = packageInfo.lastUpdateTime,
            isSystemApp = isSystemApp,
            permissions = permissions,
            overallRisk = overallRisk
        )
    }

    private fun extractPermissions(packageInfo: PackageInfo): List<PermissionInfo> {
        val requestedPermissions = packageInfo.requestedPermissions ?: return emptyList()
        val flags = packageInfo.requestedPermissionsFlags ?: IntArray(0)

        val result = ArrayList<PermissionInfo>(requestedPermissions.size)

        for (i in requestedPermissions.indices) {
            val permName = requestedPermissions[i]
            val isGranted = if (i < flags.size) {
                (flags[i] and PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0
            } else {
                false
            }

            val (label, description, protectionLevel) = getPermissionDetails(permName)
            val risk = RiskClassifier.classifyPermission(permName, protectionLevel)

            result.add(
                PermissionInfo(
                    name = permName,
                    label = label,
                    description = description,
                    risk = risk,
                    isGranted = isGranted
                )
            )
        }

        return result.sortedBy { it.risk.ordinal }
    }

    private fun getPermissionDetails(permName: String): Triple<String?, String?, Int> {
        return try {
            val permInfo = packageManager.getPermissionInfo(permName, 0)
            val label = permInfo.loadLabel(packageManager)?.toString()
            val description = permInfo.loadDescription(packageManager)?.toString()
            val protection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                permInfo.protection
            } else {
                @Suppress("DEPRECATION")
                permInfo.protectionLevel
            }
            Triple(label, description, protection)
        } catch (_: PackageManager.NameNotFoundException) {
            Triple(null, null, RiskClassifier.PROTECTION_NORMAL)
        } catch (_: Exception) {
            Triple(null, null, RiskClassifier.PROTECTION_NORMAL)
        }
    }

    private fun getPackageInfoSafe(packageName: String): PackageInfo? {
        val flags = PackageManager.GET_PERMISSIONS.toLong()
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(flags))
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, flags.toInt())
            }
        } catch (_: PackageManager.NameNotFoundException) {
            null
        } catch (_: Exception) {
            null
        }
    }
}
