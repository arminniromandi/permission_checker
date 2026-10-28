package ir.arminniromandi.permissionchecker.domain.model

data class PermissionInfo(
    val name: String,
    val label: String?,
    val description: String?,
    val risk: RiskLevel,
    val isGranted: Boolean
)
