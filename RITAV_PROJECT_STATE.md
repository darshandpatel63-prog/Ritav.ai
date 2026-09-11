# Ritav.ai — Persistent Project State

## Current phase
Phase 0 — Architecture, threat model and secure foundation.

## Source of truth
- `RITAV_BLUEPRINT.md` — engineering blueprint created from the project requirements.
- `RITAV_AI_MASTER_BLUEPRINT.pdf` — user-provided master blueprint. Key requirements from it are incorporated into the repository blueprint.

## Current status
- Repository initialized.
- Master engineering blueprint added.
- Persistent project-state mechanism added.
- Android application foundation is the next implementation step.

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

## Next implementation target
Build the minimal Android shell with:
- premium privacy-first UI foundation
- onboarding
- local settings storage abstraction
- privacy dashboard shell
- permission-center shell
- safe-mode/emergency-stop state
- no network permission in the initial shell unless a later feature explicitly needs it

## Important constraints
- Android OS restrictions are authoritative.
- Accessibility, microphone, camera and background operation are opt-in capabilities and must not be assumed universally available.
- No real-device behavior is claimed until tested on a physical Android device.
- Release signing keys never enter the repository.
- Release APK workflow should be manually triggered rather than building a release for every push.

## Development rule
For each feature: implement → test → security review → update documentation → CI verification → record result here.

## Last known commit
The repository's first commit added `RITAV_BLUEPRINT.md`. This state file was added immediately afterward.

## Continuation instruction
A future chat can continue with: “Continue Ritav.ai development. Read `RITAV_PROJECT_STATE.md` and `RITAV_BLUEPRINT.md`, inspect the repository, verify the current build/test state, and continue from the next implementation target.”
