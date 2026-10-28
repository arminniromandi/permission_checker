package ir.arminniromandi.permissionchecker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import ir.arminniromandi.permissionchecker.data.source.AppIconLoader
import ir.arminniromandi.permissionchecker.ui.navigation.AppNavHost
import ir.arminniromandi.permissionchecker.ui.theme.PermissionCheckerTheme
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var iconLoader: AppIconLoader

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PermissionCheckerTheme {
                AppNavHost(iconLoader = iconLoader)
            }
        }
    }
}
