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

### Result verification hardening
- ActionPlan now includes a deterministic expected post-action state and binds it into the exact plan hash used for authorization.
- ExecutionBridge passes adapter-observed state into ResultVerifier.
- ResultVerifier fails closed on invalid/oversized expected state, missing/oversized observed state, execution failure, or exact-state mismatch.
- Regression coverage was added for missing, mismatched and oversized result evidence.
- This change has not been executed through Gradle/CI in the available environment.

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
**Verify the deterministic result-state verification change before starting another runtime/security layer.**

Next:
1. Verify the current GitHub Actions result for the latest result-verification changes when exposed by the available integration.
2. Verify JVM tests, Android instrumentation-test compilation and managed-device instrumentation.
3. Perform the consolidated system-level review of ActionPlan hashing, authorization, execution, observed-state verification, audit and failure paths.
4. If that checkpoint is clean, continue with production Android execution composition and adapter integration incrementally.
5. Keep unsupported capabilities unavailable rather than emulating or bypassing OS restrictions.

## Continuation rule
Read the required workflow/security documents and this state before development. Treat `docs/RITAV_CROSS_PLATFORM_ARCHITECTURE.md` as the active product-scope decision. Do not redesign or duplicate existing security controls.


## Latest security hardening — 2026-09-18
- Agent ingress now bounds task IDs (non-blank, max 256 chars) and defensively copies capability scope.
- `AgentRequest` remains private-constructor/factory-only, so callers cannot bypass deterministic sensitive-data and financial-capability checks.
- Regression coverage added for task-ID bounds and post-validation scope mutation.
- Source path reviewed; Gradle/CI execution remains unverified.

Exact commits: `b957fe6064b25f86311f713ca64e6400f4b962f9`, `8b1f96e01bc31e66ccc8b7ad60cf7726684c289a`, `bf33673ed43fc67aed304694f244699a49b29850f`.

## Exact next stop point
**Verify the latest security changes with JVM tests, then continue the next security/runtime work package.**


## Latest agent-boundary hardening — 2026-09-18
- Scoped agent proposals are now bounded/validated before orchestration and financial proposals are deterministically rejected.
- Regression coverage added; Gradle/CI execution remains unverified.


## Latest security hardening — 2026-09-18
- Deterministic ActionPlan structural validation now bounds app/action/expected-state/session fields.
- Authorization tokens are length-bounded; malformed plans are rejected; TTL arithmetic overflow is rejected.
- SecurityExecutionPipeline now fails closed on malformed execution requests and requires protected actions to carry a session binding.
- Regression coverage added for these boundaries.
- Source/integration review completed; Gradle/CI execution remains unverified.

Exact new commits:
- e9d0d5c153c5affdfc2d3be868d59bb7cc9c475a
- c9ed4fee861e4e6f2262651a67b5bcf7ce10be51
- 4d71177bca448ca57064a5b4461742754ebd2270
- 9adabc18ae77f7ed80254e218fb7daf9d2d302ab
- 0681f6eae43f0448a65206bf92780a0e098a90f1
- caf3ae845af41d92f2285eaf38770496dd3a9225

Current stop: authorization/pipeline input-validation hardening implemented and source-reviewed; executable verification pending.
