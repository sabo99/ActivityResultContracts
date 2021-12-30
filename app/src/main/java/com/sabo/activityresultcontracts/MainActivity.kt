package com.sabo.activityresultcontracts

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.sabo.activityresultcontracts.cropimage.CropImageGalleryCameraActivity
import com.sabo.activityresultcontracts.databinding.ActivityMainBinding
import com.sabo.activityresultcontracts.document.OpenMultipleDocumentsActivity
import com.sabo.activityresultcontracts.document.OpenSingleDocumentActivity


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar!!.title = getString(R.string.app_name)

        binding.btnCropImageActivity.setOnClickListener {
            startActivity(Intent(applicationContext, CropImageGalleryCameraActivity::class.java))
        }
        binding.btnOpenSingleDocumentActivity.setOnClickListener {
            startActivity(Intent(applicationContext, OpenSingleDocumentActivity::class.java))
        }
        binding.btnOpenMultipleDocumentActivity.setOnClickListener {
            startActivity(Intent(applicationContext, OpenMultipleDocumentsActivity::class.java))
        }
    }
}