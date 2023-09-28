package com.dikascode.airtellivenessdetection.live_detection

import android.graphics.Color
import android.graphics.Rect
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
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.IOException

class FaceDetectionHandler(
    private val view: OverlayCanvas,
    private val callback: FaceDetectionCallback
) :
    AbstractImageAnalyzer<List<Face>>() {

    private val realTimeOpts = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
        .setContourMode(FaceDetectorOptions.CONTOUR_MODE_NONE)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
        .build()

    private var detector = FaceDetection.getClient(realTimeOpts)

    private val debounceTime = 200 // 2 second
    private var currentToast: Toast? = null

    private var lastToastMessage: String? = null
    private var toastCount = 0
    private val maxToastCount = 2

    var isStopped = false
        private set

    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    //Helps track if a face is valid to be captured or not
    private var faceIsValid = false

    override val overlayCanvas: OverlayCanvas
        get() = view

    override fun detectInImage(image: InputImage): Task<List<Face>> {
        return detector.process(image)
    }

//    fun start() {
//        detector = FaceDetection.getClient(realTimeOpts)
//    }

    override fun stop() {
        if (!isStopped) {
            isStopped = true
            callback.onFaceStateChanged(false)
            try {
                detector.close()

                // Cancel all coroutines to avoid leaks
                coroutineScope.cancel()

                // Cancel any currently showing Toast
                currentToast?.cancel()

            } catch (e: IOException) {
                Log.e(TAG, "Exception thrown while trying to close Face Detector: $e")
            }
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

            faceIsValid = true

            // Check if results list is empty
            if (results.isEmpty()) {
                scheduleToast(overlayCanvas, "No face detected")
                faceIsValid = false

                return@launch
            }

            if (results.size > 1) {
                scheduleToast(overlayCanvas, "Only one face is allowed")
                results.forEach { face ->
                    val faceGraphic = FaceOutlineGraphic(overlayCanvas, face, rect).apply {
                        color = Color.RED
                    }
                    overlayCanvas.add(faceGraphic)
                }

                faceIsValid = false

            } else {
                val face = results.first()
                val faceGraphic = FaceOutlineGraphic(overlayCanvas, face, rect)

                faceIsValid = true

                face.smilingProbability?.let {
                    if (it > SMILE_THRESHOLD) {
                        scheduleToast(overlayCanvas, "Please do not smile")
                        faceGraphic.color = Color.RED
                        faceIsValid = false
                    }
                }

                if (face.leftEyeOpenProbability?.let { it < BLINK_THRESHOLD } == true ||
                    face.rightEyeOpenProbability?.let { it < BLINK_THRESHOLD } == true) {
                    scheduleToast(overlayCanvas, "Please do not blink.")
                    faceGraphic.color = Color.RED
                    faceIsValid = false
                }

                // Check if the face is not centralized
                if (!isFaceCentralized(face, overlayCanvas)) {
                    faceGraphic.color = Color.RED
                    faceIsValid = false
                }

                // Add the graphic overlay
                overlayCanvas.add(faceGraphic)
            }
        }

        // Notify callback about face state change
        callback.onFaceStateChanged(faceIsValid)

        // Invalidate the overlay to redraw
        overlayCanvas.postInvalidate()
    }


    private fun scheduleToast(overlayCanvas: OverlayCanvas, message: String) {
        if (lastToastMessage != message) {
            lastToastMessage = message
            toastCount = 0
        }

        if (toastCount < maxToastCount) {
            toastCount++

            coroutineScope.launch {
                // Show the Toast immediately
                currentToast = Toast.makeText(overlayCanvas.context, message, Toast.LENGTH_SHORT)
                currentToast?.show()

                // Delay for debounceTime to prevent showing the next Toast immediately
                delay(debounceTime.toLong())
            }
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

interface FaceDetectionCallback {
    fun onFaceStateChanged(isValid: Boolean)
}