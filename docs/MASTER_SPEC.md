# Ritav.ai — Master Specification

This document is the implementation-oriented companion to `RITAV_BLUEPRINT.md`.

## 1. Source alignment

The user-provided `Ritav_AI_Master_Blueprint.pdf` defines the product as an Android-first, privacy-first, local-first assistant where the user remains the ultimate authority. It specifies local processing by default, zero-egress by default, capability-based permissions, technical sensitive-data isolation, fail-closed behavior and truth-first behavior. fileciteturn1file0L11-L27

It also specifies the Universal Master Orchestrator, security gate, specialist agents, action policy engine, Android bridge and result verification as the central execution architecture. fileciteturn1file0L28-L45

## 2. Security-first execution contract

```text
USER INPUT
  ↓
LOCAL SECURITY GATE
  ↓
INTENT / CONTEXT
  ↓
UNIVERSAL MASTER ORCHESTRATOR
  ↓
SPECIALIST AGENTS (only when useful)
  ↓
ACTION POLICY ENGINE
  ↓
PERMISSION + RISK + IDENTITY CHECK
  ↓
AUTHORIZATION / CONFIRMATION
  ↓
ANDROID BRIDGE
  ↓
TARGET APP / DEVICE
  ↓
RESULT VERIFICATION
  ↓
LOCAL AUDIT
  ↓
USER
```

The AI layer never receives unrestricted raw device control. The Android bridge exposes a constrained action vocabulary rather than arbitrary code execution. The source blueprint gives examples such as `findElement`, `click`, `typeText`, `scroll`, `swipe`, `back`, `home`, `openApp` and `readAllowedText`. fileciteturn1file0L39-L45

## 3. Data-privacy contract

The first implementation must assume:

- AI network access denied by default.
- No telemetry or behavioral analytics by default.
- Local encrypted memory.
- Local encrypted, user-deletable audit logs.
- Microphone, camera, screen, notification, clipboard, file and app access are separate capabilities.
- Cloud document synchronization is disabled unless explicitly authorized. fileciteturn1file0L46-L53

## 4. Sensitive-data firewall

The implementation must create a security boundary before model reasoning/vision. Protected information includes OTPs, UPI PINs, banking PINs, passwords, CVVs, security codes and recovery codes. OTP-like values should be redacted/dropped where detectable; finance/UPI UI content must be excluded from AI reasoning/vision where technically possible. fileciteturn1file0L56-L62

## 5. Finance firewall

UPI, banking, wallet, trading and investment apps are high-risk/default-deny. Ritav must not autonomously initiate payments, transfers, trades or other consequential financial actions. NFC automation is hard-blocked in the baseline design. fileciteturn1file0L63-L67

## 6. Permission contract

Permissions use the dimensions:

`App × Capability × Action × Risk Level`

The action policy engine receives structured action requests and checks identity, user intent, permission, app scope and risk before execution. fileciteturn1file0L68-L75

## 7. Message authorization

When a composed message cannot be seen by the user, Ritav must read the exact final text aloud and obtain explicit confirmation before sending. Predicting that the user would approve is never sufficient. fileciteturn1file0L76-L80

## 8. Identity

Local owner/family voice and face enrollment may be supported. Unknown users are blocked from protected capabilities. Continuous camera monitoring is not the default. Voice/face are authorization signals rather than absolute security guarantees; stronger authentication is appropriate for high-risk actions. fileciteturn1file0L81-L86

## 9. Background and emergency controls

Background operation is explicitly enabled. Lock-screen operation is limited to safe capabilities. The emergency kill switch should disable the AI service, microphone/wake listener, camera access and automation/accessibility capability. fileciteturn1file0L87-L91

## 10. Prompt-injection model

All app, website, message, notification and document content is untrusted data. Authority hierarchy:

`System Security Policy > User Explicit Command > App Content > External Content`

External content can provide information but cannot rewrite policy or grant capabilities. fileciteturn1file0L92-L98

## 11. Memory

Ritav uses temporary context plus encrypted local long-term memory. User personalization includes names, aliases, phrases, meanings, communication preferences and routines. The initial approach is a local semantic dictionary/retrieval memory; local adapters can be considered later. fileciteturn1file0L99-L103

## 12. Voice

The supplied blueprint defines the initial wake phrase as “Hey Ritav”, with a future option for a custom local assistant name. Offline STT/TTS and local wake-word detection are preferred. fileciteturn1file0L104-L106

## 13. Vision

Screen perception/accessibility can support app interaction subject to Android restrictions, app design and anti-cheat limitations. Protected screens must be filtered before vision/reasoning. Game automation remains a future research capability rather than a universal guarantee. fileciteturn1file0L107-L111

## 14. Repository/documentation contract

GitHub is the project's persistent source of truth so development can continue across chats. The supplied blueprint calls for persistent documentation covering architecture, security, privacy, permissions, orchestration, voice authorization, app capabilities, roadmap, development status, decisions, testing and release process. fileciteturn1file0L119-L126

## 15. Verification contract

Testing must cover unit, integration, security and real-device behavior, including network/egress inspection, permission boundaries, prompt injection, authorization, identity and sensitive-data isolation. Critical regression tests must cover finance isolation, secret filtering, send authorization, permission revocation, unknown identity blocking and network-disabled defaults. fileciteturn1file0L127-L133

## 16. Release contract

The target CI pipeline is lint → unit tests → security scan → dependency audit → APK build → APK verification → release artifact. Release signing keys remain outside the repository and SHA-256 checksums accompany releases. fileciteturn1file0L134-L137

## 17. Core invariants

These are permanent regression requirements:

- NO AUTONOMOUS CONSEQUENTIAL ACTION.
- NO OTP / UPI PIN / PASSWORD / CVV / SECURITY SECRET TO AI REASONING.
- NO FINANCIAL TRANSACTION AUTOMATION BY DEFAULT.
- NO HIDDEN TELEMETRY OR PRIVATE-DATA EGRESS BY DEFAULT.
- NO APP CONTENT OVERRIDES SECURITY POLICY.
- NO EXECUTION WITHOUT VALID PERMISSION + USER INTENT + RISK-APPROPRIATE AUTHORIZATION.
- UNCERTAINTY → BLOCK OR ASK; NEVER GUESS.

These invariants are explicitly defined in the source blueprint. fileciteturn1file0L166-L173
