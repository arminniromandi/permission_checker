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
import ir.arminniromandi.permissionchecker.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppIconLoader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val packageManager: PackageManager,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {

    // Target icon size in px (e.g. 192px corresponds to 96dp on 2x density)
    private val iconSizePx = 192

    // Cache ~15% of max available heap memory for icon bitmaps
    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSize = maxMemory / 8

    private val memoryCache = object : LruCache<String, ImageBitmap>(cacheSize) {
        override fun sizeOf(key: String, value: ImageBitmap): Int {
            return (value.width * value.height * 4) / 1024
        }
    }

    private val inFlightRequests = ConcurrentHashMap<String, Deferred<ImageBitmap?>>()
    private val scope = CoroutineScope(ioDispatcher)

    suspend fun getIcon(packageName: String): ImageBitmap? = withContext(ioDispatcher) {
        // 1. Check in-memory LruCache
        memoryCache.get(packageName)?.let { return@withContext it }

        // 2. Request deduplication
        val deferred = inFlightRequests.computeIfAbsent(packageName) {
            scope.async {
                loadAndRenderIcon(packageName)
            }
        }

        try {
            val bitmap = deferred.await()
            if (bitmap != null) {
                memoryCache.put(packageName, bitmap)
            }
            bitmap
        } finally {
            inFlightRequests.remove(packageName)
        }
    }

    private fun loadAndRenderIcon(packageName: String): ImageBitmap? {
        return try {
            val drawable = packageManager.getApplicationIcon(packageName)
            drawableToImageBitmap(drawable, iconSizePx)
        } catch (_: Exception) {
            null
        }
    }

    private fun drawableToImageBitmap(drawable: Drawable, size: Int): ImageBitmap {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            val original = drawable.bitmap
            val scaled = Bitmap.createScaledBitmap(original, size, size, true)
            return scaled.asImageBitmap()
        }

        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap.asImageBitmap()
    }

    fun evict(packageName: String) {
        memoryCache.remove(packageName)
    }

    fun clear() {
        memoryCache.evictAll()
    }
}
