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
`ExecutionBridge` remains the final deterministic execution boundary and `inputText` passes through the security pipeline before adapter execution. `AndroidExecutionRuntime` is now the Android composition root and is instantiated by `MainActivity`. Its trusted capability registry is currently empty, so external action execution remains deny-by-default. `AndroidIntentActionAdapter` implements only `APP_LAUNCH` + `open`, requires a trusted package signing-certificate SHA-256 pin, and observes only `LAUNCH_DISPATCHED`; target-app UI state is not independently observed.

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
- Run #176 verified the latest capability-registry package on a managed Android device; storage-specific connected-device evidence remains separate.
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


## Latest continuation — 2026-09-18
- Latest source commit: `1fc8bd2a9be235d0f1059f73f338bd60bdb731ec` hardens Android network-capability probing so permission/security failures fail closed as `networkAccess = false` rather than crashing the adapter.
- Repository tracing confirms `MainActivity` constructs `SecurityRuntimeState` for Emergency Stop/UI state, but there is still no production `ExecutionBridge` composition and no concrete production `AndroidActionAdapter`.
- The latest known managed-device workflow before this fix (run `35326936622`) reached actual Android instrumentation and had 2/3 tests pass; the remaining adapter test failed because network capability access raised `SecurityException` when the test intentionally granted no app permissions.
- Post-fix workflow evidence for `1fc8bd2a9be235d0f1059f73f338bd60bdb731ec` is not exposed by the connected workflow-run API; therefore the fix is source-reviewed but not executable-verified.
- Documentation scope contradiction was corrected in `0e1088c001af69f7e3feb6d96125f29c59ef26f3` and `f589782a13cc72f4eaba673ee1fa5c5df3a844da`: the common workflow and elite security addendum now recognize the active cross-platform product scope while preserving the current Android implementation reality and deterministic security requirements.

### Current stop point
Android security foundation is implemented and substantially source-reviewed, but the runtime execution path is intentionally incomplete. The immediate verification gate is post-fix Android CI/device evidence.

### Exact next action
1. Obtain a post-fix Android workflow result through an available GitHub Actions path.
2. If executable evidence is green, perform the consolidated system-level review of the Android adapter + security runtime + authorization/execution/result-verification path.
3. Then design the concrete Android execution adapter/composition incrementally, only for capabilities with authoritative host integration, keeping finance and unsupported capabilities denied.


## Latest Android execution composition checkpoint — 2026-09-18
- Added `AndroidExecutionRuntime` as the single Android composition root for `SecurityRuntimeState`, `SecurityExecutionPipeline`, `ActionAuthorizationGate`, `ActionAuthorizationService`, `CapabilityPolicyGate`, and the concrete Android launch adapter.
- Connected `MainActivity` to construct that runtime with an empty `AppCapabilityRegistry`; this preserves deny-by-default until a reviewed allowlist exists.
- Added `AndroidIntentActionAdapter` with one supported operation: `Capability.APP_LAUNCH` + action `open`. It requires expected state `LAUNCH_DISPATCHED`, catches host dispatch failures, and does not claim final target-UI observation.
- Added Android package signing-certificate SHA-256 metadata to `AppCapabilitySpec`; conflicting pins and invalid digest formats are rejected. The production launch adapter verifies the installed package certificate before dispatch.
- Hardened `ActionAuthorizationService.issueDeviceAuthenticationToken()` to reject malformed/wrong-risk/oversized/invalid-clock requests and to fail closed if token issuance throws.
- Added JVM regression coverage for launch-adapter rejection/success semantics, registry certificate constraints, and device-auth failure paths.
- A post-fix Actions query currently shows run `161` pending for earlier commit `ec479436b3133774148df92bedb3cc1ece6326dd`; no workflow run is currently associated with current main head `abf5155fa3ae54ef6a604e4a51e5e509183913d9`. Therefore the current package is source-reviewed but not CI-verified.
- Do not claim green until a run actually builds/tests `abf5155fa3ae54ef6a604e4a51e5e509183913d9`.

### Consolidated review result
- Authorization: existing exact-plan, risk-bound, single-use token checks remain in the call path; device-auth service now fails closed on malformed issuance requests.
- Package identity: package name is no longer the only production launch identity signal; certificate pinning is required by the concrete adapter.
- Execution ordering: `ExecutionBridge` still gates before adapter execution, and adapter exceptions are contained.
- Sensitive/finance: no bypass was introduced; sensitive input and finance hard-deny remain upstream of adapter execution.
- Permissions/privacy: no new Android permission was added. App launch uses explicit package resolution and remains unavailable without trusted registry metadata. Broad package visibility is not introduced.
- Result verification: adapter reports only dispatch observation; final UI state remains explicitly unverified.
- Unsupported capabilities: remain unavailable because the concrete adapter handles only one bounded action and the application registry is empty.


## Verification update — 2026-09-18
- Current main head is `85e634e802dd03cdd1acd24ffa7542fb157570ca`.
- GitHub Actions run `163` (`35330728748`) is attached to this exact head and is currently `pending`; no conclusion is claimed.
- Runs `161` and `162` were cancelled before providing verification for the full current package. The current code/test head therefore remains executable-unverified until run `163` completes.
- No local Gradle/Android build was executed in this environment.


## Latest continuation checkpoint — 2026-09-18
- Current main head: `03576a484266c2b8629fbeb19de36ab5a320b230`.
- CI run #164 (`35331294938`) confirmed that the previous Android package-identity compilation fix worked: `compileDebugKotlin`, `compileDebugAndroidTestKotlin`, and `assembleDebugAndroidTest` all completed. One JVM test then failed: `ActionAuthorizationServiceTest.deviceAuthorizationRejectsInvalidPostAuthenticationClock`.
- Root cause found by tracing the trusted token-minting path: `ActionAuthorizationGate.issue()` checked TTL overflow but did not reject negative `nowEpochMillis`, so an invalid clock could still mint a token.
- Central fail-closed fix committed in `ed64a884baaaf4ad751e686703699f984d478db4`; direct regression coverage committed in `07844a039729bfe345250afd8d1026475e9b525d`.
- During integration review, a second concrete defect was found in `MainActivity.kt`: the runtime initialization was embedded in a literal `\\n` sequence on the comment line, so the source compiled but `AndroidExecutionRuntime` was not actually instantiated. Corrected in `6e63b42699507a6228bf902086cc9c0b21569ec8`.
- Added managed-device Activity startup regression test `MainActivityTest` in `03576a484266c2b8629fbeb19de36ab5a320b230`, using the existing AndroidX test stack rather than adding a dependency.
- Run #166 for the intermediate test commit was cancelled by the subsequent startup-wiring change. Run #168 targets the current head and was queued at the latest inspection. No CI/device pass is claimed yet.
- Source review confirms the resulting call path is now: `MainActivity.onCreate()` → `AndroidExecutionRuntime` → `SecurityRuntimeState` / authorization gate-service / security pipeline / capability gate / `AndroidIntentActionAdapter`.

### Current stop point
Android runtime startup wiring and negative authorization-clock hardening are implemented and source-reviewed. Executable verification remains the completion gate.

### Exact next action
Verify run #168 on current head through compilation, JVM tests, Android instrumentation-test execution and managed-device Activity startup; fix only concrete failures found by that run, then perform the consolidated system-level review before advancing to another security layer.


## Latest continuation checkpoint — 2026-09-18
- Current main head: `dd07f44cdc3b346259dcaea1260cca778b660706`.
- Verified baseline: Actions run #165 (`35331595059`) succeeded end-to-end for commit `ed64a884baaaf4ad751e686703699f984d478db4`: JVM test/build stage passed and the managed Android device ran all 3 instrumentation tests successfully.
- Follow-on authorization hardening implemented after that verified baseline:
  - `ActionAuthorizationGate.consume()` rejects negative clocks.
  - `ActionAuthorizationService.issueUserConfirmationToken()` catches authorization-gate failures and returns null.
  - Device-auth request validation is fail-closed when platform availability checks throw.
  - Asynchronous device-auth callbacks are guarded with `AtomicBoolean` so only the first callback can mint/respond with a token.
  - Clock failures and platform authentication exceptions resolve to a single fail-closed null callback.
- ExecutionBridge and SecurityExecutionPipeline now validate the ActionPlan before hashing it, preventing oversized/malformed plans from triggering unnecessary stable-hash work before rejection.
- Regression tests cover negative consumption clocks, gate clock overflow through the service, duplicate authentication callbacks, platform authentication exceptions, and clock failures.
- Current-head run #174 (`35332166648`) targets this latest code and was pending at the latest inspection. Run #171 was still in progress on an intermediate commit and run #174 had no job yet. Therefore the latest package remains executable-unverified until the current-head workflow completes.

### Consolidated review of latest source changes
- Authorization/authentication: fail-closed validation, exact plan/risk binding, bounded token lifetime, negative-clock rejection, one-shot async callback handling.
- Execution path: malformed plan rejected before hashing in both bridge and pipeline; adapter remains downstream of capability/policy/authorization checks.
- Resource boundary: invalid oversized plans no longer incur stable-hash computation before rejection.
- Finance/sensitive-data: no new bypass or relaxation; existing deterministic hard-deny layers remain upstream.
- Failure paths: platform-auth availability/authentication/clock/gate failures all resolve to denial/null rather than accidental authorization.
- Evidence status: source review is complete for this package; current-head executable verification is pending.

### Current stop point
Authorization/runtime hardening is implemented and source-reviewed. The remaining completion gate is current-head CI/device evidence.

### Exact next action
Inspect run #174 for JVM results, Android-test compilation and managed-device instrumentation; fix only concrete failures, then update project state and proceed to the next major security layer only after the consolidated completion review.


## 2026-09-18 verified capability-registry security checkpoint

- Latest capability-registry security implementation: `4d0984376ff99baaa475ae3d18488618c6a15525` — defensively freeze capability registry metadata.
- Latest regression coverage: `9ac432d0e5b69e0449958599be5b384cb988f738` — immutable capability metadata and package-name bound tests.
- GitHub Actions run #176 (`35347302181`) completed successfully for `9ac432d0e5b69e0449958599be5b384cb988f738`.
- Verified workflow steps: Android SDK/system image setup, JVM unit tests, Android instrumentation-test compilation/APK assembly, and managed-device instrumentation execution.
- No local Gradle execution was performed; verification evidence comes from the connected GitHub Actions managed-device workflow.
- Consolidated security review of the affected path covered authorization/access control, token misuse/replay, identity/session binding, capability registry immutability and bounds, finance/sensitive-data isolation, Emergency Stop, execution ordering, result verification, audit/failure paths, and resource bounds. No new CRITICAL/HIGH bypass was identified in this review.

### Remaining security/runtime limitations

- Trusted external-app registry is intentionally empty in the production composition root; external execution remains deny-by-default.
- Android launch adapter supports only `APP_LAUNCH` + `open`.
- `LAUNCH_DISPATCHED` is dispatch evidence, not independent observation of the target application's final UI/state.
- Real AI/model-context ingestion choke point is not implemented.
- Real screen/OCR/accessibility producer → sanitizer → model-consumer path is not implemented.
- Native iOS/iPadOS/Windows/macOS/Linux/ChromeOS runtime implementations are not present.
- Pattern-based sensitive-data detection remains defense-in-depth rather than complete contextual classification.

### Exact stop point

**Capability-registry hardening is implemented, consolidated-reviewed, and CI/managed-device verified. This is the mandatory independent-audit gate before the next major security layer.**

### Exact next action

Perform the independent audit checkpoint. If no blocking finding remains, begin the reviewed trusted-app allowlist / permission-grant security layer. Do not weaken or bypass finance hard-deny, sensitive-data isolation, Emergency Stop, exact plan/authorization binding, package identity verification, result verification, or fail-closed behavior.


## 2026-09-18 independent security audit checkpoint

Audit scope: current main execution/security path and repository configuration, including ActionPlan, PolicyEngine / ExecutionPolicyGate, CapabilityPolicyGate, AppCapabilityRegistry, SecurePermissionStore, authorization gate/service, identity/session binding, SecurityExecutionPipeline, ExecutionBridge, Emergency Stop, sensitive-data firewall, finance firewall, prompt-injection boundary, egress firewall, result verification, Android launch adapter, tests, and the single GitHub Actions workflow.

Findings:
- CRITICAL: none identified in the reviewed path.
- HIGH: none identified in the reviewed path.
- MEDIUM: existing architectural limitations remain: no reviewed trusted-app allowlist is populated, final target-app state is not independently observed, real model-context ingestion is absent, and screen/OCR/accessibility filtering is absent.
- LOW/INFO: PromptInjectionBoundary is currently a lightweight untrusted-content wrapper and trim operation; it does not by itself constitute a complete model-ingestion defense. This is explicitly treated as incomplete until a real context-ingestion path exists. Pattern-based sensitive-data detection is defense-in-depth, not complete contextual classification.

Adversarial checks included malformed/oversized plans and identifiers, authorization mismatch/replay/clock cases, asynchronous duplicate-auth callback handling, emergency-stop state visibility, capability-registry mutation, financial capability hard-deny, sensitive-data ingress, dynamic-code search, and workflow scope. Relevant automated regressions exist, and run #176 provides managed-device evidence for the latest registry package. This audit did not execute a new local test suite; GitHub Actions is the executable verification source available for the repository.

Audit decision: No blocking CRITICAL/HIGH finding. The mandatory independent-audit gate is satisfied for the capability-registry work package. The next major security layer may begin, subject to preserving all existing deterministic boundaries and adding its own tests, adversarial review, consolidated review, and CI verification.

Exact next action: begin the trusted-app allowlist / permission-grant lifecycle layer, first by tracing the existing AppCapabilityRegistry + SecurePermissionStore responsibility and adding authorization for grant/revoke rather than introducing a parallel permission system.


## 2026-09-18 trusted capability grant layer — in progress

- Independent audit gate completed with no CRITICAL/HIGH blocking finding.
- Implemented `AppCapabilityRegistry.riskTierFor(...)` so grant authorization can bind to exact registered app/capability/action metadata.
- Added `CapabilityGrantService` as the single deterministic mutation boundary for capability grants. It rejects unknown/unregistered targets and prohibited/financial capabilities, binds grants to an exact grant `ActionPlan`, requires a one-shot authorization token, and maps lower-risk grants to at least user-confirmation authorization while preserving device-auth requirements for tier-3 targets.
- Formalized `MutablePermissionStore` and wired `SecurePermissionStore` / `InMemoryPermissionStore` through it. Runtime policy and grant mutation now share the same permission store instance.
- Integrated `CapabilityGrantService` into `AndroidExecutionRuntime`; no trusted external-app registry entries have been populated, so external execution remains deny-by-default.
- Added adversarial unit coverage for exact-plan authorization, token replay, wrong-plan substitution, unknown/financial grant rejection, and invalid authorization clocks.

**Verification status:** CI run #186 (`35361743561`) for head `46fd251da9ce53648f198fce881f605c036087b4` is currently pending. The new grant layer is therefore not yet CI/device verified and is not complete.

**Current stop point:** trusted capability grant lifecycle implementation + integration are in place; executable verification is pending.

**Next action:** inspect run #186; fix only concrete failures, then perform the grant-layer consolidated system review and update documentation with verified results before marking this security layer complete.


## 2026-09-18 product architecture update — optional online identity and premium layer

The product requirement has been reconciled into the blueprint as a **future, optional online identity/premium entitlement layer** without making the current offline-first app dependent on a backend.

Decisions recorded:
- Core local/offline functionality must remain usable without account, backend or payment.
- Google account / Google Sign-In is one canonical identity path; "Gmail login" is not a separate security identity. Ritav must never collect a Google password.
- Future premium access is a server-authoritative entitlement, not a client-side flag.
- Future payment provider integration (Razorpay is only an example; no provider is selected yet) must use server-side verification/webhooks and replay-safe entitlement state.
- Payment secrets, OAuth client secrets, backend secrets and authoritative premium rules must never be shipped as client authority.
- Release builds may use Android R8/minification and equivalent platform protections to increase reverse-engineering cost, but the project explicitly does not claim decompilation can be made impossible.
- Local AI memory/task history/security audit data remains local by default; the future backend stores only minimum account/entitlement/online-feature data.
- A free-tier backend is feasible for early scale. Current public pricing checked 2026-09-18 shows Supabase Free includes social OAuth, 50K MAU, 500 MB database and 1 GB file storage; Firebase Spark also provides no-cost social authentication with documented limits. These are time-sensitive vendor quotas, not a guarantee of permanent free service.
- No online authentication/payment SDK or backend code was added to the current offline runtime. This is deliberate: architecture is reserved now; implementation will begin only when the online identity/premium phase is reached and after a dedicated security threat model/review.

**Current status:** architecture recorded; no executable premium/backend integration claimed.

**Exact next action:** finish and verify the current trusted capability grant lifecycle first. Later, when online identity is scheduled, perform provider selection, threat modeling, backend authorization design, secure session/token implementation, sandbox payment integration, and independent security review before production enablement.


## 2026-09-19 capability-grant lifecycle checkpoint
- Current main head: `ac403593a848d7722da2f682c2fc8197ba384141`.
- Capability grant lifecycle is implemented and integrated through `CapabilityGrantService`, `MutablePermissionStore`, `SecurityRuntimeState`, and the Android execution composition root.
- Grants require an exact `ActionPlan` binding, registry-backed target/risk lookup, non-financial capability, non-Tier-4 risk, one-time authorization consumption, and fail closed on invalid clocks or authorization mismatch.
- Emergency Stop is injected from the authoritative Android security runtime and blocks new grants. Revocation remains available while stopped because it is privilege-reducing.
- Financial capability grants are hard-denied even if registry metadata is accidentally permissive.
- GitHub Actions run #194 (`35424604601`) for `ac403593a848d7722da2f682c2fc8197ba384141` completed successfully: JVM unit tests passed, Android instrumentation tests compiled/APK assembled, and managed-device instrumentation completed successfully.
- Consolidated grant-layer review covered exact plan/target binding, authorization replay and wrong-plan substitution, risk escalation, financial/Tier-4 denial, permission-store data flow, shared runtime store wiring, Emergency Stop, identity/session semantics, revoke semantics, clock/failure paths, and bounded inputs. No new blocking bypass was identified.
- Revocation is intentionally authorization-free because it only removes an existing capability; this is a privilege-reducing operation. Granting remains authorization-gated.
- Known limitation: grant UX is not yet wired to a concrete production UI/user-confirmation flow; the security service boundary is implemented, while the trusted caller path must use `ActionAuthorizationService` for real user/device authorization.

**CURRENT STOP POINT:** capability-grant lifecycle implementation, integration, tests, managed-device verification, and consolidated review are complete. This is the security checkpoint before the next major layer.

**NEXT ACTION:** begin the reviewed trusted-app allowlist/permission-grant integration incrementally, preserving empty/deny-by-default registry behavior until authoritative package identity and signing-certificate pins are deliberately populated. Do not add speculative banking/UPI targets.

Exact checkpoint commits: `76ca79ab69caf32c4318bbd3e3b1677b48450398`, `96c481397bbfa9e1d927c2b7ed5dc9e0aaa50536`, `42c118017ca50f777ea75d66cf1c7de0004e7f7b`, `df23d57cc4595b1fd80fc61ff7b1aeb3aabe069a`, `74e327354733a3009b4b4994cb59aca579c48f0d`, `bf51d6783cbb1e91718d7f49abcdde31b11f6370`, `65e7c8504ae55bc374af9ed0078d518f34461859`, `ac403593a848d7722da2f682c2fc8197ba384141`.


## 2026-09-19 trusted-app identity and registry deny hardening
- Android package identity verification now separates installed signing-certificate reading from deterministic trust evaluation.
- A trusted package requires a registry certificate SHA-256 pin and exactly one currently installed signer whose certificate digest matches that pin; missing, unreadable, wrong, or multi-signer identities fail closed.
- Regression coverage was added for exact certificate matching, unknown packages, wrong certificates, missing certificates, and multiple signers.
- AppCapabilityRegistry now independently hard-denies FINANCIAL_ACTION in both allows(...) and riskTierFor(...), strengthening defense-in-depth even when financial metadata is misconfigured.
- Regression coverage verifies the registry cannot authorize a financial capability directly.
- Run #194 (35424604601) remains the latest completed full Android CI/device verification before these new changes. Runs #195 (35435898046) and #198 (35435963654) are still not completed at this checkpoint; therefore the new identity/registry changes are source-reviewed but not executable-verified.

CURRENT STOP POINT: trusted Android package identity + registry financial deny hardening is implemented and source-reviewed; CI/device verification is pending.

NEXT ACTION: inspect the final result of the current Android workflow, fix only demonstrated failures, then complete the consolidated trusted-app identity -> registry -> grant -> permission-store -> execution-path security review before marking this layer verified.

Exact new commits: 390a48c23d165fe14ae4a2422eea2d5798ed2076, faf3eb961e0b86781c01ba08888d3229cbe4547a, ad3b29bcbc505fc75e098dc54a4be98dd8255e8d, 394b14f23fe6c8c0a24ac51c84b0569dfe6d1500.


## 2026-09-19 trusted Android package identity — implementation corrected
- Reconstructed the Android package identity boundary from the actual repository state after CI exposed that earlier identity commits contained only malformed/incomplete fragments.
- `AndroidPackageIdentityVerifier` now depends on a dedicated `AndroidPackageSigningCertificateReader`, requires an explicitly registered package and a valid registry SHA-256 pin, requires exactly one installed signer, and compares the canonical SHA-256 digest of the installed certificate bytes to the registered pin.
- Android framework certificate access is isolated behind `ContextAndroidPackageSigningCertificateReader`, with API-P+ and legacy API paths; unreadable package metadata, missing signing information, reader exceptions, multiple signers, wrong certificates, and missing pins fail closed.
- Added regression tests for exact certificate matching, wrong certificate, multiple signers, missing certificate, reader failure, unregistered package, missing pin, and adapter dispatch blocking.
- Run #195 (`35435898046`) failed on a malformed constructor fragment in the earlier identity change; the defect was traced directly to the CI compiler log and corrected. Run #199 (`35436020212`) was on an intermediate source and is not evidence for the final implementation. Runs #200/#201/#202 were superseded/cancelled as newer commits arrived. Run #203 (`35436122945`) is the current verification run for the latest test commit and is still pending at this checkpoint.

**CURRENT STOP POINT:** trusted Android package identity verification and registry financial deny hardening are implemented and source-reviewed; final JVM/Android/managed-device execution verification is still pending.

**NEXT ACTION:** inspect Run #203 (`35436122945`) to completion; if it passes, perform the consolidated identity → registry → capability grant → permission-store → execution review and then record the verified security-layer checkpoint. If it fails, fix only the demonstrated failure and repeat verification.

Exact implementation/test commits in this correction sequence: `cb9415792f65ad5b57f851a8c741ef742daa3be1`, `3fb3ce8374b21e7e0a304f913f2011feb8725644`, `45f9b1db5cea9cb2c3c49d7a85983b313e64f30b`, `603587dac6694493b4ac5045a546fa27d49d347d`.


## 2026-09-19 security hardening checkpoint — current source audit
- Latest code checkpoint: `c99f2f59c06d7ef5be50b010ceb35f849d498ac8` closes public security bypass surfaces by keeping capability-store mutation behind the internal security composition and restricting direct Emergency Stop reset/controller surfaces to internal APIs.
- Trusted Android package identity verification is implemented and was fully verified by Run #203 (`35436122945`) on `603587dac6694493b4ac5045a546fa27d49d347d`: JVM tests, instrumentation compilation/APK assembly, and managed-device instrumentation all succeeded.
- Registry ambiguity hardening was fully source-verified and Run #204 (`35437175194`) succeeded for the security implementation commit `86c7eff93ac16b7715caf7f74256887cf783369d`.
- Current source audit confirms the affected path: MainActivity → AndroidExecutionRuntime → ExecutionBridge → CapabilityPolicyGate → SecurityExecutionPipeline → policy/finance/sensitive/session/authorization checks → Android package identity verifier → adapter dispatch → deterministic result verification → local audit.
- Finance capability remains denied at PolicyEngine, CapabilityPolicyGate, AppCapabilityRegistry, CapabilityGrantService, SecurePermissionStore, and FinanceExecutionFirewall.
- Current production trusted external-app registry remains empty/deny-by-default; package/certificate infrastructure exists but no third-party target has been deliberately authorized.
- Real model/context ingestion and screen/OCR/accessibility producer-to-model filtering are not implemented; therefore those future surfaces are not being represented as verified runtime protections.
- Release R8/minification/obfuscation and signed client release packaging are not yet configured or verified; these are final release-hardening tasks, not substitutes for deterministic security controls.
- Latest consolidated code changes are awaiting Run #211 (`35437460927`) on `c99f2f59c06d7ef5be50b010ceb35f849d498ac8`. Until it completes successfully, this checkpoint remains executable-unverified.

**CURRENT STOP POINT:** security foundation plus trusted package identity and public-surface hardening are implemented and source-audited; final CI/device verification of the latest bundled hardening is pending.

**NEXT ACTION:** inspect Run #211. On success, record the final consolidated security checkpoint and independent-audit result. On failure, fix only the demonstrated defect, rerun, and repeat the affected review.

Exact current security checkpoint commit: `c99f2f59c06d7ef5be50b010ceb35f849d498ac8`.


## 2026-09-19 final security hand-off checkpoint

### Verification
- Current code/security checkpoint before documentation-only updates: c99f2f59c06d7ef5be50b010ceb35f849d498ac8.
- Run #203 (35436122945) succeeded for 603587dac6694493b4ac5045a546fa27d49d347d, including JVM tests, Android instrumentation compilation/APK assembly, and managed-device instrumentation; this verified the corrected certificate-reader failure path and trusted package identity regression set.
- Run #204 (35437175194) succeeded for 86c7eff93ac16b7715caf7f74256887cf783369d, covering the registry ambiguity/financial metadata hardening checkpoint.
- Run #211 (35437460927) succeeded for c99f2f59c06d7ef5be50b010ceb35f849d498ac8. Its job completed the JVM unit-test task, Android instrumentation-test compilation/APK assembly, and 4 managed-device instrumentation tests on pixel2api30.
- No local Gradle execution was performed in this chat; executable evidence is from GitHub Actions.

### Consolidated security review
Reviewed end-to-end: trusted package identity -> AppCapabilityRegistry -> CapabilityPolicyGate -> CapabilityGrantService -> shared MutablePermissionStore/SecurePermissionStore -> ActionAuthorizationGate/Service -> identity/session binding -> SecurityExecutionPipeline -> ExecutionBridge -> AndroidIntentActionAdapter -> ResultVerifier/audit.

Adversarial lenses covered authorization replay/wrong-plan substitution, risk escalation, package/certificate substitution, multiple/missing/exceptional certificate reads, malformed/oversized plans and identifiers, Emergency Stop grant/execution blocking, financial hard-deny at multiple layers, permission-store mutation boundaries, session binding/expiry/clock failures, and fail-closed adapter behavior. No demonstrated CRITICAL/HIGH bypass was identified in the current implementation during this review.

### Actual remaining risks / limitations
- Production trusted package registry is intentionally empty; external-app execution therefore remains deny-by-default.
- Capability-grant UX is not wired to a production user/device-authorization UI path; the deterministic service boundary exists.
- ResultVerifier receives adapter-provided observed state; target-app final UI state is not independently observed.
- Sensitive-data detection remains pattern-based defense-in-depth; the real model/context ingestion choke point and screen/OCR/accessibility producer-to-model path are not implemented.
- Native iOS/iPadOS/Windows/macOS/Linux/ChromeOS runtimes are not implemented.
- Release R8/minification/obfuscation and signed release packaging have not been configured/verified.
- GitHub Actions run #211 used a managed Android emulator (pixel2api30); this is not equivalent to independent physical-device testing.
- The workflow emitted non-blocking deprecation warnings for the Android biometric API and GitHub Actions Node 20 compatibility; they did not fail the run.

### Handoff decision
The current implementable security-foundation scope is complete for hand-off based on repository evidence and the successful CI/device-emulator checkpoint. This is not a claim that the product is production-release-ready or fully secure on every device/runtime.

### Exact next development action
After client hand-off, the next development package is release-hardening and production composition: first establish the reviewed Android trusted-app allowlist/real user authorization UI path only when an authoritative package identity is available, then perform R8/minification/signed-release hardening and physical-device verification. Do not add speculative banking/payment/backend integrations.

Exact verified security code checkpoint: c99f2f59c06d7ef5be50b010ceb35f849d498ac8.


## 2026-09-19 Android release-hardening implementation
- Release build configuration is hardened with isDebuggable=false, R8 minification, resource shrinking, optimized default ProGuard rules, and a dedicated release rules file.
- .gitignore now excludes local Gradle/build output and common signing-key/certificate file extensions; release CI independently rejects tracked signing-material files.
- Android manifest explicitly disables backup, cleartext traffic, and points to both modern data-extraction and legacy backup rules. Modern rules exclude the complete app root from cloud backup and device-to-device transfer; legacy rules exclude the complete root as well.
- Release validation workflow is controlled by workflow_dispatch plus narrow release-critical path filters. It includes structural security-config assertions, tracked-source secret-pattern scanning, release unit tests, lintRelease, assembleRelease, bundleRelease, release APK debug-state verification, non-empty R8 mapping verification, and SHA-256 checksums for validation artifacts.
- The workflow deliberately produces unsigned validation artifacts only. No signing key or credential is fabricated or committed.
- Current implementation head: ac3e6c00d3f224099170092c6dd8cd2559c98b89.
- Exact-head release CI is in progress/pending at the time of this update; the package is not yet marked verified.

**CURRENT STOP POINT:** release hardening is implemented and consolidated source-reviewed; executable release verification remains pending.

**NEXT ACTION:** inspect the exact-head release-validation result; on success, record the verified release-hardening checkpoint and then move to the next major security layer only after the consolidated review.

## 2026-09-19 verified Android release-hardening checkpoint

### Verification
- Verified code head: `81dba435e3cd0b55d398db965dc63a92cf20bae2`.
- GitHub Actions Android release validation run #12 (`35439804241`) completed successfully on the exact head.
- The release workflow passed the structural security-config assertions, tracked non-test source secret/signing-material scan, `:core:jvmTest`, `testDebugUnitTest`, `testReleaseUnitTest`, `lintRelease`, `assembleRelease`, and `bundleRelease`.
- Final artifact verification passed: release APK is non-debug, R8 mapping is non-empty, SHA-256 checksums were generated, and the unsigned validation bundle uploaded successfully.

### Consolidated release/security review
Reviewed the release build and its integration with the existing deterministic chain: ActionPlan validation/hashing → policy → capability registry/policy gate → finance/sensitive-data firewalls → authorization/session binding → SecurityExecutionPipeline → ExecutionBridge → trusted package identity → adapter → ResultVerifier/audit. Also reviewed release flags, manifest permissions, backup/data-extraction rules, secret/signing-material scanning, shared-core test coverage, release APK debug state, R8 mapping, artifact checksums, workflow permissions/triggers/concurrency, and fail-closed behavior. No new CRITICAL/HIGH bypass was identified in this review.

### Current limitations / not verified
- Production trusted external-app registry remains intentionally empty/deny-by-default; no speculative banking/UPI package or certificate has been authorized.
- Capability-grant UX is not yet wired to a production user/device authorization UI.
- Result verification still consumes adapter-provided observed state; independent target-app UI observation is not implemented.
- Sensitive-data detection remains pattern-based defense-in-depth; real model/context and screen/OCR/accessibility ingestion choke points are not implemented.
- Native non-Android runtimes are not implemented.
- The release workflow validates unsigned artifacts; signed production packaging has not been configured because no authoritative signing credentials were provided.
- GitHub managed-device execution is emulator evidence, not physical-device testing.

**CURRENT STOP POINT:** Android release-hardening is implemented, CI-verified, and consolidated-reviewed at `81dba435e3cd0b55d398db965dc63a92cf20bae2`.

**NEXT ACTION:** before the next major security layer, perform the required independent audit checkpoint for this completed release-hardening package. After a clean audit, continue with the production trusted-app/user-authorization composition incrementally, preserving the empty deny-by-default registry until authoritative package identity/certificate data exists.

**EXACT COMMIT SHA:** `81dba435e3cd0b55d398db965dc63a92cf20bae2`.


## 2026-09-20 authorization-token emergency-stop hardening — verification pending
- Current implementation head: `1495470de42438af5288784ad47ec0818d8ab714`.
- `ActionAuthorizationService` now shares the authoritative runtime Emergency Stop controller and fails closed when the stop is active before user-confirmation token issuance, before device-authentication, and again after asynchronous device authentication completes.
- Android execution composition now injects the same runtime Emergency Stop controller into `ActionAuthorizationService`; this preserves one authoritative stop state across authorization and execution.
- Regression tests cover: stopped user-confirmation token issuance, stopped device-token issuance before authentication, and stop activation during an in-flight authentication callback.
- The immediately preceding head `f76d27c020889411c2226d99aba59eea142aea38` was successfully verified by Android workflow run #242 (`35441263184`), including JVM tests and managed-device instrumentation.
- Verification of the new authorization-token hardening is not yet complete. GitHub Actions run #245 (`35489700331`) is pending on the exact current head; do not mark this layer verified until it completes successfully.

**CURRENT STOP POINT:** authorization-token issuance now respects the authoritative Emergency Stop at the service boundary and is regression-tested in source; exact-head executable verification is pending.

**NEXT ACTION:** inspect run #245 to completion. If successful, perform the consolidated authorization-service -> execution-pipeline -> Emergency Stop -> plan-binding -> one-time-token review, then record the verified checkpoint. If it fails, fix only the demonstrated failure and rerun.


## 2026-09-20 Emergency Stop state-invalidation hardening — verification pending

- Current implementation head: `6c1b58e17d97ec5a69fc736a7d0ebb1e4e57a0ef`.
- Emergency Stop transitions are synchronized and expose an internal atomic inactive-operation boundary for security-sensitive issuance/consumption.
- Authorization tokens are bound to the Emergency Stop generation at mint time; tokens issued before a stop are rejected after activation and remain invalid after a later reset.
- Authorization-token minting and consumption are both guarded by the same authoritative stop controller; runtime composition shares that controller across policy, authorization service, authorization gate and capability-grant service.
- Protected identity sessions are bound to the Emergency Stop generation at issuance; sessions issued before a stop are rejected after activation and remain invalid after reset. Direct internal session issuance also fails closed while stopped.
- Regression tests cover gate-level stop blocking, service stop inheritance, stale-token invalidation, stop-time token consumption denial, direct stopped-session issuance denial, and stale-session invalidation after stop/reset.

### Verification status
- Source integration and adversarial review completed for Emergency Stop → authorization issuance/consumption → identity-session issuance → policy/execution path.
- The earlier exact-head workflow #245 (`35489700331`) was still pending when this package was extended; it is not evidence for the newer SHA above.
- The new exact-head CI result is not yet exposed by the available connected workflow-run API, so this package is **not marked CI-verified**.

**CURRENT STOP POINT:** Emergency Stop state-invalidation hardening is implemented and source-reviewed; executable verification is pending at `6c1b58e17d97ec5a69fc736a7d0ebb1e4e57a0ef`.

**NEXT ACTION:** obtain exact-head Android CI evidence; on success perform the consolidated review again, then pass the completed security package through the required independent-audit gate before starting the next major layer.

**EXACT COMMIT SHA:** `6c1b58e17d97ec5a69fc736a7d0ebb1e4e57a0ef`.


## 2026-09-20 final Emergency Stop hardening package — CI pending

- Current implementation/security head: `47cb40c0d2200343226a6a76354f1772c50c3604`.
- Authorization token issuance/consumption is bound to the same authoritative Emergency Stop controller and Emergency Stop generation; pre-stop tokens cannot become valid again after reset.
- Protected identity sessions are bound to the same Emergency Stop generation; pre-stop sessions cannot become valid again after reset.
- Emergency Stop transitions and security-sensitive inactive-state operations are synchronized to close activation races.
- Rejected wrong-plan authorization attempts no longer burn the valid token.
- ExecutionBridge now fails closed when its security clock is unavailable before any adapter execution; later audit timestamp sampling falls back to the already-validated execution timestamp if the injected audit clock fails.
- Regression tests cover stale-token invalidation, stop-time token denial, stale-session invalidation, direct stopped-session denial, gate/service stop binding, rejected-token preservation, and execution clock failure.

### Consolidated source/system review
Reviewed the affected path end-to-end: MainActivity → AndroidExecutionRuntime → shared Emergency Stop → authorization/session issuance → ActionAuthorizationGate → SecurityExecutionPipeline → PolicyEngine/ExecutionPolicyGate → capability/finance/sensitive controls → ExecutionBridge → adapter/result verification/audit. Adversarial checks covered activation during asynchronous authentication, stop/reset state reuse, token replay/wrong-plan substitution, session substitution/expiry, authorization risk binding, malformed inputs, and final adapter reachability. No demonstrated CRITICAL/HIGH bypass was identified in this source review.

### Verification status
- The previous workflow run #245 (`35489700331`) does not cover this final head and is not used as evidence.
- The available connected workflow API does not expose the push-triggered run for this final head, and combined commit status is empty; therefore the package remains **CI-unverified**.

**CURRENT STOP POINT:** Emergency Stop + authorization/session state-invalidation hardening is implemented, integrated, regression-tested in source, and consolidated-reviewed; exact-head Android CI verification is pending.

**NEXT ACTION:** obtain/inspect exact-head Android CI. If successful, record the verified security checkpoint and pass the completed package through the required independent-audit gate before the next major layer.

**EXACT IMPLEMENTATION SHA:** `47cb40c0d2200343226a6a76354f1772c50c3604`.


## 2026-09-20 security boundary correction — CI pending

- Latest implementation head: `9bffe276bfb44cd689366eec552512f8d4b0ac70`.
- Hardened denial/audit failure paths: invalid negative execution clocks now fail closed before adapter execution, and invalid/oversized session metadata cannot cause denial auditing to throw.
- Execution-session identifiers are now bounded to 128 characters at the execution security pipeline, matching the audit storage bound.
- Regression coverage now exercises negative pipeline/bridge clocks, oversized session metadata denial, stale authorization/session invalidation across Emergency Stop, stop-time token denial, and rejected token preservation.
- Exact-head CI verification is still unavailable through the connected workflow-run API for push-triggered runs; no CI-green claim is made.

**CURRENT STOP POINT:** authorization/session/Emergency-Stop hardening plus audit failure-path correction is source-reviewed and regression-covered; exact-head Android CI remains pending.

**NEXT ACTION:** verify the exact `9bffe276bfb44cd689366eec552512f8d4b0ac70` Android CI result. After successful CI, perform the required independent audit checkpoint before starting the next major security layer.

**EXACT IMPLEMENTATION SHA:** `9bffe276bfb44cd689366eec552512f8d4b0ac70`.


## 2026-09-20 latest security boundary correction — capability grant / Emergency Stop race

- Fixed a TOCTOU gap in `CapabilityGrantService.grant`: authorization-token consumption and the durable permission-store mutation now occur inside one shared `EmergencyStopController` critical section.
- `CapabilityGrantService` now defaults to the `ActionAuthorizationGate`'s Emergency Stop controller, reducing controller-divergence risk. The Android composition root continues to inject the runtime's single shared controller explicitly.
- Added a blocking `MutablePermissionStore` regression test proving concurrent Stop activation cannot complete while a capability grant mutation is in progress.
- Existing grant protections remain intact: exact plan/token binding, one-time consumption, risk matching, invalid-clock rejection, deny-by-default registry behavior, financial hard-deny, and authorization-free revocation.
- Latest implementation/test head: `22675b28646e5b35d3bce6232d5aaaf0519ec748`.
- Direct local test execution was not available in this environment. The connected GitHub workflow API returns no push-triggered workflow runs or combined statuses for this exact head, so exact-head CI is **not verified**.
- Source-level adversarial review of the affected grant/Stop path found no demonstrated CRITICAL/HIGH bypass.

**CURRENT STOP POINT:** capability-grant / Emergency Stop race hardening is implemented, integrated, source-reviewed, and regression-covered; exact-head CI is pending.

**NEXT ACTION:** obtain exact-head Android CI evidence; after successful CI, perform the required independent audit checkpoint before the next major security layer.

**EXACT IMPLEMENTATION/TEST SHA:** `22675b28646e5b35d3bce6232d5aaaf0519ec748`.


## 2026-09-20 independent audit checkpoint — permission / grant / Emergency Stop path

### Audit scope
Re-reviewed end-to-end: `ActionAuthorizationGate` / `ActionAuthorizationService`, `AppCapabilityRegistry`, `CapabilityGrantService`, `MutablePermissionStore` / `SecurePermissionStore`, Emergency Stop state/generation, identity-session binding, `SecurityExecutionPipeline`, `ExecutionBridge`, and the relevant regression tests.

### Audit finding and remediation
One additional concurrency weakness was identified: `SecurePermissionStore.isGranted()` and `InMemoryPermissionStore.isGranted()` were unsynchronized while grant/revoke mutations were synchronized. Both reads are now synchronized so permission authorization state cannot be read concurrently with a mutation on the same store instance.

The existing capability-grant Emergency Stop TOCTOU gap remains closed: grant validation/token consumption and permission mutation execute inside the shared Emergency Stop critical section, with the default controller derived from the authorization gate.

### Independent audit result
- CRITICAL: none identified.
- HIGH: none identified.
- MEDIUM: existing architectural limitations remain — trusted external-app registry is intentionally empty; final target-app state is not independently observed; real model/context ingestion and screen/OCR/accessibility filtering are not implemented.
- LOW/INFO: pattern-based sensitive-data detection and lightweight prompt-injection wrapper remain defense-in-depth rather than complete contextual protection.

### Verification status
- Source-level consolidated/adversarial audit: completed.
- Regression coverage: present for the affected grant/Stop path and existing authorization protections.
- Direct local test execution: not performed in this environment.
- Exact-head GitHub Actions status: not exposed by the connected workflow-run/status API for push-triggered commit `a0c1fc5c15fedb8b006ff6c4db4544b06556b890`; therefore CI remains unverified.

**CURRENT STOP POINT:** independent audit completed for the current security layer; exact-head CI remains pending.

**NEXT ACTION:** establish exact-head Android CI evidence before any next major security-layer implementation.

**EXACT IMPLEMENTATION/TEST SHA:** `a0c1fc5c15fedb8b006ff6c4db4544b06556b890`.


## 2026-09-20 CI verification checkpoint — manual Android test trigger enabled

- `.github/workflows/android-test.yml` now supports controlled `workflow_dispatch` in addition to existing push/PR path filters.
- Latest implementation/test commit: `e3475543e43da44f3daa7986800054bf25d89ae6`.
- Exact-head Android test workflow run: `#280` (`35514982136`), head SHA `e3475543e43da44f3daa7986800054bf25d89ae6`, currently pending.
- Preceding workflow-only run: `#279` (`35514948615`), head SHA `14afa47078d23b49152489ac862dc94562266540`, currently in progress.
- No run is currently being claimed as successful; local tests remain unexecuted.
- Release workflow trigger policy remains unchanged.

**CURRENT STOP POINT:** audited permission/grant/Emergency-Stop layer is implemented and source-reviewed; exact-head Android CI is pending/in progress.

**NEXT ACTION:** inspect exact-head run `#280` and its jobs/conclusion. Do not start the next major security layer until successful CI evidence is established.

**EXACT IMPLEMENTATION/TEST SHA:** `e3475543e43da44f3daa7986800054bf25d89ae6`.


## 2026-09-21 new-chat handoff checkpoint

### Current repository
- Current main HEAD before this handoff documentation update: 7e0b723a3c6ed834209e4bf702ce0ffd14881991.
- Latest implementation/test checkpoint: e3475543e43da44f3daa7986800054bf25d89ae6.
- Android test workflow Run #280 (35514982136) completed successfully on e3475543e43da44f3daa7986800054bf25d89ae6.
- Run #280 passed the JVM test task, Android instrumentation-test compilation/APK assembly and managed-device instrumentation on pixel2api30.
- The current main head at the start of this handoff is documentation-only after the verified implementation/test commit; no separate exact-head CI run for the documentation-only head is being claimed.

### What is completed
- Cross-platform architecture/contracts are present; Android is the only executable runtime currently implemented.
- Deterministic policy/capability/authorization/session/Emergency-Stop/security-pipeline/execution/result/audit layers are integrated.
- Sensitive Information Firewall and Finance Execution Firewall are integrated.
- Android trusted package identity verification is implemented; trusted registry remains intentionally empty.
- Capability-grant + permission-store + Emergency Stop race/state hardening is implemented and source-reviewed.
- Release-hardening is implemented and verified at 81dba435e3cd0b55d398db965dc63a92cf20bae2 by Run #12.

### Verification status
- Latest implementation/test checkpoint is executable-verified by Android CI Run #280.
- Independent audit of the permission/grant/Emergency-Stop path was completed before the final CI checkpoint; the audit found no demonstrated CRITICAL/HIGH bypass and identified only existing architectural limitations.
- Physical-device testing is not done.
- Native non-Android runtimes are not verified or implemented.

### Current stop point
Security foundation + permission/grant/Emergency-Stop state/race hardening is complete for the current implementation scope and has CI/device-emulator evidence. The next major layer must not be started from memory or an old chat; inspect the live repository first.

### Next action
Re-check current main and CI, then begin the production trusted-app + user-authorization composition incrementally. Keep the trusted package registry empty until authoritative package/certificate identity is available. Preserve exact action-plan binding, risk-derived authorization, Emergency Stop generation checks, deterministic finance hard-deny, sensitive-data blocking and fail-closed behavior. Add tests and perform the consolidated security review before marking the new layer complete.

### New-chat handoff
RITAV_HANDOFF.md is the authoritative detailed handoff for continuation chats and records the implementation map, verification evidence, limitations, next direction and exact commits.
## 2026-09-21 trusted-app + user-authorization composition checkpoint

### CURRENT STOP POINT
The Android trusted-app + user-authorization composition layer is implemented, integrated, regression-tested, consolidated-reviewed and executable-verified at implementation/test SHA `3c28e9319f3a01702e5af93f61aec3c6976d9d40`. The production trusted-app registry remains intentionally empty, so external-app execution and capability grants remain deny-by-default.

### COMPLETED
- Added sanitized registry capability candidates that do not expose certificate material to UI code and exclude financial, Tier-4 and unpinned entries from the grant UI.
- Added `CapabilityGrantCoordinator` as the trusted caller composition around the existing `ActionAuthorizationService`, `CapabilityGrantService` and `IdentitySessionManager`.
- Bound grant plans to the active identity session and revalidated the session inside the existing Emergency Stop critical section before durable permission mutation.
- Wired Tier-2 explicit confirmation and Tier-3 device authentication to the exact grant `ActionPlan` through existing one-time authorization controls.
- Added Android `PermissionCenter` presentation flow and kept the UI free of deterministic security decisions/token issuance/permission-store mutation.
- Added adversarial/regression coverage for candidate sanitization, wrong plans, explicit-confirmation denial, matching/mismatched/stale sessions, device-auth success/failure and Emergency Stop blocking.

### VERIFIED
- GitHub Actions Android workflow Run #283 (`35557741305`) completed successfully on exact implementation/test SHA `3c28e9319f3a01702e5af93f61aec3c6976d9d40`.
- Run #283 completed JVM unit tests, Android instrumentation-test compilation/APK assembly and managed-device instrumentation on `pixel2api30`.
- Source/consolidated review covered UI-to-coordinator call paths, exact plan/risk/session binding, Emergency Stop races, permission-store boundaries, finance hard-deny, certificate prerequisites and fail-closed malformed/failure paths; no new demonstrated CRITICAL/HIGH bypass was identified.

### NOT VERIFIED
- Physical-device testing.
- Signed production release credentials/package.
- Native non-Android runtime implementations.
- Independent final target-app UI/result observation.
- Real model/context ingestion and screen/OCR/accessibility-to-model filtering.

### KNOWN LIMITATIONS
- The trusted external-app registry is empty by design; no real external application can currently be granted through the production UI.
- The authorization composition is wired, but external execution remains unavailable until authoritative trust entries are reviewed and populated.
- Sensitive-information defense is still pattern-based defense-in-depth rather than complete contextual secret classification.

### NEXT ACTION
Continue with the reviewed trusted-app allowlist/trust-entry provisioning path only when authoritative package identity and signing-certificate evidence is available. Then implement independent target-app result/UI observation. Do not invent banking/UPI identities, certificate pins, OAuth/backend integrations or native-platform support.

### EXACT COMMIT SHA
Implementation/test verification anchor: `3c28e9319f3a01702e5af93f61aec3c6976d9d40`.
This state update is documentation-only after that verified implementation/test commit and is not itself an additional Android CI verification checkpoint.

## 2026-09-21 trusted-package provisioning evidence checkpoint

### CURRENT STOP POINT
Read-only Android trusted-package provisioning evidence is implemented at `66c924927fead15c83d7abeb039f3cc43cee02df`. It does not mutate the trusted registry or authorize execution. The production registry remains empty/deny-by-default.

### COMPLETED
- Added `AndroidTrustedPackageEvidenceReader` using the existing package signing-certificate reader boundary.
- Added bounded package-name validation.
- Added single-signer requirement and SHA-256 certificate evidence generation.
- Added fail-closed regression coverage for malformed input, missing certificate data, multiple signers, and digest-only evidence.
- Preserved the existing `AndroidPackageIdentityVerifier` as the authoritative execution-time trust decision.

### VERIFIED
- Previous implementation checkpoint Run #283 remains verified on `3c28e9319f3a01702e5af93f61aec3c6976d9d40`.
- Run #284 (`35571240222`) has been triggered for `66c924927fead15c83d7abeb039f3cc43cee02df` and is currently pending/in progress.

### NOT VERIFIED
- Run #284 has not completed yet.
- No physical-device testing.
- No persistent trust-entry provisioning/mutation path.
- No real external-app trust entry has been configured.

### KNOWN LIMITATIONS
- Evidence is not trust. A package digest must not be treated as trusted merely because it was read successfully.
- The production registry remains empty until an explicit reviewed trust-decision/persistence path exists.
- Target-app result/UI observation remains outstanding.

### NEXT ACTION
1. Obtain the Run #284 result for the exact implementation SHA.
2. If green, perform the consolidated system review for this provisioning-evidence sub-layer and record the verified checkpoint.
3. Then implement the separate explicit trust-entry decision/persistence path without inventing package identities or certificate pins.
4. Keep finance/UPI hard-deny and all existing authorization/session/Emergency-Stop boundaries unchanged.

### EXACT COMMIT SHA
Implementation/test SHA: `66c924927fead15c83d7abeb039f3cc43cee02df`.

## 2026-09-21 trusted-package evidence — verified and independently reviewed

### CURRENT STOP POINT
The read-only Android trusted-package provisioning-evidence layer is implemented and exact-head CI verified at `2ee78ca995cab7fa1bd9dd0794c0ac2a0855c8b3`. The production trusted external-app registry remains empty/deny-by-default.

### COMPLETED
- Added bounded read-only Android package identity evidence using the existing `AndroidPackageSigningCertificateReader`.
- Required a syntactically bounded package name and exactly one installed signer.
- Added empty-certificate, 64 KiB certificate-size, and digest-operation failure fail-closed handling.
- Added regression coverage for malformed/oversized package names, certificate-reader failure, certificate-size boundaries, multiple signers, and digest-only evidence.
- Preserved the execution-time `AndroidPackageIdentityVerifier` as the authoritative trust decision; evidence does not equal trust.

### VERIFIED
- Run #290 (`35619464754`) succeeded on exact SHA `2ee78ca995cab7fa1bd9dd0794c0ac2a0855c8b3`.
- JVM/unit tests, instrumentation-test compilation/APK assembly, and managed-device instrumentation on `pixel2api30` all completed successfully.
- The intermediate Run #287 compile failure was caused by a Kotlin regex escaping defect in that intermediate commit and was corrected before the final verified checkpoint.
- The required independent review was completed after exact-head CI. Security, QA/adversarial, red-team, privacy/permissions, platform/integration, resource, and Guardian lenses found no demonstrated CRITICAL/HIGH/MEDIUM bypass.

### NOT VERIFIED
- Physical-device testing.
- Signed production release credentials/package.
- Native non-Android runtime implementations.
- Independent final target-app UI/result observation.
- Real model/context ingestion and screen/OCR/accessibility-to-model filtering.
- Any real production trusted-app entry; the registry is intentionally empty.

### KNOWN LIMITATIONS
- Evidence is provisioning evidence only. A successful evidence read cannot authorize execution.
- The package-name grammar and 64 KiB certificate bound are conservative and may reject unusual-but-valid platform metadata; they only reduce availability.
- No persistent trust-entry decision/mutation path exists yet.
- Sensitive-information protection remains pattern-based defense-in-depth rather than complete contextual secret classification.

### NEXT ACTION
Implement the separate explicit trust-entry decision/persistence layer. It must consume authoritative package/certificate evidence, require deterministic reviewed approval, persist only validated trust metadata through existing secure local storage boundaries, keep financial/Tier-4 entries denied, and leave the production registry empty when no reviewed entries exist. Do not invent package identities or certificate pins.

### EXACT COMMIT SHA
Verified implementation/test checkpoint: `2ee78ca995cab7fa1bd9dd0794c0ac2a0855c8b3`.


## 2026-09-22 verified trusted-app trust-entry provisioning checkpoint

### CURRENT STOP POINT
The explicit deterministic trusted-app trust-entry decision/persistence path is implemented, integrated and exact-head CI verified at `c5dad4ce6db7bf00615e649e0b77e05b033a620e`. Run #298 (`35679864201`) succeeded with JVM/unit tests, Android instrumentation-test compilation/APK assembly and managed-device instrumentation on `pixel2api30`.
### COMPLETED
- Durable reviewed trust-entry state is persisted through the existing `SecureLocalStore`; invalid/corrupt state loads as deny-by-default.
- Trust-entry mutation is constrained to `APP_LAUNCH + open`, Tier-1 execution metadata, sensitive-content blocking, non-financial status and an authoritative 64-hex SHA-256 signing-certificate digest.
- Provisioning requires a valid trusted identity session, exact package/certificate evidence, exactly one signer, exact plan binding, device authentication and the same Emergency-Stop generation.
- Certificate evidence is kept inside the deterministic service boundary; the coordinator exposes only the exact `ActionPlan` and boolean completion result.
- Certificate changes alter the plan binding and cannot reuse a token issued for another certificate-bound plan.
- Android runtime loads persisted trust only from the security-owned encrypted store; absent/invalid state produces an empty registry.
### VERIFIED
- Exact-head CI Run #298: SUCCESS.
- Adversarial/regression coverage and consolidated system review completed for authorization, session, Stop, evidence freshness, persistence, registry mutation, races, bounds, privacy and financial hard-deny.
- No demonstrated CRITICAL/HIGH/MEDIUM bypass identified in the reviewed provisioning path.
### NOT VERIFIED
- Final user-visible trusted-app provisioning UI.
- Physical-device testing.
- Signed production release.
- Native non-Android runtime implementations.
- Independent target-app UI/result observation.
- Real model/context ingestion filtering.
- Real production trust entries.
### KNOWN LIMITATIONS
- The deterministic provisioning core and Android coordinator are integrated into the runtime, but a final user-facing trusted-app management flow is still required.
- Persistence is intentionally fail-closed. The store-write-then-registry-activate sequence is race-safe within the current runtime, while an anomalous storage rollback failure remains a consistency edge case to harden if that path becomes reachable in production.
### NEXT ACTION
Implement the narrow user-visible trusted-app provisioning flow using the existing PermissionCenter/authorization patterns, preserving exact-plan and evidence binding. Do not invent package names/certificate pins or enable production trust by default.
### EXACT COMMIT SHA
`c5dad4ce6db7bf00615e649e0b77e05b033a620e`

## 2026-09-22 final authoritative state — trusted-app management

### CURRENT STOP POINT
The verified trusted-app management layer is complete at production-code/test checkpoint `9318f89e8fa62bb6b021b6938d771e6d92e144a2`. GitHub Actions Run #314 (`35694971910`) succeeded on that exact SHA.

### COMPLETED
- User-visible trusted-app add/review flow is wired through `MainActivity` and `PermissionCenter`.
- User-visible trusted-app removal/revocation flow is wired through the same deterministic provisioning coordinator.
- Trusted-package management is protected by the authenticated identity-session gate.
- Fresh installed package/signing evidence is checked before device authentication and again after device authentication.
- Exact `ActionPlan` binding, device authorization, identity-session validation and Emergency-Stop generation checks remain enforced below the UI.
- Certificate details remain outside the user-facing UI/state boundary.
- Trusted-app trust remains distinct from capability permission granting.
- Compose state caches the trusted-package list to avoid repeated encrypted-store reads during recomposition.
- Emergency Stop clears pending trusted-app add/remove state in the UI.

### VERIFIED
Run #314 (`35694971910`) on `9318f89e8fa62bb6b021b6938d771e6d92e144a2` completed successfully:
- `:core:jvmTest`
- `testDebugUnitTest`
- `assembleDebugAndroidTest`
- managed-device instrumentation on `pixel2api30`

The final coordinator regression suite passed, including changed-evidence-before/after-authentication coverage for add and removal, successful add/remove flows, and the trusted-app service's persistence, Emergency Stop, signer-count and token-binding checks.

### CONSOLIDATED REVIEW
The completed layer was re-reviewed across security/authorization, QA/adversarial testing, red-team attack paths, privacy/permissions, Android integration/lifecycle boundaries, resource bounds and final consistency/Guardian checks.

Review focus included:
- UI → coordinator → deterministic service → secure store/registry data flow.
- Exact evidence/plan/token binding and asynchronous-authentication TOCTOU.
- Emergency-Stop and identity-session invalidation.
- Removal registry/store ordering and rollback behavior.
- Certificate privacy and package/certificate input bounds.
- Finance/Tier-4 denial and separation from capability grants.
- Failure history versus final verification evidence.

No demonstrated CRITICAL/HIGH/MEDIUM bypass was identified in this completed layer.

### CI FAILURE HISTORY
- Run #312 (`35694134421`) failed because an intermediate removal-race test used a fresh empty in-memory registry for the removal coordinator. This was a test-fixture defect.
- Run #313 (`35694771907`) failed to compile because an intermediate fixture correction declared `addPlan` twice.
- Run #314 (`35694971910`) is the corrected exact-head success and is the authoritative verification for `9318f89e8fa62bb6b021b6938d771e6d92e144a2`.

### NOT VERIFIED
- Physical-device testing.
- Signed production release.
- Native iOS/iPadOS/Windows/macOS/Linux/ChromeOS runtime implementations.
- Independent target-app UI/result observation.
- Real model/context ingestion and screen/OCR/accessibility-to-model filtering.
- Any real production trusted-app entry; the registry remains empty/deny-by-default.
- Complete contextual secret classification.

### KNOWN LIMITATIONS
- The current removal UI requires the package to remain installed so authoritative signing evidence can be read again; stale/uninstalled trusted entries cannot currently be revoked through this evidence-backed path.
- Secure persistence and registry mutation are fail-closed and rollback-aware; anomalous rollback failure remains a consistency edge case, not a demonstrated authorization bypass.
- Trusting an app does not grant capability permissions automatically.
- Managed-device CI does not substitute for physical-device validation.

### NEXT ACTION
The next development chat must inspect the live `main`, latest commit(s), relevant source/tests and CI before any new work. Treat `9318f89e8fa62bb6b021b6938d771e6d92e144a2` + Run #314 as the latest verified production-code/test checkpoint unless newer code has its own exact-head verification.

Do not restart or duplicate trusted-app management. Preserve the common deterministic security boundary and continue only with the next explicitly justified security layer. Never invent trusted package identities, certificate pins, banking/UPI mappings, OAuth identities, payment integrations, backend services or external SDKs.

### EXACT VERIFIED PRODUCTION-CODE/TEST SHA
`9318f89e8fa62bb6b021b6938d771e6d92e144a2`

### EXACT VERIFIED CI RUN
Run #314 — `35694971910`

## 2026-09-23 verified security checkpoint — independent target-app foreground observation

### CURRENT STOP POINT
Independent Android target-app foreground observation is implemented, integrated, tested and exact-head CI verified at `173e3e706deb734fc06ab1ed97d9fb6a89e71cab`. PR #1 remains open until merge and post-merge main verification.

### COMPLETED
- Added `AndroidTargetAppForegroundObserver` and a framework-backed `UsageStatsManager` reader with bounded package/event processing.
- The observation boundary exposes only target package, event type and timestamp.
- `AndroidIntentActionAdapter` now requires observation availability before dispatch and independently observes the exact target package after dispatch.
- `ExecutionBridge` refuses to convert adapter `success=true, verified=false` into final success.
- Added JVM regression/adversarial coverage and managed-device instrumentation.
- Added `PACKAGE_USAGE_STATS` manifest declaration.
- Production trusted-app registry remains empty and deny-by-default.

### VERIFIED
- Run #317 (`35813576845`) succeeded for exact SHA `173e3e706deb734fc06ab1ed97d9fb6a89e71cab`.
- JVM tests passed.
- Android debug unit tests passed.
- Instrumentation-test compilation/APK assembly passed.
- Managed-device instrumentation passed on `pixel2api30`.
- Consolidated review covered call paths, data flow, trust boundaries, authorization/session/Emergency Stop preservation, failure behavior, bounds, privacy, permission semantics and adversarial cases.
- No demonstrated CRITICAL/HIGH/MEDIUM bypass identified in this layer.

### NOT VERIFIED
- Physical-device testing.
- Signed production release.
- Native non-Android runtimes.
- Semantic task/UI success inside target apps.
- Real model/context ingestion and screen/OCR/accessibility filtering.
- Real production trusted-app entries.
- Complete contextual secret classification.

### KNOWN LIMITATIONS
- The Android usage-stat access is a platform special-access boundary; managed-device instrumentation grants it for testing, which does not establish ordinary end-user production enablement.
- Observation proves only package-level foreground transition, not semantic completion.
- Production execution remains effectively deny-by-default because the trusted registry is empty.
- Removal of stale/uninstalled trusted entries still requires fresh installed signing evidence through the existing evidence-backed path.

### NEXT ACTION
Merge PR #1 after the branch remains green, then verify the resulting main merge commit with matching CI. Keep the production trusted registry empty. Do not add speculative package mappings, banking/UPI integrations, OAuth identities, backend services or external SDKs.

### EXACT COMMIT SHA
`173e3e706deb734fc06ab1ed97d9fb6a89e71cab`

### EXACT CI RUN
Run #317 — `35813576845`

