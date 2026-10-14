package ai.ritav.app

import android.content.Context

internal class OnboardingStore(context: Context) {
    private val preferences = context.getSharedPreferences(
        "ritav_ui_preferences",
        Context.MODE_PRIVATE
    )

    fun isCompleted(): Boolean =
        preferences.getBoolean(KEY_COMPLETED, false)

    fun markCompleted() {
        preferences.edit().putBoolean(KEY_COMPLETED, true).apply()
    }

    private companion object {
        const val KEY_COMPLETED = "onboarding_completed"
    }
}
