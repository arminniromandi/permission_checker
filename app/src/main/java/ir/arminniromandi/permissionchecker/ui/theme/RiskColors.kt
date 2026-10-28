package ir.arminniromandi.permissionchecker.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import ir.arminniromandi.permissionchecker.domain.model.RiskLevel

@Immutable
data class RiskColors(
    val high: Color,
    val onHigh: Color,
    val highContainer: Color,
    val onHighContainer: Color,

    val medium: Color,
    val onMedium: Color,
    val mediumContainer: Color,
    val onMediumContainer: Color,

    val low: Color,
    val onLow: Color,
    val lowContainer: Color,
    val onLowContainer: Color,

    val none: Color,
    val onNone: Color,
    val noneContainer: Color,
    val onNoneContainer: Color
) {
    fun colorFor(level: RiskLevel): Color = when (level) {
        RiskLevel.HIGH -> high
        RiskLevel.MEDIUM -> medium
        RiskLevel.LOW -> low
        RiskLevel.NONE -> none
    }

    fun containerColorFor(level: RiskLevel): Color = when (level) {
        RiskLevel.HIGH -> highContainer
        RiskLevel.MEDIUM -> mediumContainer
        RiskLevel.LOW -> lowContainer
        RiskLevel.NONE -> noneContainer
    }

    fun onContainerColorFor(level: RiskLevel): Color = when (level) {
        RiskLevel.HIGH -> onHighContainer
        RiskLevel.MEDIUM -> onMediumContainer
        RiskLevel.LOW -> onLowContainer
        RiskLevel.NONE -> onNoneContainer
    }
}

val LightRiskColors = RiskColors(
    high = Color(0xFFBA1A1A),
    onHigh = Color(0xFFFFFFFF),
    highContainer = Color(0xFFFFDAD6),
    onHighContainer = Color(0xFF410002),

    medium = Color(0xFFB56A00),
    onMedium = Color(0xFFFFFFFF),
    mediumContainer = Color(0xFFFFDDB8),
    onMediumContainer = Color(0xFF2B1700),

    low = Color(0xFF1B6D2A),
    onLow = Color(0xFFFFFFFF),
    lowContainer = Color(0xFFA6F5A3),
    onLowContainer = Color(0xFF002206),

    none = Color(0xFF5D5E62),
    onNone = Color(0xFFFFFFFF),
    noneContainer = Color(0xFFE2E2E6),
    onNoneContainer = Color(0xFF1A1C1E)
)

val DarkRiskColors = RiskColors(
    high = Color(0xFFFFB4AB),
    onHigh = Color(0xFF690005),
    highContainer = Color(0xFF93000A),
    onHighContainer = Color(0xFFFFDAD6),

    medium = Color(0xFFFFB86F),
    onMedium = Color(0xFF4B2800),
    mediumContainer = Color(0xFF6A3B00),
    onMediumContainer = Color(0xFFFFDDB8),

    low = Color(0xFF8CD889),
    onLow = Color(0xFF00390E),
    lowContainer = Color(0xFF005318),
    onLowContainer = Color(0xFFA6F5A3),

    none = Color(0xFFC6C6CA),
    onNone = Color(0xFF2F3033),
    noneContainer = Color(0xFF45474A),
    onNoneContainer = Color(0xFFE2E2E6)
)

val LocalRiskColors = staticCompositionLocalOf { LightRiskColors }
