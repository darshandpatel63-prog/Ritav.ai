package ai.ritav.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ai.ritav.app.core.security.ActionPlan
import ai.ritav.app.core.security.CapabilityGrantCandidate
import ai.ritav.app.core.security.RiskTier
import ai.ritav.app.core.security.SecuritySession

@Composable
internal fun PermissionCenter(
    stopped: Boolean,
    identitySession: SecuritySession?,
    candidates: List<CapabilityGrantCandidate>,
    pendingCandidate: CapabilityGrantCandidate?,
    pendingPlan: ActionPlan?,
    statusMessage: String?,
    trustedPackageInput: String,
    onTrustedPackageInputChanged: (String) -> Unit,
    onPrepareTrustedApp: () -> Unit,
    pendingTrustedPackage: String?,
    pendingTrustedPlan: ActionPlan?,
    onDismissTrustedApproval: () -> Unit,
    onApproveTrustedApp: () -> Unit,
    trustedPackages: List<String>,
    onTrustedPackageSelectedForRemoval: (String) -> Unit,
    pendingTrustedRemovalPackage: String?,
    pendingTrustedRemovalPlan: ActionPlan?,
    onDismissTrustedRemoval: () -> Unit,
    onApproveTrustedRemoval: () -> Unit,
    onOpenUsageAccessSettings: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onAuthenticate: () -> Unit,
    onEmergencyStop: () -> Unit,
    onResume: () -> Unit,
    onCandidateSelected: (CapabilityGrantCandidate) -> Unit,
    onDismissApproval: () -> Unit,
    onApprove: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Permission Center", style = MaterialTheme.typography.headlineSmall)
        Text(
            if (stopped) {
                "Emergency Stop is active. Protected actions are blocked."
            } else if (identitySession != null) {
                "Trusted identity session active. Capability approvals are session-bound."
            } else {
                "Authenticate before approving protected capability access."
            }
        )

        if (!stopped) {
            Text(
                "Cross-app security access",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                "Ritav uses explicit Android special-access settings for target-app observation. " +
                    "Accessibility access is user-enabled and is used only while a requested observation is armed."
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(onClick = onOpenUsageAccessSettings) {
                    Text("Usage Access")
                }
                TextButton(onClick = onOpenAccessibilitySettings) {
                    Text("Accessibility")
                }
            }
        }

        if (identitySession == null && !stopped) {
            Button(onClick = onAuthenticate) {
                Text("Authenticate protected actions")
            }
        }

        if (identitySession != null && !stopped) {
            Text(
                "Add a trusted external application",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                "Enter an installed Android package name. Ritav will verify its installed signing identity before any trust entry is persisted."
            )
            OutlinedTextField(
                value = trustedPackageInput,
                onValueChange = onTrustedPackageInputChanged,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Android package name") },
                supportingText = { Text("Certificate details are verified internally and are not shown here.") }
            )
            Button(
                onClick = onPrepareTrustedApp,
                enabled = trustedPackageInput.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Review trusted-app approval")
            }
        }

        if (identitySession != null && trustedPackages.isNotEmpty() && !stopped) {
            Text(
                "Trusted external applications",
                style = MaterialTheme.typography.titleMedium
            )
            trustedPackages.forEach { packageName ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        packageName,
                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                    )
                    TextButton(onClick = { onTrustedPackageSelectedForRemoval(packageName) }) {
                        Text("Remove trust")
                    }
                }
                HorizontalDivider()
            }
        }

        if (candidates.isEmpty() && trustedPackages.isEmpty()) {
            Text("No trusted external applications are currently configured.")
            Text("External actions remain blocked.")
        } else if (candidates.isEmpty()) {
            Text("Capability actions for trusted applications remain separately permission-controlled.")
        } else if (identitySession == null && !stopped) {
            Text("A trusted identity session is required before capability approval.")
        } else if (stopped) {
            Text("Capability approval is unavailable while Emergency Stop is active.")
        } else {
            candidates.forEach { candidate ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                    ) {
                        Text(candidate.packageName, style = MaterialTheme.typography.titleMedium)
                        Text(
                            candidate.capability.name +
                                " / " +
                                candidate.action
                        )
                        Text("Risk: " + candidate.riskTier.name)
                    }
                    Button(onClick = { onCandidateSelected(candidate) }) {
                        Text("Review")
                    }
                }
                HorizontalDivider()
            }
        }

        if (!statusMessage.isNullOrBlank()) {
            Text(
                text = statusMessage,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Button(
            onClick = if (stopped) onResume else onEmergencyStop,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (stopped) "Resume Ritav" else "Emergency Stop")
        }
    }

    val candidate = pendingCandidate
    val plan = pendingPlan
    if (candidate != null && plan != null) {
        CapabilityGrantConfirmationDialog(
            candidate = candidate,
            plan = plan,
            onDismiss = onDismissApproval,
            onConfirm = onApprove
        )
    }

    val trustedPackage = pendingTrustedPackage
    val trustedPlan = pendingTrustedPlan
    if (trustedPackage != null && trustedPlan != null) {
        TrustedAppConfirmationDialog(
            packageName = trustedPackage,
            plan = trustedPlan,
            onDismiss = onDismissTrustedApproval,
            onConfirm = onApproveTrustedApp
        )
    }
}

/**
 * Presentation-only confirmation. The displayed plan hash is the exact plan
 * that the security coordinator will authorize; approval does not mint a token.
 */
@Composable
private fun CapabilityGrantConfirmationDialog(
    candidate: CapabilityGrantCandidate,
    plan: ActionPlan,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Approve capability access?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Application: " + candidate.packageName)
                Text("Capability: " + candidate.capability.name)
                Text("Action: " + candidate.action)
                Text("Target action risk: " + candidate.riskTier.name)
                Text("Grant authorization risk: " + plan.riskTier.name)
                Text("Exact plan hash: " + plan.stableHash())
                Text("Approval is bound to the current trusted identity session.")
                if (candidate.riskTier == RiskTier.TIER_3_EXTERNAL_OR_IRREVERSIBLE) {
                    Text("Device authentication will be requested before the grant is applied.")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Approve")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}


@Composable
private fun TrustedAppConfirmationDialog(
    packageName: String,
    plan: ActionPlan,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Trust this application?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Application package: " + packageName)
                Text("Capability: " + plan.capability.name)
                Text("Action: open")
                Text("Trust-entry authorization risk: " + plan.riskTier.name)
                Text("Exact plan hash: " + plan.stableHash())
                Text("The installed signing identity will be re-verified before persistence.")
                Text("Device authentication is required to persist this trusted-app entry.")
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Trust application")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}


@Composable
private fun TrustedAppRemovalConfirmationDialog(
    packageName: String,
    plan: ActionPlan,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Remove trusted access?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Application package: " + packageName)
                Text("Trust action: remove")
                Text("Exact plan hash: " + plan.stableHash())
                Text("The installed signing identity will be re-verified before trust is removed.")
                Text("Device authentication is required to remove this trusted-app entry.")
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Remove trust")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
