package com.mulkallah.aircontrole.core

import android.util.Log

/** Single logcat tag for the end-to-end gesture pipeline. Filter with `AirControle`. */
object AirControleLog {
    const val TAG = "AirControle"

    fun d(message: String) {
        Log.d(TAG, message)
    }

    fun i(message: String) {
        Log.i(TAG, message)
    }

    fun w(message: String, error: Throwable? = null) {
        if (error == null) {
            Log.w(TAG, message)
        } else {
            Log.w(TAG, message, error)
        }
    }

    fun e(message: String, error: Throwable? = null) {
        if (error == null) {
            Log.e(TAG, message)
        } else {
            Log.e(TAG, message, error)
        }
    }
}
