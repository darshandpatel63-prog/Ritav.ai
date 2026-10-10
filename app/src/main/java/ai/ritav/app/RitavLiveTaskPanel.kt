package ai.ritav.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

internal enum class RitavTaskUiState {
    IDLE,
    BLOCKED,
    WAITING_FOR_CONFIRMATION,
    EXECUTING,
    VERIFYING,
    COMPLETED,
    FAILED_SAFELY,
    STOPPED
}

@Composable
internal fun RitavLiveTaskPanel(
    state: RitavTaskUiState,
    taskName: String? = null,
    currentStep: String? = null,
    summary: String? = null,
    buttonStyle: UiButtonStyle = UiButtonStyle.OUTLINED,
    onEmergencyStop: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val title = when (state) {
        RitavTaskUiState.IDLE -> "No active task"
        RitavTaskUiState.BLOCKED -> "Task blocked"
        RitavTaskUiState.WAITING_FOR_CONFIRMATION -> "Waiting for confirmation"
        RitavTaskUiState.EXECUTING -> "Task executing"
        RitavTaskUiState.VERIFYING -> "Verifying result"
        RitavTaskUiState.COMPLETED -> "Task completed"
        RitavTaskUiState.FAILED_SAFELY -> "Task failed safely"
        RitavTaskUiState.STOPPED -> "Task stopped"
    }

    val tone = when (state) {
        RitavTaskUiState.IDLE -> RitavStatusTone.NORMAL
        RitavTaskUiState.BLOCKED,
        RitavTaskUiState.WAITING_FOR_CONFIRMATION -> RitavStatusTone.WARNING
        RitavTaskUiState.EXECUTING,
        RitavTaskUiState.VERIFYING,
        RitavTaskUiState.COMPLETED -> RitavStatusTone.PROTECTED
        RitavTaskUiState.FAILED_SAFELY,
        RitavTaskUiState.STOPPED -> RitavStatusTone.ERROR
    }

    val accessibleState = buildString {
        append(title)
        taskName?.takeIf { it.isNotBlank() }?.let { append(": ").append(it) }
        currentStep?.takeIf { it.isNotBlank() }?.let { append(". Current step: ").append(it) }
        summary?.takeIf { it.isNotBlank() }?.let { append(". ").append(it) }
    }

    val containerColor = when (tone) {
        RitavStatusTone.ERROR -> MaterialTheme.colorScheme.errorContainer
        RitavStatusTone.WARNING -> MaterialTheme.colorScheme.tertiaryContainer
        RitavStatusTone.PROTECTED -> MaterialTheme.colorScheme.primaryContainer
        RitavStatusTone.NORMAL -> MaterialTheme.colorScheme.surfaceVariant
    }

    val contentColor = when (tone) {
        RitavStatusTone.ERROR -> MaterialTheme.colorScheme.onErrorContainer
        RitavStatusTone.WARNING -> MaterialTheme.colorScheme.onTertiaryContainer
        RitavStatusTone.PROTECTED -> MaterialTheme.colorScheme.onPrimaryContainer
        RitavStatusTone.NORMAL -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = accessibleState
                liveRegion = LiveRegionMode.Polite
            },
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Live task",
                    style = MaterialTheme.typography.titleMedium,
                    color = contentColor
                )
                RitavStatusChip(
                    label = title,
                    tone = tone,
                    accessibleDescription = accessibleState
                )
            }

            taskName?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyLarge,
                    color = contentColor
                )
            }

            currentStep?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = "Current step: $it",
                    style = MaterialTheme.typography.bodyMedium,
                    color = contentColor
                )
            }

            summary?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = contentColor
                )
            }

            if (state == RitavTaskUiState.IDLE) {
                Text(
                    text = "No task is currently being executed. Ritav will only show live progress when an authoritative runtime state exists.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = contentColor
                )
            }

            if (
                onEmergencyStop != null &&
                (state == RitavTaskUiState.EXECUTING || state == RitavTaskUiState.VERIFYING)
            ) {
                RitavButton(
                    style = buttonStyle,
                    label = "Emergency Stop",
                    destructive = true,
                    onClick = onEmergencyStop
                )
            }
        }
    }
}
