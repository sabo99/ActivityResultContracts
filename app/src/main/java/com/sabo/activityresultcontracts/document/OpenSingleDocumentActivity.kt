package com.sabo.activityresultcontracts.document

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import com.sabo.activityresultcontracts.Callback
import com.sabo.activityresultcontracts.FileCallback
import com.sabo.activityresultcontracts.R
import com.sabo.activityresultcontracts.databinding.ActivityOpenSingleDocumentBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlin.system.measureTimeMillis

class OpenSingleDocumentActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOpenSingleDocumentBinding
    private lateinit var documentViewModel: DocumentViewModel

    companion object {
        private const val TAG = "OpenSingleDocument"
        private var writePermissionGranted = false
        private var readPermissionGranted = false
    }

    /** onRequestPermissions */
    private fun onRequestPermissions() {
        val hashReadPermission = ContextCompat.checkSelfPermission(
            applicationContext,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
        val hashWritePermission = ContextCompat.checkSelfPermission(
            applicationContext,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
        val minSdk29 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

        readPermissionGranted = hashReadPermission
        writePermissionGranted = hashWritePermission || minSdk29

        val permissions = mutableListOf<String>()
        if (!readPermissionGranted) permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        if (!writePermissionGranted) permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)

        if (permissions.isNotEmpty()) requestMultiplePermissions.launch(permissions.toTypedArray())
    }

    /** Request Multiple Permission */
    private val requestMultiplePermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            readPermissionGranted = permissions[Manifest.permission.READ_EXTERNAL_STORAGE]
                ?: readPermissionGranted
            writePermissionGranted = permissions[Manifest.permission.WRITE_EXTERNAL_STORAGE]
                ?: writePermissionGranted

            Callback.logD(TAG, "requestMultiplePermissions : $permissions")
        }

    /** Open Single Document */
    private fun openSingleDocument() {
        isVisibleProgressBar(true)
        selectSingleDocumentLauncher.launch(arrayOf("application/*"))
    }

    private fun isVisibleProgressBar(isVisible: Boolean) {
        binding.progressBar.isVisible = isVisible
    }

    /** Select Single Document */
    private val selectSingleDocumentLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (readPermissionGranted && writePermissionGranted) {
                GlobalScope.launch(Dispatchers.Main) {
                    val time = measureTimeMillis {
                        uri?.let {
                            val filePath =
                                async { FileCallback.getFilePathFromUri(applicationContext, it) }
                            val fileRealName = async { FileCallback.getFileRealNameFromUri(it) }
                            val fileGenerateName =
                                async { FileCallback.getFileGenerateNameFromUri(it) }
                            val fileSize =
                                async { FileCallback.getFileSizeFromUri(applicationContext, it) }
                            val fileMimeType =
                                async { FileCallback.getMimeTypeFromUri(applicationContext, it) }

                            documentViewModel.clear()
                            documentViewModel.add(
                                DocumentModel(
                                    filePath.await(),
                                    fileRealName.await(),
                                    fileGenerateName.await(),
                                    fileSize.await(),
                                    fileMimeType.await()
                                )
                            )
                        } ?: isVisibleProgressBar(false)
                    }
                    Callback.logD(TAG, "Request '$TAG' took $time milliseconds")
                    isVisibleProgressBar(false)
                }
                Callback.logD(TAG, "OpenSingleDocument : $uri")
                Callback.logD(TAG, "readPermissionGranted : $readPermissionGranted")
                Callback.logD(TAG, "writePermissionGranted : $writePermissionGranted")
            }
        }

    /** Observe Data */
    private fun observeData() {
        documentViewModel.mutableLiveListDocument.observe(this, { docList ->
            docList!![0].let { doc ->
                findViewById<TextView>(R.id.tvFilePath).text = doc.filePath
                findViewById<TextView>(R.id.tvFileRealName).text = doc.fileRealName
                findViewById<TextView>(R.id.tvFileGenerateName).text = doc.fileGenerateName
                findViewById<TextView>(R.id.tvFileSize).text = doc.fileSize
                findViewById<TextView>(R.id.tvFileMimeType).text = doc.fileMimeType

                findViewById<ImageView>(R.id.ivRemove).isVisible = true
            }
        })
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOpenSingleDocumentBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.title = "Open Single Document"
        binding.toolbar.setNavigationIcon(R.drawable.ic_round_arrow_back_24)

        /** onRequestPermissions*/
        onRequestPermissions()

        /** Initial ViewModel */
        documentViewModel = ViewModelProvider(this)[DocumentViewModel::class.java]

        findViewById<ImageView>(R.id.ivRemove).isVisible = false
        findViewById<ImageView>(R.id.ivRemove).setOnClickListener { clearDocument() }

        /** Observe Data */
        observeData()

        /** Button Open Single Document */
        binding.btnOpenSingleDocument.setOnClickListener {
            if (readPermissionGranted && writePermissionGranted)
                openSingleDocument()
            else Callback.toast(applicationContext, "External Storage Permission Denied!")

            Callback.logD(
                TAG,
                "btnOpenSingleDocument-readPermissionGranted : $readPermissionGranted"
            )
            Callback.logD(
                TAG,
                "btnOpenSingleDocument-writePermissionGranted : $writePermissionGranted"
            )
        }

    }

    /** Clear View Document */
    private fun clearDocument() {
        findViewById<TextView>(R.id.tvFilePath).text = getString(R.string.file_path)
        findViewById<TextView>(R.id.tvFileRealName).text = getString(R.string.file_real_name)
        findViewById<TextView>(R.id.tvFileGenerateName).text =
            getString(R.string.file_generate_name)
        findViewById<TextView>(R.id.tvFileSize).text = getString(R.string.file_size)
        findViewById<TextView>(R.id.tvFileMimeType).text = getString(R.string.file_mime_type)
        findViewById<ImageView>(R.id.ivRemove).isVisible = false
        Callback.toast(this, "Clear")
    }


    /** Navigation Back */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) finish()
        return super.onOptionsItemSelected(item)
    }
}