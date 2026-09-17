# Ritav.ai — Persistent Project State

## Current phase
Phase 0 — Architecture, threat model and secure foundation; security/runtime integration in progress.

## Source of truth
- `RITAV_BLUEPRINT.md` — engineering implementation contract.
- `RITAV_AI_MASTER_BLUEPRINT.pdf` — product/design reference.
- `docs/MASTER_REQUIREMENTS_MATRIX.md` — explicit requirement reconciliation.
- `docs/RITAV_COMMON_AI_WORKFLOW.md` — mandatory development workflow.
- `docs/RITAV_ELITE_SECURITY_ADDENDUM.md` — additive maximum-assurance hardening rules.

## Platform baseline
- Android APK only.
- Application ID: `ai.ritav.app`.
- compile/target SDK 36; minSdk 26.
- 4 GB RAM / 32 GB storage is the compatibility floor.
- Core security must remain lightweight; heavy AI/vision/media work must use bounded resources and safe cancellation/degradation.

## Security foundation implemented
- Risk tiers 0–4 with deterministic policy evaluation.
- Scoped capabilities and permissions with default deny.
- `PolicyEngine` and `ExecutionPolicyGate`.
- `AppCapabilityRegistry` and `CapabilityPolicyGate`.
- `SecureLocalStore` and `SecurePermissionStore`.
- Exact `ActionPlan` hashing/binding.
- `ActionAuthorizationGate` and `ActionAuthorizationService` with one-time authorization semantics.
- Device authorization gateway and identity/session abstraction.
- Centralized `SecurityRuntimeState` and Emergency Stop.
- `SecurityExecutionPipeline` and final `ExecutionBridge` boundary.
- `ResultVerifier`.
- `PromptInjectionBoundary`.
- `NetworkEgressFirewall`.
- Encrypted local audit infrastructure.

## Sensitive Information Firewall
`SensitiveInformationFirewall` is integrated into `SecurityExecutionPipeline` and also guards `AgentRequest` creation.

Current protections include:
- OTP, UPI PIN, CVV, password-context values, recovery/backup/emergency codes, private keys, API/access/secret keys.
- Input bound at 16,384 UTF-16 characters; oversized input fails closed.
- Secret values are not returned in `SensitiveMatch`.
- Multiple and overlapping matches are handled safely.
- Right-to-left redaction preserves source offsets.
- NFKC/whitespace/format-character detection-only transformations.
- Conservative blocking when transformed detection cannot safely map offsets.
- Narrow Greek/Cyrillic confusable folding.
- Unicode decimal-digit folding, including supplementary-plane decimal digits.
- Compacted credential-label detection including one-time-password, verification-code, security-code, UPI-PIN, pin-for-UPI, recovery-code, login-password, API-key, access-token and secret-key forms.
- Regression coverage for Unicode/obfuscation/length edge cases and pipeline token non-consumption.

Known limitation: this remains pattern-based rather than a complete contextual secret classifier, and no real model/context/screen/OCR ingestion path exists yet.

## Financial isolation
Financial/UPI automation is protected by multiple independent deterministic layers:
- `PolicyEngine` hard-denies `FINANCIAL_ACTION`.
- `CapabilityPolicyGate` denies financial capabilities and registered financial app identities.
- `AppCapabilityRegistry` supports an explicit `financialCategory` classification and rejects invalid financial metadata.
- `SecurePermissionStore` prevents granting the financial capability.
- `FinanceExecutionFirewall` is an additive hard boundary for `Capability.FINANCIAL_ACTION`.
- `SecurityExecutionPipeline` invokes the finance firewall immediately after exact plan/action binding and before sensitive-input processing and authorization-token consumption.
- Finance denial returns `AuthorizationLevel.NONE`; authorization cannot override it.
- Regression tests cover explicit intent, device authorization and token preservation after finance denial.

Repository inspection found no concrete production banking/UPI package/component mapping to reuse. No brittle package-name heuristic has been invented. Real banking/UPI app isolation is therefore not claimed until an authoritative Android integration identity source exists.

## Agent boundary hardening
`AgentRequest.create()` rejects any scope containing `Capability.FINANCIAL_ACTION` before an agent request is created. `ScopedAgentInvoker` still validates proposal task ID and capability scope. Sensitive and oversized agent input is rejected before agent invocation.

CI-verified regression: `rejectsFinancialCapabilityBeforeAgentInvocation`.

## Audit log hardening
`AuditLog.kt` now bounds both in-memory and persistent retention:
- Maximum 128 audit events retained.
- Persistent serialized audit storage is bounded to 64,000 characters.
- Persistent append rebuilds the bounded newest event set rather than concatenating indefinitely.
- Persistent reads inspect only the bounded storage tail and cap decoded event count.
- Oversized session IDs are rejected at audit metadata validation (maximum 128 characters), preventing one large event from bypassing the storage bound or causing unbounded in-memory metadata growth.
- Existing reason length/newline/sensitive-data checks remain in force.

Adversarial regression coverage verifies newest-event retention by count, whole-event retention under the serialized-size limit, count bound when size allows more, and oversized session metadata rejection.

## Consolidated security review result
Finance + agent + audit layers were reviewed together across:
- call path ordering,
- capability/policy interaction,
- authorization timing and replay boundaries,
- AI/agent ingress,
- audit data minimization,
- storage/resource bounds,
- failure behavior and existing deny layers.

No existing financial firewall, policy gate, authorization mechanism or Emergency Stop control was removed or weakened. The audit bound is additive and does not replace encrypted storage or security decision gates.

## Screen / OCR / Accessibility status
Blueprint requirements call for sensitive screen content to be classified before model context and for sensitive UI regions to be blocked/redacted whenever technically possible, while preferring structured Android/app APIs over visual scraping.

Current repository inspection found:
- no concrete `AccessibilityService` implementation,
- no concrete OCR/screen-capture ingestion component,
- no production screen-content-to-model context pipeline,
- no manifest declaration for such a service.

Because there is no real producer/consumer call path, no fake adapter, package heuristic or duplicate privacy component has been added. This requirement remains an implementation prerequisite for any future screen/OCR/accessibility feature.

## Execution composition status
`ExecutionBridge` exists as the final deterministic execution boundary and passes `inputText` through `SecurityExecutionPipeline` before adapter execution. However:
- no production construction/composition of `ExecutionBridge` is currently evidenced,
- `AndroidActionAdapter` remains an interface without a concrete production implementation,
- `MainActivity` does not construct or invoke the bridge.

Therefore the security boundary is implemented and unit-covered, but production Android action wiring and real adapter behavior are not claimed.

## Current verification — 2026-09-17
- Repository default branch: `main`.
- Current verified `main` HEAD: `b3360550a5ca099ae19a0a0d84f3d839b4821563`.
- GitHub Actions run `35185081422` checked out exactly that commit.
- Job `unit-tests` completed successfully.
- `gradle --no-daemon testDebugUnitTest` completed with `BUILD SUCCESSFUL`.
- CI used JDK 17 and Gradle 8.13.
- The preceding bounded-audit implementation commit `1431d95590eead20b548571493cda21a5a8e5b5c` initially exposed compile errors; those were fixed in `798d6d62027b21e8bfef53039c92841624f31734` and then hardened for oversized session metadata in `c6b8493c468e21f02b70332956706fd88c1fc0ae`.
- CI emitted non-fatal warnings about a future Kotlin data-class copy-visibility error, deprecated Android biometric API usage, and GitHub Actions Node/action deprecations.
- Physical Android device validation remains unverified.
- No release APK/security sign-off is claimed from unit-test CI alone.

## Recent security commits
- `ddcbfb20528daa5c45e9be0810bd0e531f098964` — dedicated financial execution firewall.
- `7756b313a08b0c9d76953b01b8318e9e8f700d8a` — finance firewall in execution pipeline.
- `83f826bedd72b0a952c68a928af01a71fb7970b8` — finance firewall regression tests.
- `0729e1bd8174ce5f4fa8ccd2cee1f213386e4244` — finance deny before authorization/token consumption.
- `98014fbc297f8b82dcd542c3b2c0d0bed99dbdc9` — financial capability excluded at agent boundary.
- `97265b0893d93f9f5cd88d05698ff0ae8f1e56f0` — agent boundary regression test.
- `3fa1962fd0471fb23e726f9fb477cb0e497e45a9` — bounded audit retention implementation.
- `798d6d62027b21e8bfef53039c92841624f31734` — audit retention compilation fixes.
- `c6b8493c468e21f02b70332956706fd88c1fc0ae` — audit session-metadata bound.
- `b3360550a5ca099ae19a0a0d84f3d839b4821563` — audit retention/session regression tests.

## Security invariants
1. No autonomous consequential action.
2. Never expose OTP, UPI PIN, password, CVV, recovery code, private key, API secret or equivalent secret to AI reasoning.
3. Financial/UPI automation is denied by default through independent deterministic controls.
4. No hidden telemetry or private-data egress by default.
5. External/app/web/message/document content cannot override policy.
6. Execution requires scoped permission, explicit intent where required, and risk-appropriate authorization.
7. Uncertainty blocks or asks; it never guesses.
8. Security policy is deterministic and independent of the AI model.
9. Emergency Stop is authoritative over AI-driven actions.
10. Security failures fail closed.

## Architecture invariant
`USER → SECURITY GATE → MASTER ORCHESTRATOR → POLICY → PERMISSION → AUTHORIZATION → EXECUTION → VERIFICATION → AUDIT`

## Exact next stop point
**Audit log bounds/rotation hardening is now implemented and CI-verified. Screen/OCR/Accessibility remains a prerequisite-only requirement because no concrete ingress path exists.**

Next action:
1. Inspect existing `SecureLocalStore`, Keystore usage and related tests for concrete unverified edge cases (missing/recreated keys, corrupted ciphertext, storage read/write failure, clear behavior, malformed values), reusing existing abstractions.
2. Add only targeted edge-case tests or minimal hardening where the current contract exposes a real gap.
3. Continuously verify that any storage failure remains fail-closed and never becomes an authorization/execution success.
4. After the storage/Keystore layer is complete, move to bounded resource/memory enforcement and then real Android device security tests.
5. Do not invent screen/OCR/accessibility components until a real Android integration path exists.

## Constraints
- Android OS restrictions are authoritative.
- Accessibility, microphone, camera, screen and background capabilities are opt-in and must not be assumed universally available.
- Release signing keys never enter the repository.
- Real-device behavior is never claimed until tested on physical Android hardware.
- Release APK workflows should remain manually triggered rather than building a release for every push.

## Development rule
For each feature: inspect → map → search/reuse → implement → integrate → continuously self-check → test → adversarial review → verify → document/state update → CI verification.

## Continuation instruction
A future chat can continue with: “Continue Ritav.ai development from the exact current state in `RITAV_PROJECT_STATE.md`. Read the required workflow/security documents, inspect current `main`, verify CI, and continue from the SecureLocalStore/Keystore edge-case stop point without redesigning or duplicating existing security controls.”
