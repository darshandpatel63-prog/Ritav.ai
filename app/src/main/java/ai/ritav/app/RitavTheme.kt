package ai.ritav.app

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private fun lightColors(family: UiThemeFamily) = when (family) {
    UiThemeFamily.RITAV_DEFAULT -> lightColorScheme(
        primary = Color(0xFF315C9B), secondary = Color(0xFF50637F), tertiary = Color(0xFF66558F)
    )
    UiThemeFamily.OCEAN -> lightColorScheme(
        primary = Color(0xFF006874), secondary = Color(0xFF4A6267), tertiary = Color(0xFF3F5F90)
    )
    UiThemeFamily.EMERALD -> lightColorScheme(
        primary = Color(0xFF006C4C), secondary = Color(0xFF4D6358), tertiary = Color(0xFF3D644F)
    )
    UiThemeFamily.VIOLET -> lightColorScheme(
        primary = Color(0xFF6C3F92), secondary = Color(0xFF635A6D), tertiary = Color(0xFF7A5269)
    )
    UiThemeFamily.SUNSET -> lightColorScheme(
        primary = Color(0xFF9D4300), secondary = Color(0xFF765947), tertiary = Color(0xFF735C2A)
    )
    UiThemeFamily.GRAPHITE -> lightColorScheme(
        primary = Color(0xFF535353), secondary = Color(0xFF5E5E5E), tertiary = Color(0xFF5D5D65)
    )
}.copy(
    background = Color(0xFFF8F9FD),
    surface = Color(0xFFF8F9FD)
)

private fun darkColors(family: UiThemeFamily) = when (family) {
    UiThemeFamily.RITAV_DEFAULT -> darkColorScheme(
        primary = Color(0xFFA9C7FF), secondary = Color(0xFFB9C7E2), tertiary = Color(0xFFD0BCFF)
    )
    UiThemeFamily.OCEAN -> darkColorScheme(
        primary = Color(0xFF4FD8E8), secondary = Color(0xFFB1CBD0), tertiary = Color(0xFFA8C7FA)
    )
    UiThemeFamily.EMERALD -> darkColorScheme(
        primary = Color(0xFF70DBB0), secondary = Color(0xFFB1CCBD), tertiary = Color(0xFFA4D0B1)
    )
    UiThemeFamily.VIOLET -> darkColorScheme(
        primary = Color(0xFFD8B6FF), secondary = Color(0xFFCBC0D0), tertiary = Color(0xFFFFB0CB)
    )
    UiThemeFamily.SUNSET -> darkColorScheme(
        primary = Color(0xFFFFB783), secondary = Color(0xFFE2C2AE), tertiary = Color(0xFFE1C58A)
    )
    UiThemeFamily.GRAPHITE -> darkColorScheme(
        primary = Color(0xFFC8C6C6), secondary = Color(0xFFC5C2C2), tertiary = Color(0xFFBFC2CC)
    )
}.copy(
    background = Color(0xFF101318),
    surface = Color(0xFF101318)
)

@Composable
internal fun RitavTheme(
    preferences: UiPreferences,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (preferences.themeMode) {
        UiThemeMode.SYSTEM -> systemDark
        UiThemeMode.LIGHT -> false
        UiThemeMode.DARK -> true
    }

    MaterialTheme(
        colorScheme = if (darkTheme) {
            darkColors(preferences.themeFamily)
        } else {
            lightColors(preferences.themeFamily)
        },
        content = content
    )
}
