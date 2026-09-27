package ai.ritav.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun UiAppearanceSettings(
    uiPreferences: UiPreferences,
    navigationFixed: Boolean,
    onThemeModeChanged: (UiThemeMode) -> Unit,
    onThemeFamilyChanged: (UiThemeFamily) -> Unit,
    motionPreference: UiMotionPreference,
    onMotionPreferenceChanged: (UiMotionPreference) -> Unit,
    onButtonStyleChanged: (UiButtonStyle) -> Unit,
    onNavigationFixedChanged: (Boolean) -> Unit,
    onResetNavigationPosition: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Text("UI & Appearance", style = MaterialTheme.typography.headlineSmall)

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Theme", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                UiThemeMode.values().forEach { mode ->
                    FilterChip(
                        selected = uiPreferences.themeMode == mode,
                        onClick = { onThemeModeChanged(mode) },
                        label = { Text(mode.displayName()) }
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Color theme", style = MaterialTheme.typography.titleMedium)
            UiThemeFamily.values().toList().chunked(2).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row.forEach { family ->
                        FilterChip(
                            selected = uiPreferences.themeFamily == family,
                            onClick = { onThemeFamilyChanged(family) },
                            label = { Text(family.displayName()) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (row.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Animation", style = MaterialTheme.typography.titleMedium)
            Text("Controls non-essential interface motion. Security states remain understandable without animation.")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                UiMotionPreference.values().forEach { preference ->
                    FilterChip(
                        selected = motionPreference == preference,
                        onClick = { onMotionPreferenceChanged(preference) },
                        label = { Text(preference.displayName()) }
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Button style", style = MaterialTheme.typography.titleMedium)
            Text("Choose the shared presentation style for standard Ritav actions.")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                UiButtonStyle.values().forEach { style ->
                    FilterChip(
                        selected = uiPreferences.buttonStyle == style,
                        onClick = { onButtonStyleChanged(style) },
                        label = { Text(style.displayName()) }
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Global navigation", style = MaterialTheme.typography.titleMedium)
            Text(
                if (navigationFixed) {
                    "Fixed: the saved position is locked until you release it."
                } else {
                    "Free: drag the Ritav navigation circle to any safe position."
                }
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = !navigationFixed,
                    onClick = { onNavigationFixedChanged(false) },
                    label = { Text("Free") }
                )
                FilterChip(
                    selected = navigationFixed,
                    onClick = { onNavigationFixedChanged(true) },
                    label = { Text("Fixed") }
                )
            }
            TextButton(onClick = onResetNavigationPosition) {
                Text("Reset navigation position")
            }
        }
    }
}

private fun UiThemeMode.displayName(): String = when (this) {
    UiThemeMode.SYSTEM -> "System"
    UiThemeMode.LIGHT -> "Light"
    UiThemeMode.DARK -> "Dark"
}

private fun UiMotionPreference.displayName(): String = when (this) {
    UiMotionPreference.AUTO -> "Auto"
    UiMotionPreference.ON -> "On"
    UiMotionPreference.OFF -> "Off"
}

private fun UiThemeFamily.displayName(): String = when (this) {
    UiThemeFamily.RITAV_DEFAULT -> "Ritav"
    UiThemeFamily.OCEAN -> "Ocean"
    UiThemeFamily.EMERALD -> "Emerald"
    UiThemeFamily.VIOLET -> "Violet"
    UiThemeFamily.SUNSET -> "Sunset"
    UiThemeFamily.GRAPHITE -> "Graphite"
}
