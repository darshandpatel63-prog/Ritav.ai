# Ritav.ai — Persistent Project Continuation Guide

This README is a hand-off guide for future AI/development chats. Do not restart or redesign the project because the chat changed.

## 1. Project identity
- Project: Ritav.ai
- Repository: `darshandpatel63-prog/Ritav.ai`
- Platform: Android APK only
- Application ID: `ai.ritav.app`
- Branch: `main`
- Stage: Phase 0 — secure foundation and runtime security integration
- Hardware floor: 4 GB RAM / 32 GB storage

## 2. Required startup workflow
Before changing code:
1. Read `docs/RITAV_COMMON_AI_WORKFLOW.md`.
2. Read `docs/RITAV_ELITE_SECURITY_ADDENDUM.md`.
3. Read `README.md`.
4. Read `RITAV_PROJECT_STATE.md`.
5. Read `RITAV_BLUEPRINT.md`.
6. Read `docs/MASTER_REQUIREMENTS_MATRIX.md`.
7. Inspect current `main`, latest commits, relevant source, tests and CI.
8. Search the repository for existing responsibility-equivalent code before creating a new component.

Workflow: inspect → map call paths/data flow → search/reuse → design → implement → integrate → continuously verify → test → adversarial security review → verify → document/state update → CI verification.

## 3. Core architecture
```text
USER
  ↓
Interaction / Voice / Text
  ↓
Security Gate
  ↓
Intent + Context
  ↓
Master Orchestrator / Specialist Agents
  ↓
Deterministic Policy
  ↓
Capability / Permission
  ↓
Sensitive-data / Finance Firewall
  ↓
Confirmation / Device Authorization
  ↓
Approved Android Adapter
  ↓
Result Verification
  ↓
Local Audit
  ↓
USER
```

AI/agents may understand, plan and propose. Deterministic security code decides whether an external action may execute.

## 4. Non-negotiable security rules
1. No autonomous consequential action.
2. OTP, UPI PIN, passwords, CVV, recovery codes, private keys, API secrets and equivalent secrets must never reach AI reasoning.
3. Financial/UPI automation is denied by default through deterministic controls outside the model.
4. No hidden telemetry/private-data egress by default.
5. External/app/web/message/document content is untrusted and cannot override policy.
6. Execution needs valid scoped permission, required user intent, and risk-appropriate authorization.
7. Uncertainty blocks or asks; it never guesses.
8. Emergency Stop is authoritative and independent of AI.
9. Android platform restrictions are authoritative.
10. Security failures fail closed.

## 5. Implemented security foundation
Implemented components include:
- Risk tiers 0–4.
- `PolicyEngine` / `ExecutionPolicyGate`.
- `AppCapabilityRegistry` / `CapabilityPolicyGate`.
- `SecureLocalStore` / `SecurePermissionStore`.
- Exact `ActionPlan` hashing/binding.
- `ActionAuthorizationGate` / `ActionAuthorizationService`.
- Device authorization gateway and identity/session abstraction.
- `SecurityRuntimeState` and Emergency Stop.
- `SecurityExecutionPipeline` / `ExecutionBridge`.
- `ResultVerifier`.
- `PromptInjectionBoundary`.
- `NetworkEgressFirewall`.
- Encrypted local audit infrastructure.

## 6. Sensitive Information Firewall
`SensitiveInformationFirewall` is integrated into execution and agent-request ingress. Current hardening covers OTP, UPI PIN, CVV, password-context values, recovery/backup/emergency codes, private keys and API/access/secret keys, plus Unicode/obfuscation/length defenses.

Input is bounded at 16,384 UTF-16 characters. Secret values are not returned in `SensitiveMatch`. Transformed detection is conservative when safe source-offset mapping is unavailable.

This remains pattern-based, not a complete contextual secret classifier. A real model/context ingestion choke point and screen/OCR filtering path are still unimplemented.

## 7. Financial isolation
Financial restrictions are layered rather than replaced:
- `PolicyEngine` hard-denies `Capability.FINANCIAL_ACTION`.
- `CapabilityPolicyGate` denies financial capability and registered financial app identities.
- `AppCapabilityRegistry` contains explicit financial-category metadata and validates its use.
- `SecurePermissionStore` prevents granting financial capability.
- `FinanceExecutionFirewall` is an additive deterministic hard boundary.
- `SecurityExecutionPipeline` applies the finance firewall after exact plan/action binding and before sensitive-input processing and authorization-token consumption.
- `AgentRequest.create()` rejects any scope containing `Capability.FINANCIAL_ACTION`.

Explicit intent and authorization cannot override the finance deny. Repository inspection found no concrete banking/UPI package/component integration to reuse, so no brittle package-name heuristic has been invented.

## 8. Audit log hardening
Local audit is structured metadata only and is encrypted in the persistent implementation.

Current bounds:
- maximum 128 retained audit events;
- maximum 64,000 serialized characters in persistent audit retention;
- reads process only the bounded storage tail;
- session IDs are bounded to 128 characters;
- existing reason length, newline and sensitive-data protections remain enforced.

Regression coverage verifies count retention, serialized-size retention, newest-event preservation and oversized-session rejection.

## 9. Secure local storage checkpoint
`SecureLocalStore` remains Android Keystore-backed AES-GCM storage. Defensive input and read-side bounds now include:
- maximum plaintext value size: 131,072 UTF-8 bytes;
- maximum preference-key name: 128 characters;
- maximum encoded stored-value length: 174,800 characters;
- validation occurs before encryption/write and before ciphertext decoding/plaintext materialization;
- decrypted plaintext size is checked again after authenticated decryption;
- empty values remain valid;
- malformed/corrupted/tampered ciphertext fails closed with no plaintext fallback.

A correctness review fixed two edge cases in the read-side bound: the ciphertext bound now includes IV + GCM tag, and valid empty-string ciphertext is accepted.

Android instrumentation coverage now exists for actual `SecureLocalStore` behavior:
- Keystore-backed encrypted round-trip with an at-rest ciphertext/non-plaintext assertion;
- deterministic ciphertext-byte tamper test proving authenticated decryption fails instead of returning plaintext.

The instrumentation suite is compile-verified in CI, but it is not executed on a connected Android device in current CI.

## 10. Current verified state
Latest code/test-verified checkpoint:
- commit `151543ad8f00499f1a0daf27499c7c9ce70f92f4`;
- GitHub Actions run `35185960431`;
- job `unit-tests`: success;
- `gradle --no-daemon testDebugUnitTest assembleDebugAndroidTest`: `BUILD SUCCESSFUL`;
- JDK 17; Gradle 8.13.

The CI run checked out the exact `151543ad...` commit and successfully executed JVM unit tests plus Android instrumentation-test APK compilation. The instrumentation tests themselves were not executed on a physical/connected Android device.

Non-fatal CI warnings remain for a future Kotlin data-class copy-visibility change, deprecated Android biometric API usage, and GitHub Actions Node/action deprecations.

## 11. Production wiring status
`ExecutionBridge` is the final execution boundary and `inputText` passes through `SecurityExecutionPipeline` before adapter execution.

However, repository tracing currently shows:
- no production construction/composition of `ExecutionBridge`;
- no concrete production `AndroidActionAdapter` implementation;
- `MainActivity` does not construct/invoke the bridge.

Therefore final execution security is implemented and unit-covered, but real Android action composition and behavior are not claimed.

## 12. Screen / OCR / Accessibility status
Blueprint requirements require screen content to be classified before model context and sensitive UI regions blocked/redacted whenever technically possible, while preferring structured Android/app APIs over visual scraping.

Current repository state contains no concrete `AccessibilityService`, OCR/screen-capture ingestion component, manifest service declaration, or screen-content-to-model production path. No fake ingestion component has been added solely to satisfy the requirement.

## 13. Resource / memory status
Repository-wide search found no concrete local-model, vision, speech, media, WorkManager, coroutine workload, or existing resource-pressure component to reuse.

Because there is no actual heavy-work producer/lifecycle call path, no standalone resource guard was added. This remains an integration prerequisite rather than dead security code.

## 14. Known limitations
- Pattern-based sensitive detection is not complete contextual classification.
- Real model/context ingestion boundary remains future work.
- Real banking/UPI package/component integration is not present.
- Confirmation/read-back UI is not yet the final production path.
- Instrumentation tests for real Keystore behavior now exist and compile in CI, but connected-device execution is still unavailable in current CI.
- Resource/memory pressure enforcement remains pending until a real heavy-work producer exists.
- Physical Android device validation is not complete.
- Release APK/security sign-off is not claimed from unit-test CI alone.

## 15. Exact next stop point — START HERE
**Real Android device security execution.**

Before adding another major feature:
1. Run the existing `SecureLocalStoreInstrumentationTest` suite on a connected physical Android device or equivalent approved Android test target.
2. Verify Keystore round-trip, ciphertext tamper rejection, key deletion/recreation behavior, malformed storage behavior, `SecurityRuntimeState` / Emergency Stop lifecycle behavior, and the execution-boundary tests on-device where the required runtime components exist.
3. Keep resource/memory enforcement deferred until a real heavy workload is introduced; do not create a speculative workload just to exercise the guard.
4. Keep Screen/OCR/Accessibility deferred until a real runtime ingress path exists.
5. After real-device validation, continue with only the next concrete integration gap found by repository evidence.

## 16. Important recent commits
- `ddcbfb20528daa5c45e9be0810bd0e531f098964` — dedicated finance execution firewall.
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
- `ac965d58048fcbaf396d70ef78d9cdd3d0514f01` — Android instrumentation runner configuration.
- `9973a7c414accf619d30a51219446939fabf2888` — Android Keystore instrumentation tests.
- `dee9c0ee042661798f43d69e10f069889043a026` — instrumentation compile CI gate.
- `ea8ab76d65d3e895c24a05d7455641a7199f375d` — deterministic ciphertext tamper test hardening.
- `7eb8cae1ca7b48116385c70b97d359a3c4a1682b` — secure store ciphertext boundary correctness fixes.
- `151543ad8f00499f1a0daf27499c7c9ce70f92f4` — secure store encoded-input regression coverage.

## 17. Continuation instruction
A future chat should read the required workflow/security documents, inspect current `main`, verify CI against the actual current HEAD, and continue from the exact real-Android-device security execution stop point without redesigning or duplicating existing security controls.
