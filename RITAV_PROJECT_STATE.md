# Ritav.ai — Persistent Project State

## Current phase
Phase 0 — secure foundation and cross-platform architecture expansion; runtime integration in progress.

## Source of truth
- `RITAV_BLUEPRINT.md` — engineering implementation contract.
- `RITAV_AI_MASTER_BLUEPRINT.pdf` — product/design reference.
- `docs/MASTER_REQUIREMENTS_MATRIX.md` — explicit requirement reconciliation.
- `docs/RITAV_COMMON_AI_WORKFLOW.md` — mandatory development workflow.
- `docs/RITAV_ELITE_SECURITY_ADDENDUM.md` — additive maximum-assurance hardening rules.
- `docs/RITAV_CROSS_PLATFORM_ARCHITECTURE.md` — active product-scope expansion and portability architecture decision.

## Platform scope
Ritav.ai is no longer an Android-only product. Target platforms are Android, iOS/iPadOS, Windows, macOS, Linux and supported ChromeOS runtimes, across phones, tablets, laptops, desktops, 2-in-1 devices and other explicitly supported form factors.

The architecture is one platform-neutral core plus native platform adapters/UI/runtime implementations. A platform is not considered supported until a real implementation, integration, tests, packaging/build verification and platform-specific security review exist.

## Cross-platform foundation implemented
- Kotlin Multiplatform `core` module exists.
- `RitavPlatform` enumerates Android, iOS, iPadOS, Windows, macOS, Linux and ChromeOS.
- `RitavFormFactor` models phone, tablet, laptop, desktop and other form factors.
- `PlatformCapabilities` and `DeviceProfile` represent runtime capability facts.
- `RitavPlatformAdapter` is the platform runtime boundary.
- Common tests verify platform/form-factor coverage and that unavailable capabilities are not treated as permissions.
- Adapter contract regression coverage exists.

Relevant commits:
- `57b7bb0a2b729114224899d123be157bcc5c61c6` — platform-neutral core bootstrap.
- `02521a3847504fe5455717c8a798e9a295069d0a` — Kotlin Multiplatform plugin.
- `e99d60658c623bdae196ad660c2ff5deda84911b` — platform capability contracts.
- `e7b49aef57f37b1355d193a5d2cb4de3f3c5c43a` — cross-platform contract tests.
- `2f99367a0d09309ee15de290ccabfac2f75aca62` — runtime adapter contract.
- `7a5c80ef01a1180cd4d29b17cd2c957a9cec7149` — adapter regression test.

## Security foundation — preserved
Existing deterministic security remains authoritative across all platforms:
- Risk tiers 0–4.
- `PolicyEngine` / `ExecutionPolicyGate`.
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
- Encrypted local audit infrastructure.
- `SensitiveInformationFirewall` at execution and agent ingress.
- `FinanceExecutionFirewall` and existing financial hard-deny layers.

Platform-specific code may make a capability unavailable, but may never weaken or bypass the common security boundary.

## Sensitive Information Firewall
Current protections cover OTP, UPI PIN, CVV, password-context values, recovery/backup/emergency codes, private keys and API/access/secret keys, with input bounds and Unicode/normalization/obfuscation defenses.

Known limitation: pattern-based detection is not complete contextual classification. Real model/context ingestion and screen/OCR filtering paths are not yet implemented.

## Financial isolation
Financial/UPI automation remains hard-denied through layered deterministic controls. Explicit intent or authorization cannot override the finance deny. No concrete banking/UPI package/component mapping is claimed until an authoritative platform integration identity exists.

## Audit/storage hardening
Audit retention and session metadata are bounded. `SecureLocalStore` remains Android Keystore-backed AES-256-GCM with bounded input/ciphertext handling, authenticated tamper rejection and no plaintext fallback.

Android instrumentation tests exist for Keystore encryption round-trip and ciphertext tamper rejection, but connected-device execution is still unverified.

## Execution composition
`ExecutionBridge` remains the final deterministic execution boundary and `inputText` passes through the security pipeline before adapter execution. No production construction/composition of `ExecutionBridge` or concrete production `AndroidActionAdapter` is currently evidenced. Therefore real action execution is not claimed.

## Resource / screen privacy status
No speculative screen/OCR/accessibility ingestion or standalone resource guard has been added without a real producer/consumer path. Concrete heavy AI/vision/speech/media workloads must receive bounded-resource and safe-cancellation controls when introduced.

## Current verification
The Android instrumentation-test compilation failure from `b5a502346c41321f58f1859bd09ebfd3d58b103b` was fixed by replacing unavailable `kotlin.test` assertion imports with the existing JUnit assertion API in commit `7d7c4a2df8495ab6c83e1702ee421e704a093174`. A post-fix successful workflow/device run has not yet been observed through the connected GitHub workflow-run API; the connected commit-workflow query is limited to pull-request-triggered runs and therefore cannot establish push-run status; CI green and real-device execution remain unverified.

## Known limitations
- Native iOS/iPadOS/Windows/macOS/Linux/ChromeOS adapters are not yet complete.
- Android `RitavPlatformAdapter` is implemented; its instrumentation test source compiles against the existing JUnit Android-test API, but post-fix CI/device execution remains unverified.
- Native UI and packaging for non-Android platforms are not yet complete.
- Platform-specific CI and real-device/real-host verification are not yet complete.
- Real model/context ingestion boundary remains future work.
- Screen/OCR/accessibility runtime ingress remains unimplemented.
- Connected-device Android instrumentation execution remains unverified.
- No claim that Ritav currently runs on every listed OS/device.
- No release APK or non-Android production package sign-off is claimed.

## Exact next stop point
**Obtain post-fix CI/device evidence and continue Android adapter/runtime integration incrementally.**

Next:
1. Verify the post-fix GitHub Actions workflow on current `main`.
2. Verify JVM tests, Android instrumentation-test compilation and managed-device instrumentation.
3. Complete the consolidated system-level review of the cross-platform foundation + Android adapter.
4. Reconcile stale Android-only wording through minimal targeted documentation edits.
5. Then continue with the next concrete platform/runtime implementation.
6. Keep unsupported capabilities unavailable rather than emulating or bypassing OS restrictions.

## Continuation rule
Read the required workflow/security documents and this state before development. Treat `docs/RITAV_CROSS_PLATFORM_ARCHITECTURE.md` as the active product-scope decision. Do not redesign or duplicate existing security controls.
