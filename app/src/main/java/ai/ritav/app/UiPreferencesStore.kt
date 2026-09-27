package ai.ritav.app

import android.content.Context

internal enum class UiThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

internal enum class UiMotionPreference {
    AUTO,
    ON,
    OFF
}

internal enum class UiThemeFamily {
    RITAV_DEFAULT,
    OCEAN,
    EMERALD,
    VIOLET,
    SUNSET,
    GRAPHITE
}

internal data class UiPreferences(
    val themeMode: UiThemeMode = UiThemeMode.SYSTEM,
    val themeFamily: UiThemeFamily = UiThemeFamily.RITAV_DEFAULT,
    val motionPreference: UiMotionPreference = UiMotionPreference.AUTO
)

internal class UiPreferencesStore(context: Context) {
    private val preferences = context.getSharedPreferences("ritav_ui_preferences", Context.MODE_PRIVATE)

    fun load(): UiPreferences {
        val mode = runCatching {
            UiThemeMode.valueOf(preferences.getString(KEY_THEME_MODE, UiThemeMode.SYSTEM.name) ?: UiThemeMode.SYSTEM.name)
        }.getOrDefault(UiThemeMode.SYSTEM)
        val family = runCatching {
            UiThemeFamily.valueOf(
                preferences.getString(KEY_THEME_FAMILY, UiThemeFamily.RITAV_DEFAULT.name)
                    ?: UiThemeFamily.RITAV_DEFAULT.name
            )
        }.getOrDefault(UiThemeFamily.RITAV_DEFAULT)
        val motion = runCatching {
            UiMotionPreference.valueOf(
                preferences.getString(KEY_MOTION, UiMotionPreference.AUTO.name) ?: UiMotionPreference.AUTO.name
            )
        }.getOrDefault(UiMotionPreference.AUTO)
        return UiPreferences(mode, family, motion)
    }

    fun saveThemeMode(mode: UiThemeMode) {
        preferences.edit().putString(KEY_THEME_MODE, mode.name).apply()
    }

    fun saveThemeFamily(family: UiThemeFamily) {
        preferences.edit().putString(KEY_THEME_FAMILY, family.name).apply()
    }

    fun saveMotionPreference(preference: UiMotionPreference) {
        preferences.edit().putString(KEY_MOTION, preference.name).apply()
    }

    private companion object {
        const val KEY_THEME_MODE = "theme_mode"
        const val KEY_THEME_FAMILY = "theme_family"
        const val KEY_MOTION = "motion_preference"
    }
}
