# Ritav.ai — Persistent Project State

## Current phase
Phase 0 — Architecture, threat model and secure foundation.

## Source of truth
- `RITAV_BLUEPRINT.md` — engineering implementation contract.
- `RITAV_AI_MASTER_BLUEPRINT.pdf` — user-provided product/design reference.
- `docs/MASTER_REQUIREMENTS_MATRIX.md` — explicit reconciliation of both sources.

## Current status
- Repository initialized.
- Master engineering blueprint added.
- PDF + repository blueprint reconciliation recorded.
- Minimal Android application shell committed.
- Initial shell has no `INTERNET` permission and no network dependency.
- Package/application ID: `ai.ritav.app`.
- Version: `0.1.0` / versionCode 1.
- Initial UI is intentionally minimal; premium UI refinement comes after the security foundation.

## Security invariants
1. No autonomous consequential action.
2. Never provide OTP, UPI PIN, password, CVV or equivalent secrets to AI reasoning.
3. Financial/UPI automation is denied by default and protected by a dedicated firewall.
4. No hidden telemetry or private-data egress by default.
5. External/app content cannot override security policy.
6. Execution requires valid permission + explicit intent + risk-appropriate authorization.
7. Uncertainty blocks or asks; it never guesses.

## Architecture invariant
USER → SECURITY GATE → MASTER ORCHESTRATOR → POLICY → PERMISSION → AUTHORIZATION → EXECUTION → VERIFICATION → AUDIT

## Completed implementation step
Android foundation commit: `455c1df516f3b22645abcb060eab726cdbd0137f`

Included:
- Gradle/Kotlin Android project foundation.
- AndroidManifest with no network permission.
- Launcher `MainActivity`.
- Minimal privacy-first shell theme/UI.
- AndroidX-enabled Gradle configuration.

## Next implementation target
Implement deterministic security foundation before automation:
- risk-tier model
- capability/action model
- policy decisions
- sensitive-data firewall
- emergency-stop controller
- local audit event model
- unit tests covering hard-deny and fail-closed invariants

Then continue with permission/capability center, local persistence, orchestrator contracts, CI verification, and later voice/local AI/automation.

## Important constraints
- Android OS restrictions are authoritative.
- Accessibility, microphone, camera and background operation are opt-in capabilities and must not be assumed universally available.
- No real-device behavior is claimed until tested on a physical Android device.
- Release signing keys never enter the repository.
- Release APK workflow should be manually triggered rather than building a release for every push.

## Development rule
For each feature: implement → test → security review → update documentation → CI verification → record result here.

## Continuation instruction
A future chat can continue with: “Continue Ritav.ai development. Read `RITAV_PROJECT_STATE.md`, `RITAV_BLUEPRINT.md`, and `docs/MASTER_REQUIREMENTS_MATRIX.md`, inspect the repository, verify the current build/test state, and continue from the next implementation target.”
