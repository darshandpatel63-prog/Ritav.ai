# Ritav.ai — Cross-Platform Architecture Decision

**Decision status:** Active product-scope expansion, superseding the earlier Android-only product scope.

## 1. Product scope
Ritav.ai is no longer limited to Android. The product target is a common Ritav experience across:

- Android phones and tablets
- iPhone and iPad
- Windows laptops, desktops and compatible 2-in-1 devices
- macOS laptops/desktops
- Linux laptops/desktops
- ChromeOS devices where the supported runtime permits
- Other future form factors only through an explicitly implemented platform adapter

The goal is one security model, one platform-neutral core, and platform-specific adapters/UI/runtime integrations rather than separate product logic per operating system.

## 2. Architecture
```text
                         Ritav User Experience
                                  |
                    Platform-neutral application core
                                  |
        +-------------------------+-------------------------+
        |                         |                         |
   Android adapter          Apple adapter            Desktop adapter
   Android/Android         iOS/iPadOS               Windows/macOS/Linux
   tablet/phone APIs       APIs + permissions       OS APIs + permissions
        |                         |                         |
        +-------------------------+-------------------------+
                                  |
                   Deterministic security boundary
             Policy -> Capability -> Privacy/Finance -> Auth
                                  |
                         Approved execution
                                  |
                         Result verification
                                  |
                           Local audit
```

The platform-neutral core must not assume Android APIs, package names, accessibility semantics, notification models, filesystem semantics, biometric APIs, background-execution rules, or permission behavior.

## 3. Platform contract
`core/src/commonMain/.../platform/RitavPlatform.kt` defines:

- supported platform families;
- common form factors;
- runtime capability facts;
- `DeviceProfile`.

`RitavPlatformAdapter` defines the platform boundary. A concrete adapter reports what the current host actually supports. A capability being unavailable is never permission to perform a restricted action.

## 4. Platform implementation plan

### Android
Reuse the existing Android application and security infrastructure. Add platform adapter implementations only where a real Android API/runtime path exists.

### iOS / iPadOS
Use platform-native APIs and permission boundaries. No assumption is made that Android AccessibilityService, background execution, filesystem access, or package identity semantics exist on Apple platforms.

### Windows
Use a Windows-native application/runtime adapter. Desktop automation must remain capability-scoped and must pass through the same deterministic policy and authorization boundary.

### macOS
Use a macOS-native application/runtime adapter. Accessibility and screen-recording permissions are treated as explicit OS trust boundaries.

### Linux
Support Linux desktop environments through explicit adapters. Desktop-environment differences are capability facts, not hidden assumptions.

### ChromeOS
Support only the runtime surface actually exposed to the selected application form factor (for example Android/container capabilities where applicable). Unsupported host capabilities remain unavailable.

## 5. Shared security invariants
Every platform must preserve the existing Ritav security invariants:

1. No autonomous consequential action.
2. AI/agents never become the final security boundary.
3. OTP, UPI PIN, passwords, CVV, recovery codes, private keys, API keys/tokens and equivalent secrets never enter AI reasoning.
4. Financial/UPI automation remains hard-denied by deterministic controls.
5. External content is untrusted and cannot grant permission or override policy.
6. Execution requires scoped permission, explicit intent where required, and risk-appropriate authorization.
7. Emergency Stop remains authoritative.
8. Security failures fail closed.
9. Network/data egress remains capability-controlled and audited.
10. Platform availability never weakens security policy.

## 6. Portability rule
Platform-specific implementations may add capability restrictions but may not weaken the common security contract. When a platform cannot provide an equivalent secure mechanism, the feature must be unavailable or fall back to a safer user-directed flow rather than bypassing the boundary.

## 7. Build strategy
Phase 1: keep the existing Android product build intact while the platform-neutral core contracts are established.

Phase 2: add verified platform targets and concrete adapters incrementally, with independent CI on the native operating-system runners required by each target.

Phase 3: add platform-native UI/runtime packaging for Android, iOS/iPadOS, Windows, macOS and Linux, sharing only logic that is genuinely platform-neutral.

No platform is considered supported merely because an enum or build target exists. Support requires a real implementation, integration, relevant tests, packaging/build verification, and platform-specific security review.

## 8. Compatibility principle
The product targets phones, tablets, laptops, desktops and other supported device classes without making a universal hardware-performance claim. Runtime capability detection and bounded-resource policies determine which features are available on a specific device.

## 9. Supersession
This decision supersedes older statements that describe Ritav.ai as an Android-only product. Existing Android security controls and Android platform constraints remain valid for the Android implementation; they are not removed or weakened by this scope expansion.
