package com.sabo.activityresultcontracts.document

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.sabo.activityresultcontracts.Callback
import com.sabo.activityresultcontracts.FileCallback
import com.sabo.activityresultcontracts.R
import com.sabo.activityresultcontracts.databinding.ActivityOpenMultipleDocumentsBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlin.system.measureTimeMillis

class OpenMultipleDocumentsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOpenMultipleDocumentsBinding
    private lateinit var documentViewModel: DocumentViewModel

    companion object {
        private const val TAG = "OpenMultipleDocuments"
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
            readPermissionGranted =
                permissions[Manifest.permission.READ_EXTERNAL_STORAGE]
                    ?: readPermissionGranted
            writePermissionGranted =
                permissions[Manifest.permission.WRITE_EXTERNAL_STORAGE]
                    ?: writePermissionGranted

            Callback.logD(
                TAG,
                "requestMultiplePermissions : $permissions"
            )
        }

    /** Open Multiple Documents */
    private fun openMultipleDocuments() {
        isVisibleProgressBar(true)
        selectMultipleDocumentsLauncher.launch(arrayOf("application/*"))
    }

    private fun isVisibleProgressBar(isVisible: Boolean) {
        binding.progressBar.isVisible = isVisible
    }

    /** Select Multiple Documents */
    private val selectMultipleDocumentsLauncher =
        registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uriList ->
            if (readPermissionGranted && writePermissionGranted) {
                GlobalScope.launch(Dispatchers.Main) {
                    val time = measureTimeMillis {
                        uriList?.let {
                            Log.d(TAG, "uriList: $uriList")
                            for (i in uriList.indices) {
                                val filePath = async {
                                    FileCallback.getFilePathFromUri(applicationContext, it[i])
                                }
                                val fileRealName = async {
                                    FileCallback.getFileRealNameFromUri(it[i])
                                }
                                val fileGenerateName = async {
                                    FileCallback.getFileGenerateNameFromUri(it[i])
                                }
                                val fileSize = async {
                                    FileCallback.getFileSizeFromUri(applicationContext, it[i])
                                }
                                val fileMimeType = async {
                                    FileCallback.getMimeTypeFromUri(applicationContext, it[i])
                                }

                                documentViewModel.add(
                                    DocumentModel(
                                        filePath.await(),
                                        fileRealName.await(),
                                        fileGenerateName.await(),
                                        fileSize.await(),
                                        fileMimeType.await()
                                    )
                                )
                                binding.rvItemFileDocuments.adapter?.notifyDataSetChanged()
                            }
                        } ?: isVisibleProgressBar(false)
                    }
                    isVisibleProgressBar(false)
                    Callback.logD(TAG, "Result '$TAG' took $time milliseconds")
                }
                Callback.logD(TAG, "OpenMultipleDocuments : $uriList")
                Callback.logD(TAG, "readPermissionGranted : $readPermissionGranted")
                Callback.logD(TAG, "writePermissionGranted : $writePermissionGranted")
            }
        }

    /** Observe Data */
    private fun observeData() {
        documentViewModel.mutableLiveListDocument.observe(this) { list ->
            list?.let {
                binding.rvItemFileDocuments.adapter = DocumentAdapter(documentViewModel, it)
                binding.constraintLayout.isVisible = false
            }
            binding.constraintLayout.isVisible = true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOpenMultipleDocumentsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.title = "Open Multiple Documents"
        binding.toolbar.setNavigationIcon(R.drawable.ic_round_arrow_back_24)

        binding.rvItemFileDocuments.layoutManager = LinearLayoutManager(this)
        findViewById<ImageView>(R.id.ivRemove).isVisible = false

        /** onRequestPermissions*/
        onRequestPermissions()

        /** Initial ViewModel */
        documentViewModel = ViewModelProvider(this)[DocumentViewModel::class.java]

        /** Observe Data */
        observeData()

        /** Button Open Multiple Documents */
        binding.btnOpenMultipleDocuments.setOnClickListener {
            if (readPermissionGranted && writePermissionGranted)
                openMultipleDocuments()
            else Callback.toast(applicationContext, "External Storage Permission Denied!")


            Callback.logD(
                TAG,
                "btnOpenMultipleDocuments-readPermissionGranted : $readPermissionGranted"
            )
            Callback.logD(
                TAG,
                "btnOpenMultipleDocuments-writePermissionGranted : $writePermissionGranted"
            )
        }
    }

    /** Navigation Back */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) finish()
        return super.onOptionsItemSelected(item)
    }
}