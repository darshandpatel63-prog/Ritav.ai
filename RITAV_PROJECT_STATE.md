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
Ritav.ai's **target architecture** is cross-platform: Android, iOS/iPadOS, Windows, macOS, Linux and supported ChromeOS runtimes, across phones, tablets, laptops, desktops, 2-in-1 devices and other explicitly supported form factors.

**Current implementation reality:** the repository currently contains an Android application/runtime plus a JVM-targeted Kotlin Multiplatform contract layer. No native iOS/iPadOS/Windows/macOS/Linux/ChromeOS runtime implementation exists yet.

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


## Latest execution-boundary hardening — 2026-09-18
- ExecutionBridge now validates ActionPlan structure before capability evaluation or adapter execution.
- Regression coverage confirms malformed plans cannot reach the adapter.
- Source/integration/adversarial review completed across plan → capability → pipeline → authorization → adapter → result verification; no new bypass was identified in this change.
- Tests/build/CI remain unexecuted/unverified in the current environment.
- Exact commits: `00631d10274b54e907e67dbc079abdb370c50a96` (security), `a3eeef39ed6b83f0c82364884b1f637d8e65be04` (test).
- Current stop: authorization/execution-boundary validation package is implemented and reviewed; executable verification is pending.


## Authorization risk-binding hardening — 2026-09-18
- AuthorizationGate now binds minted/consumed token level to the ActionPlan risk tier, preventing a USER_CONFIRMATION token from being used as a DEVICE_AUTHENTICATION authorization for a higher-risk plan.
- Service/test coverage separates Tier-2 user confirmation from Tier-3 device authentication.
- Consolidated review lenses completed: auth/access-control, adversarial token misuse, execution integration, privacy/finance boundary interaction, and failure paths. The affected path remains fail-closed.
- Executable Gradle/CI/device verification remains unverified.
- Exact commits: `6aad41edf7e4e0f78c6b53088057a2ee743ce648`, `04a6fe995d3b84f98d5af690d87a2c7981fb2c61`, `a41f1fd7571d44f138dc308c12e31bf63950b616`, `7da4243103e77450570aaa118b9d2883ff013def`, `b9c2e69376d4b289be4c4a7c3af4e466bec07212`.

Current stop: major authorization hardening checkpoint reached; executable verification is the remaining gate before the next major security layer.


## Latest GitHub Actions efficiency audit — 2026-09-18
- Audited all repository `.github/workflows/*.yml` and `.yaml` files; exactly one workflow exists: `.github/workflows/android-test.yml`.
- Existing CI functionality is preserved: JVM unit tests, Android instrumentation-test compilation, and managed-device instrumentation remain in the same job.
- Trigger optimization is now limited to Android/runtime/build-relevant paths plus the workflow file itself. Documentation-only and unrelated changes no longer consume this CI runner.
- PR concurrency cancellation is enabled only for `pull_request` runs; direct `main` pushes are not cancelled, preserving post-merge verification.
- No indirect workflow trigger chain was found. No release workflow currently exists, so release/APK behavior was not removed or weakened.
- Gradle caching was not duplicated because `gradle/actions/setup-gradle@v4` is already present. Further SDK caching/build-step consolidation was deliberately not applied without executable evidence that it preserves the current Android managed-device verification path.
- Estimated savings cannot be stated as a fixed monthly minute number without historical workflow-duration/run-frequency data. Each path-filtered skip saves the full runner duration that the old workflow would have consumed; each cancelled superseded PR run saves its remaining runner time.
- GitHub Actions syntax semantics were cross-checked against current GitHub documentation for `paths` and conditional `concurrency`.
- Post-change workflow execution is not yet independently verified through the connected workflow-run API, which is limited for this repository; do not claim CI green from the source change alone.

Exact commits:
- `36e8afc7d94e704787f23708f76a747e827e0a5b` — CI trigger/concurrency optimization.
- `6271963c933b40e1c1486c38314575001dbaf2b4` — common workflow rule for Actions efficiency and verification preservation.

Current stop: workflow audit and minimal optimization are source-reviewed; executable post-change CI verification remains the gate before treating the optimization as fully verified.
Next action: obtain/execute a post-change Android CI run, then perform the final trigger-matrix/accidental-skip review against the actual run behavior before continuing the pending security verification work.


## Latest identity-session hardening — 2026-09-18
- SecuritySession is now opaque/private-constructor and protected-session issuance is internal-only.
- Session validity is bounded to the authenticated lifetime; future-clock use is rejected and TTL addition overflow is rejected.
- SecurityExecutionPipeline now requires exact identity-session ID == protected action/plan session ID.
- Adversarial source review identified and closed the prior source-level ability to construct a trusted session directly and the missing exact session-to-identity binding.
- Regression tests added for lifecycle bounds and mismatched identity sessions.
- Gradle/CI/managed-device execution remains unverified.

Exact commits:
- 1b990b39137931c044365ee7cae386044b030e93 — session lifecycle hardening.
- be7263894fcdb17ce471aab3ac635367ae70fa0a — session lifecycle tests.
- 5ceaf46d1d20834d9761f3aab3bec4a5393139bf — exact session binding in pipeline.
- 2782416c2bb063603da9935ac6d4af4f189e3062 — pipeline regression helper update.
- 8314a00d6b867e3193ba39c45ef611fc9b56ca5f — mismatched-session bridge regression test.
- 06d0fc17d0b0f46e2759c633413c3fa382e2355a — matching-session audit test correction.

Current stop: identity/session authorization hardening is implemented and source/adversarial reviewed; executable verification remains pending.
Next action: obtain a post-change Android CI run and verify JVM + instrumentation + managed-device tests before advancing to production execution composition.


## Latest continuation checkpoint — 2026-09-18
- Confirmed by repository tree/source search that the current executable implementation is Android-centric: `app` contains the Android application, Android security/storage/agent-boundary code, and `AndroidRitavPlatformAdapter`; `core` currently has only `commonMain` platform contracts plus a JVM target.
- No native iOS/iPadOS/Windows/macOS/Linux/ChromeOS source tree, runtime adapter, UI, packaging or native CI exists yet. Those platforms remain target architecture/roadmap, not implemented platform support.
- GitHub Actions run 145 (`35317140064`) completed with failure after the Android-test compilation path succeeded. The failure was in JVM unit-test compilation: one `ExecutionBridgeTest` fixture omitted the now-required `expectedState`, and `ResultVerificationTest` had malformed/obsolete verifier calls/structure.
- Both regressions were corrected together in `9dd36b4b8dd6e32d5e6e5677d6f84b63ed004d12` to avoid another fragmented test-fix sequence.
- New GitHub Actions run 146 (`35317701968`) is currently in progress for that test-fix commit; at the latest inspection it is still installing the managed-device system image. No success/failure conclusion is claimed yet.
- Documentation was clarified in `a832c1d5fa68054099337608c48d0269b4ec1827` so future continuations distinguish cross-platform target architecture from currently implemented Android/JVM code.
