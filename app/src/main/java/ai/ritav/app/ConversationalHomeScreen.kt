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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import ai.ritav.app.core.security.SecurityControlPort
import ai.ritav.app.core.security.TaskRuntimeSnapshot
import ai.ritav.app.core.security.TaskRuntimeState

internal data class ConversationMessage(
    val text: String,
    val fromUser: Boolean
)

internal const val MAX_CONVERSATION_MESSAGES = 100
internal const val MAX_CONVERSATION_INPUT_LENGTH = 4096
internal const val MAX_CONVERSATION_MESSAGE_LENGTH = 8192

private fun boundConversationMessage(text: String): String =
    text.take(MAX_CONVERSATION_MESSAGE_LENGTH)

internal fun appendConversationMessages(
    messages: MutableList<ConversationMessage>,
    userText: String,
    assistantText: String
) {
    messages += ConversationMessage(boundConversationMessage(userText), fromUser = true)
    messages += ConversationMessage(boundConversationMessage(assistantText), fromUser = false)
    val overflow = messages.size - MAX_CONVERSATION_MESSAGES
    if (overflow > 0) {
        repeat(overflow) { messages.removeAt(0) }
    }
}

@Composable
internal fun ConversationalHomeScreen(
    securityControl: SecurityControlPort,
    buttonStyle: UiButtonStyle,
    taskSnapshot: TaskRuntimeSnapshot,
    onOpenSecurityCenter: () -> Unit,
    onEmergencyStop: () -> Unit
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

    var messageRevision by remember { mutableStateOf(0) }
    val conversationListState = rememberLazyListState()
    LaunchedEffect(messageRevision) {
        // The header is item 0; message rows follow it.
        // A revision counter continues changing after bounded history reaches 100.
        if (messageRevision > 0 && messages.isNotEmpty()) {
            conversationListState.scrollToItem(messages.size)
        }
    }

    fun send() {
        val text = input.trim()
        if (text.isEmpty()) return
        input = ""
        val assistantText = if (stopped || securityControl.isEmergencyStopActive()) {
            "Emergency Stop is active. The request was not sent to any execution path."
        } else {
            "The conversational UI received your message, but the production model runtime is currently unavailable. No model/provider bypass was attempted."
        }
        appendConversationMessages(messages, text, assistantText)
        messageRevision++
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        LazyColumn(
            state = conversationListState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item(key = "home-context") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
            state = when {
                stopped -> RitavTaskUiState.STOPPED
                else -> when (taskSnapshot.state) {
                    TaskRuntimeState.IDLE -> RitavTaskUiState.IDLE
                    TaskRuntimeState.BLOCKED -> RitavTaskUiState.BLOCKED
                    TaskRuntimeState.EXECUTING -> RitavTaskUiState.EXECUTING
                    TaskRuntimeState.VERIFYING -> RitavTaskUiState.VERIFYING
                    TaskRuntimeState.COMPLETED -> RitavTaskUiState.COMPLETED
                    TaskRuntimeState.FAILED_SAFELY -> RitavTaskUiState.FAILED_SAFELY
                    TaskRuntimeState.STOPPED -> RitavTaskUiState.STOPPED
                }
            },
            taskName = taskSnapshot.taskName,
            currentStep = taskSnapshot.currentStep,
            summary = if (stopped) {
                "Emergency Stop is active. No protected task execution is available."
            } else {
                taskSnapshot.summary
                    ?: "No task is currently being executed."
            },
            buttonStyle = buttonStyle,
            onEmergencyStop = onEmergencyStop
        )
                }
            }
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
            onValueChange = { input = it.take(MAX_CONVERSATION_INPUT_LENGTH) },
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = "Message Ritav input"
                },
            enabled = !stopped,
            minLines = 1,
            maxLines = 5,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { send() }),
            label = { Text("Message Ritav") },
            supportingText = {
                Text(
                    "${input.length}/$MAX_CONVERSATION_INPUT_LENGTH",
                    modifier = Modifier.semantics {
                        contentDescription = "Message length ${input.length} of $MAX_CONVERSATION_INPUT_LENGTH characters"
                    }
                )
            }
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
                onEmergencyStop()
                stopped = true
            },
            enabled = !stopped,
            destructive = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
