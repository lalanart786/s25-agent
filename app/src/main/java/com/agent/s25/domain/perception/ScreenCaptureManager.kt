package com.agent.s25.domain.perception

import android.graphics.Bitmap
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.util.Log

class ScreenCaptureManager {
    companion object {
        private const val TAG = "ScreenCaptureManager"
    }

    private var mediaProjection: MediaProjection? = null
    private var imageReader: ImageReader? = null

    fun setMediaProjection(projection: MediaProjection) {
        this.mediaProjection = projection
        Log.i(TAG, "MediaProjection registered for screen perception.")
    }

    fun captureActiveScreen(): Bitmap? {
        val image = imageReader?.acquireLatestImage() ?: return null
        val planes = image.planes
        val buffer = planes[0].buffer
        val pixelStride = planes[0].pixelStride
        val rowStride = planes[0].rowStride
        val rowPadding = rowStride - pixelStride * image.width

        val bitmap = Bitmap.createBitmap(
            image.width + rowPadding / pixelStride,
            image.height,
            Bitmap.Config.ARGB_8888
        )
        bitmap.copyPixelsFromBuffer(buffer)
        image.close()
        Log.i(TAG, "Screen captured: ${bitmap.width}x${bitmap.height} for Qwen2-VL analysis.")
        return bitmap
    }
}
