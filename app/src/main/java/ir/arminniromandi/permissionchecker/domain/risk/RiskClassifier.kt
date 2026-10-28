package ir.arminniromandi.permissionchecker.domain.risk

import ir.arminniromandi.permissionchecker.domain.model.PermissionInfo
import ir.arminniromandi.permissionchecker.domain.model.RiskLevel

object RiskClassifier {

    const val HIGH_RISK_COUNT_THRESHOLD = 3
    const val HIGH_RISK_SCORE_THRESHOLD = 12

    const val SCORE_HIGH = 5
    const val SCORE_MEDIUM = 2
    const val SCORE_LOW = 1
    const val SCORE_NONE = 0

    // Standard Android protection base types (equivalent to PermissionInfo.PROTECTION_*)
    const val PROTECTION_NORMAL = 0
    const val PROTECTION_DANGEROUS = 1
    const val PROTECTION_SIGNATURE = 2

    private val PERMISSION_RISK_MAP: Map<String, RiskLevel> = mapOf(
        // --- HIGH RISK PERMISSIONS ---
        "android.permission.READ_SMS" to RiskLevel.HIGH,
        "android.permission.RECEIVE_SMS" to RiskLevel.HIGH,
        "android.permission.SEND_SMS" to RiskLevel.HIGH,
        "android.permission.RECEIVE_MMS" to RiskLevel.HIGH,
        "android.permission.RECEIVE_WAP_PUSH" to RiskLevel.HIGH,
        "android.permission.READ_CALL_LOG" to RiskLevel.HIGH,
        "android.permission.WRITE_CALL_LOG" to RiskLevel.HIGH,
        "android.permission.PROCESS_OUTGOING_CALLS" to RiskLevel.HIGH,
        "android.permission.RECORD_AUDIO" to RiskLevel.HIGH,
        "android.permission.CAMERA" to RiskLevel.HIGH,
        "android.permission.ACCESS_BACKGROUND_LOCATION" to RiskLevel.HIGH,
        "android.permission.READ_CONTACTS" to RiskLevel.HIGH,
        "android.permission.WRITE_CONTACTS" to RiskLevel.HIGH,
        "android.permission.GET_ACCOUNTS" to RiskLevel.HIGH,
        "android.permission.BIND_ACCESSIBILITY_SERVICE" to RiskLevel.HIGH,
        "android.permission.SYSTEM_ALERT_WINDOW" to RiskLevel.HIGH,
        "android.permission.REQUEST_INSTALL_PACKAGES" to RiskLevel.HIGH,
        "android.permission.MANAGE_EXTERNAL_STORAGE" to RiskLevel.HIGH,
        "android.permission.BIND_DEVICE_ADMIN" to RiskLevel.HIGH,
        "android.permission.READ_PHONE_NUMBERS" to RiskLevel.HIGH,
        "android.permission.USE_SIP" to RiskLevel.HIGH,
        "android.permission.BODY_SENSORS_BACKGROUND" to RiskLevel.HIGH,

        // --- MEDIUM RISK PERMISSIONS ---
        "android.permission.ACCESS_FINE_LOCATION" to RiskLevel.MEDIUM,
        "android.permission.ACCESS_COARSE_LOCATION" to RiskLevel.MEDIUM,
        "android.permission.READ_EXTERNAL_STORAGE" to RiskLevel.MEDIUM,
        "android.permission.WRITE_EXTERNAL_STORAGE" to RiskLevel.MEDIUM,
        "android.permission.READ_MEDIA_IMAGES" to RiskLevel.MEDIUM,
        "android.permission.READ_MEDIA_VIDEO" to RiskLevel.MEDIUM,
        "android.permission.READ_MEDIA_AUDIO" to RiskLevel.MEDIUM,
        "android.permission.READ_MEDIA_VISUAL_USER_SELECTED" to RiskLevel.MEDIUM,
        "android.permission.READ_CALENDAR" to RiskLevel.MEDIUM,
        "android.permission.WRITE_CALENDAR" to RiskLevel.MEDIUM,
        "android.permission.BLUETOOTH_CONNECT" to RiskLevel.MEDIUM,
        "android.permission.BLUETOOTH_SCAN" to RiskLevel.MEDIUM,
        "android.permission.BLUETOOTH_ADVERTISE" to RiskLevel.MEDIUM,
        "android.permission.POST_NOTIFICATIONS" to RiskLevel.MEDIUM,
        "android.permission.READ_PHONE_STATE" to RiskLevel.MEDIUM,
        "android.permission.CALL_PHONE" to RiskLevel.MEDIUM,
        "android.permission.ADD_VOICEMAIL" to RiskLevel.MEDIUM,
        "android.permission.BODY_SENSORS" to RiskLevel.MEDIUM,
        "android.permission.ACTIVITY_RECOGNITION" to RiskLevel.MEDIUM,
        "android.permission.NEARBY_WIFI_DEVICES" to RiskLevel.MEDIUM,
        "android.permission.SCHEDULE_EXACT_ALARM" to RiskLevel.MEDIUM,
        "android.permission.USE_FULL_SCREEN_INTENT" to RiskLevel.MEDIUM
    )

    fun classifyPermission(name: String, protectionLevel: Int): RiskLevel {
        val mapped = PERMISSION_RISK_MAP[name]
        if (mapped != null) {
            return mapped
        }

        // Check if name matches READ_MEDIA_* prefix
        if (name.startsWith("android.permission.READ_MEDIA_")) {
            return RiskLevel.MEDIUM
        }

        // Fallback by protection level base type (lower 4 bits)
        val baseProtection = protectionLevel and 0x0F
        return when (baseProtection) {
            PROTECTION_DANGEROUS -> RiskLevel.MEDIUM
            PROTECTION_SIGNATURE -> RiskLevel.LOW
            PROTECTION_NORMAL -> RiskLevel.NONE
            else -> {
                // If flagged with privileged or signature bits
                if ((protectionLevel and 0x10) != 0 || (protectionLevel and 0x20) != 0) {
                    RiskLevel.LOW
                } else {
                    RiskLevel.NONE
                }
            }
        }
    }

    fun calculateOverallRisk(permissions: List<PermissionInfo>): RiskLevel {
        if (permissions.isEmpty()) return RiskLevel.NONE

        var highCount = 0
        var mediumCount = 0
        var lowCount = 0
        var totalScore = 0

        for (permission in permissions) {
            when (permission.risk) {
                RiskLevel.HIGH -> {
                    highCount++
                    totalScore += SCORE_HIGH
                }
                RiskLevel.MEDIUM -> {
                    mediumCount++
                    totalScore += SCORE_MEDIUM
                }
                RiskLevel.LOW -> {
                    lowCount++
                    totalScore += SCORE_LOW
                }
                RiskLevel.NONE -> {
                    totalScore += SCORE_NONE
                }
            }
        }

        return when {
            highCount >= HIGH_RISK_COUNT_THRESHOLD || totalScore >= HIGH_RISK_SCORE_THRESHOLD -> RiskLevel.HIGH
            highCount >= 1 || mediumCount >= 1 -> RiskLevel.MEDIUM
            lowCount >= 1 -> RiskLevel.LOW
            else -> RiskLevel.NONE
        }
    }
}
