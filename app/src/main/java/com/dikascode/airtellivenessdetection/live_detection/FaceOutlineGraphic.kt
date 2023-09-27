package com.dikascode.airtellivenessdetection.live_detection

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import com.dikascode.airtellivenessdetection.camera.OverlayCanvas
import com.google.mlkit.vision.face.Face

class FaceOutlineGraphic(
    overlay: OverlayCanvas,
    private val face: Face,
    private val imageRect: Rect
) : OverlayCanvas.Graphic(overlay) {

    private val facePositionPaint: Paint = Paint()
    private val idPaint: Paint
    private val boxPaint: Paint

    var color = Color.GREEN

    init {

        facePositionPaint.color = color

        idPaint = Paint()
        idPaint.color = color

        boxPaint = Paint()
        boxPaint.style = Paint.Style.STROKE
        boxPaint.strokeWidth = BOX_STROKE_WIDTH
    }

    override fun draw(canvas: Canvas) {
        val rect = calculateRect(
            imageRect.height().toFloat(),
            imageRect.width().toFloat(),
            face.boundingBox
        )

        boxPaint.color = color
        canvas.drawRect(rect, boxPaint)
    }

    companion object {
        private const val BOX_STROKE_WIDTH = 5.0f
    }

}