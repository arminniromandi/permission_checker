package ir.arminniromandi.permissionchecker.domain.risk

import ir.arminniromandi.permissionchecker.domain.model.PermissionInfo
import ir.arminniromandi.permissionchecker.domain.model.RiskLevel
import org.junit.Assert.assertEquals
import org.junit.Test

class RiskClassifierTest {

    @Test
    fun testClassifyPermission_knownHighPermissions() {
        assertEquals(
            RiskLevel.HIGH,
            RiskClassifier.classifyPermission("android.permission.CAMERA", 0)
        )
        assertEquals(
            RiskLevel.HIGH,
            RiskClassifier.classifyPermission("android.permission.READ_SMS", 0)
        )
        assertEquals(
            RiskLevel.HIGH,
            RiskClassifier.classifyPermission("android.permission.RECORD_AUDIO", 0)
        )
        assertEquals(
            RiskLevel.HIGH,
            RiskClassifier.classifyPermission("android.permission.BIND_ACCESSIBILITY_SERVICE", 0)
        )
    }

    @Test
    fun testClassifyPermission_knownMediumPermissions() {
        assertEquals(
            RiskLevel.MEDIUM,
            RiskClassifier.classifyPermission("android.permission.ACCESS_FINE_LOCATION", 0)
        )
        assertEquals(
            RiskLevel.MEDIUM,
            RiskClassifier.classifyPermission("android.permission.POST_NOTIFICATIONS", 0)
        )
        assertEquals(
            RiskLevel.MEDIUM,
            RiskClassifier.classifyPermission("android.permission.BLUETOOTH_CONNECT", 0)
        )
        assertEquals(
            RiskLevel.MEDIUM,
            RiskClassifier.classifyPermission("android.permission.READ_MEDIA_CUSTOM_TYPE", 0)
        )
    }

    @Test
    fun testClassifyPermission_fallbackByProtectionLevel() {
        // Unknown dangerous permission
        assertEquals(
            RiskLevel.MEDIUM,
            RiskClassifier.classifyPermission("com.custom.UNKNOWN_DANGEROUS", RiskClassifier.PROTECTION_DANGEROUS)
        )
        // Unknown signature permission
        assertEquals(
            RiskLevel.LOW,
            RiskClassifier.classifyPermission("com.custom.UNKNOWN_SIGNATURE", RiskClassifier.PROTECTION_SIGNATURE)
        )
        // Unknown normal permission
        assertEquals(
            RiskLevel.NONE,
            RiskClassifier.classifyPermission("com.custom.UNKNOWN_NORMAL", RiskClassifier.PROTECTION_NORMAL)
        )
    }

    @Test
    fun testCalculateOverallRisk_emptyPermissions_returnsNone() {
        val result = RiskClassifier.calculateOverallRisk(emptyList())
        assertEquals(RiskLevel.NONE, result)
    }

    @Test
    fun testCalculateOverallRisk_onlyNonePermissions_returnsNone() {
        val perms = listOf(
            createPerm("android.permission.INTERNET", RiskLevel.NONE),
            createPerm("android.permission.ACCESS_NETWORK_STATE", RiskLevel.NONE)
        )
        assertEquals(RiskLevel.NONE, RiskClassifier.calculateOverallRisk(perms))
    }

    @Test
    fun testCalculateOverallRisk_singleHighPermission_returnsMedium() {
        // As per project rule: 1 single HIGH does not make the app HIGH, but MEDIUM
        val perms = listOf(
            createPerm("android.permission.CAMERA", RiskLevel.HIGH)
        )
        assertEquals(RiskLevel.MEDIUM, RiskClassifier.calculateOverallRisk(perms))
    }

    @Test
    fun testCalculateOverallRisk_twoHighPermissions_score10_returnsMedium() {
        val perms = listOf(
            createPerm("android.permission.CAMERA", RiskLevel.HIGH),
            createPerm("android.permission.RECORD_AUDIO", RiskLevel.HIGH)
        )
        assertEquals(RiskLevel.MEDIUM, RiskClassifier.calculateOverallRisk(perms))
    }

    @Test
    fun testCalculateOverallRisk_threeHighPermissions_thresholdReached_returnsHigh() {
        val perms = listOf(
            createPerm("android.permission.CAMERA", RiskLevel.HIGH),
            createPerm("android.permission.RECORD_AUDIO", RiskLevel.HIGH),
            createPerm("android.permission.READ_SMS", RiskLevel.HIGH)
        )
        assertEquals(RiskLevel.HIGH, RiskClassifier.calculateOverallRisk(perms))
    }

    @Test
    fun testCalculateOverallRisk_scoreThreshold12Reached_returnsHigh() {
        // 2 HIGH (10) + 1 MEDIUM (2) = 12 -> HIGH
        val perms = listOf(
            createPerm("android.permission.CAMERA", RiskLevel.HIGH),
            createPerm("android.permission.RECORD_AUDIO", RiskLevel.HIGH),
            createPerm("android.permission.ACCESS_FINE_LOCATION", RiskLevel.MEDIUM)
        )
        assertEquals(RiskLevel.HIGH, RiskClassifier.calculateOverallRisk(perms))
    }

    @Test
    fun testCalculateOverallRisk_onlyLowPermissions_returnsLow() {
        val perms = listOf(
            createPerm("android.permission.VIBRATE", RiskLevel.LOW),
            createPerm("android.permission.WAKE_LOCK", RiskLevel.LOW)
        )
        assertEquals(RiskLevel.LOW, RiskClassifier.calculateOverallRisk(perms))
    }

    private fun createPerm(name: String, risk: RiskLevel): PermissionInfo {
        return PermissionInfo(
            name = name,
            label = name,
            description = null,
            risk = risk,
            isGranted = true
        )
    }
}
