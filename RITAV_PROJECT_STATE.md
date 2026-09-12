# Ritav.ai — Persistent Project State

## Current phase
Phase 0 — Architecture, threat model and secure foundation; security/runtime integration in progress.

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
- Sensitive-data protection helpers and contextual secret detection/redaction.
- Emergency stop.
- Local audit event contract with secret-safe action logging.
- Unit tests covering permission boundaries, authorization, finance blocking, emergency stop and secret detection.

## Device compatibility baseline
- Minimum target: Android devices with **4 GB RAM and 32 GB storage**.
- The application must remain functional on this baseline without requiring flagship-class hardware.
- 4 GB/32 GB is a compatibility floor, not a guarantee that every future AI model or advanced feature will fit or run well within those resources.
- Larger-memory/storage devices should automatically receive better performance where available, without changing the security policy.
- Core security, permission, emergency-stop and policy components must stay lightweight and must not depend on large local models.
- Heavy capabilities such as local LLMs, vision, speech models and media processing must use capability-aware resource limits, graceful degradation and cancellation rather than assuming unlimited RAM/CPU/storage.
- Low-memory conditions must fail safely: release resources, cancel optional work, preserve security state, and avoid force-running workloads that can destabilize the app.

## Security/runtime integration completed
- Emergency stop is represented by a centralized process-local `SecurityRuntimeState`.
- Main UI is wired to the same runtime safety state rather than maintaining an unrelated UI-only boolean.
- Emergency stop activation is immediate and does not depend on the AI/model.
- Resume requires explicit user-confirmation signal through the security controller API.
- Final execution is routed through capability and security pipeline checks before an Android adapter can run.
- Android device-authentication integration has been added as a platform gateway; physical-device validation is still required.
- `SensitiveInformationFirewall` is enforced inside `SecurityExecutionPipeline` before authorization/execution.

## Sensitive Information Firewall — current state
The firewall remains the active development layer; **Finance Firewall has not been started**.

Implemented in the current `main` branch:
- Deterministic detection for OTP, UPI PIN, CVV, password-context values, recovery/backup codes, private keys, and API/access/secret keys.
- `SensitiveMatch` contains only type and source offsets; it does not contain the matched secret.
- Multiple matches are handled and overlapping matches are deduplicated.
- Redaction is performed right-to-left to preserve original UTF-16 source offsets.
- Input inspection is bounded at 16,384 characters.
- Inputs over the limit now fail closed with an explicit `INPUT_TOO_LARGE` result instead of throwing.
- Failed/blocked inspection never forwards the original oversized value through the security pipeline.
- NFKC/whitespace-compacted inspection is detection-only; if a sensitive pattern is found after normalization, the firewall conservatively blocks rather than attempting unsafe offset mapping/redaction.
- Security pipeline tests verify that oversized and normalization-detected inputs are blocked before authorization is consumed.

## Dedicated firewall test coverage added
`app/src/test/java/ai/ritav/app/core/security/SensitiveInformationFirewallTest.kt` now covers:
- OTP.
- UPI PIN.
- CVV.
- Password-context values.
- Recovery codes.
- Private keys.
- API/access/secret keys.
- Benign text.
- Multiple secrets.
- Overlapping detection/redaction.
- Redaction preservation of surrounding text.
- No secret value in `SensitiveMatch`.
- Exact maximum input length.
- Oversized input fail-closed behavior.
- Oversized input containing a secret.
- Conservative Unicode normalization detection.
- Unusual/malformed Unicode input not crashing the test call.

## Important security assessment
This is a hardened **foundation**, not a claim of mathematically bug-free or production-complete security. Regex detection is not comprehensive secret detection. Unicode/obfuscation resistance, contextual detection, OCR/screen filtering, structured input isolation, and full model/context ingestion remain unfinished. No real-device security result is claimed until physical-device testing occurs.

## Current verification status — 2026-09-12
- Repository default branch: `main`.
- Latest repository commit observed before this work: `3ca1d099e8262bab2b08c1f61bfafbdc43a461c0`.
- No GitHub commit status checks were present for that commit when inspected.
- No repository GitHub Actions workflow was found under `.github/workflows` during inspection.
- Therefore **CI/build/tests are NOT claimed as passed in this environment**. The new unit tests and code require actual Gradle/Android test execution before this layer can be marked verified.

## Latest commits from this continuation
- `998bfba9f05682a6edf1cef348613240bfd72a1c` — sensitive firewall fail-closed inspection hardening.
- `e4564805f48a67f22fd1b2552a1f493c3f6aacca` — security pipeline blocks every failed sensitive-input inspection.
- `c74cce08238a6ac7c2434f98d72394900583063d` — comprehensive sensitive firewall regression tests.
- `402073cb40c5c3e66163a0ab81eaea37c3670a60` — pipeline tests for oversized and normalized sensitive inputs.

## Security invariants
1. No autonomous consequential action.
2. Never provide OTP, UPI PIN, password, CVV or equivalent secrets to AI reasoning.
3. Financial/UPI automation is denied by default and protected by a dedicated firewall.
4. No hidden telemetry or private-data egress by default.
5. External/app content cannot override security policy.
6. Execution requires valid scoped permission + explicit intent where required + risk-appropriate authorization.
7. Uncertainty blocks or asks; it never guesses.
8. Security policy is deterministic and independent of the AI model.
9. Emergency stop must be authoritative over AI-driven actions in the process.
10. Resuming from emergency stop must never be an AI/model decision.

## Architecture invariant
USER → SECURITY GATE → MASTER ORCHESTRATOR → POLICY → PERMISSION → AUTHORIZATION → EXECUTION → VERIFICATION → AUDIT

## Exact next stop point
**Stay in Sensitive Information Firewall hardening/verification. Do not begin Finance Firewall yet.**

Before the next code change:
1. Re-fetch the current firewall, pipeline, authorization, and related test files and use their current SHAs.
2. Execute the actual Gradle unit test suite (or the most specific available firewall/pipeline test task) in a real build environment.
3. Fix any compile/test failures rather than assuming API compatibility.
4. Review the Unicode normalization/obfuscation design for false positives and bypasses.
5. Add further false-positive/false-negative cases where justified.
6. Confirm that the firewall remains mandatory before any future AI/context ingestion boundary.
7. Perform security review and only then document/commit the verified state.

## After this layer is actually verified
Expected order remains:
1. Finance Firewall / financial-app isolation.
2. Screen/OCR/Accessibility sensitive-content filtering.
3. Stronger app capability registry integration.
4. Mandatory unified security execution choke point.
5. Confirmation/read-back/device-auth UI.
6. Secure audit log bounds/rotation/reason-code hardening.
7. Secure storage/Keystore edge-case testing.
8. Resource/memory pressure enforcement.
9. Real Android device security tests.
10. Then higher-level orchestration/voice/local AI/automation.

## Constraints
- Android OS restrictions are authoritative.
- Accessibility, microphone, camera and background operation are opt-in capabilities and must not be assumed universally available.
- No real-device behavior is claimed until tested on a physical Android device.
- Release signing keys never enter the repository.
- Release APK workflow should be manually triggered rather than building a release for every push.

## Development rule
For each feature: implement → test → security review → update documentation → CI verification → record result here.

## Continuation instruction
A future chat can continue with: “Continue Ritav.ai development. Read `README.md`, `RITAV_PROJECT_STATE.md`, `RITAV_BLUEPRINT.md`, and `docs/MASTER_REQUIREMENTS_MATRIX.md`, inspect the repository, verify the current build/test state, and continue from the exact Sensitive Information Firewall verification stop point.”
