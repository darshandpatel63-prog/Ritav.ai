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
- Dedicated `FinanceExecutionFirewall` hard boundary for `Capability.FINANCIAL_ACTION`.
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
- `SecurityExecutionPipeline` applies the dedicated finance firewall before sensitive-input processing and before authorization-token consumption.
- `ExecutionBridge` and `SecurityExecutionPipeline` now share the same audit sink by default, so capability, pipeline, execution and verification events are not split across independent in-memory logs.
- `ExecutionBridge` now samples the audit timestamp at each emitted event rather than reusing the initial entry timestamp for later execution/verification events.

## Sensitive Information Firewall — current state
Implemented and CI-verified on the current `main` branch:
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
- Credential labels whose separators are removed by compaction are matched in compact form, including one-time-password, verification-code, security-code, UPI-PIN, pin-for-UPI, recovery-code, login-password, API-key, access-token, and secret-key labels.
- Whitespace/zero-width, Unicode normalization, homoglyph, non-ASCII decimal-digit, supplementary decimal-digit, supplementary-plane format-character, and compacted-label obfuscation regression cases are covered.
- Security pipeline tests verify that oversized and normalization-detected inputs are blocked before authorization is consumed.
- ExecutionBridge coverage verifies that sensitive `inputText` is blocked at the bridge path and a token remains usable after that blocked inspection.

## Finance Execution Firewall — current state
Implemented and integrated as an additive deterministic hard boundary:
- `FinanceExecutionFirewall` denies every `ActionRequest` with `Capability.FINANCIAL_ACTION`.
- Existing `PolicyEngine`, `CapabilityPolicyGate`, `AppCapabilityRegistry`, and `SecurePermissionStore` financial restrictions remain in force; the dedicated firewall does not replace them.
- `SecurityExecutionPipeline` invokes the finance firewall immediately after exact plan/action binding validation.
- A finance deny returns `AuthorizationLevel.NONE`, so authorization cannot convert the financial deny into an executable path.
- Finance denial occurs before sensitive-input inspection and before authorization-token consumption.
- Regression coverage proves explicit user intent and a device-authorization token do not override the finance deny and that the token remains unconsumed after the finance rejection.
- Repository search for finance/UPI/payment/app-package execution mappings found no existing concrete financial-app integration to reuse. No brittle package-name allow/deny mapping has been invented.
- Physical financial-app isolation is therefore not claimed yet; any future Android banking/UPI integration must use authoritative package/component identity from a concrete integration contract, not string heuristics.

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

`app/src/test/java/ai/ritav/app/core/security/FinanceExecutionFirewallTest.kt` covers:
- Financial capability always denied.
- Explicit intent and authorization cannot override the finance deny.
- Non-financial capabilities remain outside this boundary.

`SecurityExecutionPipelineTest.kt` covers:
- Plan/request binding mismatch denial.
- Protected-action trusted-session enforcement.
- Valid one-time authorization and replay rejection.
- Sensitive input blocking before authorization/execution.
- Oversized sensitive input blocking before authorization and token consumption.
- Normalized and obfuscated sensitive input blocking without consuming authorization.
- Explicit sensitive-data flag denial.
- Finance firewall denial before authorization and preservation of the unconsumed authorization token.

## Continuous verification / security review notes
Transformed representations can change UTF-16 offsets, so transformed detections are never used to redact source text unless an offset mapping is proven correct. They remain conservative block signals.

The direct `security code` pattern intentionally has security-sensitive semantics because it is used for CVV/verification-code detection. Generic false-positive coverage therefore does not assert that an unqualified `security code + digits` phrase is always benign.

The explicit confusable mapping is intentionally narrow. It is defense-in-depth for common Latin look-alikes, not a complete Unicode confusables implementation. Unmapped homoglyphs and other linguistic obfuscations remain a known limitation.

Unicode decimal-digit folding is defense-in-depth: it iterates Unicode code points and converts characters classified as decimal digits when `Character.digit(codePoint, 10)` succeeds. This covers supplementary-plane decimal digits without UTF-16 surrogate misinterpretation. It does not attempt broad numeric-script transliteration.

Compacted credential-label matching specifically addresses a representation mismatch: the inspection pass removes whitespace/format characters, so labels that require a separator in ordinary text must also be recognized without that separator. The behavior remains deterministic and detection-only; it does not broaden to arbitrary transliteration.

Natural-language API credential matching intentionally covers the common `is`, `:`, and `=` assignment forms. This is still pattern-based and cannot prove that arbitrary opaque strings are secrets without context.

An asynchronous authorization timing weakness was fixed: `ActionAuthorizationService.issueDeviceAuthenticationToken` now samples the clock only after successful authentication, and regression coverage simulates delayed authentication so the token gets its full TTL from post-authentication time.

The firewall is mandatory in `SecurityExecutionPipeline` before protected-action authorization/execution. `ExecutionBridge` passes its `inputText` through that pipeline before adapter execution. Broader real model/context ingestion is still future work and must use an equivalent mandatory boundary rather than relying on callers to remember the helper.

Execution-path tracing found `ExecutionBridge` and its security dependencies plus dedicated tests, but no production construction of `ExecutionBridge` and no concrete `AndroidActionAdapter` implementation beyond the interface. `SecurityRuntimeState` composes policy/audit/emergency-stop state for the UI, while `MainActivity` does not construct or invoke the execution bridge. This means the final security boundary is well-defined and unit-covered but its real Android action composition is not yet evidenced as production-wired. No new adapter or duplicate composition root was invented because the repository does not currently expose an existing concrete action implementation to integrate.

## Current verification status — 2026-09-17
- Repository default branch: `main`.
- Current verified `main` HEAD: `86b5e1513149bad4882c81a263d37eb4a52d4129`.
- GitHub Actions run `35183890834` checked out exactly that commit.
- Job `unit-tests` completed successfully.
- `gradle --no-daemon testDebugUnitTest` completed with `BUILD SUCCESSFUL`.
- The CI log showed the compile/test pipeline reaching `:app:testDebugUnitTest` successfully; no test failure was reported.
- The CI run used JDK 17 and Gradle 8.13.
- CI emitted non-fatal warnings about a future Kotlin data-class copy-visibility error, a deprecated Android biometric API, and GitHub Actions Node 20 deprecation.
- Physical Android device validation remains unverified.
- Production `ExecutionBridge` construction and concrete Android action adapter wiring remain unverified.
- No Android release APK/security sign-off is claimed from this unit-test run.

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
**Finance-app isolation remains intentionally unimplemented; do not invent package-name heuristics.**

Next action:
1. Trace the requirements matrix and Android integration contracts for any authoritative financial-app/package/component identity source.
2. Re-search the repository for existing app-identity/capability registration mechanisms before creating anything new.
3. If an authoritative identity source exists, integrate finance isolation through the existing capability/security boundary and add targeted regression tests.
4. If no authoritative identity source exists, document this as a prerequisite and move to the next already-defined security gap rather than adding brittle heuristics.
5. Keep the dedicated finance firewall and all existing financial controls unchanged.

## After the current finance-isolation decision
Expected security order remains:
1. Screen/OCR/Accessibility sensitive-content filtering.
2. Stronger app capability registry integration.
3. Mandatory unified security execution choke point.
4. Confirmation/read-back/device-auth UI.
5. Secure audit log bounds/rotation/reason-code hardening.
6. Secure storage/Keystore edge-case testing.
7. Resource/memory pressure enforcement.
8. Real Android device security tests.
9. Then higher-level orchestration/voice/local AI/automation.

## Constraints
- Android OS restrictions are authoritative.
- Accessibility, microphone, camera and background operation are opt-in capabilities and must not be assumed universally available.
- No real-device behavior is claimed until tested on a physical Android device.
- Release signing keys never enter the repository.
- Release APK workflow should be manually triggered rather than building a release for every push.

## Development rule
For each feature: implement → test → security review → update documentation → CI verification → record result here.

## Continuation instruction
A future chat can continue with: “Continue Ritav.ai development. Read `docs/RITAV_COMMON_AI_WORKFLOW.md`, `README.md`, `RITAV_PROJECT_STATE.md`, `RITAV_BLUEPRINT.md`, and `docs/MASTER_REQUIREMENTS_MATRIX.md`, inspect the repository, verify the current build/test state, and continue from the exact finance-app isolation decision stop point.”