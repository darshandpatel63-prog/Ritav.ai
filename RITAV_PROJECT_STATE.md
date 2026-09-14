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
- `ExecutionBridge` and `SecurityExecutionPipeline` now share the same audit sink by default, so capability, pipeline, execution and verification events are not split across independent in-memory logs.
- `ExecutionBridge` now samples the audit timestamp at each emitted event rather than reusing the initial entry timestamp for later execution/verification events.

## Sensitive Information Firewall — current state
The firewall remains the active development layer; **Finance Firewall has not been started**.

Implemented in the current `main` branch:
- Deterministic detection for OTP, UPI PIN, CVV, password-context values, recovery/backup codes, private keys, and API/access/secret keys.
- Natural-language API/access/secret-key assignments using `is`, `:`, or `=` are detected as well as symbolic assignments.
- `SensitiveMatch` contains only type and source offsets; it does not contain the matched secret.
- Multiple matches are handled and overlapping matches are deduplicated.
- Redaction is performed right-to-left to preserve original UTF-16 source offsets.
- Input inspection is bounded at 16,384 characters.
- Inputs over the limit fail closed with an explicit `INPUT_TOO_LARGE` result.
- NFKC/whitespace-compacted inspection is detection-only; if a sensitive pattern is found after a representation-changing normalization/compaction pass, the firewall conservatively blocks rather than attempting unsafe offset mapping/redaction.
- Unicode format-character compaction is iterated by Unicode code point, closing a supplementary-plane `Cf`/format-character bypass class.
- Common Greek/Cyrillic Latin look-alike characters are folded through a small explicit mapping for transformed detection, reducing homoglyph bypass risk without broad transliteration.
- Unicode decimal digits are folded by Unicode code point to ASCII digits for transformed detection, including supplementary-plane decimal digits, reducing script-specific digit bypass risk while leaving unrelated Unicode numbers allowed.
- Credential labels whose separators are removed by compaction are now matched in their compact form, including one-time-password, verification-code, security-code, UPI-PIN, pin-for-UPI, recovery-code, login-password, API-key, access-token, and secret-key labels.
- Whitespace/zero-width, Unicode normalization, homoglyph, non-ASCII decimal-digit, supplementary decimal-digit, supplementary-plane format-character, and compacted-label obfuscation regression cases are covered.
- Security pipeline tests verify that oversized and normalization-detected inputs are blocked before authorization is consumed.
- ExecutionBridge coverage verifies that sensitive `inputText` is blocked at the bridge path and a token remains usable after that blocked inspection.

## Elite security hardening rules added
`docs/RITAV_ELITE_SECURITY_ADDENDUM.md` is additive to the common workflow. It does not replace existing controls. It adds maximum-assurance review lenses, secure-by-construction requirements, adversarial attack classes, least-privilege checks, dependency/configuration hygiene, secret-safe logging, bounded resource use, asynchronous security-state checks, and an evidence-based completion gate.

`docs/RITAV_COMMON_AI_WORKFLOW.md` explicitly requires this addendum for relevant non-trivial security/privacy/data/AI/execution work and preserves the stricter existing Ritav.ai control when requirements differ.

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

The code-point-safe compaction change was paired with regression tests for supplementary-plane Unicode format characters inserted into both a sensitive marker and sensitive digits. These changes are committed but remain unexecuted in the current environment.

A security review identified an asynchronous authorization timing weakness: `ActionAuthorizationService.issueDeviceAuthenticationToken` previously reused the caller's pre-authentication timestamp after the device-auth callback completed. That could shorten the intended post-auth token lifetime based on how long authentication took. The service now samples the clock only after successful authentication, and a regression test simulates delayed authentication and verifies the token remains valid for the full TTL from that post-auth timestamp.

The firewall is mandatory in `SecurityExecutionPipeline` before protected-action authorization/execution. `ExecutionBridge` passes its `inputText` through that pipeline before adapter execution. Broader real model/context ingestion is still future work and must use an equivalent mandatory boundary rather than relying on callers to remember the helper.

Execution-path tracing on the current branch found `ExecutionBridge` and its security dependencies plus dedicated tests, but no production construction of `ExecutionBridge` and no concrete `AndroidActionAdapter` implementation beyond the interface. `SecurityRuntimeState` currently composes policy/audit/emergency-stop state for the UI, while `MainActivity` does not construct or invoke the execution bridge. This means the final security boundary is well-defined and unit-covered but its real Android action composition is not yet evidenced as production-wired. No new adapter or duplicate composition root was invented because the repository does not currently expose an existing concrete action implementation to integrate.

The execution bridge audit path was additionally hardened so capability-denial, pipeline-denial, authorization, execution and verification events use the shared sink by default and later events receive timestamps sampled at emission. These changes are source-reviewed but remain unexecuted in the current environment.

## Important security assessment
This is a hardened **foundation**, not a claim of mathematically bug-free or production-complete security. Regex detection is not comprehensive secret detection. Unicode/obfuscation resistance, contextual detection, OCR/screen filtering, structured input isolation, and full model/context ingestion remain unfinished. No real-device security result is claimed until physical-device testing occurs.

## Current verification status — 2026-09-14
- Repository default branch: `main`.
- The required project workflow document and elite security addendum were re-read before this continuation.
- Current `SensitiveInformationFirewall.kt`, its dedicated tests, `RITAV_PROJECT_STATE.md`, and the asynchronous authorization service/tests were inspected before modification.
- A concrete asynchronous authorization timing weakness was identified and fixed: device-authentication token issuance now uses a clock sampled after authentication succeeds rather than a caller-supplied pre-auth timestamp.
- A regression test now simulates delayed authentication and proves the token is still valid at the full TTL boundary measured from post-authentication time.
- Source-level integration/call-path inspection found no other production call sites for `ActionAuthorizationService` beyond its definition and dedicated tests, so no additional caller migration was required by this signature change.
- Source-level execution tracing found no production `ExecutionBridge` constructor call and no concrete `AndroidActionAdapter` implementation, so production execution wiring remains an explicit unfinished integration point rather than an assumed capability.
- No executable Gradle wrapper is present through repository inspection, and no GitHub Actions workflow/status result is available for the current commit.
- The repository source tree is not locally mounted for Android/JUnit execution in this environment.
- Therefore **Tests were not executed.** No Android build/test/CI pass is claimed.

## Latest commits from this continuation
- `1c3c32110b5abb1c24664f0a6d3a8709394f85d7` — security: timestamp execution audit events at emission.
- `5ebd84995c696e66812966947f6c17a963fdcb73` — test: verify execution and pipeline share audit sink.
- `5192fc2b23b9d3dd7936d28118a1c475d43cdc09` — security: unify execution and pipeline audit sinks.
- `a66ac8ae928332053d9f8c581adb9385a25a21dd` — security: share pipeline audit sink with execution bridge.
- `1053514314d63753ebfeff8ef09d512dd55b1278` — security: require explicit shared audit sink for execution bridge.
- `33c83b58776143b5c4a39a6b4fe31e2d19dd664a` — security: remove duplicate legacy sensitive-data firewall.
- `1542a0437e8ea430c1c2327e79d8b7e5e2c9a461` — docs: record async authorization timestamp hardening.
- `68cf28f0b6f109010e26269daa8b383ae6c54913` — test: prove device token uses post-auth timestamp.
- `3810f540c7ef78c92402c95aa24f346d76ba04e1` — security: mint device tokens from post-auth time.
- `0b8d729fa0488b59c57c4161e751e6b9caf44ab0` — test: cover hyphenated standalone credential formats.
- `cffa8ebc612ec3592d9b01f6956a549f8b5feb43` — security: cover hyphenated standalone credential formats.
- `5629566ac84b62a9410d601af7a52c1e24565c4c` — test: cover punctuation-obfuscated sensitive labels.
- `75be44f0e167277cec10afc61ce68b105b019ae9` — security: make audit reasons secret-safe.
- `2de57bd9546b836d79aea04c227041e155ca3fa9` — test: verify audit reasons never retain secrets.
- `9dddc15943f0629cadcec60544eb2bdd02f6f466` — fix: preserve assignment separators during punctuation compaction.
- `94e48d0d9cbf5fb13e58711aa7a8ceaec5b1a61a` — security: harden punctuation-obfuscated secret detection.

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
2. Run the most specific available firewall unit tests and the security pipeline/bridge/authorization service tests.
3. Repair any compile/test failures.
4. Continue adversarial Unicode/obfuscation and false-positive/false-negative review.
5. Confirm the firewall boundary remains mandatory for future AI/context ingestion.
6. In parallel, continue tracing the intended production action composition; do not invent an adapter or bypass the bridge when none exists.
7. Perform the consolidated security review for this layer only after executable verification is available.
8. Only after the layer is justified as complete, document the verified result and create the major-security-layer audit checkpoint.

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
