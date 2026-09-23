# Ritav.ai — Persistent Project Continuation Guide

This README is the hand-off guide for future AI/development chats. Continue the existing project; do not restart or replace working architecture without an explicit architecture decision.

## 1. Project identity
- Project: Ritav.ai
- Repository: `darshandpatel63-prog/Ritav.ai`
- **Product target: cross-platform** — Android, iOS/iPadOS, Windows, macOS, Linux and supported ChromeOS/device form factors.
- **Current executable implementation: Android + JVM-targeted shared contracts/security only.** Native iOS/iPadOS/Windows/macOS/Linux/ChromeOS runtimes are not present in this repository yet.
- Android application ID: `ai.ritav.app`
- Branch: `main`
- Stage: Security completion — semantic verification merged; real AI/model/context ingestion is the next security layer
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

A production Android composition root now exists in `AndroidExecutionRuntime` and is instantiated by `MainActivity`. The current application supplies an empty trusted capability registry, so external action execution remains deny-by-default. `AndroidIntentActionAdapter` implements only `APP_LAUNCH` + `open`, requires a trusted package signing-certificate pin, and binds launch success to a security-owned semantic verification contract. Current semantic evidence proves only the exact target package produced a qualifying foreground transition after dispatch; it does not prove arbitrary in-app task completion. Real external-app execution therefore remains disabled until the reviewed allowlist, package visibility, permission/grant path, and required platform-specific evidence/verification paths are deliberately enabled.

## 11. Cross-platform implementation status
Completed:
- Kotlin Multiplatform `core` module exists.
- Platform-neutral platform/form-factor/capability contracts exist.
- Cross-platform capability contract tests exist.
- `RitavPlatformAdapter` runtime adapter contract and regression test have been added.

Not yet completed:
- Concrete iOS/iPadOS/Windows/macOS/Linux/ChromeOS runtime adapters.
- Latest capability-registry CI/managed-device verification is confirmed by workflow run #176; older package-specific verification notes below are historical.
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

The Android adapter test compilation fix is committed in `7d7c4a2df8495ab6c83e1702ee421e704a093174`. Subsequent documentation commits and the Android network-capability correction are on `main`; the connected commit-workflow query only exposes pull-request-triggered runs, so it cannot establish the status of these push-triggered workflow runs; CI is therefore not claimed green.

## 14. Known limitations
- Cross-platform contracts are implemented; native platform implementations are not yet complete.
- No claim that Ritav currently runs on every listed OS/device.
- Native OS permission, accessibility, background, screen capture and secure-storage semantics still require platform-specific implementations and tests.
- Pattern-based sensitive detection is not complete contextual classification.
- Real model/context ingestion boundary remains future work.
- Some older storage-specific instrumentation execution remains unverified; run #176 verified the latest capability-registry package on a managed Android device.
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
- `AndroidExecutionRuntime` now composes the deterministic security pipeline, authorization gate/service, capability gate and concrete Android launch adapter. The default application registry is empty, so no external app is currently executable.

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

## 18. Latest verified handoff — 2026-09-20

The current Android release-hardening package is implemented and verified on the exact main head `81dba435e3cd0b55d398db965dc63a92cf20bae2`.

### Verified release gates
- GitHub Actions release-validation run #12 (`35439804241`) completed successfully.
- Release security configuration assertions passed.
- Tracked-source scan for embedded release credentials/signing material passed.
- `:core:jvmTest`, Android debug/release unit tests, `lintRelease`, `assembleRelease`, and `bundleRelease` passed.
- Release APK verification confirmed the APK is not debug-enabled.
- R8 mapping verification passed with a non-empty mapping file.
- SHA-256 checksums and unsigned release validation artifacts were produced successfully.
- A previous CI failure in the APK verification command was fixed in commit `81dba435e3cd0b55d398db965dc63a92cf20bae2`; run #12 verifies the corrected command.

### Release security posture
- Release builds are non-debuggable, minified with R8, and resource-shrunk.
- Backup/cloud/device-transfer data extraction is explicitly restricted by modern and legacy backup rules.
- Cleartext traffic is disabled.
- No `INTERNET` permission is granted by the current app path; `ACCESS_NETWORK_STATE` is used only to inspect local connectivity state.
- CI uses least-privilege repository contents read permission and controlled release-critical path triggers plus manual dispatch.
- Release artifacts are intentionally unsigned; no signing keys or credentials are fabricated or committed.

### Consolidated system review
The release package was reviewed end-to-end against the existing security chain: plan validation/hashing → policy/capability gates → sensitive/finance firewalls → authorization/session binding → execution pipeline/bridge → trusted package identity → adapter dispatch → result verification/audit. Release configuration, permissions, backup rules, secret scanning, core test coverage, APK debug state, R8 mapping, checksums, CI permissions/triggers/concurrency, and fail-closed paths were also reviewed. No new CRITICAL/HIGH bypass was identified.

### Current limitations
- Trusted external-app registry is intentionally empty/deny-by-default.
- Android production capability-grant/user-authorization composition is now wired; the trusted external-app registry remains empty, so no external-app grant is enabled.
- Target-app UI/result observation is not independently implemented.
- Real model/context and screen/OCR/accessibility ingestion boundaries are not implemented.
- Native non-Android runtimes are not implemented.
- Signed production packaging and physical-device testing are not verified.

## 19. Exact next stop point
**Independent audit of the completed Android release-hardening package.** After a clean audit checkpoint, continue the next major security layer: production trusted-app composition and real user authorization UI, while keeping the trusted registry empty until authoritative package/certificate identity is available and preserving deterministic finance hard-deny and fail-closed behavior.

## 20. Continuation rule
For every subsequent `Start/Continue`, inspect the current `main` head and relevant source/CI state first. Preserve the existing deterministic security boundary; do not add speculative banking/UPI/backend integrations or claim physical-device verification without evidence.


## 2026-09-20 authorization emergency-stop hardening checkpoint
- `ActionAuthorizationService` now shares the authoritative runtime Emergency Stop controller and fails closed both before and after asynchronous device authentication when the stop is active.
- Android runtime composition injects the same controller into the authorization service; regression coverage verifies stopped user-confirmation and device-authentication token issuance is blocked, including an in-flight authentication race.
- Exact-head Android verification is pending at run #245 (`35489700331`) for commit `1495470de42438af5288784ad47ec0818d8ab714`. This change is not yet marked verified.


## Latest security checkpoint — 2026-09-20

Emergency Stop hardening has been extended beyond a point-in-time execution check. Authorization tokens and protected identity sessions now carry the Emergency Stop generation under the same authoritative runtime controller, so security state issued before an Emergency Stop does not become usable again merely because the stop is later reset. Token minting/consumption and direct internal session issuance fail closed while stopped, with regression coverage for stale-state invalidation.

Current implementation head: `6c1b58e17d97ec5a69fc736a7d0ebb1e4e57a0ef`.

The package is source-reviewed but **not yet CI-verified** on this exact head. Do not treat the older run #245 (`35489700331`) as verification evidence for this newer SHA.

Signed release packaging remains intentionally unconfigured; the production external-app registry remains empty/deny-by-default; physical-device testing and independent target-app result observation remain unverified.


## 2026-09-20 Emergency Stop security checkpoint

The latest security package extends Emergency Stop from a point-in-time execution check into explicit security-state invalidation. Authorization tokens and protected identity sessions carry the current Emergency Stop generation; activating the stop invalidates pre-stop state and resetting the stop does not revive that old state. Token issuance/consumption and session issuance are race-guarded by the same authoritative controller.

The package also hardens authorization-token consumption so a rejected wrong-plan attempt does not consume the valid token, and hardens the final execution boundary to fail closed when its security clock is unavailable before adapter execution.

Current implementation SHA: `47cb40c0d2200343226a6a76354f1772c50c3604`.

Source and adversarial review found no demonstrated CRITICAL/HIGH bypass in this affected path. Exact-head Android CI verification is still pending because the connected workflow API is not exposing the push-triggered run for this SHA. The older run #245 (`35489700331`) is not evidence for this head.


## 2026-09-20 latest security boundary checkpoint

A further failure-path review found that malformed execution clocks and oversized execution session metadata could make denial auditing throw. The execution pipeline now uses an audit-safe timestamp fallback, rejects execution session IDs above 128 characters to match the audit bound, and safely records/handles rejected malformed requests. The final execution bridge also rejects negative clocks before any adapter call.

Current implementation SHA: `9bffe276bfb44cd689366eec552512f8d4b0ac70`.

Emergency Stop generation invalidation, authorization token binding, protected-session invalidation, wrong-plan token preservation, and these audit/clock failure paths are source-reviewed with regression coverage. Exact-head Android CI is still pending through the available connector, so this checkpoint is not yet CI-verified.


## 2026-09-20 latest security boundary correction — capability grant / Emergency Stop race

A consolidated failure-path review identified a time-of-check/time-of-use gap in capability granting: the authorization token could be accepted before a concurrent Emergency Stop activation, while the durable permission-store mutation occurred afterward. The grant path is now executed inside the same Emergency Stop critical section, so Stop activation cannot interleave between authorization acceptance and the grant mutation. The service default also derives its Emergency Stop controller from the authorization gate, preventing accidental controller divergence.

A blocking permission-store regression test was added to verify that a concurrent Stop activation cannot complete while a capability grant mutation is in progress, while the existing Stop, replay, wrong-plan, invalid-clock, risk, financial, and revocation coverage remains intact.

Current implementation/test SHA: `22675b28646e5b35d3bce6232d5aaaf0519ec748`.

Source and adversarial review found no demonstrated CRITICAL/HIGH bypass in this affected grant path. Direct local test execution was not available in this environment, and the connected GitHub workflow API exposes no push-triggered run/status for this exact head; therefore exact-head CI remains **not verified**.

**CURRENT STOP POINT:** capability-grant / Emergency-Stop race hardening is implemented, integrated, source-reviewed, and regression-covered; exact-head CI is pending.

**NEXT ACTION:** obtain exact-head Android CI evidence. After successful CI, perform the required independent audit checkpoint before beginning the next major security layer.


## 2026-09-20 independent audit checkpoint — permission / grant / Emergency Stop path

The completed capability-grant and Emergency Stop hardening was independently re-reviewed end-to-end. The review covered the authorization gate/service, capability registry, capability-grant mutation boundary, shared permission store, Emergency Stop generation/locking, session binding, downstream execution gates, and related adversarial tests.

One additional concurrency weakness was identified during that review: `SecurePermissionStore.isGranted()` and the test `InMemoryPermissionStore.isGranted()` were not synchronized with grant/revoke mutations. Both authorization reads are now serialized with mutations to prevent stale/concurrent permission-state decisions within the same store instance.

Audit result: no demonstrated CRITICAL/HIGH bypass remains in the reviewed path after the fix. Existing MEDIUM/architectural limitations remain: the production trusted-app registry is intentionally empty, final target-app state is not independently observed, and real model/context plus screen/OCR/accessibility ingestion paths are not implemented.

Latest implementation/test head: `a0c1fc5c15fedb8b006ff6c4db4544b06556b890`.

Direct local tests were not executed, and the connected GitHub workflow API exposes no push-triggered run or combined status for this exact head. CI/device verification therefore remains **not verified**.

**CURRENT STOP POINT:** independent audit checkpoint completed for this security layer; exact-head CI is still pending.

**NEXT ACTION:** obtain exact-head Android CI evidence. Do not begin the next major security layer until the CI verification state is established and this audited package is formally closed.


## 2026-09-20 CI verification checkpoint — manual Android test trigger enabled

The Android test workflow now supports `workflow_dispatch` in addition to its existing main push and pull-request path filters. This change was made so an exact repository head can be deliberately verified through GitHub Actions when the connected workflow-run integration cannot otherwise expose push-triggered history.

The latest security implementation/test commit is `e3475543e43da44f3daa7986800054bf25d89ae6`. GitHub Actions run `#280` (`35514982136`) was triggered by that exact head and is currently pending; run `#279` (`35514948615`) is the preceding workflow-config head and is still in progress. Neither is treated as a completed verification result yet.

The release workflow remains separately controlled with manual dispatch plus release-critical push path filters; no release trigger was added for ordinary code changes.

**CURRENT STOP POINT:** audited permission/grant/Emergency-Stop layer is implemented; exact-head Android CI is executing/queued and remains unverified.

**NEXT ACTION:** inspect run `#280` for conclusion and job results. Only after a successful exact-head CI checkpoint should this security layer be formally closed and the next major security layer begin.


## 21. Latest trusted-package provisioning-evidence checkpoint — 2026-09-21

### Current repository state
- Implementation commit: `66c924927fead15c83d7abeb039f3cc43cee02df`.
- Android workflow Run #284 (`35571240222`) is executing against that exact implementation SHA; verification is pending.
- The previous verified Android checkpoint remains Run #283 on `3c28e9319f3a01702e5af93f61aec3c6976d9d40`.
- The production trusted external-app registry remains intentionally empty, so external execution stays deny-by-default.

### What was implemented
- Added a read-only `AndroidTrustedPackageEvidenceReader` for the reviewed provisioning path.
- The reader obtains the currently installed package's signing certificate through the existing Android certificate-reader boundary and returns only the package name, single-signer count and SHA-256 digest.
- Malformed/oversized package identifiers, missing/unreadable certificates and multi-signer identities fail closed.
- No registry mutation, capability grant, authorization token issuance or execution permission is performed by this evidence layer.
- Existing package identity verification continues to be the authoritative execution-time trust decision.

### Verification state
- Implemented: yes.
- Integrated: yes.
- Tested by source-level regression coverage: yes.
- CI Verified: pending Run #284.
- Real-device Tested: no new physical-device testing; managed-device verification is pending for Run #284.

### Known limitations
- No trusted package is currently populated into production configuration.
- This layer intentionally produces evidence only; a separate explicit trust-decision/persistence path is still required.
- Final target-app UI/result observation remains unimplemented.

## 2026-09-21 verified trusted-package provisioning-evidence checkpoint

The hardened read-only Android trusted-package provisioning-evidence layer is implemented and exact-head CI verified.

- Implementation/test checkpoint: `2ee78ca995cab7fa1bd9dd0794c0ac2a0855c8b3`.
- Android workflow Run #290 (`35619464754`) completed successfully on that exact SHA.
- Verification included JVM/unit tests, Android instrumentation-test compilation/APK assembly, and managed-device instrumentation on `pixel2api30`.
- The evidence reader reuses the existing Android signing-certificate reader, validates a bounded package name, requires exactly one signer, rejects empty or oversized certificate material, hashes only bounded certificate bytes with SHA-256, and returns digest-only evidence.
- Regression coverage includes malformed package input, the 256/257-character package-name boundary, reader failure, empty/oversized/max-size certificate boundaries, multiple signers, and absence of raw certificate bytes from returned evidence.
- Runs #287 and #289 exposed and isolated an intermediate Kotlin regex-escaping defect; Run #290 is the final exact-head successful checkpoint after that defect was corrected. Run #288 was cancelled while superseded.
- Evidence remains distinct from trust. The reader cannot mutate the registry, grant capabilities, issue authorization, or execute an action.
- The production trusted external-app registry remains empty/deny-by-default. No package identity or certificate pin has been invented or enabled.

### Independent security review checkpoint

The completed evidence sub-layer received the required consolidated and independent review across security/authorization, QA/adversarial, red-team, privacy/permissions, Android integration, resource bounds, and final Guardian lenses.

- No demonstrated CRITICAL, HIGH, or MEDIUM bypass was identified.
- Package-name and certificate-size bounds are conservative availability restrictions that fail closed rather than weakening trust.
- Existing execution-time `AndroidPackageIdentityVerifier` remains authoritative and still requires an explicit registry certificate pin plus exactly one matching installed signer.
- Raw certificate bytes are not returned by the evidence object and are not persisted by the evidence reader.
- Known limitations remain: no production trust entries, no independent target-app UI/result observation, no physical-device testing, and no real model/context ingestion filtering path.
- This is a completed audited checkpoint before the next major security layer.

**Next action:** wire the verified trusted-app provisioning service/coordinator into a user-visible, reviewable flow without bypassing the deterministic core. Preserve authoritative package/certificate evidence, device authorization, exact-plan binding, Emergency Stop and deny-by-default behavior.


## 2026-09-22 verified trusted-app trust-entry provisioning checkpoint

The explicit deterministic trusted-app trust-entry decision/persistence layer is now implemented, integrated and exact-head CI verified. The production trusted external-app registry remains empty/deny-by-default because no real package identity/certificate pin has been approved.
### CURRENT STOP POINT
- Verified implementation/test head: `c5dad4ce6db7bf00615e649e0b77e05b033a620e`.
- Android workflow Run #298 (`35679864201`) completed successfully on that exact SHA.
- CI covered JVM/unit tests, Android instrumentation-test compilation/APK assembly, and managed-device instrumentation on `pixel2api30`.
### COMPLETED
- Added bounded encrypted durable trust-entry storage using the existing `SecureLocalStore`.
- Added deterministic trust-entry provisioning bound to exact package identity evidence, certificate digest, single-signer state, identity session, device-auth token, Emergency-Stop generation and short-lived pending state.
- Added evidence binding into `ActionPlan.expectedState` so certificate changes produce distinct plan hashes and cannot reuse authorization tokens.
- Added Android coordinator checks before and after device authentication so package/certificate evidence must remain consistent before persistence.
- Kept the trusted registry mutation internal and constrained to the reviewed `APP_LAUNCH + open` capability shape; financial/Tier-4 trust entries are rejected.
- Added regression coverage for wrong evidence, certificate-change plan binding, malformed package names, multiple signers, Emergency Stop and durable-store failure.
### VERIFIED
- Run #298 exact-head CI: SUCCESS.
- Consolidated security review covered authorization/token binding, identity sessions, Emergency Stop, evidence/TOCTOU boundaries, persistence validation, registry mutation, concurrency, resource limits, privacy boundary, and finance/Tier-4 denial.
- No demonstrated CRITICAL/HIGH/MEDIUM bypass was identified in this reviewed provisioning path.
### NOT VERIFIED
- Physical-device testing.
- Signed production release.
- Native non-Android runtimes.
- Independent target-app UI/result observation.
- Real model/context ingestion filtering.
- Real production trust entries.
### KNOWN LIMITATIONS
- The provisioning core/coordinator exists but is not yet wired to a final user-visible trusted-app management UI.
- Store/registry persistence is fail-closed and rollback-aware; a catastrophic storage failure during an anomalous in-memory registry mutation rollback remains an architectural consistency edge case, not a demonstrated authorization bypass.
- Target-app result observation remains outstanding.
### NEXT ACTION
Wire the verified provisioning coordinator into a narrow user-visible trusted-app management flow using the existing authorization/session UI patterns. Do not expose certificate material to UI, do not invent package/certificate identities, and do not enable any production trust entry without authoritative evidence and explicit review.
### EXACT COMMIT SHA
`c5dad4ce6db7bf00615e649e0b77e05b033a620e`

## 21. Latest verified trusted-app management checkpoint — 2026-09-22

The trusted-app user-visible management layer is now implemented, integrated, tested and exact-head CI verified. The verified production-code/test checkpoint is `9318f89e8fa62bb6b021b6938d771e6d92e144a2`; GitHub Actions Run #314 (`35694971910`) completed successfully on that exact SHA.

### What is now implemented

- `MainActivity` is wired to the existing `AndroidTrustedAppProvisioningCoordinator` for both trusted-app add and trusted-app removal.
- `PermissionCenter` exposes a narrow session-gated trusted-app management surface:
  - installed Android package-name input for trust review;
  - reviewable add approval;
  - authenticated trusted-package list;
  - explicit `Remove trust` action and review flow.
- Trusted-app management UI state is cached in Compose state rather than re-reading encrypted storage during recomposition.
- Emergency Stop clears pending add/remove plans and active protected identity state.
- Certificate material is not shown in UI or exposed as ordinary UI state; the coordinator/service retain deterministic evidence internally.
- Existing capability granting remains separate from trust-entry provisioning; trusting an app does not grant unrestricted capability access.
- The production trusted external-app registry remains intentionally empty/deny-by-default.

### Verification

Run #314 (`35694971910`) on exact SHA `9318f89e8fa62bb6b021b6938d771e6d92e144a2` passed:
- `:core:jvmTest`;
- Android debug unit tests (`testDebugUnitTest`);
- instrumentation-test compilation/APK assembly (`assembleDebugAndroidTest`);
- managed-device instrumentation on `pixel2api30`.

The final regression suite reported all tests passing. Two superseded intermediate runs are retained only as failure history:
- Run #312 failed in `changedEvidenceAfterRemovalAuthenticationAlsoBlocksPersistence` because the test fixture created a fresh empty in-memory registry for the removal coordinator; this was a test-fixture defect, not an authorization bypass.
- Run #313 failed to compile because an intermediate fixture correction accidentally declared `addPlan` twice; that was corrected before Run #314.

### Consolidated security review

The completed trusted-app add/remove UI composition was reviewed end-to-end across:
- UI → coordinator → deterministic provisioning service → encrypted store/registry call paths;
- exact package/certificate evidence binding before and after device authentication;
- exact `ActionPlan` hashing and one-time authorization-token use;
- identity-session and Emergency-Stop generation binding;
- removal ordering, rollback behavior and persistence failure;
- certificate privacy boundaries;
- package/certificate bounds and pending-plan bounds;
- finance/Tier-4 denial and capability-grant separation;
- stale/wrong evidence and asynchronous authentication race coverage.

No demonstrated CRITICAL/HIGH/MEDIUM bypass was identified in this completed layer.

### Important current limitations

- Physical-device testing is not verified.
- Signed production release is not verified.
- Native iOS/iPadOS/Windows/macOS/Linux/ChromeOS runtimes are not implemented.
- Independent target-app UI/result observation is not implemented.
- Real model/context ingestion filtering and screen/OCR/accessibility-to-model filtering are not implemented.
- The production trusted-app registry is intentionally empty; no package name or certificate pin has been invented or approved.
- Sensitive-information protection remains pattern-based defense-in-depth rather than complete contextual secret classification.
- The managed-device result verifies the automated test environment, not a physical consumer device.

### Exact continuation point

The next chat must first inspect the actual current `main` head and CI, then treat `9318f89e8fa62bb6b021b6938d771e6d92e144a2` + Run #314 as the latest verified production-code/test checkpoint unless newer code is independently verified.

Do not restart the trusted-app management layer. Do not invent trusted packages/certificate pins or add speculative banking, UPI, OAuth, payment, backend or external-SDK integrations.

**Next development direction:** continue from the verified trusted-app management checkpoint into the next explicitly justified security layer, only after inspecting the live repository and preserving the existing deterministic security boundary.

## 2026-09-23 target-app foreground observation checkpoint

### CURRENT STOP POINT
The independent Android target-app foreground observation layer is implemented and exact-head CI verified at `173e3e706deb734fc06ab1ed97d9fb6a89e71cab`. PR #1 is open against `main`.

### COMPLETED
- Added a bounded Android `UsageStatsManager` observation boundary for `APP_LAUNCH + open`.
- Retained only exact target package identity, event type and timestamp; UI text, accessibility nodes, class names and event extras are not exposed to the result-verification path.
- Required observation capability before launch dispatch.
- Required an exact target-package foreground event after dispatch within a bounded 2-second observation window.
- Preserved the existing trust, capability, authorization, session, Emergency Stop, sensitive-data and finance controls.
- Added JVM negative/adversarial tests and managed-device instrumentation coverage.
- Added the Android `PACKAGE_USAGE_STATS` special-access declaration required by the observation boundary.
- Kept the production trusted-app registry empty/deny-by-default.

### VERIFIED
- GitHub Actions Run #317 (`35813576845`) succeeded on exact SHA `173e3e706deb734fc06ab1ed97d9fb6a89e71cab`.
- JVM/unit tests passed.
- Android instrumentation-test compilation/APK assembly passed.
- Managed-device instrumentation passed on `pixel2api30`.
- Consolidated system review covered the adapter/bridge call path, observation data flow, permission boundary, fail-closed behavior, bounded polling/event processing, negative/adversarial tests, Emergency Stop/authorization preservation, privacy, finance hard-deny and deny-by-default trust state.
- No demonstrated CRITICAL/HIGH/MEDIUM bypass was identified in the reviewed layer.

### NOT VERIFIED
- Physical-device testing.
- Signed production release.
- Native iOS/iPadOS/Windows/macOS/Linux/ChromeOS runtimes.
- Semantic target-app UI/task-result verification; the observer proves only a package-level foreground transition.
- Real model/context ingestion filtering and screen/OCR/accessibility-to-model filtering.
- Any real production trusted-app entry; the registry remains intentionally empty.
- Complete contextual secret classification.

### KNOWN LIMITATIONS
- `PACKAGE_USAGE_STATS` is a special Android access boundary and is not equivalent to ordinary runtime permission approval; production user enablement is not claimed by managed-device CI.
- Foreground observation is intentionally minimal and cannot prove that the requested task completed successfully inside the target app.
- Managed-device CI is not physical-device validation.

### NEXT ACTION
After this reviewed security layer, merge PR #1 only after the branch remains green and then verify the resulting `main` merge commit with its own matching CI evidence. Do not restart trusted-app management. The next major layer must continue to preserve deterministic authorization and should address semantic result verification only if a real, privacy-preserving producer/consumer path can be established without exposing untrusted UI content as authority.

### EXACT COMMIT SHA
`173e3e706deb734fc06ab1ed97d9fb6a89e71cab`

### EXACT CI RUN
Run #317 — `35813576845`

## 2026-09-23 latest verified checkpoint — observation evidence freshness hardening

### CURRENT STOP POINT
The independent Android target-app foreground observation layer and its timestamp-freshness hardening are merged into `main`. The implementation/test head `f5a22bdb547f97dfabce75dc31bc7a8b1767a351` was exact-head CI verified by Run #321 (`35821727487`).

### COMPLETED
- Foreground observation remains package-only and bounded.
- Accepted foreground evidence is now additionally bound to the actual post-dispatch query window and the fixed two-second deadline.
- Added regression coverage rejecting out-of-window observation timestamps.
- Existing trust, capability, authorization, session, Emergency Stop, sensitive-data and finance controls remain unchanged.
- Production trusted-app registry remains empty/deny-by-default.

### VERIFIED
- Run #321 (`35821727487`) succeeded on exact SHA `f5a22bdb547f97dfabce75dc31bc7a8b1767a351`.
- JVM/unit tests passed.
- Android instrumentation-test compilation/APK assembly passed.
- Managed-device instrumentation passed on `pixel2api30`.
- Final affected-path review found no demonstrated CRITICAL/HIGH/MEDIUM bypass.

### NOT VERIFIED
- A separate post-merge CI run for merge commit `6b31f3013b2b044f2f1352ae712638d4ba9db2a5`; the connected workflow-run/status API currently exposes no run for that merge commit.
- Physical-device testing.
- Signed production release.
- Semantic target-app task/UI verification.
- Native non-Android runtimes.
- Real model/context and screen/OCR/accessibility filtering.
- Real production trusted-app entries.
- Complete contextual secret classification.

### KNOWN LIMITATIONS
- `PACKAGE_USAGE_STATS` remains a special-access boundary; managed-device test enablement is not end-user production enablement.
- Foreground observation proves only package-level foreground transition, not task completion.
- Managed-device CI is not physical-device validation.

### NEXT ACTION
Continue from the merged observation layer. Do not restart trusted-app management. Evaluate semantic result verification only when a real, privacy-preserving producer/consumer path can be established without making untrusted UI content authoritative.

## 2026-09-23 latest security checkpoint — post-dispatch observation timestamp hardening

### CURRENT STOP POINT
Target-app observation timestamp hardening is merged into `main`. Merge commit: `41711ae54da4fddfc8d5fc77b21c733a4f967a28`.

### COMPLETED
- The Android launch adapter still performs a fail-closed security-clock preflight before dispatch.
- After a successful launch dispatch, a fresh clock sample is taken and only that post-dispatch timestamp is passed to the independent foreground observer.
- Added regression coverage proving observation starts from the post-dispatch timestamp.
- Existing trust, capability, authorization, session, Emergency Stop, sensitive-data and finance controls remain unchanged.

### VERIFIED
- Exact implementation/test SHA `0b8e15a58830690ba9fc9b10055e5eaad31f3d35`.
- GitHub Actions Run #323 (`35822250738`) succeeded.
- JVM/unit tests, instrumentation-test compilation/APK assembly, and managed-device instrumentation on `pixel2api30` passed.
- Consolidated affected-path review found no demonstrated CRITICAL/HIGH/MEDIUM bypass.

### NOT VERIFIED
- Separate post-merge CI evidence for merge commit `41711ae54da4fddfc8d5fc77b21c733a4f967a28`; the connected workflow/status API may not expose push-triggered runs for merge commits.
- Physical-device testing, signed production release, semantic task verification, native non-Android runtimes, real model/context filtering, and real trusted-app entries.

### NEXT ACTION
Continue from the merged observation layer. Do not restart trusted-app management. Any future semantic verification work must use a concrete, privacy-preserving, deterministically bounded evidence source and keep untrusted UI/content from becoming authority.

## 2026-09-23 independent audit checkpoint — target-app observation layer

### AUDIT RESULT
A fresh repository-level review of the completed target-app foreground observation path found no demonstrated CRITICAL/HIGH/MEDIUM security bypass.

Reviewed lenses:
- Security/authorization: execution remains behind existing deterministic trust, capability, authorization, identity-session and Emergency Stop controls.
- Privacy: only package/event/timestamp evidence crosses the observation boundary; UI content, accessibility nodes and event extras are excluded.
- Platform integration: `PACKAGE_USAGE_STATS` is a special Android access that users must grant through Settings; failure is handled fail-closed. citeturn658278search0
- Adversarial/race: wrong package, wrong event type, dispatch-time events, out-of-window timestamps, clock regression/failure, observation-source failure, query/event bounds and post-dispatch timestamp ordering are covered.
- Resource bounds: event inspection, polling interval and observation deadline are bounded.

### AUDIT LIMITATIONS / OPEN ITEMS
- No production user-facing Usage Access settings flow has been implemented; without the special access, cross-app observation fails closed. Android documents that declaring `PACKAGE_USAGE_STATS` does not itself grant usage access; the user must enable it in Settings. citeturn658278search0
- AccessibilityService is the realistic candidate for future richer UI-state evidence, but Android requires the user to explicitly enable such a service in device settings, and window-content access is an explicit service capability. citeturn366553search0
- Screen capture is not being added speculatively; MediaProjection requires user consent for capture sessions, and Android 14+ requires consent for each capture session. citeturn366553search3turn366553search1

### AUDIT GATE
This checkpoint satisfies the independent-audit requirement for the completed observation security layer. The next major layer may proceed only with a concrete, privacy-preserving evidence producer/consumer path; no raw UI/content ingestion is assumed.


## 2026-09-23 latest security checkpoint — semantic task-completion verification

### CURRENT STOP POINT
Semantic verification is implemented, integrated into the final `ExecutionBridge`, independently tested and merged to `main` at **`86d70f90c05692d72464056601dc2fb8f165b01a`** through PR #6.

### COMPLETED
- Added a deterministic, security-owned `SemanticResultVerifier`.
- Added structured `VerificationEvidence` with bounded type, target and timestamp fields.
- Centralized the authoritative `ExpectedActionStateRegistry` in the security package.
- Required exact expected-state agreement with the security-owned action contract.
- Required exact evidence type and exact target package.
- Required evidence to be non-future, non-stale and inside a bounded verification window.
- Removed reliance on the adapter's Boolean `verified` field for final success.
- Bound the existing Android foreground observer to concrete structured evidence.
- Preserved the existing policy, capability, authorization, session, Emergency Stop, sensitive-data, finance and trusted-signing controls.
- Added adversarial regression coverage for missing, forged, stale, future, wrong-target/type, mismatched-state and clock-regression evidence.
- Managed-device instrumentation now exercises the updated observation contract.

### VERIFIED
- PR #6 exact head: `faec08d8083150da2fe4ef961fe3562b31edf9af`.
- GitHub Actions Run #380 (`35830655333`) completed **SUCCESS**.
- Run #380 passed JVM tests, Android instrumentation-test compilation/APK assembly, and managed-device instrumentation on `pixel2api30`.
- CI history included intermediate source/test failures; they were corrected and the final exact PR-head run passed.
- Consolidated review covered call paths, data flow, authorization binding, evidence provenance/target/timing, fail-closed behavior, privacy, resource bounds, race/timestamp behavior and adversarial misuse. No demonstrated CRITICAL/HIGH/MEDIUM bypass was identified in the reviewed path.

### NOT VERIFIED
- A separate post-merge push-triggered CI run for merge commit `86d70f90c05692d72464056601dc2fb8f165b01a` is not exposed by the connected workflow-run query.
- Physical-device validation.
- Signed production-release validation.
- Native iOS/iPadOS/Windows/macOS/Linux/ChromeOS runtime implementations.
- Real model/provider runtime integration.
- Screen/OCR/accessibility-to-model filtering.
- Complete contextual secret classification.
- Any real production trusted-app entry; the registry remains empty/deny-by-default.

### KNOWN LIMITATIONS
- Current executable semantic evidence is intentionally limited to **APP_LAUNCH + open** and establishes package-level foreground transition, not arbitrary in-app task success.
- `PACKAGE_USAGE_STATS` remains a user-granted Android special-access boundary.
- Managed-device CI is not physical-device validation.
- `SensitiveInformationFirewall` remains pattern-based defense-in-depth rather than a complete contextual secret classifier.

### NEXT ACTION
Implement the next major security layer: a real model/context runtime boundary built on the existing `ModelContextBoundary`, with no speculative external model SDK or cloud dependency. Then implement explicit screen/OCR/accessibility ingestion filtering before adding broader semantic task adapters.

### EXACT VERIFIED PRODUCTION-CODE/TEST SHA
`faec08d8083150da2fe4ef961fe3562b31edf9af`

### EXACT VERIFIED CI
Run #380 — `35830655333`

### EXACT MERGE SHA
`86d70f90c05692d72464056601dc2fb8f165b01a`


## 2026-09-23 latest security/handoff checkpoint

The repository is currently private. Open security work is in PR #8 (`security/accessibility-model-context-filter`) and PR #9 (`security/ai-model-runtime-boundary`). Both currently have failing exact-head CI and must be repaired before merge; no green status is claimed. The latest merged semantic-verification checkpoint remains `86d70f90c05692d72464056601dc2fb8f165b01a`, with exact implementation/test SHA `faec08d8083150da2fe4ef961fe3562b31edf9af` and Run #380 (`35830655333`) SUCCESS.

GitHub currently documents standard GitHub-hosted Actions as free for public repositories, while private repositories consume the plan's monthly included minutes. Therefore making this repository public should prevent standard public-repository Actions runs from consuming the private minute allowance, although already-used minutes are not retroactively erased and other Actions limits still apply. Before making the repository public, perform a complete history-aware secret scan because public visibility exposes the repository's full Git history. The assistant has not changed repository visibility.

For continuation, read `RITAV_HANDOFF.md` and `RITAV_PROJECT_STATE.md` for the exact security roadmap, CI failures, open PR heads, verification evidence and new-chat procedure. Do not restart completed trusted-app, observation or semantic-verification layers.
