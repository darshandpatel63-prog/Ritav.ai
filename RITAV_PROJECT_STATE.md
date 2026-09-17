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
- Compacted credential-label detection for common one-time/verification/UPI/password/API-secret forms.
- Regression coverage for Unicode, obfuscation and length edge cases plus pipeline token non-consumption.

Known limitation: this remains pattern-based rather than a complete contextual secret classifier, and no real model/context/screen/OCR ingress path exists yet.

## Financial isolation
Financial/UPI automation is protected by multiple independent deterministic layers:
- `PolicyEngine` hard-denies `FINANCIAL_ACTION`.
- `CapabilityPolicyGate` denies financial capabilities and registered financial app identities.
- `AppCapabilityRegistry` supports explicit `financialCategory` classification and validates its use.
- `SecurePermissionStore` prevents granting financial capability.
- `FinanceExecutionFirewall` is an additive hard boundary for `Capability.FINANCIAL_ACTION`.
- `SecurityExecutionPipeline` invokes the finance firewall after exact plan/action binding and before sensitive-input processing and authorization-token consumption.
- Finance denial returns `AuthorizationLevel.NONE`; authorization cannot override it.
- Regression tests cover explicit intent, device authorization and token preservation after finance denial.

Repository inspection found no concrete production banking/UPI package/component mapping to reuse. No brittle package-name heuristic has been invented. Real banking/UPI app isolation is therefore not claimed until an authoritative Android integration identity source exists.

## Agent boundary hardening
`AgentRequest.create()` rejects any scope containing `Capability.FINANCIAL_ACTION` before an agent request is created. `ScopedAgentInvoker` still validates proposal task ID and capability scope. Sensitive and oversized agent input is rejected before agent invocation.

CI-verified regression: financial capability cannot be admitted into an agent request scope.

## Audit log hardening
`AuditLog.kt` bounds both in-memory and persistent retention:
- maximum 128 retained audit events;
- persistent serialized audit storage bounded to 64,000 characters;
- persistent append rebuilds the newest bounded event set;
- persistent reads inspect only the bounded storage tail and cap decoded count;
- session IDs are bounded to 128 characters;
- existing reason length, newline and sensitive-data checks remain enforced.

Regression coverage verifies newest-event retention by count, serialized-size retention, count bound and oversized session metadata rejection. This layer was reviewed together with finance and agent boundaries; no existing enforcement control was removed or weakened.

## Secure local storage / Keystore checkpoint
`SecureLocalStore` remains Android Keystore-backed AES-256-GCM storage with encrypted values in `SharedPreferences`.

Additional defensive bounds now enforced before writes:
- maximum plaintext value size: 131,072 UTF-8 bytes;
- maximum preference-key name: 128 characters;
- validation applies on put/get/remove key usage;
- multibyte UTF-8 byte-count boundary is tested.

The existing authenticated-decryption behavior remains fail-closed: malformed Base64, corrupt/truncated ciphertext, authentication-tag failure or a recreated/missing Keystore key does not fall back to plaintext. Those runtime scenarios are code-traced but not physically Android/Keystore-verified.

The current CI environment runs JVM unit tests only. No existing `SecureLocalStore` instrumentation test suite or configured connected-device test runner was found, so real Keystore round-trip/tamper tests remain an explicit validation gap.

## Screen / OCR / Accessibility status
Blueprint requirements call for sensitive screen content to be classified before model context and sensitive UI regions blocked/redacted whenever technically possible, preferring structured Android/app APIs over visual scraping.

Current repository inspection found:
- no concrete `AccessibilityService` implementation,
- no concrete OCR/screen-capture ingestion component,
- no production screen-content-to-model context pipeline,
- no manifest declaration for such a service.

Because there is no real producer/consumer call path, no fake adapter or duplicate privacy component has been added.

## Execution composition status
`ExecutionBridge` exists as the final deterministic execution boundary and passes `inputText` through `SecurityExecutionPipeline` before adapter execution. However:
- no production construction/composition of `ExecutionBridge` is currently evidenced,
- `AndroidActionAdapter` remains an interface without a concrete production implementation,
- `MainActivity` does not construct/invoke the bridge.

Therefore final execution security is implemented and unit-covered, but production Android action wiring and real adapter behavior are not claimed.

## Current verification — 2026-09-17
- Latest code/test-verified checkpoint: `47a39a4215f362aa17901872ed6e3492e2b55a00`.
- GitHub Actions run `35185425350` checked out exactly that code commit.
- Job `unit-tests` completed successfully.
- `gradle --no-daemon testDebugUnitTest` completed with `BUILD SUCCESSFUL`.
- CI used JDK 17 and Gradle 8.13.
- CI logs confirm `:app:testDebugUnitTest` succeeded.
- Non-fatal warnings remain for a future Kotlin data-class copy-visibility change, deprecated Android biometric API usage, and GitHub Actions Node/action deprecations.
- `main` is now ahead of the code/test checkpoint only because documentation commits may follow; inspect current HEAD before claiming HEAD itself is test-verified.
- Physical Android device validation and connected Keystore tests remain unverified.
- No release APK/security sign-off is claimed from JVM unit-test CI alone.

## Recent security commits
- `ddcbfb20528daa5c45e9be0810bd0e531f098964` — dedicated financial execution firewall.
- `7756b313a08b0c9d76953b01b8318e9e8f700d8a` — finance firewall in pipeline.
- `0729e1bd8174ce5f4fa8ccd2cee1f213386e4244` — finance deny before authorization/token consumption.
- `98014fbc297f8b82dcd542c3b2c0d0bed99dbdc9` — finance capability excluded at agent boundary.
- `97265b0893d93f9f5cd88d05698ff0ae8f1e56f0` — agent boundary regression.
- `3fa1962fd0471fb23e726f9fb477cb0e497e45a9` — bounded audit retention implementation.
- `798d6d62027b21e8bfef53039c92841624f31734` — audit retention compilation fixes.
- `c6b8493c468e21f02b70332956706fd88c1fc0ae` — audit session-metadata bound.
- `b3360550a5ca099ae19a0a0d84f3d839b4821563` — audit retention/session regression tests.
- `08491cf74d28bf78e3c3423e44edc06a3e87eb47` — secure store value/name bounds.
- `47a39a4215f362aa17901872ed6e3492e2b55a00` — secure store input-bound regression tests.
- `3eaceab4418b05e25564c4038ae4a7abf47653be` — README continuation checkpoint sync.

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
**Bounded resource/memory pressure enforcement is next, but only through a real workload/lifecycle call path.** Repository search currently shows no local-model/vision/speech/media workload or existing resource-pressure component to reuse.

Next action:
1. Re-search current `main` for any newly introduced workload/resource path before adding code.
2. If a real heavy-work producer exists, integrate bounded admission/cancellation/degradation with the smallest existing lifecycle boundary and preserve security state.
3. If no workload producer exists, document the requirement as a future integration prerequisite rather than adding dead security code.
4. Then perform real Android device security tests for Keystore, lifecycle, Emergency Stop and execution-boundary behavior.
5. Keep Screen/OCR/Accessibility blocked until a real runtime ingress path exists.

## Constraints
- Android OS restrictions are authoritative.
- Accessibility, microphone, camera, screen and background capabilities are opt-in and must not be assumed universally available.
- Release signing keys never enter the repository.
- Real-device behavior is never claimed until tested on physical Android hardware.
- Release APK workflows should remain manually triggered rather than building a release for every push.

## Development rule
For each feature: inspect → map → search/reuse → implement → integrate → continuously self-check → test → adversarial review → verify → document/state update → CI verification.

## Continuation instruction
A future chat should read the required workflow/security documents, inspect current `main`, verify CI against the actual current HEAD, and continue from the exact bounded resource/memory pressure stop point without redesigning or duplicating existing security controls.
