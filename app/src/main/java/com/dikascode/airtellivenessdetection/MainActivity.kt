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

class MainActivity : AppCompatActivity() {

    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private val cameraManager by lazy {
        CameraManager(this, binding.previewViewFinder, this, binding.graphicOverlayFinder)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        checkForPermissions()
        onClicks()
    }

    private fun checkForPermissions() {
        if (REQUIRED_PERMISSIONS.all {
                ContextCompat.checkSelfPermission(
                    baseContext,
                    it
                ) == PackageManager.PERMISSION_GRANTED
            }) {
            cameraManager.startCamera()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }
    }

    private fun onClicks() {
        binding.btnSwitch.setOnClickListener {
            cameraManager.toggleCameraSelector()
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
                    cameraManager.startCamera()
                } else {
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
        finish()
    }

    companion object {
        private const val TAG = "MainActivity"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}




