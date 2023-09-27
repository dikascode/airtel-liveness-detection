package com.dikascode.airtellivenessdetection.camera

import android.annotation.SuppressLint
import android.graphics.Rect
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage

abstract class AbstractImageAnalyzer<T> : ImageAnalysis.Analyzer {

    abstract val overlayCanvas: OverlayCanvas

    @OptIn(ExperimentalGetImage::class)
    @SuppressLint("UnsafeExperimentalUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image ?: return

        detectInImage(InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees))
            .addOnSuccessListener { results ->
                onSuccess(results, overlayCanvas, mediaImage.cropRect)
            }
            .addOnFailureListener(this::onFailure)
            .addOnCompleteListener {
                imageProxy.close()
            }
    }

    protected abstract fun detectInImage(image: InputImage): Task<T>

    abstract fun stop()

    protected abstract fun onSuccess(results: T, overlayCanvas: OverlayCanvas, rect: Rect)

    protected abstract fun onFailure(e: Exception)

}
