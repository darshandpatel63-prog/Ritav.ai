package ai.ritav.app.platform

import android.content.Context
import android.content.Intent

/** Host-side launcher for an exact package selected by trusted security configuration. */
internal interface AndroidAppLaunchDispatcher {
    fun dispatchLaunch(packageName: String): Boolean
}

internal class ContextAndroidAppLaunchDispatcher(
    private val context: Context
) : AndroidAppLaunchDispatcher {
    override fun dispatchLaunch(packageName: String): Boolean {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return false
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        return true
    }
}
