package ai.ritav.app

import android.content.Context

internal data class NavigationPositionPreference(
    val xFraction: Float = 0.5f,
    val yFraction: Float = 0.5f,
    val fixed: Boolean = false
)

internal class NavigationPositionStore(context: Context) {
    private val preferences = context.getSharedPreferences("ritav_ui_preferences", Context.MODE_PRIVATE)

    fun load(): NavigationPositionPreference {
        return NavigationPositionPreference(
            xFraction = preferences.getFloat(KEY_X, 0.5f).coerceIn(0f, 1f),
            yFraction = preferences.getFloat(KEY_Y, 0.5f).coerceIn(0f, 1f),
            fixed = preferences.getBoolean(KEY_FIXED, false)
        )
    }

    fun savePosition(xFraction: Float, yFraction: Float) {
        preferences.edit()
            .putFloat(KEY_X, xFraction.coerceIn(0f, 1f))
            .putFloat(KEY_Y, yFraction.coerceIn(0f, 1f))
            .apply()
    }

    fun setFixed(fixed: Boolean) {
        preferences.edit().putBoolean(KEY_FIXED, fixed).apply()
    }

    fun resetPosition() {
        preferences.edit()
            .putFloat(KEY_X, 0.5f)
            .putFloat(KEY_Y, 0.5f)
            .apply()
    }

    private companion object {
        const val KEY_X = "global_nav_x_fraction"
        const val KEY_Y = "global_nav_y_fraction"
        const val KEY_FIXED = "global_nav_fixed"
    }
}
