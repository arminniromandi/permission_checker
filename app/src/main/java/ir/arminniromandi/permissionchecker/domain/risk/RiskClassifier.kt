package ir.arminniromandi.permissionchecker.domain.risk

import ir.arminniromandi.permissionchecker.domain.model.PermissionInfo
import ir.arminniromandi.permissionchecker.domain.model.RiskLevel

object RiskClassifier {

    // Permission risk scores
    const val SCORE_HIGH = 6
    const val SCORE_MEDIUM = 2

    // Overall risk thresholds
    const val HIGH_RISK_SCORE_THRESHOLD = 12
    const val MEDIUM_RISK_SCORE_THRESHOLD = 4

    /**
     * Critical runtime permissions.
     *
     * These permissions trigger immediate HIGH risk only when granted.
     */
    private val CRITICAL_RUNTIME_PERMISSIONS = setOf(
        "android.permission.READ_SMS",
        "android.permission.RECEIVE_SMS",
    )

    /**
     * Critical special or privileged permissions.
     *
     * Their effective access state must be determined by the scanning
     * layer. Do not rely solely on requestedPermissionsFlags for AppOps
     * or special-access permissions.
     */
    val CRITICAL_SPECIAL_PERMISSIONS = setOf(
        "android.permission.SYSTEM_ALERT_WINDOW",
        "android.permission.REQUEST_INSTALL_PACKAGES",
        "android.permission.MANAGE_EXTERNAL_STORAGE",
        "android.permission.PACKAGE_USAGE_STATS",
        "android.permission.WRITE_SECURE_SETTINGS",
    )

    private val ALL_CRITICAL_PERMISSIONS =
        CRITICAL_RUNTIME_PERMISSIONS + CRITICAL_SPECIAL_PERMISSIONS

    /**
     * Classifies the potential risk of a single permission.
     *
     * This method does not receive grant state. Therefore, HIGH here
     * describes the permission's risk category, not proof that the
     * application currently has the corresponding access.
     */
    fun classifyPermission(
        name: String,
        protectionLevel: Int,
    ): RiskLevel {
        if (name in ALL_CRITICAL_PERMISSIONS) {
            return RiskLevel.HIGH
        }

        return PERMISSION_RISK_MAP[name]
            ?: if (name.startsWith("android.permission.READ_MEDIA_")) {
                RiskLevel.MEDIUM
            } else {
                classifyByProtectionLevel(protectionLevel)
            }
    }

    /**
     * Fallback classification using Android protection base type and flags.
     *
     * Only the documented base-type mask and privileged flag are used.
     * DEVELOPMENT (0x20) is not treated as PRIVILEGED.
     */
    private fun classifyByProtectionLevel(
        protectionLevel: Int,
    ): RiskLevel {
        val baseProtection = protectionLevel and PROTECTION_MASK_BASE
        val isPrivileged = (protectionLevel and PROTECTION_FLAG_PRIVILEGED) != 0

        return when {
            baseProtection == PROTECTION_DANGEROUS ->
                RiskLevel.MEDIUM

            baseProtection == PROTECTION_SIGNATURE ||
                    baseProtection == PROTECTION_SIGNATURE_OR_SYSTEM ||
                    isPrivileged ->
                RiskLevel.LOW

            else ->
                RiskLevel.NONE
        }
    }

    /**
     * Calculates the overall risk of an application.
     *
     * CRITICAL runtime and special permissions immediately produce
     * HIGH only when their effective access state is granted.
     *
     * Other permissions contribute to the weighted score.
     * Ungranted permissions contribute half their base score.
     */
    fun calculateOverallRisk(
        permissions: List<PermissionInfo>,
    ): RiskLevel {
        if (permissions.isEmpty()) {
            return RiskLevel.NONE
        }

        val hasGrantedCriticalPermission = permissions.any { permission ->
            permission.isGranted && permission.name in ALL_CRITICAL_PERMISSIONS
        }

        if (hasGrantedCriticalPermission) {
            return RiskLevel.HIGH
        }

        val score = permissions.sumOf { permission ->
            val baseScore = when (permission.risk) {
                RiskLevel.HIGH -> SCORE_HIGH
                RiskLevel.MEDIUM -> SCORE_MEDIUM
                else -> 0
            }

            if (permission.isGranted) {
                baseScore
            } else {
                baseScore / 2
            }
        }

        return when {
            score >= HIGH_RISK_SCORE_THRESHOLD ->
                RiskLevel.HIGH

            score >= MEDIUM_RISK_SCORE_THRESHOLD ->
                RiskLevel.MEDIUM

            permissions.any { it.risk != RiskLevel.NONE } ->
                RiskLevel.LOW

            else ->
                RiskLevel.NONE
        }
    }

    // Android protection base types and masks.
    private const val PROTECTION_MASK_BASE = 0x0F
    private const val PROTECTION_NORMAL = 0
    private const val PROTECTION_DANGEROUS = 1
    private const val PROTECTION_SIGNATURE = 2
    private const val PROTECTION_SIGNATURE_OR_SYSTEM = 3

    // Android protection flags.
    private const val PROTECTION_FLAG_PRIVILEGED = 0x10

    private val PERMISSION_RISK_MAP: Map<String, RiskLevel> = mapOf(

        // HIGH risk permissions
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
        "android.permission.BODY_SENSORS_BACKGROUND" to RiskLevel.HIGH,
        "android.permission.READ_LOGS" to RiskLevel.HIGH,
        "android.permission.INSTALL_PACKAGES" to RiskLevel.HIGH,
        "android.permission.DELETE_PACKAGES" to RiskLevel.HIGH,
        "android.permission.CAPTURE_AUDIO_OUTPUT" to RiskLevel.HIGH,
        "android.permission.BROADCAST_SMS" to RiskLevel.HIGH,
        "android.permission.MANAGE_DEVICE_ADMINS" to RiskLevel.HIGH,

        // MEDIUM risk permissions
        "android.permission.ACCESS_FINE_LOCATION" to RiskLevel.MEDIUM,
        "android.permission.ACCESS_COARSE_LOCATION" to RiskLevel.MEDIUM,
        "android.permission.READ_MEDIA_IMAGES" to RiskLevel.MEDIUM,
        "android.permission.READ_MEDIA_VIDEO" to RiskLevel.MEDIUM,
        "android.permission.READ_MEDIA_AUDIO" to RiskLevel.MEDIUM,
        "android.permission.READ_CALENDAR" to RiskLevel.MEDIUM,
        "android.permission.WRITE_CALENDAR" to RiskLevel.MEDIUM,
        "android.permission.BLUETOOTH_CONNECT" to RiskLevel.MEDIUM,
        "android.permission.BLUETOOTH_SCAN" to RiskLevel.MEDIUM,
        "android.permission.READ_PHONE_STATE" to RiskLevel.MEDIUM,
        "android.permission.READ_PHONE_NUMBERS" to RiskLevel.MEDIUM,
        "android.permission.GET_ACCOUNTS" to RiskLevel.MEDIUM,
        "android.permission.CALL_PHONE" to RiskLevel.MEDIUM,
        "android.permission.ANSWER_PHONE_CALLS" to RiskLevel.MEDIUM,
        "android.permission.BODY_SENSORS" to RiskLevel.MEDIUM,
        "android.permission.ACTIVITY_RECOGNITION" to RiskLevel.MEDIUM,
        "android.permission.NEARBY_WIFI_DEVICES" to RiskLevel.MEDIUM,
        "android.permission.QUERY_ALL_PACKAGES" to RiskLevel.MEDIUM,
        "android.permission.ACCESS_MEDIA_LOCATION" to RiskLevel.MEDIUM,
        "android.permission.READ_CELL_BROADCASTS" to RiskLevel.MEDIUM,
        "android.permission.WRITE_SETTINGS" to RiskLevel.MEDIUM,
        "android.permission.REQUEST_DELETE_PACKAGES" to RiskLevel.MEDIUM,
        "android.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION" to RiskLevel.MEDIUM,

        // LOW risk permissions
        "android.permission.POST_NOTIFICATIONS" to RiskLevel.LOW,
        "android.permission.READ_EXTERNAL_STORAGE" to RiskLevel.LOW,
        "android.permission.WRITE_EXTERNAL_STORAGE" to RiskLevel.LOW,
        "android.permission.READ_MEDIA_VISUAL_USER_SELECTED" to RiskLevel.LOW,
        "android.permission.SCHEDULE_EXACT_ALARM" to RiskLevel.LOW,
        "android.permission.USE_FULL_SCREEN_INTENT" to RiskLevel.LOW,
        "android.permission.BLUETOOTH_ADVERTISE" to RiskLevel.LOW,
        "android.permission.ADD_VOICEMAIL" to RiskLevel.LOW,
        "android.permission.USE_SIP" to RiskLevel.LOW,
        "android.permission.RECEIVE_BOOT_COMPLETED" to RiskLevel.LOW,
        "android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" to RiskLevel.LOW,
        "android.permission.USE_BIOMETRIC" to RiskLevel.LOW,
        "android.permission.ACCESS_WIFI_STATE" to RiskLevel.LOW,
        "android.permission.CHANGE_WIFI_STATE" to RiskLevel.LOW,
        "android.permission.DISABLE_KEYGUARD" to RiskLevel.LOW,
        "android.permission.FOREGROUND_SERVICE_CAMERA" to RiskLevel.LOW,
        "android.permission.FOREGROUND_SERVICE_MICROPHONE" to RiskLevel.LOW
    )
}
