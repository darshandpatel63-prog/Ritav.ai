package ai.ritav.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.fragment.app.FragmentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ai.ritav.app.core.security.AndroidExecutionRuntime
import ai.ritav.app.core.security.AppCapabilityRegistry
import ai.ritav.app.core.security.SecurityRuntimeState

class MainActivity : FragmentActivity() {
    private lateinit var executionRuntime: AndroidExecutionRuntime
    private lateinit var securityState: SecurityRuntimeState

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Empty trusted registry keeps external actions deny-by-default until a reviewed allowlist exists.\n        executionRuntime = AndroidExecutionRuntime(this, AppCapabilityRegistry())\n        securityState = executionRuntime.securityState

        setContent {
            var stopped by remember { mutableStateOf(securityState.isEmergencyStopActive()) }

            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("Ritav", style = MaterialTheme.typography.displaySmall)
                    Text(
                        if (stopped) "Emergency Stop active" else "Privacy-first local AI foundation",
                        modifier = Modifier.padding(top = 12.dp)
                    )
                    Button(
                        onClick = {
                            if (stopped) {
                                securityState.resumeAfterUserConfirmation(confirmed = true)
                            } else {
                                securityState.activateEmergencyStop()
                            }
                            stopped = securityState.isEmergencyStopActive()
                        },
                        modifier = Modifier.padding(top = 24.dp)
                    ) {
                        Text(if (stopped) "Resume Ritav" else "Emergency Stop")
                    }
                }
            }
        }
    }
}
