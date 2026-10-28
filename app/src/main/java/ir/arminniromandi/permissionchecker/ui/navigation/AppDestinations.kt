package ir.arminniromandi.permissionchecker.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
object HomeRoute

@Serializable
object AppListRoute

@Serializable
data class DetailRoute(val packageName: String)
