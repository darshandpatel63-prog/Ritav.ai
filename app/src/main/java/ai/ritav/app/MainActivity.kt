package ai.ritav.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import ai.ritav.app.core.security.SecurityControlPort
import ai.ritav.app.core.security.SecuritySession

class MainActivity : FragmentActivity() {
    private lateinit var executionRuntime: AndroidExecutionRuntime
    private lateinit var securityControl: SecurityControlPort
    private var activeIdentitySession by mutableStateOf<SecuritySession?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Empty trusted registry keeps external actions deny-by-default until a reviewed allowlist exists.
        executionRuntime = AndroidExecutionRuntime(this)
        securityControl = executionRuntime.securityControl

        setContent {
            val navigationPositionStore = remember { NavigationPositionStore(this@MainActivity) }
            val navigationPosition = remember { navigationPositionStore.load() }
            var stopped by remember { mutableStateOf(securityControl.isEmergencyStopActive()) }
            var pendingCandidate by remember { mutableStateOf<CapabilityGrantCandidate?>(null) }
            var pendingGrantPlan by remember { mutableStateOf<ActionPlan?>(null) }
            var trustedPackageInput by remember { mutableStateOf("") }
            var trustedPackages by remember {
                mutableStateOf(securityControl.trustedPackageNames())
            }
            var pendingTrustedPackage by remember { mutableStateOf<String?>(null) }
            var pendingTrustedPlan by remember { mutableStateOf<ActionPlan?>(null) }
            var pendingTrustedRemovalPackage by remember { mutableStateOf<String?>(null) }
            var pendingTrustedRemovalPlan by remember { mutableStateOf<ActionPlan?>(null) }
            var statusMessage by remember { mutableStateOf<String?>(null) }
            var adminMode by remember { mutableStateOf(false) }

            val identitySession = activeIdentitySession?.takeIf {
                it.isActive(System.currentTimeMillis())
            }

            fun dismissPendingApproval() {
                pendingCandidate = null
                pendingGrantPlan = null
            }

            fun dismissPendingTrustedApproval() {
                pendingTrustedPackage = null
                pendingTrustedPlan = null
            }

            fun dismissPendingTrustedRemoval() {
                pendingTrustedRemovalPackage = null
                pendingTrustedRemovalPlan = null
            }

            fun authenticateProtectedActions() {
                statusMessage = null
                securityControl.authenticateProtectedActions(
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
            }

            fun reviewCandidate(candidate: CapabilityGrantCandidate) {
                statusMessage = null
                val session = activeIdentitySession?.takeIf {
                    it.isActive(System.currentTimeMillis())
                }
                if (session == null || securityControl.isEmergencyStopActive()) {
                    statusMessage = "Authenticate a protected identity session before approval."
                    return
                }

                val plan = securityControl.prepareCapabilityGrant(candidate, session)
                if (plan == null) {
                    statusMessage = "Capability approval could not be prepared."
                    return
                }

                pendingCandidate = candidate
                pendingGrantPlan = plan
            }

            fun reviewTrustedApp() {
                statusMessage = null
                val session = activeIdentitySession?.takeIf {
                    it.isActive(System.currentTimeMillis())
                }
                if (session == null || securityControl.isEmergencyStopActive()) {
                    statusMessage = "Authenticate a protected identity session before trusting an application."
                    return
                }

                val packageName = trustedPackageInput.trim()
                if (packageName.isEmpty()) {
                    statusMessage = "Enter an installed Android package name."
                    return
                }

                val plan = securityControl.prepareTrustedApp(
                    packageName = packageName,
                    identitySession = session
                )
                if (plan == null) {
                    statusMessage = "The installed package identity could not be verified or is not eligible for trust."
                    return
                }

                pendingTrustedPackage = plan.appId
                pendingTrustedPlan = plan
            }

            fun approvePendingTrustedApp() {
                val plan = pendingTrustedPlan ?: return
                val session = activeIdentitySession ?: run {
                    dismissPendingTrustedApproval()
                    statusMessage = "Trusted identity session is unavailable."
                    return
                }

                dismissPendingTrustedApproval()
                statusMessage = "Authorizing trusted-application approval..."

                securityControl.approveTrustedApp(
                    plan = plan,
                    identitySession = session,
                    userConfirmed = true
                ) { success ->
                    runOnUiThread {
                        statusMessage =
                            if (success) {
                                trustedPackageInput = ""
                                trustedPackages = (trustedPackages + plan.appId).distinct().sorted()
                                "Trusted application added. Capability access still requires its separate grant flow."
                            } else {
                                "Trusted-application approval was denied or became invalid."
                            }
                    }
                }
            }

            fun reviewTrustedRemoval(packageName: String) {
                statusMessage = null
                val session = activeIdentitySession?.takeIf {
                    it.isActive(System.currentTimeMillis())
                }
                if (session == null || securityControl.isEmergencyStopActive()) {
                    statusMessage = "Authenticate a protected identity session before removing trusted access."
                    return
                }

                val plan = securityControl.prepareTrustedRemoval(
                    packageName = packageName,
                    identitySession = session
                )
                if (plan == null) {
                    statusMessage = "Trusted-app removal could not be prepared; installed identity or stored trust state changed."
                    return
                }

                pendingTrustedRemovalPackage = plan.appId
                pendingTrustedRemovalPlan = plan
            }

            fun approvePendingTrustedRemoval() {
                val plan = pendingTrustedRemovalPlan ?: return
                val session = activeIdentitySession ?: run {
                    dismissPendingTrustedRemoval()
                    statusMessage = "Trusted identity session is unavailable."
                    return
                }

                dismissPendingTrustedRemoval()
                statusMessage = "Authorizing trusted-application removal..."

                securityControl.approveTrustedRemoval(
                    plan = plan,
                    identitySession = session,
                    userConfirmed = true
                ) { success ->
                    runOnUiThread {
                        statusMessage =
                            if (success) {
                                trustedPackages = trustedPackages.filterNot { it == plan.appId }
                                "Trusted application removed."
                            } else {
                                "Trusted-application removal was denied or became invalid."
                            }
                    }
                }
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

                securityControl.approveCapabilityGrant(
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

            RitavTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (!adminMode) {
                            ConversationalHomeScreen(
                                securityControl = securityControl,
                                onOpenSecurityCenter = { adminMode = true }
                            )
                        } else {
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
                                    candidates = securityControl.capabilityGrantOptions(),
                                    pendingCandidate = pendingCandidate,
                                    pendingPlan = pendingGrantPlan,
                                    statusMessage = statusMessage,
                                    trustedPackageInput = trustedPackageInput,
                                    onTrustedPackageInputChanged = { value ->
                                        trustedPackageInput = value.take(256)
                                    },
                                    onPrepareTrustedApp = ::reviewTrustedApp,
                                    pendingTrustedPackage = pendingTrustedPackage,
                                    pendingTrustedPlan = pendingTrustedPlan,
                                    onDismissTrustedApproval = {
                                        dismissPendingTrustedApproval()
                                        statusMessage = null
                                    },
                                    onApproveTrustedApp = ::approvePendingTrustedApp,
                                    trustedPackages = trustedPackages,
                                    onTrustedPackageSelectedForRemoval = ::reviewTrustedRemoval,
                                    pendingTrustedRemovalPackage = pendingTrustedRemovalPackage,
                                    pendingTrustedRemovalPlan = pendingTrustedRemovalPlan,
                                    onDismissTrustedRemoval = {
                                        dismissPendingTrustedRemoval()
                                        statusMessage = null
                                    },
                                    onApproveTrustedRemoval = ::approvePendingTrustedRemoval,
                                    onAuthenticate = ::authenticateProtectedActions,
                                    onEmergencyStop = {
                                        securityControl.activateEmergencyStop()
                                        stopped = true
                                        activeIdentitySession = null
                                        dismissPendingApproval()
                                        dismissPendingTrustedApproval()
                                        dismissPendingTrustedRemoval()
                                        statusMessage = "Emergency Stop activated. Protected actions are blocked."
                                    },
                                    onResume = {
                                        securityControl.resumeAfterUserConfirmation()
                                        stopped = securityControl.isEmergencyStopActive()
                                        activeIdentitySession = null
                                        dismissPendingApproval()
                                        dismissPendingTrustedApproval()
                                        dismissPendingTrustedRemoval()
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
                                TextButton(onClick = { adminMode = false }) {
                                    Text("Back to Ritav")
                                }
                            }
                        }

                        GlobalAdaptiveFloatingNavigation(
                            items = listOf(
                                GlobalNavItem("Home", "⌂") { adminMode = false },
                                GlobalNavItem("Security", "◈") { adminMode = true }
                            ),
                            initialPosition = navigationPosition,
                            onPositionSettled = navigationPositionStore::savePosition,
                            modifier = Modifier.fillMaxSize()
                        )
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

    override fun onDestroy() {
        if (::executionRuntime.isInitialized) {
            executionRuntime.close()
        }
        super.onDestroy()
    }
}
