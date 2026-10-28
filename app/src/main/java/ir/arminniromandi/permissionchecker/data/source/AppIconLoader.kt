package ir.arminniromandi.permissionchecker.data.source

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.collection.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppIconLoader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val packageManager: PackageManager
) {

    private val iconSizePx = 192

    // Cache ~12.5% of max available heap memory for icon bitmaps, minimum 4MB
    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSize = (maxMemory / 8).coerceAtLeast(4096)

    private val memoryCache = object : LruCache<String, ImageBitmap>(cacheSize) {
        override fun sizeOf(key: String, value: ImageBitmap): Int {
            return ((value.width * value.height * 4) / 1024).coerceAtLeast(1)
        }
    }

    private val inFlightRequests = ConcurrentHashMap<String, Deferred<ImageBitmap?>>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    suspend fun getIcon(packageName: String): ImageBitmap? = withContext(Dispatchers.IO) {
        // 1. Check in-memory LruCache
        memoryCache.get(packageName)?.let { return@withContext it }

        // 2. Request deduplication
        val deferred = inFlightRequests.computeIfAbsent(packageName) {
            scope.async {
                try {
                    // Check cache inside async in case another coroutine populated it
                    memoryCache.get(packageName)?.let { return@async it }
                    val bitmap = loadAndRenderIcon(packageName)
                    if (bitmap != null) {
                        memoryCache.put(packageName, bitmap)
                    }
                    bitmap
                } finally {
                    inFlightRequests.remove(packageName)
                }
            }
        }

        deferred.await()
    }

    private fun loadAndRenderIcon(packageName: String): ImageBitmap? {
        return try {
            val drawable = packageManager.getApplicationIcon(packageName)
            drawableToImageBitmap(drawable, iconSizePx)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Throwable) {
            null
        }
    }

    private fun drawableToImageBitmap(drawable: Drawable, size: Int): ImageBitmap? {
        if (drawable is BitmapDrawable) {
            val original = drawable.bitmap ?: return null
            if (original.isRecycled) return null
            val scaled = Bitmap.createScaledBitmap(original, size, size, true)
            return scaled.asImageBitmap()
        }

        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap.asImageBitmap()
    }


    //زمانی اپ حذف شد
    //
    fun evict(packageName: String) {
        memoryCache.remove(packageName)
        inFlightRequests.remove(packageName)?.cancel()
    }

    fun clear() {
        val requests = inFlightRequests.values.toList()
        inFlightRequests.clear()
        requests.forEach { it.cancel() }
        memoryCache.evictAll()
    }
}
