# Ritav.ai — Persistent Project State

## Current phase
Phase 0 — Architecture, threat model and secure foundation; security/runtime integration in progress.

## Source of truth
- `RITAV_BLUEPRINT.md` — engineering implementation contract.
- `RITAV_AI_MASTER_BLUEPRINT.pdf` — user-provided product/design reference.
- `docs/MASTER_REQUIREMENTS_MATRIX.md` — explicit reconciliation of both sources.
- `docs/RITAV_COMMON_AI_WORKFLOW.md` — mandatory AI/development workflow contract.

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
- NFKC/whitespace-compacted inspection is detection-only; if a sensitive pattern is found after a representation-changing normalization/compaction pass, the firewall conservatively blocks rather than attempting unsafe offset mapping/redaction.
- Unicode format-character inspection uses an explicit code-point conversion for Java `Character.getType`, avoiding a Char/int interop ambiguity in the security-critical normalization path.
- A false-positive regression case was added for generic text without secret markers.
- Whitespace/zero-width obfuscation regression cases are covered and conservatively blocked.
- Security pipeline tests verify that oversized and normalization-detected inputs are blocked before authorization is consumed.
- ExecutionBridge coverage now verifies that sensitive `inputText` is blocked at the bridge path and that a token remains usable after that blocked inspection.

## Dedicated firewall test coverage added
`app/src/test/java/ai/ritav/app/core/security/SensitiveInformationFirewallTest.kt` covers:
- OTP.
- UPI PIN.
- CVV.
- Password-context values.
- Recovery codes.
- Private keys.
- API/access/secret keys.
- Benign text.
- Generic non-secret text without sensitive markers.
- Multiple secrets.
- Overlapping detection/redaction.
- Redaction preservation of surrounding text.
- No secret value in `SensitiveMatch`.
- Exact maximum input length.
- Oversized input fail-closed behavior.
- Oversized input containing a secret.
- Whitespace-obfuscation detection.
- Zero-width obfuscation detection.
- Unicode normalization detection.
- Normalized benign text.
- Unusual/malformed Unicode input not crashing the test call.

## Continuous verification / security review notes
The current source review identified an important design boundary: transformed representations may change UTF-16 offsets, so transformed detections must not be used to redact source text unless an offset mapping is proven correct. The implementation therefore uses transformed inspection only as a conservative block signal.

The direct `security code` pattern intentionally has security-sensitive semantics because it is used for CVV/verification-code detection. Generic false-positive coverage therefore avoids asserting that an unqualified `security code + digits` phrase is always benign.

The firewall is mandatory in `SecurityExecutionPipeline` before protected-action authorization/execution. `ExecutionBridge` passes its `inputText` through that pipeline before adapter execution. Broader real model/context ingestion is still future work and must use an equivalent mandatory boundary rather than relying on callers to remember the helper.

## Important security assessment
This is a hardened **foundation**, not a claim of mathematically bug-free or production-complete security. Regex detection is not comprehensive secret detection. Unicode/obfuscation resistance, contextual detection, OCR/screen filtering, structured input isolation, and full model/context ingestion remain unfinished. No real-device security result is claimed until physical-device testing occurs.

## Current verification status — 2026-09-12
- Repository default branch: `main`.
- The required project workflow document was re-read at the beginning of this continuation.
- `README.md`, `RITAV_PROJECT_STATE.md`, `RITAV_BLUEPRINT.md`, and `docs/MASTER_REQUIREMENTS_MATRIX.md` were inspected before code changes.
- Current firewall, security pipeline, authorization gate/service, bridge, tests, Android build configuration, manifest, and recent commits were inspected.
- Current firewall blob SHA after the Unicode interop correction: `c7defb7da36b5d4e57dcf4106dc7993f87df4167`.
- Current firewall test blob SHA: `f14e81393820c44bbd32abcf60d6594c65307b02`.
- Current ExecutionBridge test blob SHA after bridge-boundary coverage: `632ed320df292f01d54131c05880365deb900002`.
- No executable Gradle wrapper was present through repository inspection, and no GitHub Actions workflow/status result is available for the current commit.
- The current GitHub commit has an empty combined-status result.
- Local Gradle execution remains unavailable in this environment.
- Therefore **Tests were not executed.** No build/test/CI pass is claimed.

## Latest commits from this continuation
- `7b349b74fa6bf5a1bf2a9b081e052fe6b34a5799` — fix: use code point conversion for Unicode format inspection.
- `58efefec7bcb39250c64ead004f000317d5110a7` — test: cover bridge sensitive-input boundary and token preservation.
- `1f9025f11dea7a785aa780a6994d2f9ab356c7ff` — test: verify obfuscated secret rejection preserves authorization token.
- `e06640b99359b91a4145925fe91b6bfce5168d86` — test: expand sensitive firewall adversarial Unicode and context coverage.
- `10cbb1cb27ce2d5a3d11afc01b2bfca15bc0a4dc` — test: cover zero-width obfuscation and punctuation-ended secrets.

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

Next action:
1. Obtain an executable Android/Gradle environment with the repository's current source.
2. Run the most specific available firewall unit tests and the security pipeline/bridge tests.
3. Repair any compile/test failures.
4. Continue adversarial Unicode/obfuscation and false-positive/false-negative review.
5. Confirm the firewall boundary remains mandatory for future AI/context ingestion.
6. Perform the consolidated security review for this layer only after executable verification is available.
7. Only after the layer is justified as complete, document the verified result and create the major-security-layer audit checkpoint.

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
A future chat can continue with: “Continue Ritav.ai development. Read `docs/RITAV_COMMON_AI_WORKFLOW.md`, `README.md`, `RITAV_PROJECT_STATE.md`, `RITAV_BLUEPRINT.md`, and `docs/MASTER_REQUIREMENTS_MATRIX.md`, inspect the repository, verify the current build/test state, and continue from the exact Sensitive Information Firewall verification stop point.”
