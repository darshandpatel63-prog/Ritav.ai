# Ritav.ai — Persistent Project State

## Current phase
Phase 0 — Architecture, threat model and secure foundation.

## Source of truth
- `RITAV_BLUEPRINT.md` — engineering implementation contract.
- `RITAV_AI_MASTER_BLUEPRINT.pdf` — user-provided product/design reference.
- `docs/MASTER_REQUIREMENTS_MATRIX.md` — explicit reconciliation of both sources.

## Security foundation status
Implemented baseline deterministic security gates:
- Risk tiers 0–4.
- Scoped capability grants: global/app/capability/action/session shape, with default deny.
- Explicit authorization levels: none, user confirmation, device authentication.
- Financial-action hard block.
- Sensitive-data firewall with contextual secret detection/redaction helper.
- Emergency stop.
- Local audit event contract with secret-safe action logging.
- Unit tests covering permission boundaries, authorization, finance blocking, emergency stop and secret detection.

## Important security assessment
This is a hardened **foundation**, not a claim of mathematically bug-free or production-complete security. The architecture still requires implementation and testing of Android secure storage/key management, screen/input isolation, identity, accessibility boundaries, network enforcement, app capability registry, confirmation UI/device-auth integration, encrypted audit persistence, prompt-injection defenses, dependency/security scanning, and real-device testing before release.

## Security invariants
1. No autonomous consequential action.
2. Never provide OTP, UPI PIN, password, CVV or equivalent secrets to AI reasoning.
3. Financial/UPI automation is denied by default and protected by a dedicated firewall.
4. No hidden telemetry or private-data egress by default.
5. External/app content cannot override security policy.
6. Execution requires valid scoped permission + explicit intent where required + risk-appropriate authorization.
7. Uncertainty blocks or asks; it never guesses.
8. Security policy is deterministic and independent of the AI model.

## Architecture invariant
USER → SECURITY GATE → MASTER ORCHESTRATOR → POLICY → PERMISSION → AUTHORIZATION → EXECUTION → VERIFICATION → AUDIT

## Next implementation target
- Integrate security gates with Safe Mode/Emergency Stop UI state.
- Encrypted local persistence and Android Keystore-backed key management.
- App capability registry and Android capability adapters.
- Orchestrator contracts with strictly scoped agent capabilities.
- Confirmation/read-back and device-auth flow.
- Prompt-injection/content trust boundary.
- Network egress firewall and data-classification gate.
- Identity/session model.
- CI verification, dependency/security scanning and Android build validation.
- Real-device security tests.
- Later: voice, local AI, Android bridge and app adapters.

## Constraints
- Android OS restrictions are authoritative.
- Accessibility, microphone, camera and background operation are opt-in capabilities and must not be assumed universally available.
- No real-device behavior is claimed until tested on a physical Android device.
- Release signing keys never enter the repository.
- Release APK workflow should be manually triggered rather than building a release for every push.

## Development rule
For each feature: implement → test → security review → update documentation → CI verification → record result here.

## Continuation instruction
A future chat can continue with: “Continue Ritav.ai development. Read `RITAV_PROJECT_STATE.md`, `RITAV_BLUEPRINT.md`, and `docs/MASTER_REQUIREMENTS_MATRIX.md`, inspect the repository, verify the current build/test state, and continue from the next implementation target.”
