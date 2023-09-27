package com.dikascode.airtellivenessdetection.live_detection

import android.graphics.Color
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.dikascode.airtellivenessdetection.camera.AbstractImageAnalyzer
import com.dikascode.airtellivenessdetection.camera.OverlayCanvas
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException

class FaceDetectionHandler(private val view: OverlayCanvas) :
    AbstractImageAnalyzer<List<Face>>() {

    private val realTimeOpts = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
        .setContourMode(FaceDetectorOptions.CONTOUR_MODE_NONE)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
        .build()

    private val detector = FaceDetection.getClient(realTimeOpts)

    private val debounceTime = 1500 // 1.5 second
    private var isToastScheduled = false
    private var toastRunnable: Runnable? = null
    private val handler = Handler(Looper.getMainLooper())

    override val overlayCanvas: OverlayCanvas
        get() = view

    override fun detectInImage(image: InputImage): Task<List<Face>> {
        return detector.process(image)
    }

    override fun stop() {
        try {
            detector.close()
            toastRunnable?.let { handler.removeCallbacks(it) }
        } catch (e: IOException) {
            Log.e(TAG, "Exception thrown while trying to close Face Detector: $e")
        }
    }

    override fun onSuccess(
        results: List<Face>,
        overlayCanvas: OverlayCanvas,
        rect: Rect
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            // Clear previous graphics
            overlayCanvas.clear()

            // Check if results list is empty
            if (results.isEmpty()) {
                scheduleToast(overlayCanvas, "No face detected")
                return@launch
            }

            // Check for the number of faces detected
            if (results.size > 1) {
                scheduleToast(overlayCanvas, "Only one face is allowed")
                results.forEach { face ->
                    val faceGraphic = FaceOutlineGraphic(overlayCanvas, face, rect).apply {
                        color = Color.RED
                    }
                    overlayCanvas.add(faceGraphic)
                }
            } else {
                val face = results.first()
                val faceGraphic = FaceOutlineGraphic(overlayCanvas, face, rect)

                face.smilingProbability?.let {
                    if (it > SMILE_THRESHOLD) {
                        scheduleToast(overlayCanvas, "Please do not smile")
                        faceGraphic.color = Color.RED
                    }
                }

                if (face.leftEyeOpenProbability?.let { it < BLINK_THRESHOLD } == true ||
                    face.rightEyeOpenProbability?.let { it < BLINK_THRESHOLD } == true) {
                    scheduleToast(overlayCanvas, "Please do not blink.")
                    faceGraphic.color = Color.RED
                }

                // Check if the face is not centralized
                if (!isFaceCentralized(face, overlayCanvas)) {
                    faceGraphic.color = Color.RED
                }

                // Add the graphic overlay
                overlayCanvas.add(faceGraphic)
            }
        }

        // Invalidate the overlay to redraw
        overlayCanvas.postInvalidate()
    }


    private fun scheduleToast(overlayCanvas: OverlayCanvas, message: String) {
        if (!isToastScheduled) {
            isToastScheduled = true
            toastRunnable = Runnable {
                Toast.makeText(overlayCanvas.context, message, Toast.LENGTH_SHORT).show()
                isToastScheduled = false
            }
            handler.postDelayed(toastRunnable!!, debounceTime.toLong())
        }
    }

    //Wasn't really successful with this
    private fun isFaceCentralized(face: Face, overlay: OverlayCanvas): Boolean {
        val faceBounds = face.boundingBox

        // Calculate the center of the face
        val faceCenterX = faceBounds.exactCenterX()
        val faceCenterY = faceBounds.exactCenterY()

        // Calculate the center of the overlay
        val overlayCenterX = overlay.width / 2f
        val overlayCenterY = overlay.height / 2f

        // Define a tolerance range, within which the face is considered centralized
        val toleranceX = overlay.width * 0.4f //
        val toleranceY = overlay.height * 0.4f //

        // Check if any part of the face's bounding box is outside the overlay
        val isPartlyOutOfFrame = faceBounds.left < 0 || faceBounds.top < 0 ||
                faceBounds.right > overlay.width || faceBounds.bottom > overlay.height

        // Check if the face's center is within the tolerance range of the overlay's center
        // and the face is not partly out of frame
        return !isPartlyOutOfFrame &&
                (faceCenterX in (overlayCenterX - toleranceX)..(overlayCenterX + toleranceX)) &&
                (faceCenterY in (overlayCenterY - toleranceY)..(overlayCenterY + toleranceY))
    }


    override fun onFailure(e: Exception) {
        Log.w(TAG, "Face Detector failed.$e")
    }

    companion object {
        private const val TAG = "FaceDetectorProcessor"
        private const val SMILE_THRESHOLD = 0.4f
        private const val BLINK_THRESHOLD = 0.5f
    }

}