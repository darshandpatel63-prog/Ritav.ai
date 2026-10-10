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

internal enum class UiButtonStyle {
    FILLED,
    TONAL,
    OUTLINED,
    MINIMAL
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
    val motionPreference: UiMotionPreference = UiMotionPreference.AUTO,
    val buttonStyle: UiButtonStyle = UiButtonStyle.FILLED
)

internal class UiPreferencesStore(
    context: Context,
    preferencesName: String = "ritav_ui_preferences"
) {
    private val preferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)

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
        val buttonStyle = runCatching {
            UiButtonStyle.valueOf(
                preferences.getString(KEY_BUTTON_STYLE, UiButtonStyle.FILLED.name) ?: UiButtonStyle.FILLED.name
            )
        }.getOrDefault(UiButtonStyle.FILLED)
        return UiPreferences(mode, family, motion, buttonStyle)
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

    fun saveButtonStyle(style: UiButtonStyle) {
        preferences.edit().putString(KEY_BUTTON_STYLE, style.name).apply()
    }

    private companion object {
        const val KEY_THEME_MODE = "theme_mode"
        const val KEY_THEME_FAMILY = "theme_family"
        const val KEY_MOTION = "motion_preference"
        const val KEY_BUTTON_STYLE = "button_style"
    }
}
