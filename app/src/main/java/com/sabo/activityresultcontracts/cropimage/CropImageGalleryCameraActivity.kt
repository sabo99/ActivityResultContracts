package com.sabo.activityresultcontracts.cropimage

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.sabo.activityresultcontracts.BuildConfig
import com.sabo.activityresultcontracts.Callback
import com.sabo.activityresultcontracts.R
import com.sabo.activityresultcontracts.databinding.ActivityCropImageGalleryCameraBinding
import com.theartofdev.edmodo.cropper.CropImage
import com.theartofdev.edmodo.cropper.CropImageView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.io.File
import kotlin.system.measureTimeMillis

class CropImageGalleryCameraActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCropImageGalleryCameraBinding

    companion object {
        private const val TAG = "CropImageGalleryCamera"

        private var readPermissionGranted = false
        private var writePermissionGranted = false
        private var cameraPermissionGranted = false

        private var tmpTakePhotoUri: Uri? = null
        private var tmpResultUri: Uri? = null
    }

    /** onRequestPermissions */
    private fun onRequestPermissions() {
        val hashReadPermission = ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        val hashWritePermission = ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        val hashCameraPermission = ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val minSdk29 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

        readPermissionGranted = hashReadPermission
        writePermissionGranted = hashWritePermission || minSdk29
        cameraPermissionGranted = hashCameraPermission

        val permissions = mutableListOf<String>()
        if (!readPermissionGranted) permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        if (!writePermissionGranted) permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        if (!cameraPermissionGranted) permissions.add(Manifest.permission.CAMERA)

        if (permissions.isNotEmpty()) requestMultiplePermissions.launch(permissions.toTypedArray())
    }

    /** Request Multiple Permission */
    private val requestMultiplePermissions =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
                readPermissionGranted = permissions[Manifest.permission.READ_EXTERNAL_STORAGE]
                        ?: readPermissionGranted
                writePermissionGranted = permissions[Manifest.permission.WRITE_EXTERNAL_STORAGE]
                        ?: writePermissionGranted
                cameraPermissionGranted = permissions[Manifest.permission.CAMERA]
                        ?: cameraPermissionGranted

                Callback.logD(TAG, "requestMultiplePermissions : $permissions")
            }

    /** Open Camera */
    private fun openCamera() {
        GlobalScope.launch(Dispatchers.IO){
            val time = measureTimeMillis {
                val tmpFile = File.createTempFile("tmp_image_file", ".png", cacheDir).apply {
                    createNewFile()
                    deleteOnExit()
                }
                val tmpFileUri = async { FileProvider.getUriForFile(applicationContext, BuildConfig.APPLICATION_ID, tmpFile) }

                lifecycleScope.launchWhenStarted {
                    tmpFileUri.await().let { uri ->
                        tmpTakePhotoUri = uri
                        pickImageLauncher.launch(uri)
                    }
                }
            }

            Callback.logD(TAG, "Request '$TAG' took $time millis")
        }

    }

    /** Pick Image - Camera */
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { isSaved ->
        if (isSaved && cameraPermissionGranted) {
            tmpTakePhotoUri?.let { uri ->
                tmpResultUri = uri
                cropImageActivityResultLauncher.launch(null)
            }
        }
        Callback.logD(TAG, "FromCamera : $tmpResultUri")
        Callback.logD(TAG, "cameraPermissionGranted : $cameraPermissionGranted")
    }

    /** Open Gallery */
    private fun openGallery() {
        selectImageLauncher.launch("image/*")
    }

    /** Select Image */
    private val selectImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (readPermissionGranted || writePermissionGranted) {
            tmpResultUri = uri
            cropImageActivityResultLauncher.launch(null)
        }
        Callback.logD(TAG, "FromGallery : $uri")
        Callback.logD(TAG, "readPermissionGranted : $readPermissionGranted")
        Callback.logD(TAG, "writePermissionGranted : $writePermissionGranted")
    }

    /** Open CropImageActivity*/
    private val cropImageActivityResultLauncher =
            registerForActivityResult(object : ActivityResultContract<Any?, Uri?>() {
                override fun createIntent(context: Context, input: Any?): Intent {
                    return tmpResultUri.let { uri ->
                        CropImage.activity(uri)
                                .setActivityTitle("Crop Image")
                                .setGuidelines(CropImageView.Guidelines.ON)
                                .getIntent(applicationContext)
                    }
                }

                override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
                    return when (resultCode) {
                        RESULT_OK -> CropImage.getActivityResult(intent)?.uri
                        else -> null
                    }
                }
            }) { uri ->
                tmpTakePhotoUri = null
                tmpResultUri = null

                binding.ivItem.setImageURI(uri)
                binding.ivItemPlaceholder.isVisible = uri != null
            }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCropImageGalleryCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.title = "Crop Image"
        binding.toolbar.setNavigationIcon(R.drawable.ic_round_arrow_back_24)

        /** onRequestPermissions */
        onRequestPermissions()

        /** Button Open Gallery */
        binding.btnOpenGallery.setOnClickListener {
            if (readPermissionGranted && writePermissionGranted)
                openGallery()
            else Callback.toast(applicationContext, "External Storage Permission Denied!")

            Callback.logD(TAG, "btnOpenGallery-readPermissionGranted : $readPermissionGranted")
            Callback.logD(TAG, "btnOpenGallery-writePermissionGranted : $writePermissionGranted")
        }

        /** Button Open Camera */
        binding.btnOpenCamera.setOnClickListener {
            if (cameraPermissionGranted)
                openCamera()
            else Callback.toast(applicationContext, "Camera Permission Denied!")

            Callback.logD(TAG, "btnOpenCamera-cameraPermissionGranted : $cameraPermissionGranted")
        }
    }

    /** Navigation Back */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) finish()
        return super.onOptionsItemSelected(item)
    }
}