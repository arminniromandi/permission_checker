package ir.arminniromandi.permissionchecker.domain.risk

import ir.arminniromandi.permissionchecker.domain.model.PermissionInfo
import ir.arminniromandi.permissionchecker.domain.model.RiskLevel

object RiskClassifier {

    const val SCORE_HIGH = 6
    const val SCORE_MEDIUM = 2

    const val HIGH_RISK_SCORE_THRESHOLD = 12
    const val MEDIUM_RISK_SCORE_THRESHOLD = 4
    private const val PROTECTION_MASK_BASE = 0x0F
    private const val PROTECTION_NORMAL = 0
    private const val PROTECTION_DANGEROUS = 1
    private const val PROTECTION_SIGNATURE = 2
    private const val PROTECTION_SIGNATURE_OR_SYSTEM = 3

    private const val PROTECTION_FLAG_PRIVILEGED = 0x10



    private val PERMISSION_RISK_MAP: Map<String, RiskLevel> = mapOf(


        // HIGH RISK
        // Sensitive personal data, private media, tracking,


        // SMS and messaging
        "android.permission.READ_SMS" to RiskLevel.HIGH,
        "android.permission.RECEIVE_SMS" to RiskLevel.HIGH,
        "android.permission.SEND_SMS" to RiskLevel.HIGH,
        "android.permission.RECEIVE_MMS" to RiskLevel.HIGH,
        "android.permission.RECEIVE_WAP_PUSH" to RiskLevel.HIGH,

        // Call history and call control
        "android.permission.READ_CALL_LOG" to RiskLevel.HIGH,
        "android.permission.WRITE_CALL_LOG" to RiskLevel.HIGH,
        "android.permission.PROCESS_OUTGOING_CALLS" to RiskLevel.HIGH,
        "android.permission.CALL_PHONE" to RiskLevel.HIGH,
        "android.permission.ANSWER_PHONE_CALLS" to RiskLevel.HIGH,

        // Camera and microphone
        "android.permission.CAMERA" to RiskLevel.HIGH,
        "android.permission.RECORD_AUDIO" to RiskLevel.HIGH,

        // Precise and background location
        "android.permission.ACCESS_FINE_LOCATION" to RiskLevel.HIGH,
        "android.permission.ACCESS_BACKGROUND_LOCATION" to RiskLevel.HIGH,

        // Location metadata embedded in photos and other media
        "android.permission.ACCESS_MEDIA_LOCATION" to RiskLevel.HIGH,

        // Contacts
        "android.permission.READ_CONTACTS" to RiskLevel.HIGH,
        "android.permission.WRITE_CONTACTS" to RiskLevel.HIGH,

        // Health and body sensor data
        "android.permission.BODY_SENSORS" to RiskLevel.HIGH,
        "android.permission.BODY_SENSORS_BACKGROUND" to RiskLevel.HIGH,

        // Physical activity can reveal behavioral patterns
        "android.permission.ACTIVITY_RECOGNITION" to RiskLevel.HIGH,

        // Personal calendars and schedules
        "android.permission.READ_CALENDAR" to RiskLevel.HIGH,

        // Broad access to personal media
        "android.permission.READ_MEDIA_IMAGES" to RiskLevel.HIGH,
        "android.permission.READ_MEDIA_VIDEO" to RiskLevel.HIGH,
        "android.permission.READ_MEDIA_AUDIO" to RiskLevel.HIGH,

        // Legacy storage access.
        // Actual access depends on Android version and targetSdkVersion.
        "android.permission.READ_EXTERNAL_STORAGE" to RiskLevel.HIGH,
        "android.permission.WRITE_EXTERNAL_STORAGE" to RiskLevel.HIGH,

        // Discovering installed apps can expose a user's private profile
        "android.permission.QUERY_ALL_PACKAGES" to RiskLevel.HIGH,

        // Special access with significant privacy or security impact
        "android.permission.SYSTEM_ALERT_WINDOW" to RiskLevel.HIGH,
        "android.permission.REQUEST_INSTALL_PACKAGES" to RiskLevel.HIGH,
        "android.permission.MANAGE_EXTERNAL_STORAGE" to RiskLevel.HIGH,
        "android.permission.PACKAGE_USAGE_STATS" to RiskLevel.HIGH,
        "android.permission.WRITE_SECURE_SETTINGS" to RiskLevel.HIGH,
        "android.permission.MANAGE_MEDIA" to RiskLevel.HIGH,

        // Powerful platform-level capabilities.
        // See PLATFORM_OR_RESTRICTED_PERMISSIONS below.
        "android.permission.READ_LOGS" to RiskLevel.HIGH,
        "android.permission.INSTALL_PACKAGES" to RiskLevel.HIGH,
        "android.permission.DELETE_PACKAGES" to RiskLevel.HIGH,
        "android.permission.CAPTURE_AUDIO_OUTPUT" to RiskLevel.HIGH,
        "android.permission.BROADCAST_SMS" to RiskLevel.HIGH,

        // Voicemail data; these use the actual Android permission names
        "com.android.voicemail.permission.READ_VOICEMAIL" to RiskLevel.HIGH,
        "com.android.voicemail.permission.WRITE_VOICEMAIL" to RiskLevel.HIGH,



        // MEDIUM RISK
        // Personal identifiers, limited location, nearby devices,
        // data modification, or capabilities with narrower scope.


        // Approximate location
        "android.permission.ACCESS_COARSE_LOCATION" to RiskLevel.MEDIUM,

        // Phone information
        "android.permission.READ_PHONE_STATE" to RiskLevel.MEDIUM,
        "android.permission.READ_PHONE_NUMBERS" to RiskLevel.MEDIUM,

        // Account identifiers
        "android.permission.GET_ACCOUNTS" to RiskLevel.MEDIUM,

        // Calendar modification
        "android.permission.WRITE_CALENDAR" to RiskLevel.MEDIUM,

        // Bluetooth
        "android.permission.BLUETOOTH_CONNECT" to RiskLevel.MEDIUM,
        "android.permission.BLUETOOTH_SCAN" to RiskLevel.MEDIUM,

        // Nearby Wi-Fi devices
        "android.permission.NEARBY_WIFI_DEVICES" to RiskLevel.MEDIUM,

        // User-selected photos or videos only
        "android.permission.READ_MEDIA_VISUAL_USER_SELECTED" to RiskLevel.MEDIUM,

        // Legacy/system-restricted permission
        "android.permission.READ_CELL_BROADCASTS" to RiskLevel.MEDIUM,

        // Special settings access
        "android.permission.WRITE_SETTINGS" to RiskLevel.MEDIUM,

        // Requests removal; not the same as silently uninstalling
        "android.permission.REQUEST_DELETE_PACKAGES" to RiskLevel.MEDIUM,

        // The permission alone does not capture the screen
        "android.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION" to RiskLevel.MEDIUM,

        // Voicemail creation
        "com.android.voicemail.permission.ADD_VOICEMAIL" to RiskLevel.MEDIUM,

        // Internet telephony
        "android.permission.USE_SIP" to RiskLevel.MEDIUM,



        // LOW RISK
        // Permissions that do not, by themselves, expose
        //در خیلی از غاپلیکیشن ها هم پیدا میشه و داده های حساسی را واکشی نمیکند یا نمینویسد


        // Notifications
        "android.permission.POST_NOTIFICATIONS" to RiskLevel.LOW,

        // Exact alarm scheduling
        "android.permission.SCHEDULE_EXACT_ALARM" to RiskLevel.LOW,

        // Full-screen notification behavior
        "android.permission.USE_FULL_SCREEN_INTENT" to RiskLevel.LOW,

        // Bluetooth advertising
        "android.permission.BLUETOOTH_ADVERTISE" to RiskLevel.LOW,

        // Boot receiver
        "android.permission.RECEIVE_BOOT_COMPLETED" to RiskLevel.LOW,

        // Battery optimization exemption request
        "android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" to RiskLevel.LOW,

        // Biometric authentication API; not raw biometric data access
        "android.permission.USE_BIOMETRIC" to RiskLevel.LOW,

        // Wi-Fi status and configuration
        "android.permission.ACCESS_WIFI_STATE" to RiskLevel.LOW,
        "android.permission.CHANGE_WIFI_STATE" to RiskLevel.LOW,

        // Keyguard behavior, subject to system restrictions
        "android.permission.DISABLE_KEYGUARD" to RiskLevel.LOW,

        // Foreground-service type declarations.
        // These do not independently grant camera or microphone access.
        "android.permission.FOREGROUND_SERVICE_CAMERA" to RiskLevel.LOW,
        "android.permission.FOREGROUND_SERVICE_MICROPHONE" to RiskLevel.LOW,

        // This permission does not independently grant screen capture
        "android.permission.ACCESS_NOTIFICATION_POLICY" to RiskLevel.LOW
    )


    /**
     * مجوز هایی است که بهرته بررسی شوند که ایا کاربر اجازه دسترسی داده یا نه
     * بیشترشان در اندروید api های جدید از کاربر گرفته میشود
     */
    private val CRITICAL_RUNTIME_PERMISSIONS: Set<String> = setOf(

        // SMS
        "android.permission.READ_SMS",
        "android.permission.RECEIVE_SMS",
        "android.permission.SEND_SMS",
        "android.permission.RECEIVE_MMS",
        "android.permission.RECEIVE_WAP_PUSH",

        // Calls
        "android.permission.READ_CALL_LOG",
        "android.permission.WRITE_CALL_LOG",
        "android.permission.PROCESS_OUTGOING_CALLS",
        "android.permission.CALL_PHONE",
        "android.permission.ANSWER_PHONE_CALLS",

        // Private conversations and identity
        "android.permission.READ_CONTACTS",
        "android.permission.WRITE_CONTACTS",

        // Location
        "android.permission.ACCESS_FINE_LOCATION",
        "android.permission.ACCESS_BACKGROUND_LOCATION",
        "android.permission.ACCESS_MEDIA_LOCATION",

        // Camera and microphone
        "android.permission.CAMERA",
        "android.permission.RECORD_AUDIO",

        // Personal media
        "android.permission.READ_MEDIA_IMAGES",
        "android.permission.READ_MEDIA_VIDEO",
        "android.permission.READ_MEDIA_AUDIO",

        // Calendar
        "android.permission.READ_CALENDAR",

        // Health and activity
        "android.permission.BODY_SENSORS",
        "android.permission.BODY_SENSORS_BACKGROUND",
        "android.permission.ACTIVITY_RECOGNITION",

        // Legacy storage permissions
        "android.permission.READ_EXTERNAL_STORAGE",
        "android.permission.WRITE_EXTERNAL_STORAGE"
    )

    val CRITICAL_SPECIAL_PERMISSIONS: Set<String> = setOf(

        // Display over other apps
        "android.permission.SYSTEM_ALERT_WINDOW",

        // Request installation of packages
        "android.permission.REQUEST_INSTALL_PACKAGES",

        // Broad file access
        "android.permission.MANAGE_EXTERNAL_STORAGE",

        // App usage history
        "android.permission.PACKAGE_USAGE_STATS",

        // Modify system settings
        "android.permission.WRITE_SETTINGS",

        // Exact alarms
        "android.permission.SCHEDULE_EXACT_ALARM",

        // Notification policy / Do Not Disturb
        "android.permission.ACCESS_NOTIFICATION_POLICY",

        // Modify or delete media without the usual confirmation flow
        "android.permission.MANAGE_MEDIA",

        // Exemption from battery optimization
        "android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS"
    )





    private val ALL_CRITICAL_PERMISSIONS =
        CRITICAL_RUNTIME_PERMISSIONS + CRITICAL_SPECIAL_PERMISSIONS




    fun classifyPermission(
        name: String,
        protectionLevel: Int,
    ): RiskLevel {

        // 1. Critical permissions always receive HIGH risk.
        if (name in ALL_CRITICAL_PERMISSIONS) {
            return RiskLevel.HIGH
        }

        // 2. Prefer explicit rules over heuristics.
        PERMISSION_RISK_MAP[name]?.let { riskLevel ->
            return riskLevel
        }

        // 4. Unknown media-reading permissions are treated conservatively.
        if (name.startsWith("android.permission.READ_MEDIA_")) {
            return RiskLevel.HIGH
        }

        // 5. Fall back to Android's protection level.
        return classifyByProtectionLevel(protectionLevel)
    }


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


}
