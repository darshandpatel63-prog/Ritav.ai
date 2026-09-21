package ai.ritav.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import ai.ritav.app.core.security.ActionPlan
import ai.ritav.app.core.security.AndroidExecutionRuntime
import ai.ritav.app.core.security.CapabilityGrantCandidate
import ai.ritav.app.core.security.SecurityRuntimeState
import ai.ritav.app.core.security.SecuritySession

class MainActivity : FragmentActivity() {
    private lateinit var executionRuntime: AndroidExecutionRuntime
    private lateinit var securityState: SecurityRuntimeState
    private var activeIdentitySession by mutableStateOf<SecuritySession?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Empty trusted registry keeps external actions deny-by-default until a reviewed allowlist exists.
        executionRuntime = AndroidExecutionRuntime(this)
        securityState = executionRuntime.securityState

        setContent {
            var stopped by remember { mutableStateOf(securityState.isEmergencyStopActive()) }
            var pendingCandidate by remember { mutableStateOf<CapabilityGrantCandidate?>(null) }
            var pendingGrantPlan by remember { mutableStateOf<ActionPlan?>(null) }
            var statusMessage by remember { mutableStateOf<String?>(null) }

            val identitySession = activeIdentitySession?.takeIf {
                it.isActive(System.currentTimeMillis())
            }

            fun dismissPendingApproval() {
                pendingCandidate = null
                pendingGrantPlan = null
            }

            fun authenticateProtectedActions() {
                statusMessage = null
                executionRuntime.identitySessionService.authenticate(
                    reason = "Authorize protected Ritav actions"
                ) { session ->
                    runOnUiThread {
                        activeIdentitySession = session?.takeIf {
                            it.isActive(System.currentTimeMillis())
                        }
                        statusMessage =
                            if (activeIdentitySession != null) {
                                "Protected identity session established."
                            } else {
                                "Device authentication did not establish a trusted session."
                            }
                    }
                }
            }

            fun reviewCandidate(candidate: CapabilityGrantCandidate) {
                statusMessage = null
                val session = activeIdentitySession?.takeIf {
                    it.isActive(System.currentTimeMillis())
                }
                if (session == null || securityState.isEmergencyStopActive()) {
                    statusMessage = "Authenticate a protected identity session before approval."
                    return
                }

                val plan = executionRuntime.capabilityGrantCoordinator.prepare(candidate, session)
                if (plan == null) {
                    statusMessage = "Capability approval could not be prepared."
                    return
                }

                pendingCandidate = candidate
                pendingGrantPlan = plan
            }

            fun approvePendingGrant() {
                val plan = pendingGrantPlan ?: return
                val session = activeIdentitySession ?: run {
                    dismissPendingApproval()
                    statusMessage = "Trusted identity session is unavailable."
                    return
                }

                dismissPendingApproval()
                statusMessage = "Authorizing capability approval..."

                executionRuntime.capabilityGrantCoordinator.approveAndGrant(
                    plan = plan,
                    identitySession = session,
                    userConfirmed = true
                ) { success ->
                    runOnUiThread {
                        statusMessage =
                            if (success) {
                                "Capability approved for the current trusted identity session."
                            } else {
                                "Capability approval was denied or became invalid."
                            }
                    }
                }
            }

            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    PermissionCenter(
                        stopped = stopped,
                        identitySession = identitySession,
                        candidates = executionRuntime.capabilityGrantCoordinator.options(),
                        pendingCandidate = pendingCandidate,
                        pendingPlan = pendingGrantPlan,
                        statusMessage = statusMessage,
                        onAuthenticate = ::authenticateProtectedActions,
                        onEmergencyStop = {
                            securityState.activateEmergencyStop()
                            stopped = true
                            activeIdentitySession = null
                            dismissPendingApproval()
                            statusMessage = "Emergency Stop activated. Protected actions are blocked."
                        },
                        onResume = {
                            securityState.resumeAfterUserConfirmation(confirmed = true)
                            stopped = securityState.isEmergencyStopActive()
                            activeIdentitySession = null
                            dismissPendingApproval()
                            statusMessage =
                                if (stopped) {
                                    "Emergency Stop remains active."
                                } else {
                                    "Ritav resumed. Protected actions require fresh authentication."
                                }
                        },
                        onCandidateSelected = ::reviewCandidate,
                        onDismissApproval = {
                            dismissPendingApproval()
                            statusMessage = null
                        },
                        onApprove = ::approvePendingGrant
                    )
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
