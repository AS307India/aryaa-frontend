package com.as307.aryaa.util

object TestEnv {
    var isUnderTest: Boolean = false

    fun logDebug(tag: String, msg: String) {
        try {
            android.util.Log.d(tag, msg)
        } catch (e: Throwable) {
            println("$tag: $msg")
        }
    }

    fun logError(tag: String, msg: String, tr: Throwable? = null) {
        try {
            if (tr != null) {
                android.util.Log.e(tag, msg, tr)
            } else {
                android.util.Log.e(tag, msg)
            }
        } catch (e: Throwable) {
            System.err.println("$tag ERROR: $msg ${tr?.message ?: ""}")
        }
    }
}
