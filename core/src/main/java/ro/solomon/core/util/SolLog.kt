package ro.solomon.core.util

import android.util.Log

object SolLog {
    private const val SUBSYSTEM = "ro.solomon.app"

    fun d(category: String, message: String) {
        Log.d("$SUBSYSTEM.$category", message)
    }

    fun i(category: String, message: String) {
        Log.i("$SUBSYSTEM.$category", message)
    }

    fun w(category: String, message: String) {
        Log.w("$SUBSYSTEM.$category", message)
    }

    fun e(category: String, message: String, throwable: Throwable? = null) {
        Log.e("$SUBSYSTEM.$category", message, throwable)
    }

    object Categories {
        const val ONBOARDING = "Onboarding"
        const val PERSISTENCE = "Persistence"
        const val LLM = "LLM"
        const val MOMENTS = "Moments"
        const val INGESTION = "Ingestion"
        const val BG_TASK = "BackgroundTask"
        const val DOWNLOAD = "ModelDownload"
        const val APP = "App"
    }
}
