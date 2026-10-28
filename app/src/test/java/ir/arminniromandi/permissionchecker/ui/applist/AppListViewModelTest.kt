package ir.arminniromandi.permissionchecker.ui.applist

import app.cash.turbine.test
import io.mockk.every
import io.mockk.mockk
import ir.arminniromandi.permissionchecker.domain.model.AppInfo
import ir.arminniromandi.permissionchecker.domain.model.RiskLevel
import ir.arminniromandi.permissionchecker.domain.usecase.GetAppListUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
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
class AppListViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val getAppListUseCase: GetAppListUseCase = mockk()
    private val sampleAppsFlow = MutableStateFlow<List<AppInfo>>(emptyList())

    private val userHighApp = AppInfo(
        packageName = "com.test.high",
        appName = "High Risk User App",
        versionName = "1.0",
        versionCode = 1,
        firstInstallTime = 0,
        lastUpdateTime = 0,
        isSystemApp = false,
        permissions = emptyList(),
        overallRisk = RiskLevel.HIGH
    )

    private val userLowApp = AppInfo(
        packageName = "com.test.low",
        appName = "Low Risk User App",
        versionName = "1.0",
        versionCode = 1,
        firstInstallTime = 0,
        lastUpdateTime = 0,
        isSystemApp = false,
        permissions = emptyList(),
        overallRisk = RiskLevel.LOW
    )

    private val systemApp = AppInfo(
        packageName = "com.android.system",
        appName = "System App",
        versionName = "1.0",
        versionCode = 1,
        firstInstallTime = 0,
        lastUpdateTime = 0,
        isSystemApp = true,
        permissions = emptyList(),
        overallRisk = RiskLevel.HIGH
    )

    private lateinit var viewModel: AppListViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { getAppListUseCase() } returns sampleAppsFlow
        sampleAppsFlow.value = listOf(userHighApp, userLowApp, systemApp)
        viewModel = AppListViewModel(getAppListUseCase)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testDefaultFilter_excludesSystemApps() = runTest(testDispatcher) {
        viewModel.uiState.test {
            // Default should not include system apps
            val state = awaitItem()
            // May start with initial value or emission
            advanceUntilIdle()
            val updatedState = if (state.apps.isEmpty()) awaitItem() else state

            assertEquals(2, updatedState.apps.size)
            assertTrue(updatedState.apps.none { it.isSystemApp })
        }
    }

    @Test
    fun testIncludeSystemAppsToggle_includesSystemApps() = runTest(testDispatcher) {
        viewModel.uiState.test {
            awaitItem() // initial or first emission
            advanceUntilIdle()

            viewModel.onIncludeSystemAppsToggled(true)
            advanceUntilIdle()

            val state = expectMostRecentItem()
            assertEquals(3, state.apps.size)
            assertTrue(state.apps.any { it.isSystemApp })
        }
    }

    @Test
    fun testRiskFilter_filtersOnlyMatchingRisk() = runTest(testDispatcher) {
        viewModel.uiState.test {
            awaitItem()
            advanceUntilIdle()

            viewModel.onRiskFilterChanged(RiskLevel.HIGH)
            advanceUntilIdle()

            val state = expectMostRecentItem()
            assertEquals(1, state.apps.size)
            assertEquals("com.test.high", state.apps.first().packageName)
        }
    }

    @Test
    fun testSearchQuery_debouncesAndFiltersByName() = runTest(testDispatcher) {
        viewModel.uiState.test {
            awaitItem()
            advanceUntilIdle()

            viewModel.onSearchQueryChanged("Low Risk")
            advanceTimeBy(250) // wait for 200ms debounce
            advanceUntilIdle()

            val state = expectMostRecentItem()
            assertEquals(1, state.apps.size)
            assertEquals("com.test.low", state.apps.first().packageName)
        }
    }
}
