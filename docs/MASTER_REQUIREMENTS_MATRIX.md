# Ritav.ai — Combined Master Requirements

## Source basis

Ritav.ai development uses **both** of these sources together:

1. The user-provided `Ritav_AI_Master_Blueprint.pdf` — the broader product, security and feature suggestions supplied as the project design reference.
2. `RITAV_BLUEPRINT.md` — the repository engineering blueprint that converts the project discussion into implementation rules.

Neither source is silently discarded. The PDF is treated as the broader product/design reference; the repository blueprint is the implementation contract. Where they describe the same requirement, the implementation preserves the intent of both. Any future conflict must be recorded as an explicit architecture decision rather than silently choosing one.

## Combined rules being implemented

- Local-first/offline-first AI whenever technically feasible.
- No hidden telemetry or private-data egress by default.
- User remains the authority; AI is never the security boundary.
- Deterministic policy/security gates sit below the model/agent layer.
- Dynamic Universal Master Orchestrator with task-specific specialist agents.
- Agents receive scoped capabilities, not unrestricted Android privileges.
- Granular permissions: global → app → capability → action → session/context.
- Financial/UPI automation is isolated and denied by default.
- OTP, PIN, password, CVV, recovery code and equivalent secrets are hard privacy boundaries.
- Consequential actions require risk-appropriate authorization and confirmation.
- Generated unseen messages require exact read-back before sending where policy requires it.
- Voice identity and optional local face verification are authorization signals, not universal proof.
- Accessibility and Android automation are constrained by platform rules and must verify outcomes.
- External/app/document content is untrusted data and cannot override security policy.
- Network access is capability-controlled and audited.
- Local memory is encrypted and must not retain secrets.
- Emergency Stop/Safe Mode can stop AI-driven external actions.
- Testing must include privacy, egress, permission, prompt-injection, identity, sensitive-data isolation and regression tests.
- CI/CD must verify code before APK release; signing keys never enter the repository.

## Current implementation strategy

Build the secure foundation first:

1. Android shell and premium UI foundation.
2. Deterministic policy/security model.
3. Permission and capability model.
4. Safe mode/emergency stop.
5. Local persistence abstraction.
6. Testable orchestration contracts.
7. Only then add voice, local AI and Android automation adapters.

The product must remain functional and safe even when AI models, network access, voice recognition or automation services are unavailable.
