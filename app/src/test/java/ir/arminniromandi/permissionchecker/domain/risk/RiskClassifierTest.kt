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
            RiskClassifier.classifyPermission("android.permission.SYSTEM_ALERT_WINDOW", 0)
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
            RiskClassifier.classifyPermission("android.permission.BLUETOOTH_CONNECT", 0)
        )
        assertEquals(
            RiskLevel.MEDIUM,
            RiskClassifier.classifyPermission("android.permission.READ_CALENDAR", 0)
        )
        assertEquals(
            RiskLevel.MEDIUM,
            RiskClassifier.classifyPermission("android.permission.READ_MEDIA_CUSTOM_TYPE", 0)
        )
    }

    @Test
    fun testClassifyPermission_knownLowPermissions() {
        assertEquals(
            RiskLevel.LOW,
            RiskClassifier.classifyPermission("android.permission.POST_NOTIFICATIONS", 0)
        )
        assertEquals(
            RiskLevel.LOW,
            RiskClassifier.classifyPermission("android.permission.READ_EXTERNAL_STORAGE", 0)
        )
    }

    @Test
    fun testClassifyPermission_fallbackByProtectionLevel() {
        // Unknown dangerous permission (protectionLevel = 1)
        assertEquals(
            RiskLevel.MEDIUM,
            RiskClassifier.classifyPermission("com.custom.UNKNOWN_DANGEROUS", 1)
        )
        // Unknown signature permission (protectionLevel = 2)
        assertEquals(
            RiskLevel.LOW,
            RiskClassifier.classifyPermission("com.custom.UNKNOWN_SIGNATURE", 2)
        )
        // Unknown normal permission (protectionLevel = 0)
        assertEquals(
            RiskLevel.NONE,
            RiskClassifier.classifyPermission("com.custom.UNKNOWN_NORMAL", 0)
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
    fun testCalculateOverallRisk_grantedCriticalPermission_immediatelyHigh() {
        // Granted critical runtime permission triggers immediate HIGH
        val perms = listOf(
            createPerm("android.permission.READ_SMS", RiskLevel.HIGH, isGranted = true)
        )
        assertEquals(RiskLevel.HIGH, RiskClassifier.calculateOverallRisk(perms))
    }

    @Test
    fun testCalculateOverallRisk_ungrantedCriticalPermission_doesNotTriggerImmediateHigh() {
        // Ungranted critical permission: baseScore/2 = 6/2 = 3 < MEDIUM_RISK_SCORE_THRESHOLD(4) -> LOW
        val perms = listOf(
            createPerm("android.permission.READ_SMS", RiskLevel.HIGH, isGranted = false)
        )
        assertEquals(RiskLevel.LOW, RiskClassifier.calculateOverallRisk(perms))
    }

    @Test
    fun testCalculateOverallRisk_singleHighNonCriticalPermission_returnsMedium() {
        // CAMERA is non-critical HIGH, granted score = 6 >= MEDIUM_RISK_SCORE_THRESHOLD(4) -> MEDIUM
        val perms = listOf(
            createPerm("android.permission.CAMERA", RiskLevel.HIGH, isGranted = true)
        )
        assertEquals(RiskLevel.MEDIUM, RiskClassifier.calculateOverallRisk(perms))
    }

    @Test
    fun testCalculateOverallRisk_twoHighNonCriticalPermissions_score12_returnsHigh() {
        // 2 granted non-critical HIGH: 6 + 6 = 12 >= HIGH_RISK_SCORE_THRESHOLD(12) -> HIGH
        val perms = listOf(
            createPerm("android.permission.CAMERA", RiskLevel.HIGH, isGranted = true),
            createPerm("android.permission.RECORD_AUDIO", RiskLevel.HIGH, isGranted = true)
        )
        assertEquals(RiskLevel.HIGH, RiskClassifier.calculateOverallRisk(perms))
    }

    @Test
    fun testCalculateOverallRisk_mediumPermissions_reachThreshold4_returnsMedium() {
        // 2 granted MEDIUM: 2 + 2 = 4 >= MEDIUM_RISK_SCORE_THRESHOLD(4) -> MEDIUM
        val perms = listOf(
            createPerm("android.permission.ACCESS_FINE_LOCATION", RiskLevel.MEDIUM, isGranted = true),
            createPerm("android.permission.BLUETOOTH_CONNECT", RiskLevel.MEDIUM, isGranted = true)
        )
        assertEquals(RiskLevel.MEDIUM, RiskClassifier.calculateOverallRisk(perms))
    }

    @Test
    fun testCalculateOverallRisk_onlyLowPermissions_returnsLow() {
        val perms = listOf(
            createPerm("android.permission.VIBRATE", RiskLevel.LOW),
            createPerm("android.permission.WAKE_LOCK", RiskLevel.LOW)
        )
        assertEquals(RiskLevel.LOW, RiskClassifier.calculateOverallRisk(perms))
    }

    private fun createPerm(
        name: String,
        risk: RiskLevel,
        isGranted: Boolean = true
    ): PermissionInfo {
        return PermissionInfo(
            name = name,
            label = name,
            description = null,
            risk = risk,
            isGranted = isGranted
        )
    }
}
