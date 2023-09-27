package com.dikascode.airtellivenessdetection.camera

import android.content.Context
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import com.dikascode.airtellivenessdetection.live_detection.FaceDetectionHandler
import java.util.concurrent.Executors

class CameraManager(
    context: Context,
    private val finderView: PreviewView,
    private val lifecycleOwner: LifecycleOwner,
    private val overlayCanvas: OverlayCanvas
) {

    private var cameraProvider: ProcessCameraProvider? = null

    private val cameraExecutor by lazy { Executors.newSingleThreadExecutor() }

    private val cameraSelectorOption = MutableLiveData(CameraSelector.LENS_FACING_FRONT)

    init {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener(
            {
                cameraProvider = cameraProviderFuture.get()
                startCamera()
            },
            ContextCompat.getMainExecutor(context)
        )
    }

    fun startCamera() {
        val preview = Preview.Builder().build()
        val imageAnalyzer = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .apply { setAnalyzer(cameraExecutor, FaceDetectionHandler(overlayCanvas)) }

        cameraSelectorOption.value?.let { lensFacing ->
            val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
            bindCameraUseCases(preview, imageAnalyzer, cameraSelector)
        }
    }

    private fun bindCameraUseCases(
        preview: Preview,
        imageAnalyzer: ImageAnalysis,
        cameraSelector: CameraSelector
    ) {
        try {
            cameraProvider?.unbindAll()
            cameraProvider?.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageAnalyzer
            )
            preview.setSurfaceProvider(finderView.surfaceProvider)
        } catch (e: Exception) {
            Log.e(TAG, "Use case binding failed", e)
        }
    }

    fun toggleCameraSelector() {
        cameraSelectorOption.value =
            if (cameraSelectorOption.value == CameraSelector.LENS_FACING_BACK) {
                CameraSelector.LENS_FACING_FRONT
            } else {
                CameraSelector.LENS_FACING_BACK
            }
        overlayCanvas.toggleSelector()
        startCamera()
    }

    companion object {
        private const val TAG = "CameraXBasic"
    }
}
