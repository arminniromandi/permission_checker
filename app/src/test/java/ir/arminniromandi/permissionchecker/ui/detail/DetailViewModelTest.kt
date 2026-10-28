package ir.arminniromandi.permissionchecker.ui.detail

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import androidx.lifecycle.SavedStateHandle
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import ir.arminniromandi.permissionchecker.domain.model.AppInfo
import ir.arminniromandi.permissionchecker.domain.model.RiskLevel
import ir.arminniromandi.permissionchecker.domain.usecase.GetAppDetailUseCase
import ir.arminniromandi.permissionchecker.domain.usecase.RemoveAppUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DetailViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val getAppDetailUseCase: GetAppDetailUseCase = mockk()
    private val removeAppUseCase: RemoveAppUseCase = mockk(relaxed = true)
    private val packageManager: PackageManager = mockk()

    private val testPackageName = "com.test.app"
    private val testApp = AppInfo(
        packageName = testPackageName,
        appName = "Test App",
        versionName = "1.0",
        versionCode = 1,
        firstInstallTime = 1000L,
        lastUpdateTime = 2000L,
        isSystemApp = false,
        permissions = emptyList(),
        overallRisk = RiskLevel.MEDIUM
    )

    private lateinit var savedStateHandle: SavedStateHandle
    private lateinit var viewModel: DetailViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        savedStateHandle = SavedStateHandle(mapOf("packageName" to testPackageName))
        coEvery { getAppDetailUseCase(testPackageName) } returns testApp
        viewModel = DetailViewModel(
            savedStateHandle = savedStateHandle,
            getAppDetailUseCase = getAppDetailUseCase,
            removeAppUseCase = removeAppUseCase,
            packageManager = packageManager
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testLoadApp_success() = runTest(testDispatcher) {
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is DetailUiState.Success)
        assertEquals(testApp, (state as DetailUiState.Success).app)
    }

    @Test
    fun testUninstallResultReturned_whenUninstalled_callsRemoveAppUseCase() = runTest(testDispatcher) {
        advanceUntilIdle()

        // Simulate package not found (uninstalled)
        every { packageManager.getPackageInfo(testPackageName, 0) } throws PackageManager.NameNotFoundException()
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            every { packageManager.getPackageInfo(testPackageName, any<PackageManager.PackageInfoFlags>()) } throws PackageManager.NameNotFoundException()
        }

        viewModel.onUninstallResultReturned()
        advanceUntilIdle()

        coVerify { removeAppUseCase(testPackageName) }
    }
}
