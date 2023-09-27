package com.dikascode.airtellivenessdetection.camera

import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.camera.core.CameraSelector
import kotlin.math.ceil
import kotlin.math.max

class OverlayCanvas(context: Context?, attrs: AttributeSet?) : View(context, attrs) {

    private val lock = Any()
    private val graphics = mutableListOf<Graphic>()

    var scale: Float = 1f
        private set
    var offsetX: Float = 0f
        private set
    var offsetY: Float = 0f
        private set

    private var cameraSelector = CameraSelector.LENS_FACING_FRONT
        private set

    abstract class Graphic(private val overlay: OverlayCanvas) {
        abstract fun draw(canvas: Canvas)

        fun calculateRect(height: Float, width: Float, boundingBoxT: Rect): RectF {
            val isLandscape =
                overlay.context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
            val adjustedWidth = if (isLandscape) width else height
            val adjustedHeight = if (isLandscape) height else width

            val scaleX = overlay.width / adjustedWidth
            val scaleY = overlay.height / adjustedHeight
            val scale = max(scaleX, scaleY)

            overlay.scale = scale

            val offsetX = (overlay.width - ceil(adjustedWidth * scale)) / 2.0f
            val offsetY = (overlay.height - ceil(adjustedHeight * scale)) / 2.0f

            overlay.offsetX = offsetX
            overlay.offsetY = offsetY

            val mappedBox = RectF().apply {
                left = boundingBoxT.right * scale + offsetX
                top = boundingBoxT.top * scale + offsetY
                right = boundingBoxT.left * scale + offsetX
                bottom = boundingBoxT.bottom * scale + offsetY
            }

            if (overlay.isFrontMode()) {
                val centerX = overlay.width / 2f
                mappedBox.apply {
                    left = centerX + (centerX - left)
                    right = centerX - (right - centerX)
                }
            }

            return mappedBox
        }
    }

    fun isFrontMode() = cameraSelector == CameraSelector.LENS_FACING_FRONT

    fun toggleSelector() {
        cameraSelector = if (cameraSelector == CameraSelector.LENS_FACING_BACK) {
            CameraSelector.LENS_FACING_FRONT
        } else {
            CameraSelector.LENS_FACING_BACK
        }
    }

    fun clear() {
        synchronized(lock) { graphics.clear() }
        postInvalidate()
    }

    fun add(graphic: Graphic) {
        synchronized(lock) { graphics.add(graphic) }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        synchronized(lock) {
            for (graphic in graphics) {
                graphic.draw(canvas)
            }
        }
    }
}
