package com.dikascode.airtellivenessdetection.camera

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import com.dikascode.airtellivenessdetection.live_detection.FaceDetectionCallback
import com.dikascode.airtellivenessdetection.live_detection.FaceDetectionHandler
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

class CameraManager(
    context: Context,
    private val finderView: PreviewView,
    private val lifecycleOwner: LifecycleOwner,
    private val overlayCanvas: OverlayCanvas,
    private val faceDetectionCallback: FaceDetectionCallback
) {

    private var cameraProvider: ProcessCameraProvider? = null

    private val cameraExecutor by lazy { Executors.newSingleThreadExecutor() }

    private val cameraSelectorOption = MutableLiveData(CameraSelector.LENS_FACING_FRONT)

    private var imageCapture: ImageCapture? = null
    private lateinit var imageAnalyzer: ImageAnalysis

    init {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener(
            {
                cameraProvider = cameraProviderFuture.get()

                if (allPermissionsGranted(context)) {
                    startCamera(context)
                } else {
                    ActivityCompat.requestPermissions(
                        Activity(),
                        REQUIRED_PERMISSIONS,
                        REQUEST_CODE_PERMISSIONS
                    )
                }
            },
            ContextCompat.getMainExecutor(context)
        )
    }

    private fun allPermissionsGranted(context: Context) = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

    fun startCamera(context: Context) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener(
            {
                cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build()

                //initializing image capture
                imageCapture = ImageCapture.Builder().build()

                imageAnalyzer = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .apply {
                        setAnalyzer(
                            cameraExecutor,
                            FaceDetectionHandler(overlayCanvas, faceDetectionCallback)
                        )
                    }

                cameraSelectorOption.value?.let { lensFacing ->
                    val cameraSelector =
                        CameraSelector.Builder().requireLensFacing(lensFacing).build()
                    bindCameraUseCases(preview, imageCapture!!, imageAnalyzer, cameraSelector)
                }
            },
            ContextCompat.getMainExecutor(context)
        )
    }

    private fun bindCameraUseCases(
        preview: Preview,
        imageCapture: ImageCapture,
        imageAnalyzer: ImageAnalysis,
        cameraSelector: CameraSelector
    ) {
        try {
            cameraProvider?.unbindAll()
            cameraProvider?.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageCapture,
                imageAnalyzer
            )
            preview.setSurfaceProvider(finderView.surfaceProvider)
        } catch (e: Exception) {
            Log.e(TAG, "Use case binding failed", e)
        }
    }

    fun toggleCameraSelector(context: Context) {
        cameraSelectorOption.value =
            if (cameraSelectorOption.value == CameraSelector.LENS_FACING_BACK) {
                CameraSelector.LENS_FACING_FRONT
            } else {
                CameraSelector.LENS_FACING_BACK
            }
        overlayCanvas.toggleSelector()
        startCamera(context)
    }

    fun captureImage(context: Context, callback: (Bitmap) -> Unit) {

        val imageCapture = imageCapture ?: return

        // Creating a temporary file to store the captured image
        val photoFile = File(
            context.cacheDir, SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(
                Date()
            ) + ".jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions, cameraExecutor, object : ImageCapture.OnImageSavedCallback {
                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "Image capture failed", exception)
                }

                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(photoFile)
                    val bitmap = BitmapFactory.decodeFile(savedUri.path)
                    callback(bitmap)

                    // Save the captured image to external storage using MediaStore API
                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, photoFile.name)
                        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                    }

                    val externalUri = context.contentResolver.insert(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        contentValues
                    )

                    externalUri?.let {
                        context.contentResolver.openOutputStream(it).use { outputStream ->
                            if (outputStream != null) {
                                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                            }
                        }
                    }
                }
            })
    }


    companion object {
        private const val TAG = "CameraXBasic"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}
