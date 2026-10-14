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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

internal enum class RitavFeedbackTone {
    INFO,
    WARNING,
    ERROR
}

@Composable
internal fun RitavFeedbackCard(
    title: String,
    message: String,
    tone: RitavFeedbackTone,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    val statusTone = when (tone) {
        RitavFeedbackTone.INFO -> RitavStatusTone.NORMAL
        RitavFeedbackTone.WARNING -> RitavStatusTone.WARNING
        RitavFeedbackTone.ERROR -> RitavStatusTone.ERROR
    }

    val containerColor = when (tone) {
        RitavFeedbackTone.INFO -> MaterialTheme.colorScheme.surfaceVariant
        RitavFeedbackTone.WARNING -> MaterialTheme.colorScheme.tertiaryContainer
        RitavFeedbackTone.ERROR -> MaterialTheme.colorScheme.errorContainer
    }

    val contentColor = when (tone) {
        RitavFeedbackTone.INFO -> MaterialTheme.colorScheme.onSurfaceVariant
        RitavFeedbackTone.WARNING -> MaterialTheme.colorScheme.onTertiaryContainer
        RitavFeedbackTone.ERROR -> MaterialTheme.colorScheme.onErrorContainer
    }

    val accessibleMessage = "$title. $message"

    Card(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                liveRegion = LiveRegionMode.Polite
                contentDescription = accessibleMessage
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
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = contentColor
                )
                RitavStatusChip(
                    label = when (tone) {
                        RitavFeedbackTone.INFO -> "Info"
                        RitavFeedbackTone.WARNING -> "Attention"
                        RitavFeedbackTone.ERROR -> "Action needed"
                    },
                    tone = statusTone,
                    accessibleDescription = accessibleMessage
                )
            }

            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor
            )

            if (actionLabel != null && onAction != null) {
                RitavButton(
                    style = UiButtonStyle.MINIMAL,
                    label = actionLabel,
                    onClick = onAction
                )
            }
        }
    }
}
