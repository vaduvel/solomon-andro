package ro.solomon.app.services

import android.content.Context
import android.content.Intent
import android.net.Uri

object CSALBDeeplink {
    const val MAIN_URL = "https://www.csalb.ro"
    const val START_PROCEDURE_URL = "https://www.csalb.ro/incepe-procedura"
    const val INFO_URL = "https://www.csalb.ro/despre-csalb"

    fun openStartProcedure(context: Context) {
        openUrl(context, START_PROCEDURE_URL)
    }

    fun openMainSite(context: Context) {
        openUrl(context, MAIN_URL)
    }

    fun openInfo(context: Context) {
        openUrl(context, INFO_URL)
    }

    private fun openUrl(context: Context, url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}
