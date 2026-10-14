package ai.ritav.app

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test

class UiPreferencesStoreTest {
    @Test
    fun settings_survive_store_recreation_and_invalid_values_fail_back_to_defaults() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val preferencesName = "ritav_ui_preferences_store_test"
        val preferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
        preferences.edit().clear().commit()

        try {
            val firstStore = UiPreferencesStore(context, preferencesName)
            firstStore.saveThemeMode(UiThemeMode.DARK)
            firstStore.saveThemeFamily(UiThemeFamily.OCEAN)
            firstStore.saveMotionPreference(UiMotionPreference.OFF)
            firstStore.saveButtonStyle(UiButtonStyle.OUTLINED)

            assertEquals(
                UiPreferences(
                    themeMode = UiThemeMode.DARK,
                    themeFamily = UiThemeFamily.OCEAN,
                    motionPreference = UiMotionPreference.OFF,
                    buttonStyle = UiButtonStyle.OUTLINED
                ),
                UiPreferencesStore(context, preferencesName).load()
            )

            preferences.edit().putString("theme_mode", "NOT_A_THEME").commit()
            assertEquals(UiThemeMode.SYSTEM, UiPreferencesStore(context, preferencesName).load().themeMode)
        } finally {
            preferences.edit().clear().commit()
        }
    }
}
