package com.dikascode.airtellivenessdetection

import android.content.pm.PackageManager
import com.dikascode.airtellivenessdetection.databinding.ActivityMainBinding
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.widget.Toast
import com.dikascode.airtellivenessdetection.camera.CameraManager
import android.Manifest
import android.content.ContentResolver
import android.content.ContentValues
import android.graphics.Bitmap
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import com.dikascode.airtellivenessdetection.live_detection.FaceDetectionCallback
import com.dikascode.airtellivenessdetection.live_detection.FaceDetectionHandler
import java.io.IOException

class MainActivity : AppCompatActivity(), FaceDetectionCallback {

    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private val cameraManager by lazy {
        CameraManager(this, binding.previewViewFinder, this, binding.graphicOverlayFinder, this)
    }

    private val faceDetectionHandler by lazy {
        FaceDetectionHandler(
            binding.graphicOverlayFinder,
            this
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        checkForPermissions()
        setupClickListener()
    }

    private fun checkForPermissions() {
        if (REQUIRED_PERMISSIONS.all {
                ContextCompat.checkSelfPermission(
                    baseContext,
                    it
                ) == PackageManager.PERMISSION_GRANTED
            }) {
            cameraManager.startCamera(this)
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }
    }

    private fun setupClickListener() {
        binding.btnSwitch.setOnClickListener {
            cameraManager.toggleCameraSelector(this)
        }

        binding.btnCapture.setOnClickListener {
            if (binding.btnCapture.text == getString(R.string.capture)) {
                captureImage()
            } else {
                detectFaces()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_CODE_PERMISSIONS -> {
                if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                    cameraManager.startCamera(this)
                } else {
                    Log.e(TAG, "Permissions: ${permissions.contentToString()}, Results: ${grantResults.contentToString()}")
                    Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT)
                        .show()
                    finish()
                }
            }

            else -> {
                Log.e(TAG, "Unexpected request code")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        faceDetectionHandler.stop()
    }

    override fun onStop() {
        super.onStop()
        faceDetectionHandler.stop()
    }

    private fun captureImage() {
        cameraManager.captureImage(this) { bitmap ->
            runOnUiThread {
                binding.imagePreview.setImageBitmap(bitmap)
                binding.imagePreview.visibility = View.VISIBLE
                binding.graphicOverlayFinder.visibility = View.GONE
                binding.previewViewFinder.visibility = View.GONE
                binding.btnSwitch.isEnabled = false
                binding.btnCapture.text = getString(R.string.detect_faces)
                faceDetectionHandler.stop()
                saveImageToStorage(bitmap)
            }
        }
    }


    private fun saveImageToStorage(bitmap: Bitmap) {
        val filename = "${System.currentTimeMillis()}.jpg"
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
        }

        val resolver: ContentResolver = contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        try {
            uri?.let {
                val outStream = resolver.openOutputStream(it)
                if (outStream != null) {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outStream)
                }
                outStream?.flush()
                outStream?.close()
                Toast.makeText(this, "Image Saved: $filename", Toast.LENGTH_LONG).show()
            } ?: run {
                Toast.makeText(this, "Failed to create new MediaStore entry", Toast.LENGTH_LONG).show()
            }
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to save image: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }


    companion object {
        private const val TAG = "MainActivity"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)

    }

    private fun detectFaces() {
        binding.imagePreview.visibility = View.GONE
        binding.graphicOverlayFinder.visibility = View.VISIBLE
        binding.previewViewFinder.visibility = View.VISIBLE
        binding.imagePreview.visibility = View.GONE
        binding.btnSwitch.isEnabled = true
        binding.btnCapture.text = getString(R.string.capture)
    }

    override fun onFaceStateChanged(isValid: Boolean) {
        if(binding.imagePreview.visibility == View.GONE)
            binding.btnCapture.isEnabled = isValid
        else{
            binding.btnCapture.isEnabled = true
            if (!isValid && !faceDetectionHandler.isStopped) {
                faceDetectionHandler.stop()
            }
        }

    }
}




