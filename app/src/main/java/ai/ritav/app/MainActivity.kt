package ai.ritav.app

import android.os.Bundle
import androidx.activity.compose.setContent
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import ai.ritav.app.core.security.AndroidExecutionRuntime
import ai.ritav.app.core.security.AppCapabilityRegistry
import ai.ritav.app.core.security.SecurityRuntimeState
import ai.ritav.app.core.security.SecuritySession

class MainActivity : FragmentActivity() {
    private lateinit var executionRuntime: AndroidExecutionRuntime
    private lateinit var securityState: SecurityRuntimeState
    private var activeIdentitySession by mutableStateOf<SecuritySession?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Empty trusted registry keeps external actions deny-by-default until a reviewed allowlist exists.
        executionRuntime = AndroidExecutionRuntime(this, AppCapabilityRegistry())
        securityState = executionRuntime.securityState

        setContent {
            val stopped by mutableStateOf(securityState.isEmergencyStopActive())
            val identitySession = activeIdentitySession?.takeIf {
                it.isActive(System.currentTimeMillis())
            }

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
                        when {
                            stopped -> "Emergency Stop active"
                            identitySession != null -> "Trusted identity session active"
                            else -> "Protected actions require device authentication"
                        },
                        modifier = Modifier.padding(top = 12.dp)
                    )

                    if (!stopped && identitySession == null) {
                        Button(
                            onClick = {
                                executionRuntime.identitySessionService.authenticate(
                                    reason = "Authorize protected Ritav actions"
                                ) { session ->
                                    activeIdentitySession = session?.takeIf {
                                        it.isActive(System.currentTimeMillis())
                                    }
                                }
                            },
                            modifier = Modifier.padding(top = 24.dp)
                        ) {
                            Text("Authenticate protected actions")
                        }
                    }

                    Button(
                        onClick = {
                            if (stopped) {
                                securityState.resumeAfterUserConfirmation(confirmed = true)
                            } else {
                                securityState.activateEmergencyStop()
                            }
                            activeIdentitySession = null
                        },
                        modifier = Modifier.padding(top = 12.dp)
                    ) {
                        Text(if (stopped) "Resume Ritav" else "Emergency Stop")
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        activeIdentitySession = activeIdentitySession?.takeIf {
            it.isActive(System.currentTimeMillis())
        }
    }
}
