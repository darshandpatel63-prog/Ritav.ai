package ai.ritav.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@androidx.compose.runtime.Composable
internal fun RitavOnboardingScreen(
    onContinue: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(Modifier.height(8.dp))

        Surface(
            modifier = Modifier
                .semantics {
                    contentDescription = "Ritav.ai app identity mark"
                },
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            tonalElevation = 6.dp,
            shadowElevation = 4.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "R",
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Text(
                    ".ai",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }

        Text(
            "Welcome to Ritav.ai",
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            "A local-first assistant that stays under your control.",
            style = MaterialTheme.typography.titleMedium
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Your control comes first.", style = MaterialTheme.typography.titleMedium)
                Text("You decide what Ritav may access. Security policy and authorization decide what protected actions may proceed.")
                Text("AI cannot authorize itself, bypass security, or silently execute an action.")
                Text("Sensitive credentials such as OTPs, UPI PINs, passwords, and private keys are protected from AI automation.")
                Text("Emergency Stop can block protected activity immediately.")
                Text("The production model/provider runtime is not connected yet, so Ritav will not pretend to generate a model answer.")
            }
        }

        RitavButton(
            style = UiButtonStyle.FILLED,
            label = "Continue to Ritav",
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
