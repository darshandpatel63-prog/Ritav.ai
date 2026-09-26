package ai.ritav.app

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val RitavLightColors = lightColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFF315C9B),
    secondary = androidx.compose.ui.graphics.Color(0xFF50637F),
    tertiary = androidx.compose.ui.graphics.Color(0xFF66558F),
    background = androidx.compose.ui.graphics.Color(0xFFF8F9FD),
    surface = androidx.compose.ui.graphics.Color(0xFFF8F9FD),
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFFE1E5EC),
    error = androidx.compose.ui.graphics.Color(0xFFBA1A1A)
)

private val RitavDarkColors = darkColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFFA9C7FF),
    secondary = androidx.compose.ui.graphics.Color(0xFFB9C7E2),
    tertiary = androidx.compose.ui.graphics.Color(0xFFD0BCFF),
    background = androidx.compose.ui.graphics.Color(0xFF101318),
    surface = androidx.compose.ui.graphics.Color(0xFF101318),
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFF42474F),
    error = androidx.compose.ui.graphics.Color(0xFFFFB4AB)
)

@Composable
internal fun RitavTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) RitavDarkColors else RitavLightColors,
        content = content
    )
}
