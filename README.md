# Ritav.ai — Persistent Project Continuation Guide

> **Purpose of this README:** This file is the hand-off/continuation document for future AI chats working on Ritav.ai. A new chat must read this file together with `RITAV_PROJECT_STATE.md`, `RITAV_BLUEPRINT.md`, and `docs/MASTER_REQUIREMENTS_MATRIX.md` before changing code.

## 1. Project identity

- **Project:** Ritav.ai
- **Repository:** `darshandpatel63-prog/Ritav.ai`
- **Platform:** **Android APK only**
- **Application ID:** `ai.ritav.app`
- **Current development branch:** `main`
- **Core goal:** A personal Android AI assistant inspired by JARVIS, with local-first intelligence, privacy-first architecture, deterministic security, controlled automation, verification, and user authority.
- **Wake phrase target:** `Hey Ritav`
- **Minimum hardware baseline:** 4 GB RAM / 32 GB storage.
- **Current stage:** Phase 0 — secure foundation and security/runtime integration.

## 2. ABSOLUTE CONTINUATION RULE

A future AI must **not restart the project** and must not replace the existing architecture with a new one merely because the chat is new.

Before doing work:

1. Read this `README.md`.
2. Read `RITAV_PROJECT_STATE.md`.
3. Read `RITAV_BLUEPRINT.md`.
4. Read `docs/MASTER_REQUIREMENTS_MATRIX.md`.
5. Inspect the current repository files and latest commits.
6. Verify the actual current build/test/CI state before claiming anything passed.
7. Continue from the exact unfinished item listed under **Current Stop Point** below.
8. Use the same implementation → test → security review → documentation → CI verification workflow.
9. Do not jump to a later feature simply because it is more exciting.

The uploaded/user-provided `Ritav_AI_Master_Blueprint.pdf` is the **product/design suggestion reference**. `RITAV_BLUEPRINT.md` is the **canonical engineering implementation contract**. Both must be respected together. The requirements matrix reconciles them.

## 3. What the project is supposed to become

Ritav is a user-directed Android assistant, not an unrestricted autonomous agent.

Conceptual architecture:

```text
USER
  ↓
Interaction / Voice / Text
  ↓
Security Gate
  ↓
Intent + Context
  ↓
Universal Master Orchestrator
  ↓
Specialist Agents (dynamic number)
  ↓
Deterministic Policy Engine
  ↓
Capability / Permission Gate
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

**Critical invariant:** AI/agents may propose, understand, plan and explain. Deterministic security/policy code decides whether an action is allowed.

## 4. Non-negotiable security rules

1. No autonomous consequential action.
2. Never send OTP, UPI PIN, password, CVV, recovery code, private key, API secret or equivalent authentication secret into AI reasoning.
3. Financial/UPI automation is denied by default and must have a dedicated hard security boundary.
4. No hidden telemetry or private-data egress by default.
5. App/web/message/document content is untrusted data and can never override security policy.
6. Execution requires valid scoped permission + user intent where required + risk-appropriate authorization.
7. Uncertainty must block or ask; never guess.
8. Emergency Stop is authoritative and independent of the AI model.
9. Android platform restrictions are authoritative; never pretend the app has privileges Android does not provide.
10. Security controls must exist in actual working code, not only in prompts/documentation.
11. Security audit logic and actual security enforcement must be developed together.
12. Do not claim “fully secure”, “bug-free”, “production-ready”, or “tests passed” unless the relevant implementation and verification actually support that statement.

## 5. Product/security modes

- **Local Offline Mode:** local processing, local policy, local memory, no external AI request.
- **Connected Mode:** only when explicitly enabled and required; outbound requests are task-scoped and policy-controlled.
- **Safe Mode:** disables AI-driven external actions while retaining safe/informational functions.
- **Emergency Stop:** immediately stops AI-driven active automation and nonessential background work; resume requires explicit user confirmation.

## 6. Risk model already defined

- **Tier 0:** informational.
- **Tier 1:** reversible low-risk action.
- **Tier 2:** user-content mutation.
- **Tier 3:** external communication / irreversible action.
- **Tier 4:** sensitive/prohibited; hard denied to AI automation.

Examples of Tier 4: UPI PIN, banking password, OTP use/retrieval, financial authorization, secret extraction.

## 7. Permission architecture

Permissions are scoped as:

```text
Global
 → App
   → Capability
     → Action
       → Session / Context
```

Unknown apps/actions/capabilities fail closed.

A convenience “allow all non-finance apps” concept may exist later, but financial/sensitive categories must remain outside it.

## 8. Work completed so far

### 8.1 Android foundation

- Android application foundation created.
- Application ID: `ai.ritav.app`.
- Compile/target SDK baseline was established at Android 36 in the current project foundation.
- Minimum SDK was established at 26.
- Compose-based UI foundation exists.
- Manifest intentionally started without `INTERNET` permission as part of the local-first baseline.
- Backup was disabled in the initial foundation.

### 8.2 Deterministic policy/security foundation

Implemented baseline components include:

- Risk tiers.
- `ActionRequest` / policy decisions.
- `PolicyEngine`.
- `ExecutionPolicyGate`.
- Scoped permission grants and exact session matching.
- Persistent encrypted permission storage through `SecurePermissionStore` + `SecureLocalStore`.
- `AppCapabilityRegistry`.
- `CapabilityPolicyGate`.
- `ActionPlan` and exact plan hashing/binding.
- `ActionAuthorizationGate`.
- `ActionAuthorizationService`.
- `DeviceAuthorizationGateway` abstraction.
- `SecurityRuntimeState`.
- Emergency Stop / safe-state control.
- `SecurityExecutionPipeline`.
- `ExecutionBridge`.
- `ResultVerifier`.
- Local audit event contracts and encrypted local audit implementation.
- Prompt-injection trust boundary.
- Network egress/data-classification firewall.
- Identity/session abstraction.

### 8.3 Emergency Stop

Emergency Stop was centralized so the UI/runtime does not maintain an unrelated safety boolean. It is process-local and independent of the AI model.

Activation is audited. Resume requires an explicit user-confirmation signal.

### 8.4 Authorization hardening

The one-time authorization token is bound to the exact `ActionPlan`, expires, and is consumed atomically.

A security weakness was then found and fixed: token minting was externally callable. `ActionAuthorizationGate.issue()` is now `internal`, so trusted production paths use `ActionAuthorizationService` instead of arbitrary callers self-asserting authorization levels.

### 8.5 Sensitive Information Firewall

A deterministic pre-AI/pre-execution `SensitiveInformationFirewall` has been implemented and hardened.

It currently covers pattern-based detection for categories including:

- OTP
- UPI PIN
- CVV
- private keys
- API/access/secret keys
- recovery/backup codes
- password-context values

Important properties already added:

- Maximum inspected input length: 16,384 characters.
- Matched secret values are not returned in `SensitiveMatch`.
- Original secret values are not intentionally persisted by the firewall.
- Multiple matches are handled.
- Overlapping matches are deduplicated/removed safely.
- Redaction is performed right-to-left so source offsets remain valid.

**Important:** this firewall is **not complete yet**. Regex detection is not sufficient for a production-grade sensitive-data boundary. Unicode/obfuscation, contextual detection, OCR/screen filtering, structured input handling, false-negative testing, and full AI-input integration still need work.

### 8.6 Security execution integration

`SecurityExecutionPipeline` currently checks:

1. Exact action-plan/request identity.
2. Sensitive input through the firewall.
3. Explicit `containsSensitiveData` flag.
4. Trusted active identity session for protected actions.
5. Deterministic execution policy.
6. One-time authorization token where required.
7. Audit events for decisions/authorization.

`ExecutionBridge` performs the capability-policy check before entering the security pipeline, then calls only an approved `AndroidActionAdapter`, catches adapter exceptions, verifies the result, and audits execution/verification.

### 8.7 Network boundary

A deterministic `NetworkEgressFirewall` exists with data classifications:

- PUBLIC
- USER_DATA
- SENSITIVE
- SECRET

Secrets are denied. User data/sensitive data require explicit authorization. This is currently a policy component; actual Android networking must later be forced through the same boundary.

### 8.8 Prompt-injection boundary

`PromptInjectionBoundary` treats user commands differently from app/external content and does not allow untrusted content to authorize actions or override security policy.

This is a foundation only; it must later be integrated into real model/context ingestion.

### 8.9 Identity/session foundation

`IdentitySessionManager` exists as a minimal local session abstraction with expiry and unknown/owner/trusted identity levels.

This is **not** real voice/face biometric authentication yet. Physical Android/device-auth integration still requires implementation and real-device validation.

## 9. Important latest commits

The security foundation has been built through many incremental commits. Important recent commits include:

- `455c1df516f3b22645abcb060eab726cdbd0137f` — Android foundation.
- `dbad37d5913eaf92c41a9eecda8aaa9520662396` — requirements matrix.
- `139be776d60b38ac0ee023026beb04def112dc86` — `SecureLocalStore`.
- `02b0d624` — persistent secure permission store.
- `55234287` — Emergency Stop API correction.
- `dd2e56ac6d43af0eb11fbdbe762f5ff36ecfafa9` — `AppCapabilityRegistry`.
- `7b567110ade5c02778c412558fe0f5426568d0ac` — `ActionAuthorizationGate`.
- `8b56548f9535db4efa5c54bc25c1d8f64510ad6d` — `ExecutionPolicyGate`.
- `b3ab039990df06749d58958ebf02728f37dceb` — persistent runtime security state.
- `8fd2ecbe0a2d090bf0f461e92bc1c4bec21f9759` — corrected shared Emergency Stop controller wiring.
- `27b6e9d5f903c9e4e55d2fedc5d9c7afd5cfdb7b` — device authorization gateway.
- `cddc854e44e4f075266e85fa059287ff4254620b` — prompt-injection boundary.
- `d0cf6d053f15d7e0a082dc27fea41d0ab37bc4bb` — network egress firewall.
- `a8f70394a6048d1750f8dd322dd1990221f305de` — identity/session model.
- `2e91aaa3944d850731576c9a81308f877378f984` — result verification.
- `ddac6f73cdc80b6295e679eac48244194478f7a1` — security audit.
- `e0483c20104560c851ba70e6c7410015d1839c43` — security execution pipeline.
- `b59ee1991014af1d5f8883af47e073cc7c27d4b9` — pipeline tests.
- `b6c3914b45dd12775025f3f19a04c8b415796597` — secure audit integration for Emergency Stop.
- `39f70d6bec37d27ce159267cd99591f77e404e1b` — ExecutionBridge audit/fail-closed adapter handling.
- `d52a223921e524c450902ae16dd7159f49122799` — `CapabilityPolicyGate`.
- `0cefbb55a294e8d0ec6831051a54749e33473eeb` — capability policy tests.
- `b9561a1e5c89fff2eb32cc15fc14593c181b1e8e` — device compatibility baseline.
- `119c9e54aef...` — initial Sensitive Information Firewall.
- `ed72ee24b523b9ba734775016b623f96ab11caf2` — firewall update.
- `678f0158c0c39962a365cdfbcc791bb2cc2e7e34` — firewall input-length hardening.
- `e0631055d2d4f09496f80c6bdd3560d14c927214` — firewall overlap-handling hardening.
- `cb66ca5e25ecb196ace9c399d25c18189b99f70b` — authorization token minting restricted.
- `43f6738d6642d6b9bcd718dcf8c8b157ceccc635` — authorization tests aligned with action plans.

If an abbreviated historical SHA above is insufficient, inspect Git history rather than guessing.

## 10. Current exact stop point — START HERE

### We are currently stopped in Sensitive Information Firewall hardening/verification.

**Do NOT jump to Finance Firewall yet.**

The next work sequence is:

### Step A — Re-read current code

Fetch the current versions of:

- `app/src/main/java/ai/ritav/app/core/security/SensitiveInformationFirewall.kt`
- `app/src/main/java/ai/ritav/app/core/security/SecurityExecutionPipeline.kt`
- `app/src/main/java/ai/ritav/app/core/security/ActionAuthorizationGate.kt`
- `app/src/main/java/ai/ritav/app/core/security/ActionAuthorizationService.kt`
- related security tests.

Never assume an old SHA is still current; use the current file SHA for sequential updates.

### Step B — Create/finish dedicated firewall tests

If missing, create:

`app/src/test/java/ai/ritav/app/core/security/SensitiveInformationFirewallTest.kt`

Tests should cover at least:

- OTP detection.
- UPI PIN detection.
- CVV detection.
- Password-context detection.
- Recovery-code detection.
- Private-key detection.
- API-key/secret-key detection.
- Benign text is allowed.
- Multiple secrets in one input.
- Overlapping matches do not corrupt redaction.
- Redaction contains no original secret.
- `SensitiveMatch` does not contain the secret itself.
- Boundary-length input.
- Over-limit input behavior.
- Malformed/unusual input does not crash the app.
- False-positive/false-negative edge cases as the implementation evolves.

### Step C — Fix oversized-input behavior

Current implementation historically used `require(text.length <= MAX_INPUT_LENGTH)`, which can throw on oversized input.

The security boundary should fail closed **without crashing the application**. Design a clean result state such as an explicit blocked/inspection-failed reason rather than pretending that an oversized input was safely inspected.

Do not send an uninspected oversized value onward to AI reasoning.

### Step D — Harden normalization/obfuscation

After tests establish the current behavior, investigate safe Unicode normalization/obfuscation handling.

Do not blindly transform text and then reuse incorrect source offsets. Prefer either:

- a mapping-preserving normalization approach, or
- a conservative “cannot safely inspect → block” outcome.

Goal: reduce secret-detection bypasses without introducing crashes/corrupted redaction.

### Step E — Verify AI boundary integration

The firewall must eventually sit before sensitive content reaches AI reasoning/vision, not merely be a helper that a caller may forget to invoke.

Current `SecurityExecutionPipeline` already invokes it for `inputText`, but the broader model/context ingestion path is not yet built. Later, make the architectural choke point mandatory.

### Step F — Test and verify

Run the actual project tests/CI. If no CI status exists, say so. Do not write “passed” without evidence.

### Step G — Only after this layer is sufficiently verified

Move to the next security layer, expected to be:

1. Finance Firewall / financial-app isolation.
2. Screen/OCR/Accessibility sensitive-content filtering.
3. Stronger app capability registry integration.
4. Mandatory unified security execution choke point.
5. Confirmation/read-back/device-auth UI.
6. Secure audit log bounds/rotation/reason-code hardening.
7. Secure storage/Keystore edge-case testing.
8. Resource/memory pressure enforcement.
9. Real Android device security testing.
10. Then higher-level orchestration/voice/local AI/automation.

## 11. Known unfinished issues / technical debt

### Sensitive firewall

- Regex is not comprehensive secret detection.
- Unicode and obfuscation bypass resistance needs improvement.
- Contextual detection needs improvement.
- OCR/screen redaction integration is not complete.
- Dedicated comprehensive tests are still required.
- Oversized-input handling should fail closed without throwing.

### Policy architecture

`CapabilityPolicyGate` currently exists as a final capability boundary used by `ExecutionBridge`. The long-term architecture should make it impossible for an execution path to bypass the unified security pipeline.

### Authorization

`ActionAuthorizationGate.issue()` is now `internal`, which prevents ordinary external callers from self-minting tokens. Continue checking that trusted authorization paths are the only production path.

### Secure local storage

`SecureLocalStore` uses Android Keystore-backed AES-GCM for low-risk local state. It is not a universal secret vault and must not be used for OTPs/passwords/PINs/etc.

Still required:

- Keystore invalidation testing.
- Device restore/backup behavior testing.
- Atomicity/corruption behavior testing.
- More robust key lifecycle handling where required.

### Audit log

The current encrypted audit log exists, but the architecture should later add bounded storage/rotation and structured reason codes so arbitrary sensitive information cannot be smuggled into audit reasons and storage cannot grow without bound.

### Device authentication

`DeviceAuthorizationGateway` is an abstraction. Physical Android authentication UI and real-device testing are not complete.

### Identity

Current identity/session code is only a foundation. Real local voice/face enrollment/verification has not been completed.

### Network enforcement

A deterministic egress policy exists, but actual network clients must later be forced through it. A policy class alone is not sufficient if another code path can open a network connection.

### Prompt injection

The trust model exists, but real app/notification/web/document content ingestion must later route through it.

### Android automation

The approved adapter architecture exists, but actual Accessibility/Android app adapters are not yet the finished automation layer.

### Orchestrator / AI

The Master Orchestrator and specialist-agent architecture is specified but the full local AI/orchestration runtime has not been implemented.

### Voice

Wake phrase `Hey Ritav`, offline/local STT/TTS and authorization/read-back flows are planned, not finished.

### Finance

Financial/UPI automation is intended to be hard-denied. The dedicated firewall/isolation implementation is still unfinished.

## 12. What MUST NOT be done now

- Do not add a web app.
- Do not add web deployment just because the project is easier to demo that way.
- Do not change the project to iOS/desktop.
- Do not add cloud AI as a hidden dependency.
- Do not add telemetry by default.
- Do not weaken sensitive/finance rules to make demos easier.
- Do not put secrets in logs, prompts, tests, source code, GitHub Actions output, or repository files.
- Do not use a prompt as the only security mechanism.
- Do not assume AccessibilityService means unrestricted Android control.
- Do not claim real-device support until physically tested.
- Do not create a release-signing secret inside the repository.
- Do not turn release APK builds into automatic builds on every push; release workflow should remain manually triggered.

## 13. Development method — keep exactly this style

For every security/feature layer:

```text
1. Read blueprint/state/current code
2. Identify exact gap
3. Implement actual working code in repository
4. Add/adjust tests
5. Review security boundaries and failure modes
6. Run/inspect build + tests/CI
7. Fix failures
8. Update documentation/state
9. Commit to GitHub
10. Record exact stop point
11. Continue to next layer only when the current layer is adequately verified
```

**Important:** Do not do ten partially implemented security systems at once. Finish and test one layer before moving on whenever practical.

## 14. Blueprint requirements that must remain in scope

The full details live in `RITAV_BLUEPRINT.md`, but future chats must remember these major requirements:

- Local-first / offline-first.
- Zero-egress by default.
- Explicit intent for consequential external actions.
- Capability-based permissions.
- Sensitive Information Firewall.
- Finance Firewall.
- Prompt-injection boundary.
- Identity/session security.
- Emergency Stop.
- Encrypted local memory.
- Encrypted local audit.
- Voice authorization/read-back.
- Android device authentication for stronger actions.
- Safe lock-screen behavior; never bypass device lock.
- Background operation only within Android limits.
- Accessibility as a scoped capability, not unrestricted privilege.
- Dynamic Universal Master Orchestrator.
- Specialist agents with scoped capabilities.
- Verification after execution.
- Truth-first reporting.
- Resource-aware behavior for 4 GB RAM / 32 GB storage baseline.
- Cancellation/graceful degradation under resource pressure.
- Dependency/security scanning.
- Unit/integration/security/real-device testing.
- Manual release workflow and external signing keys.

## 15. Master continuation prompt — paste this into a new chat

Copy the block below as the first message in a new Ritav.ai development chat:

```text
જય શ્રી ગણેશ 🙏

CONTINUE RITAV.AI FROM THE EXISTING REPOSITORY — DO NOT RESTART.

You are continuing an existing Android-only project. The connected private GitHub repository is:
`darshandpatel63-prog/Ritav.ai`

IMPORTANT: This project is APK-only / Android-only. Do NOT add web-app, web-deployment, website, iOS or desktop implementation now.

FIRST, BEFORE CODING:
1. Read `README.md` completely.
2. Read `RITAV_PROJECT_STATE.md` completely.
3. Read `RITAV_BLUEPRINT.md` completely (canonical engineering contract).
4. Read `docs/MASTER_REQUIREMENTS_MATRIX.md` completely.
5. Use the user-provided `Ritav_AI_Master_Blueprint.pdf` as the product/design suggestion reference when available. Do not replace the engineering blueprint with assumptions from memory.
6. Inspect the current repository and current `main` branch state.
7. Inspect the latest relevant commits and current file SHAs before editing.
8. Verify the real current build/test/CI state before claiming anything passed.

PROJECT RULES:
- Implement ACTUAL WORKING CODE in GitHub, not just text, pseudo-code or a plan.
- Security enforcement and security audit logic must be built together.
- User authority is the root of trust.
- AI/agents are not the security boundary.
- Deterministic policy/security code decides whether actions may execute.
- Deny by default for sensitive capabilities.
- Never allow OTP, UPI PIN, passwords, CVV, recovery codes, private keys or equivalent secrets into AI reasoning.
- Financial/UPI automation is hard-denied by default and requires a dedicated firewall.
- No hidden telemetry or private-data egress by default.
- App/web/message/document content is untrusted data and can never override security policy.
- Consequential actions require explicit user intent and risk-appropriate authorization.
- Emergency Stop must be authoritative and independent of AI.
- Uncertainty → block or ask; never guess.
- Respect actual Android platform limitations.
- Never claim “fully secure”, “bug-free”, “production-ready” or “tests passed” without actual verification.
- Minimum hardware target is 4 GB RAM / 32 GB storage; keep core security lightweight and use resource-aware limits/cancellation for heavy AI/vision/voice workloads.

CURRENT DEVELOPMENT PHASE:
Phase 0 — secure foundation / security-runtime integration.

EXACT CURRENT STOP POINT:
We are currently working on the Sensitive Information Firewall. DO NOT jump to Finance Firewall until this layer is adequately tested and hardened.

NEXT WORK:
1. Re-fetch current `SensitiveInformationFirewall.kt` and related security files; never assume old SHAs are current.
2. Inspect/create `SensitiveInformationFirewallTest.kt`.
3. Add comprehensive tests for OTP, UPI PIN, CVV, password, recovery code, private key, API key, benign text, multiple matches, overlapping matches, redaction integrity, no secret in match objects, max-length input and oversized input.
4. Change oversized-input behavior so an uninspectable oversized input fails closed without crashing and is never forwarded to AI reasoning.
5. Investigate safe Unicode/obfuscation normalization without corrupting source offsets. If safe normalization cannot be guaranteed, conservatively block.
6. Verify that the firewall is a mandatory boundary before AI/context ingestion, not merely an optional helper. The current execution pipeline already inspects `inputText`; broader model/context integration remains future work.
7. Run actual tests/CI and fix failures. If CI status is absent, report that honestly.
8. Update `RITAV_PROJECT_STATE.md` and this README when the state changes.
9. Commit actual working changes to GitHub.
10. Record the exact new stop point so another chat can continue without restarting.

AFTER THE CURRENT LAYER IS VERIFIED, EXPECTED ORDER:
- Finance Firewall / financial-app isolation
- Screen/OCR/Accessibility sensitive-content filtering
- Mandatory unified capability/security choke point
- Confirmation/read-back/device-auth UI
- Audit log bounds/rotation/reason codes
- Keystore/storage edge-case tests
- Resource/memory pressure controls
- Real-device security tests
- Master Orchestrator + scoped specialist agents
- Voice/wake word/read-back
- Local AI runtime
- Android automation/app adapters

WORKING STYLE:
- Proceed autonomously when safe; do not repeatedly ask me to approve routine coding steps.
- Prefer one security layer at a time: implement → test → security review → CI → document → commit.
- If you discover a security weakness, fix it before moving forward.
- If a test/API mismatch exists, inspect the current repository and repair it rather than guessing.
- Keep all changes compatible with the blueprint.
- At the end of each turn, give me a short Gujarati status: what actually changed, commit SHA, what was verified, what remains, and the exact next stop point.

START NOW FROM THE EXACT CURRENT STOP POINT ABOVE.
```

## 16. If the new chat behaves incorrectly

If a new chat ignores the continuation instructions, does not inspect the repository, starts redesigning the project, adds web work, or jumps to unrelated features, stop it and return to this chat.

You can also tell the new chat:

```text
Do not restart Ritav.ai. You are violating the persistent continuation instructions. Re-read README.md, RITAV_PROJECT_STATE.md, RITAV_BLUEPRINT.md and docs/MASTER_REQUIREMENTS_MATRIX.md, then continue from the exact Current Stop Point.
```

## 17. Truth about this hand-off file

This README is a **living project hand-off document**. It must be updated as actual repository work progresses.

The repository is the authoritative record of implementation. This README is the continuation map. If README and actual code differ, inspect the current code and update the documentation rather than silently trusting stale text.

The goal is simple: **a new chat should be able to continue the same Ritav.ai engineering process from the exact point where the previous chat stopped, without losing architecture, security requirements, decisions, completed work, unfinished work, or development discipline.**
