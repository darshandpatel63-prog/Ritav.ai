package ai.ritav.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ai.ritav.app.core.security.SecurityControlPort

internal data class ConversationMessage(
    val text: String,
    val fromUser: Boolean
)

@Composable
internal fun ConversationalHomeScreen(
    securityControl: SecurityControlPort,
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
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Ritav.ai", style = MaterialTheme.typography.headlineSmall)
                Text(
                    if (stopped) "Emergency Stop active" else "Secure local-first assistant",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            TextButton(onClick = onOpenSecurityCenter) {
                Text("Security")
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    if (stopped) "Protected actions are blocked." else "Security boundary active.",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(4.dp))
                Text("AI output never authorizes or executes an action directly.")
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages) { message ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = message.text,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }

        OutlinedTextField(
            value = input,
            onValueChange = { input = it.take(4096) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !stopped,
            minLines = 1,
            maxLines = 5,
            label = { Text("Message Ritav") }
        )

        Button(
            onClick = ::send,
            enabled = input.isNotBlank() && !stopped,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Send")
        }

        Button(
            onClick = {
                securityControl.activateEmergencyStop()
                stopped = true
            },
            enabled = !stopped,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Emergency Stop")
        }
    }
}
