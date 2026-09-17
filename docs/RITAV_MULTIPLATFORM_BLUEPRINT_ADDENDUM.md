# Ritav.ai — Multi-Platform Product & Engineering Blueprint Addendum

**Status:** Approved product-direction change from the project owner; implementation begins incrementally from the existing Android codebase.

## 1. Platform scope expansion

Ritav.ai is no longer Android-only. The product target is a cross-platform assistant that can run on the major device families and operating systems relevant to the product, including:

- Android phones and tablets.
- iPhone and iPad through iOS/iPadOS.
- Windows PCs and compatible Windows tablets/laptops.
- macOS Macs.
- Linux desktops/laptops and supported Linux distributions.
- ChromeOS devices where the supported Android/Linux runtime permits the feature.
- Other future device/OS targets only after an explicit platform adapter and security review.

“All devices” means broad supported-platform coverage, not an unsupported promise that every hardware model, OS version, vendor build, or form factor will expose identical capabilities.

## 2. Architecture rule

The existing deterministic security model remains the product-wide security boundary. Platform expansion must not create separate, weaker security implementations.

```text
                    Ritav Universal Core
      ┌──────────────────────────────────────────┐
      │ Intent / Orchestration contracts          │
      │ Policy / Capability / Authorization       │
      │ Sensitive-data / Finance boundaries       │
      │ Action plans / Verification / Audit       │
      │ Cross-platform task and context contracts │
      └──────────────────────────────────────────┘
             │          │          │          │
          Android      iOS/iPadOS  Desktop    Linux
             │          │       Windows/macOS   │
             └──────── Platform Adapters ────────┘
                          │
                  OS-native capabilities
```

The universal core may propose and authorize platform-neutral operations, but OS-specific adapters are the only layer allowed to touch platform capabilities. Every adapter remains below the deterministic policy/security boundary.

## 3. Platform adapter model

Each supported platform gets a dedicated adapter layer rather than copying the Android implementation.

A platform adapter is responsible for:

- OS-native lifecycle/background rules.
- Secure storage and key-management primitives.
- Device authentication APIs.
- Notifications and user interaction APIs.
- Accessibility/assistive APIs only where legitimate and permitted.
- Screen/vision capture only where explicitly authorized and technically supported.
- App/service integration mechanisms exposed by that OS.
- Platform-specific networking and permission APIs.
- Result observation and verification.

Adapters must expose only the minimum capabilities supported by that platform and device. Unsupported capability means `DENY/UNAVAILABLE`, never a simulated success.

## 4. Security invariants across every platform

The following remain universal and cannot be weakened by an OS adapter:

1. AI/agents are never the final security boundary.
2. No autonomous consequential action.
3. OTP, PIN, password, CVV, recovery codes, private keys, API/access tokens and equivalent secrets never enter AI reasoning.
4. Financial/UPI/payment authorization automation remains default-deny/hard-denied where required by the security model.
5. External content cannot modify policy or grant authorization.
6. Execution requires scoped capability, required user intent and risk-appropriate authorization.
7. Emergency Stop/Safe Mode remains authoritative.
8. Security failures fail closed.
9. Private data egress is denied unless explicitly authorized by policy.
10. Platform restrictions and store/policy requirements are authoritative.

## 5. Cross-platform capability negotiation

Ritav must not assume feature parity.

At runtime, the platform layer reports a capability profile such as:

```text
platform
osVersion
formFactor
secureStorage
biometricAuthentication
voiceInput
screenCapture
accessibilityAutomation
backgroundExecution
notifications
localModelSupport
networking
```

Each capability has an availability state and security policy. A task is planned against the capabilities actually available on the current device.

## 6. Device/form-factor model

Phones, tablets, laptops, desktops and other supported form factors share the universal core but may use different interaction and execution adapters.

The UI is responsive/adaptive rather than assuming a phone screen. Desktop keyboard/mouse/windowing, touch-first tablet interaction, mobile lifecycle constraints and platform-specific accessibility must be treated as distinct adapter concerns.

## 7. Local-first and AI runtime strategy

The core architecture remains local-first. Local AI/runtime implementations are platform-specific behind a common interface.

A device may use:

- on-device local model;
- platform-native AI/runtime where explicitly permitted;
- an explicitly authorized remote model/service in Connected Mode.

Remote processing never becomes implicit merely because a platform lacks local model support.

## 8. Cross-platform security storage

The common layer must depend on a secure-storage abstraction, not Android `Context`, `SharedPreferences`, or Android Keystore APIs directly.

Platform implementations use the strongest supported native mechanism, for example Android Keystore, Apple Keychain/Secure Enclave where applicable, Windows credential/key-protection facilities, macOS Keychain, and Linux secret-storage/key facilities where available.

The exact primitive is platform-specific and must be independently reviewed; the common layer must not pretend that different operating systems provide identical guarantees.

## 9. Cross-platform automation

Prefer official APIs, intents, deep links, URL schemes, accessibility/assistive APIs, automation frameworks, and IPC mechanisms according to the platform.

Visual/UI automation is a fallback and must remain capability-scoped, privacy-filtered, policy-controlled and result-verified.

No platform adapter may bypass the universal security pipeline.

## 10. Screen/OCR/accessibility privacy

Screen, OCR, accessibility, notification and clipboard ingestion are separate platform-specific ingress boundaries.

Sensitive content must be classified before entering AI context. If safe classification/redaction cannot be completed, the data is blocked from AI context.

No platform gets a blanket "read everything" privilege.

## 11. Networking and egress

The existing deterministic egress policy applies to every platform. Platform-native networking libraries are implementation details, not authorization mechanisms.

Every external request remains subject to destination identity, reason, data classification and authorization.

## 12. Build and release strategy

The product will evolve from the existing Android application without deleting working Android security infrastructure.

Initial engineering sequence:

1. Preserve and stabilize the current Android security foundation.
2. Introduce a platform-neutral core contract module.
3. Keep Android as the first concrete platform adapter while migrating only genuinely platform-neutral contracts.
4. Add iOS/iPadOS adapter architecture.
5. Add Windows/macOS/Linux desktop adapter architecture.
6. Add device/form-factor adaptive UI contracts.
7. Add platform-specific secure storage/authentication/automation implementations one platform at a time.
8. Add per-platform CI and device validation before declaring a platform supported.

No platform is marked supported merely because it compiles. Support requires relevant automated tests, security review and an approved runtime/device validation path.

## 13. Compatibility policy

Supported versions and device classes will be maintained in a platform matrix. Unsupported or unverified combinations must be surfaced as unavailable rather than silently falling back to unsafe behavior.

## 14. Migration rule

Existing Android security controls remain authoritative during migration. Common-code extraction must be behavior-preserving and test-backed. No security control may be removed simply to make code portable.

## 15. Completion definition

Cross-platform expansion is complete only when:

- universal security contracts are implemented;
- each target platform has a real adapter;
- adapter permissions/capabilities are mapped;
- platform-specific secure storage/authentication is verified;
- sensitive-data ingress boundaries are implemented where the platform exposes them;
- automation paths are policy-gated and result-verified;
- CI/builds exist for the supported target;
- representative real-device/real-OS validation exists;
- documentation accurately lists supported and unverified combinations.

Until then, a platform is **planned**, **implemented**, **CI-verified**, or **device-verified** according to actual evidence.
