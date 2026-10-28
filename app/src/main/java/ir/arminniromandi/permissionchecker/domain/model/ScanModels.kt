package ir.arminniromandi.permissionchecker.domain.model

data class ScanProgress(
    val scanned: Int,
    val total: Int,
    val highCount: Int = 0,
    val mediumCount: Int = 0,
    val lowCount: Int = 0
) {
    val progressFraction: Float
        get() = if (total > 0) scanned.toFloat() / total.toFloat() else 0f
}

data class ScanSummary(
    val userHigh: Int,
    val userMedium: Int,
    val userLow: Int,
    val userTotal: Int,
    val systemHigh: Int = 0,
    val systemMedium: Int = 0,
    val systemLow: Int = 0,
    val systemTotal: Int = 0
) {
    val totalApps: Int get() = userTotal + systemTotal
}

sealed interface ScanState {
    data object Idle : ScanState
    data class Scanning(val progress: ScanProgress) : ScanState
    data class Done(val summary: ScanSummary) : ScanState
    data class Error(val message: String) : ScanState
}

sealed interface ScanEvent {
    data class Progress(val progress: ScanProgress) : ScanEvent
    data class Completed(val apps: List<AppInfo>, val summary: ScanSummary) : ScanEvent
    data class Error(val message: String) : ScanEvent
}
