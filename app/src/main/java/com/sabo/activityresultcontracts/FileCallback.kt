package com.sabo.activityresultcontracts

import android.content.Context
import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import java.io.*
import java.util.*
import kotlin.math.roundToInt

class FileCallback
{
    companion object {
        /**
         * ----------------------------
         * File Utils
         * ----------------------------
         * */
        fun getFilePathFromUri(context: Context, uri: Uri?): String? {
            if (uri == null) return null
            val fileName = getFileName(uri)
            val dir = context.getExternalFilesDir("$uri")

            if (fileName.isNotEmpty()) {
                val copyFile = File(dir.toString() + File.separator + fileName)
                copy(context, uri, copyFile)
                return copyFile.absolutePath
            }
            return null
        }

        private fun getFileName(uri: Uri?): String {
            var fileName = ""
            try {
                val path = uri?.path
                fileName = path?.substring(path.lastIndexOf('/') + 1) ?: "unknown"
            } catch (e: Exception) {
                e.printStackTrace()
            }

            return fileName
        }

        private fun copy(context: Context, uri: Uri?, copyFile: File) {
            try {
                val inputStream = uri?.let { context.contentResolver.openInputStream(it) } ?: return
                val outputStream: OutputStream = FileOutputStream(copyFile)
                copyStream(inputStream, outputStream)
                inputStream.close()
                outputStream.close()
            } catch (e: IOException) {
                e.printStackTrace()
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
        }

        private fun copyStream(inputStream: InputStream, outputStream: OutputStream): Int {
            val bufferSize = 1024 * 10240
            val buffer = ByteArray(bufferSize)

            val `in` = BufferedInputStream(inputStream, bufferSize)
            val out = BufferedOutputStream(outputStream, bufferSize)
            var count = 0
            var nC: Int
            try {
                while (`in`.read(buffer, 0, bufferSize).also { nC = it } != -1) {
                    out.write(buffer, 0, nC)
                    count += nC
                }
                out.flush()
            } finally {
                try {
                    out.close()
                } catch (e: IOException) {
                    Log.e(e.message, e.toString())
                }
                try {
                    `in`.close()
                } catch (e: IOException) {
                    Log.e(e.message, e.toString())
                }
            }
            return count
        }

        /** Get File Real Name  */
        fun getFileRealNameFromUri(uri: Uri?): String? {
            if (uri == null) return null
            var fileName: String? = null
            try {
                val path = uri.path.toString()
                fileName = path.substring(path.lastIndexOf('/') + 1)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return fileName
        }

        /** Get File Generate Name */
        fun getFileGenerateNameFromUri(uri: Uri?): String? {
            if (uri == null) return null
            return UUID.randomUUID().toString()
        }

        /** Get File Size  */
        fun getFileSizeFromUri(context: Context, uri: Uri?): String? {
            if (uri == null) return null
            return File("${getFilePathFromUri(context, uri)}").length().div(1000).toDouble()
                .roundToInt().toString() + " kB"
        }


        /** Get File Extension  */
        fun getMimeTypeFromUri(context: Context, uri: Uri?): String? {
            if (uri == null) return null
            return MimeTypeMap.getSingleton()
                .getExtensionFromMimeType(context.contentResolver.getType(uri))
        }
    }
}