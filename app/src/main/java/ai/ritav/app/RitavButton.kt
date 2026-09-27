package ai.ritav.app

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
internal fun RitavButton(
    style: UiButtonStyle,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    destructive: Boolean = false,
    content: (@Composable RowScope.() -> Unit)? = null
) {
    val contentBlock: @Composable RowScope.() -> Unit = content ?: { Text(label) }

    when (style) {
        UiButtonStyle.FILLED -> Button(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            colors = if (destructive) {
                ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            } else {
                ButtonDefaults.buttonColors()
            },
            content = contentBlock
        )
        UiButtonStyle.TONAL -> FilledTonalButton(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            colors = if (destructive) {
                ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            } else {
                ButtonDefaults.filledTonalButtonColors()
            },
            content = contentBlock
        )
        UiButtonStyle.OUTLINED -> OutlinedButton(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            colors = if (destructive) {
                ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            } else {
                ButtonDefaults.outlinedButtonColors()
            },
            content = contentBlock
        )
        UiButtonStyle.MINIMAL -> TextButton(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            colors = if (destructive) {
                ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            } else {
                ButtonDefaults.textButtonColors()
            },
            content = contentBlock
        )
    }
}

internal fun UiButtonStyle.displayName(): String = when (this) {
    UiButtonStyle.FILLED -> "Filled"
    UiButtonStyle.TONAL -> "Tonal"
    UiButtonStyle.OUTLINED -> "Outlined"
    UiButtonStyle.MINIMAL -> "Minimal"
}
