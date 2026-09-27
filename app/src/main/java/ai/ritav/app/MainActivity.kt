package ai.ritav.app

import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
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
            var navigationPosition by remember { mutableStateOf(navigationPositionStore.load()) }
            val uiPreferencesStore = remember { UiPreferencesStore(this@MainActivity) }
            var uiPreferences by remember { mutableStateOf(uiPreferencesStore.load()) }
            var destination by remember { mutableStateOf(AppDestination.HOME) }
            BackHandler(enabled = destination != AppDestination.HOME) {
                destination = AppDestination.HOME
            }
            var stopped by remember { mutableStateOf(securityControl.isEmergencyStopActive()) }
            var taskRuntimeSnapshot by remember {
                mutableStateOf(executionRuntime.taskRuntimeState.snapshot())
            }
            DisposableEffect(executionRuntime.taskRuntimeState) {
                val subscription = executionRuntime.taskRuntimeState.observe { snapshot ->
                    runOnUiThread {
                        taskRuntimeSnapshot = snapshot
                    }
                }
                onDispose { subscription.close() }
            }

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
            var statusTone by remember { mutableStateOf(RitavFeedbackTone.INFO) }

            fun setStatus(
                message: String?,
                tone: RitavFeedbackTone = RitavFeedbackTone.INFO
            ) {
                statusMessage = message
                statusTone = if (message == null) RitavFeedbackTone.INFO else tone
            }

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

            fun activateEmergencyStopFromShell() {
                securityControl.activateEmergencyStop()
                stopped = true
                activeIdentitySession = null
                dismissPendingApproval()
                dismissPendingTrustedApproval()
                dismissPendingTrustedRemoval()
                setStatus(
                    "Emergency Stop activated. Protected actions are blocked.",
                    RitavFeedbackTone.ERROR
                )
            }

            fun authenticateProtectedActions() {
                setStatus(null)
                securityControl.authenticateProtectedActions(
                    reason = "Authorize protected Ritav actions"
                ) { session ->
                    runOnUiThread {
                        activeIdentitySession = session?.takeIf {
                            it.isActive(System.currentTimeMillis())
                        }
                        val authenticated = activeIdentitySession != null
                        setStatus(
                            if (authenticated) {
                                "Protected identity session established."
                            } else {
                                "Device authentication did not establish a trusted session."
                            },
                            if (authenticated) {
                                RitavFeedbackTone.INFO
                            } else {
                                RitavFeedbackTone.ERROR
                            }
                        )
                    }
                }
            }

            fun reviewCandidate(candidate: CapabilityGrantCandidate) {
                setStatus(null)
                val session = activeIdentitySession?.takeIf {
                    it.isActive(System.currentTimeMillis())
                }
                if (session == null || securityControl.isEmergencyStopActive()) {
                    setStatus(
                        "Authenticate a protected identity session before approval.",
                        RitavFeedbackTone.ERROR
                    )
                    return
                }

                val plan = securityControl.prepareCapabilityGrant(candidate, session)
                if (plan == null) {
                    setStatus(
                        "Capability approval could not be prepared.",
                        RitavFeedbackTone.ERROR
                    )
                    return
                }

                pendingCandidate = candidate
                pendingGrantPlan = plan
            }

            fun reviewTrustedApp() {
                setStatus(null)
                val session = activeIdentitySession?.takeIf {
                    it.isActive(System.currentTimeMillis())
                }
                if (session == null || securityControl.isEmergencyStopActive()) {
                    setStatus(
                        "Authenticate a protected identity session before trusting an application.",
                        RitavFeedbackTone.ERROR
                    )
                    return
                }

                val packageName = trustedPackageInput.trim()
                if (packageName.isEmpty()) {
                    setStatus(
                        "Enter an installed Android package name.",
                        RitavFeedbackTone.WARNING
                    )
                    return
                }

                val plan = securityControl.prepareTrustedApp(
                    packageName = packageName,
                    identitySession = session
                )
                if (plan == null) {
                    setStatus(
                        "The installed package identity could not be verified or is not eligible for trust.",
                        RitavFeedbackTone.ERROR
                    )
                    return
                }

                pendingTrustedPackage = plan.appId
                pendingTrustedPlan = plan
            }

            fun approvePendingTrustedApp() {
                val plan = pendingTrustedPlan ?: return
                val session = activeIdentitySession ?: run {
                    dismissPendingTrustedApproval()
                    setStatus(
                        "Trusted identity session is unavailable.",
                        RitavFeedbackTone.ERROR
                    )
                    return
                }

                dismissPendingTrustedApproval()
                setStatus("Authorizing trusted-application approval...")

                securityControl.approveTrustedApp(
                    plan = plan,
                    identitySession = session,
                    userConfirmed = true
                ) { success ->
                    runOnUiThread {
                        setStatus(
                            if (success) {
                                trustedPackageInput = ""
                                trustedPackages = (trustedPackages + plan.appId).distinct().sorted()
                                "Trusted application added. Capability access still requires its separate grant flow."
                            } else {
                                "Trusted-application approval was denied or became invalid."
                            },
                            if (success) RitavFeedbackTone.INFO else RitavFeedbackTone.ERROR
                        )
                    }
                }
            }

            fun reviewTrustedRemoval(packageName: String) {
                setStatus(null)
                val session = activeIdentitySession?.takeIf {
                    it.isActive(System.currentTimeMillis())
                }
                if (session == null || securityControl.isEmergencyStopActive()) {
                    setStatus(
                        "Authenticate a protected identity session before removing trusted access.",
                        RitavFeedbackTone.ERROR
                    )
                    return
                }

                val plan = securityControl.prepareTrustedRemoval(
                    packageName = packageName,
                    identitySession = session
                )
                if (plan == null) {
                    setStatus(
                        "Trusted-app removal could not be prepared; installed identity or stored trust state changed.",
                        RitavFeedbackTone.ERROR
                    )
                    return
                }

                pendingTrustedRemovalPackage = plan.appId
                pendingTrustedRemovalPlan = plan
            }

            fun approvePendingTrustedRemoval() {
                val plan = pendingTrustedRemovalPlan ?: return
                val session = activeIdentitySession ?: run {
                    dismissPendingTrustedRemoval()
                    setStatus(
                        "Trusted identity session is unavailable.",
                        RitavFeedbackTone.ERROR
                    )
                    return
                }

                dismissPendingTrustedRemoval()
                setStatus("Authorizing trusted-application removal...")

                securityControl.approveTrustedRemoval(
                    plan = plan,
                    identitySession = session,
                    userConfirmed = true
                ) { success ->
                    runOnUiThread {
                        setStatus(
                            if (success) {
                                trustedPackages = trustedPackages.filterNot { it == plan.appId }
                                "Trusted application removed."
                            } else {
                                "Trusted-application removal was denied or became invalid."
                            },
                            if (success) RitavFeedbackTone.INFO else RitavFeedbackTone.ERROR
                        )
                    }
                }
            }
            fun approvePendingGrant() {
                val plan = pendingGrantPlan ?: return
                val session = activeIdentitySession ?: run {
                    dismissPendingApproval()
                    setStatus(
                        "Trusted identity session is unavailable.",
                        RitavFeedbackTone.ERROR
                    )
                    return
                }

                dismissPendingApproval()
                setStatus("Authorizing capability approval...")

                securityControl.approveCapabilityGrant(
                    plan = plan,
                    identitySession = session,
                    userConfirmed = true
                ) { success ->
                    runOnUiThread {
                        setStatus(
                            if (success) {
                                "Capability approved for the current trusted identity session."
                            } else {
                                "Capability approval was denied or became invalid."
                            },
                            if (success) RitavFeedbackTone.INFO else RitavFeedbackTone.ERROR
                        )
                    }
                }
            }

            RitavTheme(preferences = uiPreferences) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        when (destination) {
                            AppDestination.HOME -> ConversationalHomeScreen(
                                securityControl = securityControl,
                                buttonStyle = uiPreferences.buttonStyle,
                                taskSnapshot = taskRuntimeSnapshot,
                                onOpenSecurityCenter = { destination = AppDestination.SECURITY },
                                onEmergencyStop = ::activateEmergencyStopFromShell
                            )
                            AppDestination.SECURITY -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                PermissionCenter(
                                    stopped = stopped,
                                    buttonStyle = uiPreferences.buttonStyle,
                                    identitySession = identitySession,
                                    candidates = securityControl.capabilityGrantOptions(),
                                    pendingCandidate = pendingCandidate,
                                    pendingPlan = pendingGrantPlan,
                                    statusMessage = statusMessage,
                                    statusTone = statusTone,
                                    trustedPackageInput = trustedPackageInput,
                                    onTrustedPackageInputChanged = { value ->
                                        trustedPackageInput = value.take(256)
                                    },
                                    onPrepareTrustedApp = ::reviewTrustedApp,
                                    pendingTrustedPackage = pendingTrustedPackage,
                                    pendingTrustedPlan = pendingTrustedPlan,
                                    onDismissTrustedApproval = {
                                        dismissPendingTrustedApproval()
                                        setStatus(null)
                                    },
                                    onApproveTrustedApp = ::approvePendingTrustedApp,
                                    trustedPackages = trustedPackages,
                                    onTrustedPackageSelectedForRemoval = ::reviewTrustedRemoval,
                                    pendingTrustedRemovalPackage = pendingTrustedRemovalPackage,
                                    pendingTrustedRemovalPlan = pendingTrustedRemovalPlan,
                                    onDismissTrustedRemoval = {
                                        dismissPendingTrustedRemoval()
                                        setStatus(null)
                                    },
                                    onApproveTrustedRemoval = ::approvePendingTrustedRemoval,
                                    onAuthenticate = ::authenticateProtectedActions,
                                    onEmergencyStop = ::activateEmergencyStopFromShell,
                                    onResume = {
                                        securityControl.resumeAfterUserConfirmation()
                                        stopped = securityControl.isEmergencyStopActive()
                                        activeIdentitySession = null
                                        dismissPendingApproval()
                                        dismissPendingTrustedApproval()
                                        dismissPendingTrustedRemoval()
                                        setStatus(
                                            if (stopped) {
                                                "Emergency Stop remains active."
                                            } else {
                                                "Ritav resumed. Protected actions require fresh authentication."
                                            },
                                            if (stopped) RitavFeedbackTone.ERROR else RitavFeedbackTone.INFO
                                        )
                                    },
                                    onCandidateSelected = ::reviewCandidate,
                                    onDismissApproval = {
                                        dismissPendingApproval()
                                        setStatus(null)
                                    },
                                    onApprove = ::approvePendingGrant
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    RitavButton(
                                        style = uiPreferences.buttonStyle,
                                        label = "Back to Ritav",
                                        onClick = { destination = AppDestination.HOME }
                                    )
                                    RitavButton(
                                        style = uiPreferences.buttonStyle,
                                        label = "UI & Appearance",
                                        onClick = { destination = AppDestination.SETTINGS }
                                    )
                                }
                            }
                        }
                            AppDestination.SETTINGS -> UiAppearanceSettings(
                                uiPreferences = uiPreferences,
                                navigationFixed = navigationPosition.fixed,
                                onThemeModeChanged = { mode ->
                                    uiPreferences = uiPreferences.copy(themeMode = mode)
                                    uiPreferencesStore.saveThemeMode(mode)
                                },
                                onThemeFamilyChanged = { family ->
                                    uiPreferences = uiPreferences.copy(themeFamily = family)
                                    uiPreferencesStore.saveThemeFamily(family)
                                },
                                motionPreference = uiPreferences.motionPreference,
                                onMotionPreferenceChanged = { preference ->
                                    uiPreferences = uiPreferences.copy(motionPreference = preference)
                                    uiPreferencesStore.saveMotionPreference(preference)
                                },
                                onButtonStyleChanged = { style ->
                                    uiPreferences = uiPreferences.copy(buttonStyle = style)
                                    uiPreferencesStore.saveButtonStyle(style)
                                },
                                onNavigationFixedChanged = { fixed ->
                                    navigationPosition = navigationPosition.copy(fixed = fixed)
                                    navigationPositionStore.setFixed(fixed)
                                },
                                onResetNavigationPosition = {
                                    navigationPosition = navigationPosition.copy(
                                        xFraction = 0.5f,
                                        yFraction = 0.5f
                                    )
                                    navigationPositionStore.resetPosition()
                                }
                            )
                        }

                        GlobalAdaptiveFloatingNavigation(
                            items = listOf(
                                GlobalNavItem("Home", "⌂", selected = destination == AppDestination.HOME) { destination = AppDestination.HOME },
                                GlobalNavItem("Security", "◈", selected = destination == AppDestination.SECURITY) { destination = AppDestination.SECURITY },
                                GlobalNavItem("Settings", "⚙", selected = destination == AppDestination.SETTINGS) { destination = AppDestination.SETTINGS }
                            ),
                            initialPosition = navigationPosition,
                            motionPreference = uiPreferences.motionPreference,
                            onPositionSettled = { xFraction, yFraction ->
                                navigationPosition = navigationPosition.copy(
                                    xFraction = xFraction,
                                    yFraction = yFraction
                                )
                                navigationPositionStore.savePosition(xFraction, yFraction)
                            },
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
