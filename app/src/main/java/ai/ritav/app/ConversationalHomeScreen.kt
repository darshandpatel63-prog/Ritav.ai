package ai.ritav.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import ai.ritav.app.core.security.SecurityControlPort

internal data class ConversationMessage(
    val text: String,
    val fromUser: Boolean
)

@Composable
internal fun ConversationalHomeScreen(
    securityControl: SecurityControlPort,
    buttonStyle: UiButtonStyle,
    onOpenSecurityCenter: () -> Unit
) {
    var input by remember { mutableStateOf("") }
    var stopped by remember { mutableStateOf(securityControl.isEmergencyStopActive()) }
    val messages = remember {
        mutableStateListOf(
            ConversationMessage(
                "Ritav is ready. The secure model provider is not connected yet, so I will not pretend to generate an AI answer.",
                false
            )
        )
    }

    fun send() {
        val text = input.trim()
        if (text.isEmpty()) return
        input = ""
        messages += ConversationMessage(text, true)
        messages += ConversationMessage(
            if (stopped || securityControl.isEmergencyStopActive()) {
                "Emergency Stop is active. The request was not sent to any execution path."
            } else {
                "The conversational UI received your message, but the production model runtime is currently unavailable. No model/provider bypass was attempted."
            },
            false
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Ritav.ai", style = MaterialTheme.typography.headlineSmall)
                Text(
                    if (stopped) "Emergency Stop active" else "Secure local-first assistant",
                    style = MaterialTheme.typography.bodyMedium
                )
                RitavStatusChip(
                    label = if (stopped) "Emergency Stop" else "Security Active",
                    tone = if (stopped) RitavStatusTone.ERROR else RitavStatusTone.PROTECTED,
                    accessibleDescription = if (stopped) {
                        "Emergency Stop is active; protected actions are blocked."
                    } else {
                        "Security boundary is active."
                    }
                )
                RitavStatusChip(
                    label = "Model unavailable",
                    tone = RitavStatusTone.WARNING,
                    accessibleDescription = "Production model runtime is unavailable; no model response has been generated."
                )
            }
            RitavButton(
                style = buttonStyle,
                label = "Security",
                onClick = onOpenSecurityCenter
            )
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    liveRegion = LiveRegionMode.Polite
                    contentDescription =
                        if (stopped) {
                            "Security status: Emergency Stop active. Protected actions are blocked."
                        } else {
                            "Security status: active. AI output does not authorize or execute actions."
                        }
                },
            colors = CardDefaults.cardColors(
                containerColor = if (stopped) {
                    MaterialTheme.colorScheme.errorContainer
                } else {
                    MaterialTheme.colorScheme.secondaryContainer
                }
            )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    if (stopped) "Protected actions are blocked." else "Security boundary active.",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(4.dp))
                Text("AI output never authorizes or executes an action directly.")
            }
        }

        RitavFeedbackCard(
            title = "Model runtime unavailable",
            message = "No model response was generated. You can continue using the available local interface, or revisit this screen when an approved model runtime is connected.",
            tone = RitavFeedbackTone.WARNING
        )

        RitavLiveTaskPanel(
            state = if (stopped) {
                RitavTaskUiState.STOPPED
            } else {
                RitavTaskUiState.IDLE
            },
            summary = if (stopped) {
                "Emergency Stop is active. No protected task execution is available."
            } else {
                "Live progress will appear here only when an authoritative task runtime becomes available."
            }
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages) { message ->
                val label = if (message.fromUser) "You" else "Ritav"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (message.fromUser) {
                        Arrangement.End
                    } else {
                        Arrangement.Start
                    }
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(0.86f)
                            .semantics {
                                contentDescription = label + " message: " + message.text
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (message.fromUser) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            }
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(text = message.text)
                        }
                    }
                }
            }
        }

        OutlinedTextField(
            value = input,
            onValueChange = { input = it.take(4096) },
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = "Message Ritav input"
                },
            enabled = !stopped,
            minLines = 1,
            maxLines = 5,
            label = { Text("Message Ritav") }
        )

        RitavButton(
            style = buttonStyle,
            label = "Send",
            onClick = ::send,
            enabled = input.isNotBlank() && !stopped,
            modifier = Modifier.fillMaxWidth()
        )

        RitavButton(
            style = buttonStyle,
            label = "Emergency Stop",
            onClick = {
                securityControl.activateEmergencyStop()
                stopped = true
            },
            enabled = !stopped,
            destructive = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
