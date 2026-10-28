package ir.arminniromandi.permissionchecker.domain.usecase

import app.cash.turbine.test
import io.mockk.every
import io.mockk.mockk
import ir.arminniromandi.permissionchecker.domain.model.ScanEvent
import ir.arminniromandi.permissionchecker.domain.model.ScanProgress
import ir.arminniromandi.permissionchecker.domain.model.ScanSummary
import ir.arminniromandi.permissionchecker.domain.repository.AppScanRepository
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ScanAppsUseCaseTest {

    private val repository: AppScanRepository = mockk()
    private val useCase = ScanAppsUseCase(repository)

    @Test
    fun testInvoke_emitsScanEventsFromRepository() = runTest {
        val progressEvent = ScanEvent.Progress(ScanProgress(scanned = 1, total = 2))
        val completedEvent = ScanEvent.Completed(
            apps = emptyList(),
            summary = ScanSummary(0, 0, 0, 0, 0, 0, 0, 0)
        )

        every { repository.scan() } returns flowOf(progressEvent, completedEvent)

        useCase().test {
            assertEquals(progressEvent, awaitItem())
            assertEquals(completedEvent, awaitItem())
            awaitComplete()
        }
    }
}
