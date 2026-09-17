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
- Concrete Android/iOS/iPadOS/Windows/macOS/Linux/ChromeOS runtime adapters.
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

The latest adapter-contract commit currently has no associated pull-request workflow run returned by the connected GitHub workflow query, so it is not claimed CI-verified yet.

## 14. Known limitations
- Cross-platform contracts are implemented; native platform implementations are not yet complete.
- No claim that Ritav currently runs on every listed OS/device.
- Native OS permission, accessibility, background, screen capture and secure-storage semantics still require platform-specific implementations and tests.
- Pattern-based sensitive detection is not complete contextual classification.
- Real model/context ingestion boundary remains future work.
- Connected-device Android instrumentation execution remains unverified.
- Release APK and non-Android packages are not yet production artifacts.

## 15. Exact next stop point
**Build the first concrete platform adapter while preserving the common security boundary.**

Next action:
1. Verify the current `core` multiplatform build/test path on the available CI environment.
2. Implement the Android platform adapter against real Android APIs without moving or duplicating the deterministic security boundary.
3. Add the first native-host capability mapping and integration tests.
4. Then expand to Apple and desktop targets through platform-specific adapters and CI runners.
5. Keep unsupported capabilities unavailable rather than emulating or bypassing platform restrictions.

## 16. Continuation rule
Future chats must treat `docs/RITAV_CROSS_PLATFORM_ARCHITECTURE.md` as the active scope decision. Existing Android-only wording in older documents is superseded for product scope, but Android security controls remain active for Android. Never weaken or duplicate the security architecture while adding platform support.
