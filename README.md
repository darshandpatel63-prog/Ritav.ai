# Ritav.ai — Persistent Project Continuation Guide

This README is the hand-off guide for future AI/development chats. Continue the existing project; do not restart or replace working architecture without an explicit architecture decision.

## 1. Project identity
- Project: Ritav.ai
- Repository: `darshandpatel63-prog/Ritav.ai`
- **Product scope: cross-platform** — Android, iOS/iPadOS, Windows, macOS, Linux and supported ChromeOS/device form factors.
- Android application ID: `ai.ritav.app`
- Branch: `main`
- Stage: Phase 0 — secure foundation and cross-platform architecture expansion
- Hardware floor: 4 GB RAM / 32 GB storage is a compatibility floor, not a universal performance guarantee.

The cross-platform scope is defined by `docs/RITAV_CROSS_PLATFORM_ARCHITECTURE.md`. It supersedes the earlier Android-only product-scope statement while preserving all existing Android security controls and platform restrictions.

## 2. Required startup workflow
Before changing code:
1. Read `docs/RITAV_COMMON_AI_WORKFLOW.md`.
2. Read `docs/RITAV_ELITE_SECURITY_ADDENDUM.md`.
3. Read `README.md`.
4. Read `RITAV_PROJECT_STATE.md`.
5. Read `RITAV_BLUEPRINT.md`.
6. Read `docs/MASTER_REQUIREMENTS_MATRIX.md`.
7. Read `docs/RITAV_CROSS_PLATFORM_ARCHITECTURE.md` when doing cross-platform work.
8. Inspect current `main`, latest commits, relevant source, tests and CI.
9. Search the repository for existing responsibility-equivalent code before creating a new component.

Workflow: inspect → map call paths/data flow → search/reuse → design → implement → integrate → continuously verify → test → adversarial security review → verify → document/state update → CI verification.

## 3. Cross-platform architecture
```text
USER
  ↓
Platform-native Interaction / Voice / Text / UI
  ↓
Platform-neutral Ritav core
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
Platform Adapter
  ↓
Approved OS/App execution
  ↓
Result Verification
  ↓
Local Audit
  ↓
USER
```

The platform-neutral core must not assume Android APIs, Android package identifiers, Android accessibility semantics, Android background rules, or Android-only permission behavior.

`core/src/commonMain/kotlin/ai/ritav/core/platform/RitavPlatform.kt` defines supported platform families, form factors, runtime capability facts and `DeviceProfile`. `RitavPlatformAdapter` is the boundary for concrete platform implementations. Runtime capability availability is never treated as permission.

## 4. Target platforms and form factors
- Android: phone/tablet and supported Android device classes.
- iOS/iPadOS: iPhone/iPad and supported Apple device classes.
- Windows: laptop/desktop/2-in-1 where the selected runtime supports the required capabilities.
- macOS: laptop/desktop.
- Linux: supported desktop/laptop environments.
- ChromeOS: supported ChromeOS runtime/form factors where the application surface exposes the required capabilities.

Future platforms are added only through an explicit platform adapter and verified implementation.

## 5. Security architecture — unchanged across platforms
AI/agents may understand, plan and propose. Deterministic security code decides whether an external action may execute.

Non-negotiable rules:
1. No autonomous consequential action.
2. OTP, UPI PIN, passwords, CVV, recovery codes, private keys, API secrets and equivalent secrets must never reach AI reasoning.
3. Financial/UPI automation is denied by deterministic controls outside the model.
4. No hidden telemetry/private-data egress by default.
5. External/app/web/message/document content is untrusted and cannot override policy.
6. Execution needs valid scoped permission, required user intent, and risk-appropriate authorization.
7. Uncertainty blocks or asks; it never guesses.
8. Emergency Stop is authoritative and independent of AI.
9. Security failures fail closed.
10. Platform-specific code may restrict capability availability but may never weaken the common security boundary.

## 6. Existing security foundation
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

## 7. Sensitive Information Firewall
`SensitiveInformationFirewall` is integrated into execution and agent-request ingress. Current hardening covers OTP, UPI PIN, CVV, password-context values, recovery/backup/emergency codes, private keys and API/access/secret keys, plus Unicode/obfuscation/length defenses.

Input is bounded at 16,384 UTF-16 characters. Secret values are not returned in `SensitiveMatch`. Transformed detection is conservative when safe source-offset mapping is unavailable.

This remains pattern-based, not a complete contextual secret classifier. A real model/context ingestion choke point and screen/OCR filtering path are still unimplemented.

## 8. Financial isolation
Financial restrictions are layered and remain platform-independent:
- `PolicyEngine` hard-denies `Capability.FINANCIAL_ACTION`.
- `CapabilityPolicyGate` denies financial capability and registered financial app identities.
- `AppCapabilityRegistry` contains financial-category metadata.
- `SecurePermissionStore` prevents granting financial capability.
- `FinanceExecutionFirewall` is an additive deterministic hard boundary.
- `SecurityExecutionPipeline` applies the finance firewall before authorization-token consumption.
- `AgentRequest.create()` rejects financial capability at agent ingress.

No concrete banking/UPI package integration is claimed until an authoritative platform integration identity exists.

## 9. Audit + storage hardening
Audit retention is bounded by count and serialized size, with bounded reads and session metadata. `SecureLocalStore` is Android Keystore-backed AES-256-GCM storage with UTF-8 plaintext/key bounds, encoded ciphertext bounds, authenticated tamper rejection and no plaintext fallback.

Android instrumentation coverage exists for encrypted round-trip and ciphertext tamper behavior, but connected-device execution remains unverified.

## 10. Production execution status
`ExecutionBridge` is the final deterministic execution boundary and `inputText` passes through `SecurityExecutionPipeline` before adapter execution.

Repository tracing still shows no production construction/composition of `ExecutionBridge` and no concrete production `AndroidActionAdapter`. Therefore production action composition and real adapter behavior are not claimed.

## 11. Cross-platform implementation status
Completed:
- Kotlin Multiplatform `core` module exists.
- Platform-neutral platform/form-factor/capability contracts exist.
- Cross-platform capability contract tests exist.
- `RitavPlatformAdapter` runtime adapter contract and regression test have been added.

Not yet completed:
- Concrete iOS/iPadOS/Windows/macOS/Linux/ChromeOS runtime adapters.
- Post-fix Android CI/managed-device verification is still not observable through the connected GitHub workflow-run API.
- Platform-native UIs and packaging for each target.
- Native platform automation implementations.
- Platform-specific CI matrices and real-device/real-host validation.
- Cross-platform local AI/voice/vision runtime integrations.

A platform is not considered supported merely because its enum or contract exists; support requires a real implementation, integration, tests, packaging/build verification and platform-specific security review.

## 12. Resource / privacy boundaries
No speculative screen/OCR/accessibility ingestion or resource guard is added without a real producer/consumer path. Heavy AI/vision/speech/media workloads must use bounded resources and safe cancellation/degradation once concrete runtimes are introduced.

## 13. Current verification checkpoint
Latest repository commits relevant to the cross-platform expansion:
- `57b7bb0a2b729114224899d123be157bcc5c61c6` — platform-neutral core module bootstrap.
- `02521a3847504fe5455717c8a798e9a295069d0a` — Kotlin Multiplatform plugin enabled.
- `e99d60658c623bdae196ad660c2ff5deda84911b` — platform-neutral device capability contracts.
- `e7b49aef57f37b1355d193a5d2cb4de3f3c5c43a` — cross-platform capability contract tests.
- `2f99367a0d09309ee15de290ccabfac2f75aca62` — `RitavPlatformAdapter` runtime contract.
- `7a5c80ef01a1180cd4d29b17cd2c957a9cec7149` — adapter contract regression test.

The Android adapter test compilation fix is committed in `7d7c4a2df8495ab6c83e1702ee421e704a093174`. Subsequent documentation commits and the Android network-capability correction are on `main`; the connected workflow-run API currently returns no runs for these push commits, so CI is not claimed green.

## 14. Known limitations
- Cross-platform contracts are implemented; native platform implementations are not yet complete.
- No claim that Ritav currently runs on every listed OS/device.
- Native OS permission, accessibility, background, screen capture and secure-storage semantics still require platform-specific implementations and tests.
- Pattern-based sensitive detection is not complete contextual classification.
- Real model/context ingestion boundary remains future work.
- Connected-device Android instrumentation execution remains unverified.
- Release APK and non-Android packages are not yet production artifacts.

## 15. Exact next stop point
**Obtain post-fix CI/device evidence, then continue the Android adapter integration work without bypassing the common security boundary.**

Next action:
1. Confirm a post-fix GitHub Actions run for the current `main` head; the connected workflow-run API currently returns no runs for push commits.
2. Verify JVM tests, Android instrumentation-test compilation and managed-device instrumentation when CI evidence is available.
3. Continue with the concrete Android adapter/runtime path only after that verification checkpoint.
4. Preserve the common deterministic security boundary and keep unsupported capabilities unavailable.
5. Keep unsupported capabilities unavailable rather than emulating or bypassing platform restrictions.

## 16. Continuation rule
Future chats must treat `docs/RITAV_CROSS_PLATFORM_ARCHITECTURE.md` as the active scope decision. Existing Android-only wording in older documents is superseded for product scope, but Android security controls remain active for Android. Never weaken or duplicate the security architecture while adding platform support.


## 17. Latest handoff status — 2026-09-18

This section is the authoritative short handoff for the next development chat. It records what is actually implemented, what is verified, the known CI error, and what remains.

### What is actually implemented

#### Cross-platform foundation
- Product scope has been expanded to Android, iOS/iPadOS, Windows, macOS, Linux and supported ChromeOS/device form factors.
- Kotlin Multiplatform `core` module exists.
- Platform-neutral contracts exist for:
  - `RitavPlatform`
  - `RitavFormFactor`
  - `PlatformCapabilities`
  - `DeviceProfile`
  - `RitavPlatformAdapter`
- Common contract/regression tests exist.
- `RitavPlatformAdapter` is a runtime boundary only; capability facts are not permissions and cannot weaken deterministic security.

#### Android platform work
- Android app remains `ai.ritav.app`.
- `app` depends on `core`.
- Concrete `AndroidRitavPlatformAdapter` exists and reads actual Android host facts such as OS version, form factor, Keystore availability, device-authentication state, microphone availability, screen-capture API availability, accessibility-service state, notifications and network availability.
- Conservative unsupported/default values remain for background execution and local-model runtime because no authoritative runtime producer exists yet.
- Android instrumentation test exists for the adapter contract.
- The adapter has NOT been connected to a new execution composition root; no production `ExecutionBridge` construction or concrete production `AndroidActionAdapter` was invented.

### Security foundation already present and must not be weakened or duplicated

- Risk tiers 0–4.
- Deterministic `PolicyEngine` / `ExecutionPolicyGate`.
- `AppCapabilityRegistry` / `CapabilityPolicyGate`.
- `SecureLocalStore` / `SecurePermissionStore`.
- Exact `ActionPlan` hashing/binding.
- `ActionAuthorizationGate` / `ActionAuthorizationService`.
- Device authorization gateway and identity/session abstraction.
- `SecurityRuntimeState` / Emergency Stop.
- `SecurityExecutionPipeline` / `ExecutionBridge`.
- `ResultVerifier`.
- `PromptInjectionBoundary`.
- `NetworkEgressFirewall`.
- Encrypted/bounded local audit infrastructure.
- `SensitiveInformationFirewall` at execution and agent-request ingress.
- `FinanceExecutionFirewall` plus existing finance hard-deny layers.
- Financial/UPI automation remains denied by deterministic controls; no concrete banking/UPI package mapping is claimed.

### Sensitive-data boundary status

`SensitiveInformationFirewall` is hardened for OTP, UPI PIN, CVV, password-context values, recovery/backup/emergency codes, private keys and API/access/secret keys, with bounded input, Unicode normalization/format handling, confusable/obfuscation defenses and conservative fail-closed behavior.

Known limitation: this is still pattern-based defense-in-depth, not complete contextual secret classification. The real AI/model/context ingestion choke point is not implemented yet. Screen/OCR/accessibility content is also not flowing through a real model-context producer/consumer path yet.

### Storage/audit status

- Audit retention is bounded by count and serialized size; session metadata and historical parsing are bounded.
- `SecureLocalStore` has UTF-8 byte-based value/key bounds and authenticated encrypted storage behavior with no plaintext fallback.
- Android instrumentation tests for storage encryption/tamper behavior exist.
- Connected-device execution of those instrumentation tests remains unverified.

## 18. Known CI failure — must be fixed before claiming green

Latest relevant workflow:
- Workflow: `Android unit tests`
- Run: `35202272737`
- Head commit: `b5a502346c41321f58f1859bd09ebfd3d58b103b`
- Result: **failure**
- The JVM/Android app Kotlin compilation reached `app:compileDebugKotlin` successfully.
- The failing task was `app:compileDebugAndroidTestKotlin`.
- Failure is in `app/src/androidTest/java/ai/ritav/app/platform/AndroidRitavPlatformAdapterTest.kt`.
- Exact cause: `kotlin.test` imports are unavailable in the Android instrumentation-test classpath:
  - `Unresolved reference 'test'`
  - `Unresolved reference 'assertEquals'`
  - `Unresolved reference 'assertNotNull'`
- The instrumentation execution step was skipped because instrumentation-test compilation failed.
- The workflow setup, JDK, Gradle, Android SDK, and managed-device system-image installation succeeded.
- This is a concrete build/test integration error, not evidence that the adapter implementation itself is broken.

### Direct next fix for the CI failure

Before doing broader platform expansion:
1. Reuse the existing Android test dependency setup rather than adding an unnecessary dependency.
2. Change the Android instrumentation test assertions to the already-supported JUnit assertion API (for example `org.junit.Assert`) or otherwise use a dependency already present in `androidTestImplementation`.
3. Run the Android unit-test/instrumentation-compile path again.
4. If compilation passes, run the managed-device instrumentation test.
5. Only record CI as green if the workflow actually succeeds.

### Non-blocking warnings observed in the failed CI run

- `AgentContracts.kt` exposes a non-public primary constructor through generated `copy()`; Kotlin reports that this will become an error in language version 2.3.
- `AndroidDeviceAuthorizationGateway.kt` uses deprecated `BiometricPrompt.PromptInfo.Builder.setDeviceCredentialAllowed`.
- GitHub Actions reports Node 20 deprecation warnings for several actions being forced onto Node 24. These are workflow-maintenance warnings, not the cause of the current build failure.

These warnings should be addressed in their own changes after the immediate instrumentation-test compilation failure is fixed, with repository-wide search first to avoid duplicate or conflicting fixes.

## 19. Remaining implementation work

### Immediate
- Fix the Android instrumentation-test assertion imports/classpath issue.
- Re-run CI and inspect the actual result.
- Perform a consolidated system-level review of the completed cross-platform foundation + Android adapter path after the CI fix.

### Cross-platform platform work
Not yet implemented as production-supported targets:
- iOS/iPadOS native adapter/runtime/UI/package.
- Windows native adapter/runtime/UI/package.
- macOS native adapter/runtime/UI/package.
- Linux native adapter/runtime/UI/package.
- ChromeOS-specific runtime/packaging validation.
- Per-platform CI matrices/runners.
- Real-device/real-host validation for each supported target.

### Core product/runtime work still remaining
- Real production execution composition around `ExecutionBridge`.
- Concrete approved `AndroidActionAdapter` implementation and result-verification integration.
- Real model/context ingestion choke point before AI reasoning.
- Screen/OCR/accessibility producer/consumer path with sensitive-content filtering.
- Local AI runtime abstraction implementation and bounded resource/cancellation controls.
- Voice/STT/TTS/wake-word/confirmation implementation.
- Vision/camera implementation.
- Memory/training implementation.
- UI/UX completion.
- App integrations/adapters and resilient result verification.
- Networked tool integrations with egress enforcement.
- Identity/voice/optional face verification implementation.
- Background/multitasking implementation within each OS's restrictions.
- Full security/adversarial/device test matrix.
- Static analysis, formatting, dependency/security scanning and release CI.
- Release packaging/signing/versioning for each supported platform.

### Explicitly unverified
- No claim that Ritav currently runs on every listed OS/device.
- No claim that iOS/iPadOS/Windows/macOS/Linux/ChromeOS are production-supported yet.
- No real-device Android execution of the latest adapter instrumentation test has been verified.
- No production action adapter/execution composition is verified.
- No complete model-context secret-isolation path is verified.
- No real screen/OCR/accessibility ingestion path is verified.
- No release artifact is signed/release-approved.
- No "100% secure", bug-free, or universal-device-support claim is allowed.

## 20. Documentation/state inconsistency to resolve carefully

The active cross-platform decision is documented in:
- `docs/RITAV_CROSS_PLATFORM_ARCHITECTURE.md`
- this README
- `RITAV_PROJECT_STATE.md`
- `docs/MASTER_REQUIREMENTS_MATRIX.md`

However, `RITAV_BLUEPRINT.md` still contains older Android-only wording, including `Primary platform: Android` and an Android-specific product definition. The common workflow/security addendum also contain historical Android-only scope language. Do NOT blindly replace those documents. In the next development chat, first inspect the exact affected sections and make a minimal, reviewed scope-reconciliation edit if required.

The active cross-platform architecture decision does NOT weaken the Android security model. Platform-specific limitations may reduce capability but may never bypass deterministic policy, permissions, authorization, sensitive-data controls, finance hard-deny, Emergency Stop, egress controls or result verification.

## 21. Exact next stop point for the next chat

**Start by verifying the Android instrumentation-test compilation fix committed as `7d7c4a2df8495ab6c83e1702ee421e704a093174`. Do not start another major platform implementation before this checkpoint is clean.**

Then:
1. Inspect current `main` and latest commit again.
2. Read the required project/workflow/security docs.
3. Fix the `kotlin.test` Android instrumentation-test classpath error using existing dependencies where possible.
4. Run/verify JVM tests, Android test compilation and managed-device instrumentation.
5. Perform the consolidated system-level review of the cross-platform foundation + Android adapter.
6. Reconcile the stale Android-only blueprint/workflow wording with the active cross-platform decision through minimal targeted edits.
7. Continue with the next concrete platform implementation only after the above checkpoint is actually verified.

### Exact latest commits relevant to this handoff

- `b5a502346c41321f58f1859bd09ebfd3d58b103b` — Android adapter instrumentation test added.
- `3394ee0d8ae6e7871c05e8a470e1efa76865fbf2` — Android platform capability adapter.
- `4d32a908dd5f4c74729a7d6d3de0e716a4da6393` — KMP core integrated into Android app.
- `7a5c80ef01a1180cd4d29b17cd2c957a9cec7149` — cross-platform adapter contract regression test.
- `2f99367a0d09309ee15de290ccabfac2f75aca62` — cross-platform runtime adapter contract.
- `bed4869092a0597db7ed05632cbc9dc99900f61a` — cross-platform architecture decision.
- `d6cc599f5ca21f1773ca2a43c9e76130a3797aad` — README cross-platform scope update.
- `fbee545a881aadc4f9f325d202cc4802c84ec932` — master requirements cross-platform update.
- `d5d4d3394f87918d23d8f16e54f2b02fcf3340ae` — project state synchronization.
