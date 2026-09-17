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

## 9. Current verified state
As of the current development checkpoint:
- Latest code/test-verified commit: `b3360550a5ca099ae19a0a0d84f3d839b4821563`.
- GitHub Actions run: `35185081422`.
- Job `unit-tests`: success.
- Command: `gradle --no-daemon testDebugUnitTest`.
- JDK: 17.
- Gradle: 8.13.

The repository may receive documentation-only commits after that verification; always inspect current `main` before claiming the current HEAD is test-verified.

CI has shown non-fatal warnings for a future Kotlin data-class copy-visibility change, deprecated Android biometric API usage, and GitHub Actions Node/action deprecations.

## 10. Production wiring status
`ExecutionBridge` is the final execution boundary and `inputText` passes through `SecurityExecutionPipeline` before adapter execution.

However, repository tracing currently shows:
- no production construction/composition of `ExecutionBridge`;
- no concrete production `AndroidActionAdapter` implementation;
- `MainActivity` does not construct/invoke the bridge.

Therefore final execution security is implemented and unit-covered, but real Android action composition and behavior are not claimed.

## 11. Screen / OCR / Accessibility status
Blueprint requirements require screen content to be classified before model context and sensitive UI regions blocked/redacted whenever technically possible, while preferring structured Android/app APIs over visual scraping.

Current repository state contains no concrete `AccessibilityService`, OCR/screen-capture ingestion component, manifest service declaration, or screen-content-to-model production path. No fake ingestion component has been added solely to satisfy the requirement.

## 12. Known limitations
- Pattern-based sensitive detection is not complete contextual classification.
- Real model/context ingestion boundary remains future work.
- Real banking/UPI package/component integration is not present.
- Confirmation/read-back UI is not yet the final production path.
- Secure storage/Keystore edge-case validation still needs focused work.
- Resource/memory pressure enforcement still needs implementation.
- Physical Android device validation is not complete.
- Release APK/security sign-off is not claimed from unit-test CI alone.

## 13. Exact next stop point — START HERE
**Continue with concrete `SecureLocalStore` / Android Keystore edge-case hardening.**

Inspect existing storage code and tests first. Reuse the current abstractions. Focus on missing coverage for key creation/recreation, corrupted ciphertext or malformed stored values, clear behavior, storage read/write failures, and failure-closed interaction with authorization/execution paths.

Do not add new security components unless repository search proves the responsibility is not already implemented. Do not bypass or weaken `PolicyEngine`, permission gates, authorization, finance firewall, SensitiveInformationFirewall or Emergency Stop.

After the storage/Keystore layer is logically complete, perform a consolidated review and move to bounded resource/memory enforcement, then real Android device security testing. Screen/OCR/Accessibility remains blocked on the absence of a real runtime ingress path.

## 14. Important recent commits
- `ddcbfb20528daa5c45e9be0810bd0e531f098964` — dedicated finance execution firewall.
- `7756b313a08b0c9d76953b01b8318e9e8f700d8a` — finance firewall in pipeline.
- `83f826bedd72b0a952c68a928af01a71fb7970b8` — finance firewall tests.
- `0729e1bd8174ce5f4fa8ccd2cee1f213386e4244` — finance deny before authorization/token consumption.
- `98014fbc297f8b82dcd542c3b2c0d0bed99dbdc9` — finance capability excluded at agent boundary.
- `97265b0893d93f9f5cd88d05698ff0ae8f1e56f0` — agent boundary regression.
- `3fa1962fd0471fb23e726f9fb477cb0e497e45a9` — bounded audit retention implementation.
- `798d6d62027b21e8bfef53039c92841624f31734` — audit retention compilation fixes.
- `c6b8493c468e21f02b70332956706fd88c1fc0ae` — audit session-metadata bound.
- `b3360550a5ca099ae19a0a0d84f3d839b4821563` — audit retention/session regression tests.

## 15. Continuation instruction
A future chat should read the required workflow/security documents, inspect current `main`, verify CI against the actual current HEAD, and continue from the exact `SecureLocalStore` / Keystore edge-case stop point without redesigning or duplicating existing security controls.
