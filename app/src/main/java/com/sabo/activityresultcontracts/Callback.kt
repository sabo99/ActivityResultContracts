package com.sabo.activityresultcontracts

import android.content.Context
import android.util.Log
import android.widget.Toast

class Callback  {
    companion object {
        fun toast(context: Context, message: String){
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }

        fun logD(TAG: String, message: String) {
            Log.d(TAG, message)
            Log.d(TAG, "==============================================================")
        }
    }
}