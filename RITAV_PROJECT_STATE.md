# Ritav.ai — Persistent Project State

## Current phase
Phase 0 — Architecture, threat model and secure foundation; security/runtime integration in progress.

## Source of truth
- `RITAV_BLUEPRINT.md` — engineering implementation contract.
- `RITAV_AI_MASTER_BLUEPRINT.pdf` — user-provided product/design reference.
- `docs/MASTER_REQUIREMENTS_MATRIX.md` — explicit reconciliation of both sources.
- `docs/RITAV_COMMON_AI_WORKFLOW.md` — mandatory AI/development workflow contract.
- `docs/RITAV_ELITE_SECURITY_ADDENDUM.md` — additive maximum-assurance security hardening rules.

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
- Natural-language API/access/secret-key assignments using `is`, `:`, or `=` are now detected as well as symbolic assignments.
- `SensitiveMatch` contains only type and source offsets; it does not contain the matched secret.
- Multiple matches are handled and overlapping matches are deduplicated.
- Redaction is performed right-to-left to preserve original UTF-16 source offsets.
- Input inspection is bounded at 16,384 characters.
- Inputs over the limit fail closed with an explicit `INPUT_TOO_LARGE` result.
- NFKC/whitespace-compacted inspection is detection-only; if a sensitive pattern is found after a representation-changing normalization/compaction pass, the firewall conservatively blocks rather than attempting unsafe offset mapping/redaction.
- Unicode format-character compaction is now iterated by Unicode code point, closing a supplementary-plane `Cf`/format-character bypass class.
- Common Greek/Cyrillic Latin look-alike characters are folded through a small explicit mapping for transformed detection, reducing homoglyph bypass risk without broad transliteration.
- Unicode decimal digits are folded by Unicode code point to ASCII digits for transformed detection, including supplementary-plane decimal digits, reducing script-specific digit bypass risk while leaving unrelated Unicode numbers allowed.
- Credential labels whose separators are removed by compaction are now matched in their compact form, including one-time-password, verification-code, security-code, UPI-PIN, pin-for-UPI, recovery-code, login-password, API-key, access-token, and secret-key labels.
- Whitespace/zero-width, Unicode normalization, homoglyph, non-ASCII decimal-digit, supplementary decimal-digit, supplementary-plane format-character, and compacted-label obfuscation regression cases are covered.
- Security pipeline tests verify that oversized and normalization-detected inputs are blocked before authorization is consumed.
- ExecutionBridge coverage verifies that sensitive `inputText` is blocked at the bridge path and a token remains usable after that blocked inspection.

## Elite security hardening rules added
`docs/RITAV_ELITE_SECURITY_ADDENDUM.md` is additive to the common workflow. It does not replace existing controls. It adds maximum-assurance review lenses, secure-by-construction requirements, adversarial attack classes, least-privilege checks, dependency/configuration hygiene, secret-safe logging, bounded resource use, asynchronous security-state checks, and an evidence-based completion gate.

`docs/RITAV_COMMON_AI_WORKFLOW.md` now explicitly requires this addendum for relevant non-trivial security/privacy/data/AI/execution work and preserves the stricter existing Ritav.ai control when requirements differ.

## Dedicated firewall test coverage added
`app/src/test/java/ai/ritav/app/core/security/SensitiveInformationFirewallTest.kt` covers:
- OTP, UPI PIN, CVV, password-context values, recovery codes, private keys, and API/access/secret keys.
- Natural-language API/access/secret-key assignments.
- Benign/generic non-secret text and secret markers without values.
- Multiple secrets and overlapping detection/redaction.
- Redaction preservation of surrounding text.
- No secret value in `SensitiveMatch`.
- Exact maximum length and oversized fail-closed behavior, including oversized input containing a secret.
- Whitespace, zero-width, Unicode-normalization, Greek/Cyrillic homoglyph, Unicode decimal-digit, supplementary-plane decimal-digit, supplementary-plane format-character, and compacted credential-label obfuscation.
- Compacted verification-code, security-code, UPI-PIN, and pin-for-UPI regression cases.
- Confusable-fold and decimal-digit false-positive regressions without sensitive markers.
- Unusual/malformed Unicode input not crashing the call.

## Continuous verification / security review notes
Transformed representations can change UTF-16 offsets, so transformed detections are never used to redact source text unless an offset mapping is proven correct. They remain conservative block signals.

The direct `security code` pattern intentionally has security-sensitive semantics because it is used for CVV/verification-code detection. Generic false-positive coverage therefore does not assert that an unqualified `security code + digits` phrase is always benign.

The explicit confusable mapping is intentionally narrow. It is defense-in-depth for common Latin look-alikes, not a complete Unicode confusables implementation. Unmapped homoglyphs and other linguistic obfuscations remain a known limitation.

Unicode decimal-digit folding is defense-in-depth: it iterates Unicode code points and converts characters classified as decimal digits when `Character.digit(codePoint, 10)` succeeds. This covers supplementary-plane decimal digits without UTF-16 surrogate misinterpretation. It does not attempt broad numeric-script transliteration.

Compacted credential-label matching specifically addresses a representation mismatch: the inspection pass removes whitespace/format characters, so labels that require a separator in ordinary text must also be recognized without that separator. This includes verification/security code and UPI marker variants. The behavior remains deterministic and detection-only; it does not broaden to arbitrary transliteration.

Natural-language API credential matching intentionally covers the common `is`, `:`, and `=` assignment forms. This is still pattern-based and cannot prove that arbitrary opaque strings are secrets without context.

The latest code-point-safe compaction change was paired with regression tests for supplementary-plane Unicode format characters inserted into both a sensitive marker and sensitive digits. These tests are committed but remain unexecuted in the current environment.

The firewall is mandatory in `SecurityExecutionPipeline` before protected-action authorization/execution. `ExecutionBridge` passes its `inputText` through that pipeline before adapter execution. Broader real model/context ingestion is still future work and must use an equivalent mandatory boundary rather than relying on callers to remember the helper.

## Important security assessment
This is a hardened **foundation**, not a claim of mathematically bug-free or production-complete security. Regex detection is not comprehensive secret detection. Unicode/obfuscation resistance, contextual detection, OCR/screen filtering, structured input isolation, and full model/context ingestion remain unfinished. No real-device security result is claimed until physical-device testing occurs.

## Current verification status — 2026-09-13
- Repository default branch: `main`.
- The required project workflow document was re-read at the beginning of this continuation.
- Relevant firewall and test sources were re-fetched before modification.
- Elite security addendum was added without removing existing security rules.
- A supplementary-plane Unicode format-character compaction weakness was identified by source inspection, then fixed and paired with regression tests.
- Latest elite security addendum commit: `52d2ea5068a79da7e378c1b0ee980184066f8e15`.
- Latest security hardening commit: `d617c99bde7583a4b6633f93f841828176126d1d`.
- Latest firewall test commit: `9a4b46ad1acfb2f3c4656a3382c1feb50f8f6db0`.
- Workflow/addendum integration commit: `02453795f19d6444db025e8fd3953cf283b6fbc2`.
- No executable Gradle wrapper is present through repository inspection, and no GitHub Actions workflow/status result is available for the current commit.
- Local Gradle execution remains unavailable in this environment.
- Therefore **Tests were not executed.** No build/test/CI pass is claimed.

## Latest commits from this continuation
- `02453795f19d6444db025e8fd3953cf283b6fbc2` — docs: link elite security addendum.
- `9a4b46ad1acfb2f3c4656a3382c1feb50f8f6db0` — test: cover supplementary Unicode format bypass.
- `d617c99bde7583a4b6633f93f841828176126d1d` — security: make firewall format filtering code-point safe.
- `52d2ea5068a79da7e378c1b0ee980184066f8e15` — docs: add elite security hardening rules.

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
