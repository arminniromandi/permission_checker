package ir.arminniromandi.permissionchecker.ui.home

import app.cash.turbine.test
import io.mockk.every
import io.mockk.mockk
import ir.arminniromandi.permissionchecker.domain.model.ScanEvent
import ir.arminniromandi.permissionchecker.domain.model.ScanProgress
import ir.arminniromandi.permissionchecker.domain.model.ScanSummary
import ir.arminniromandi.permissionchecker.domain.repository.AppScanRepository
import ir.arminniromandi.permissionchecker.domain.usecase.ScanAppsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
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
class HomeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val repository: AppScanRepository = mockk()
    private val scanAppsUseCase: ScanAppsUseCase = mockk()
    private val lastSummaryFlow = MutableStateFlow<ScanSummary?>(null)

    private lateinit var viewModel: HomeViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        every { repository.lastSummary } returns lastSummaryFlow
        viewModel = HomeViewModel(scanAppsUseCase, repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialState_isIdle() {
        assertEquals(HomeUiState.Idle, viewModel.uiState.value)
    }

    @Test
    fun testStartScan_transitionsFromScanningToDone() = runTest(testDispatcher) {
        val progress = ScanProgress(scanned = 1, total = 5, highCount = 1)
        val summary = ScanSummary(userHigh = 1, userMedium = 0, userLow = 0, userTotal = 1)

        every { scanAppsUseCase() } returns flowOf(
            ScanEvent.Progress(progress),
            ScanEvent.Completed(emptyList(), summary)
        )

        viewModel.uiState.test {
            assertEquals(HomeUiState.Idle, awaitItem())

            viewModel.startScan()

            val scanningState = awaitItem()
            assertTrue(scanningState is HomeUiState.Scanning)
            assertEquals(progress, (scanningState as HomeUiState.Scanning).progress)

            val doneState = awaitItem()
            assertTrue(doneState is HomeUiState.Done)
            assertEquals(summary, (doneState as HomeUiState.Done).summary)
        }
    }

    @Test
    fun testCancelScan_restoresPreviousOrIdleState() = runTest(testDispatcher) {
        every { scanAppsUseCase() } returns flowOf(
            ScanEvent.Progress(ScanProgress(scanned = 1, total = 10))
        )

        viewModel.startScan()
        advanceUntilIdle()

        viewModel.cancelScan()
        advanceUntilIdle()

        assertEquals(HomeUiState.Idle, viewModel.uiState.value)
    }
}
