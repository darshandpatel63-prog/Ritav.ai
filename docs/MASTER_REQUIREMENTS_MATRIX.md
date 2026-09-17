# Ritav.ai — Combined Master Requirements

## Source basis
Ritav.ai development uses the product/design reference, `RITAV_BLUEPRINT.md`, the security workflow/addendum, and the active cross-platform architecture decision in `docs/RITAV_CROSS_PLATFORM_ARCHITECTURE.md`.

## Product scope
Ritav.ai is a cross-platform personal AI assistant/automation system. Target platforms and form factors are:

- Android — phones/tablets and supported Android devices.
- iOS/iPadOS — iPhone/iPad and supported Apple devices.
- Windows — laptops/desktops/2-in-1 devices where supported.
- macOS — laptops/desktops.
- Linux — supported laptop/desktop environments.
- ChromeOS — supported runtime/form factors where required capabilities are exposed.

The product uses one platform-neutral core plus platform-native adapters and UI/runtime implementations. A platform is not considered supported merely because a contract or build target exists; real implementation, integration, testing, packaging/build verification and platform-specific security review are required.

## Combined rules being implemented

- Local-first/offline-first AI whenever technically feasible.
- No hidden telemetry or private-data egress by default.
- User remains the authority; AI is never the security boundary.
- Deterministic policy/security gates sit below the model/agent layer.
- Dynamic Universal Master Orchestrator with task-specific specialist agents.
- Agents receive scoped capabilities, not unrestricted OS privileges.
- Granular permissions: global → app → capability → action → session/context.
- Financial/UPI automation is isolated and denied by default.
- OTP, PIN, password, CVV, recovery code and equivalent secrets are hard privacy boundaries.
- Consequential actions require risk-appropriate authorization and confirmation.
- Generated unseen messages require exact read-back before sending where policy requires it.
- Voice identity and optional local face verification are authorization signals, not universal proof.
- Screen capture, accessibility, automation and other privileged platform capabilities are explicit opt-in boundaries and are constrained by each OS.
- External/app/document content is untrusted data and cannot override security policy.
- Network access is capability-controlled and audited.
- Local memory is encrypted where supported and must not retain secrets.
- Emergency Stop/Safe Mode can stop AI-driven external actions.
- Testing must include privacy, egress, permission, prompt-injection, identity, sensitive-data isolation and regression tests.
- CI/CD must verify each platform build/test path before release; signing keys never enter the repository.
- Platform-specific limitations must reduce capability rather than weaken security controls.

## Current implementation strategy

1. Preserve the existing Android security foundation and application build.
2. Maintain a platform-neutral Kotlin Multiplatform core for contracts and genuinely shared logic.
3. Maintain `RitavPlatform`/`DeviceProfile` capability facts and `RitavPlatformAdapter` as the runtime boundary.
4. Implement concrete platform adapters one platform at a time using native APIs and permission models.
5. Add platform-native UI/runtime packaging for Android, iOS/iPadOS, Windows, macOS and Linux, with ChromeOS support where the selected runtime permits it.
6. Add per-platform CI and real-device/real-host verification.
7. Only then claim a platform as supported.

The product must remain safe when a platform capability, AI runtime, network, voice recognition or automation service is unavailable.

## Security preservation rule
The cross-platform expansion does not replace or weaken any existing security layer. Finance firewall, sensitive-information firewall, deterministic policy, capability/permission gates, authorization, Emergency Stop, egress controls, result verification and audit remain authoritative. Platform adapters may only expose capabilities already permitted by the common security model and the host OS.
