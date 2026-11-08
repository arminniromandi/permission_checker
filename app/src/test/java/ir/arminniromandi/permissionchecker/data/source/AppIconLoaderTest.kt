package ir.arminniromandi.permissionchecker.data.source

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppIconLoaderTest {

    private val context: Context = mockk(relaxed = true)
    private val packageManager: PackageManager = mockk()
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var iconLoader: AppIconLoader

    private val mockBitmap: Bitmap = mockk(relaxed = true)
    private val mockImageBitmap: ImageBitmap = mockk(relaxed = true)

    @Before
    fun setUp() {
        mockkStatic(Bitmap::class)
        mockkStatic("androidx.compose.ui.graphics.AndroidImageBitmap_androidKt")

        every { mockBitmap.width } returns 192
        every { mockBitmap.height } returns 192
        every { mockBitmap.isRecycled } returns false
        every { Bitmap.createScaledBitmap(any(), any(), any(), any()) } returns mockBitmap
        every { Bitmap.createBitmap(any<Int>(), any<Int>(), any<Bitmap.Config>()) } returns mockBitmap
        every { mockBitmap.asImageBitmap() } returns mockImageBitmap
        every { mockImageBitmap.width } returns 192
        every { mockImageBitmap.height } returns 192

        iconLoader = AppIconLoader(
            context = context,
            packageManager = packageManager
        )
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun testGetIcon_whenSuccessful_cachesAndReturnsIcon() = runTest(testDispatcher) {
        val packageName = "com.example.app"
        val drawable = mockk<BitmapDrawable>(relaxed = true)
        every { drawable.bitmap } returns mockBitmap
        every { packageManager.getApplicationIcon(packageName) } returns drawable

        val result1 = iconLoader.getIcon(packageName)
        assertNotNull(result1)

        // Second call should come from cache
        val result2 = iconLoader.getIcon(packageName)
        assertNotNull(result2)

        // getApplicationIcon should be called only once due to cache
        verify(exactly = 1) { packageManager.getApplicationIcon(packageName) }
    }

    @Test
    fun testGetIcon_whenPackageNotFound_returnsNull() = runTest(testDispatcher) {
        val packageName = "com.nonexistent.app"
        every { packageManager.getApplicationIcon(packageName) } throws PackageManager.NameNotFoundException()

        val result = iconLoader.getIcon(packageName)
        assertNull(result)
    }

    @Test
    fun testGetIcon_concurrentRequests_areDeduplicated() = runTest(testDispatcher) {
        val packageName = "com.concurrent.app"
        val drawable = mockk<BitmapDrawable>(relaxed = true)
        every { drawable.bitmap } returns mockBitmap
        every { packageManager.getApplicationIcon(packageName) } returns drawable

        // Launch two concurrent requests
        val deferred1 = async { iconLoader.getIcon(packageName) }
        val deferred2 = async { iconLoader.getIcon(packageName) }

        val result1 = deferred1.await()
        val result2 = deferred2.await()

        assertNotNull(result1)
        assertNotNull(result2)

        // Verify only 1 binder call was made
        verify(exactly = 1) { packageManager.getApplicationIcon(packageName) }
    }

    @Test
    fun testGetIcon_cancellationOfOneCaller_doesNotBreakOtherCallersOrCache() = runTest(testDispatcher) {
        val packageName = "com.cancel.app"
        val drawable = mockk<BitmapDrawable>(relaxed = true)
        every { drawable.bitmap } returns mockBitmap
        every { packageManager.getApplicationIcon(packageName) } returns drawable

        // Launch job 1 and job 2
        var result2: ImageBitmap? = null
        val job1 = launch {
            iconLoader.getIcon(packageName)
        }
        val job2 = launch {
            result2 = iconLoader.getIcon(packageName)
        }

        // Cancel job 1 immediately
        job1.cancel()

        // Advance dispatcher to allow the async loader and job2 to finish
        advanceUntilIdle()

        job2.join()
        assertNotNull(result2)

        // Subsequent call should hit cache directly
        val result3 = iconLoader.getIcon(packageName)
        assertNotNull(result3)

        // Only 1 fetch should have occurred
        verify(exactly = 1) { packageManager.getApplicationIcon(packageName) }
    }

    @Test
    fun testEvict_removesIconFromCache() = runTest(testDispatcher) {
        val packageName = "com.evict.app"
        val drawable = mockk<BitmapDrawable>(relaxed = true)
        every { drawable.bitmap } returns mockBitmap
        every { packageManager.getApplicationIcon(packageName) } returns drawable

        iconLoader.getIcon(packageName)
        verify(exactly = 1) { packageManager.getApplicationIcon(packageName) }

        // Evict package
        iconLoader.evict(packageName)

        // Fetch again, should re-query packageManager
        iconLoader.getIcon(packageName)
        verify(exactly = 2) { packageManager.getApplicationIcon(packageName) }
    }

    @Test
    fun testGetIcon_whenOOMThrown_returnsNullGracefully() = runTest(testDispatcher) {
        val packageName = "com.oom.app"
        val drawable = mockk<BitmapDrawable>(relaxed = true)
        every { drawable.bitmap } returns mockBitmap
        every { Bitmap.createScaledBitmap(any(), any(), any(), any()) } throws OutOfMemoryError("Simulated OOM")
        every { packageManager.getApplicationIcon(packageName) } returns drawable

        val result = iconLoader.getIcon(packageName)
        assertNull(result)
    }

    @Test
    fun testGetIcon_whenRecycledBitmap_returnsNullAndDoesNotCrash() = runTest(testDispatcher) {
        val packageName = "com.recycled.app"
        val recycledBitmap = mockk<Bitmap>(relaxed = true)
        every { recycledBitmap.isRecycled } returns true
        val drawable = mockk<BitmapDrawable>(relaxed = true)
        every { drawable.bitmap } returns recycledBitmap
        every { packageManager.getApplicationIcon(packageName) } returns drawable

        val result = iconLoader.getIcon(packageName)
        assertNull(result)
    }

    @Test
    fun testClear_cancelsInFlightRequestsAndEvictsCache() = runTest(testDispatcher) {
        val packageName = "com.clear.app"
        val drawable = mockk<BitmapDrawable>(relaxed = true)
        every { drawable.bitmap } returns mockBitmap
        every { packageManager.getApplicationIcon(packageName) } returns drawable

        iconLoader.getIcon(packageName)
        verify(exactly = 1) { packageManager.getApplicationIcon(packageName) }

        iconLoader.clear()

        iconLoader.getIcon(packageName)
        verify(exactly = 2) { packageManager.getApplicationIcon(packageName) }
    }
}
