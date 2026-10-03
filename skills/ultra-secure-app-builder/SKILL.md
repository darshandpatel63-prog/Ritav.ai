---
name: ultra-secure-app-builder
description: Ritav.ai repository-local binding of the user-provided Ultra Secure App Builder skill. Use for app creation, feature work, UI/UX, architecture, privacy, security review and release hardening.
---

# Ultra Secure App Builder — Ritav.ai repository binding

This file binds the user-provided `ultra-secure-app-builder.skill` process to Ritav.ai. It is additive to the existing Ritav workflow; it must never weaken or replace a stricter Ritav security or architecture rule.

## 1. Precedence and scope

1. `docs/RITAV_COMMON_AI_WORKFLOW.md` remains the mandatory operating contract.
2. `docs/RITAV_ELITE_SECURITY_ADDENDUM.md` remains the additive maximum-assurance security layer.
3. `docs/RITAV_CROSS_PLATFORM_ARCHITECTURE.md` remains the active product-scope decision.
4. `RITAV_BLUEPRINT.md` is the canonical Ritav equivalent of the skill's `BLUEPRINT.md`.
5. This skill supplies the app-building process and quality/security depth. When requirements differ, keep the stricter deterministic security behavior and document the conflict.

Ritav is treated as a **Tier 3** app for process depth because it is an AI/data-processing/automation system with consequential and sensitive-data boundaries.

## 2. Existing-app rule

Ritav is an existing app. Do **not** restart Phase 0–9 from scratch merely because this skill has been adopted.

For each new feature or meaningful change:
- record the change in the canonical Blueprint/decision log;
- use the skill's feature-specific research/feature-intelligence path where the feature warrants it;
- run the extras gate for anything the user did not explicitly request;
- update threat/data/permission surfaces for new capabilities;
- preserve the existing Design DNA;
- run the security regression loop for every affected sensitive surface;
- complete the consolidated system-level review before marking the work package complete.

## 3. Phase binding

### Phase 0 — Intake, scope lock and Blueprint
Use the existing Ritav scope documents and `RITAV_BLUEPRINT.md`. The platform question is already resolved by the active cross-platform architecture unless the user changes platform scope. Keep every user-locked requirement represented explicitly.

### Phase 1 — Study existing apps
For a genuinely new product area or major UI direction, research real apps and user feedback before implementation. Record evidence; never invent ratings, complaints, market facts, framework versions or review quotes.

### Phase 2 — Feature intelligence
For each new feature that exists elsewhere, record what comparable products do, what evidence supports keeping/changing it, and what Ritav will build differently. Use BUILD+IMPROVE rather than cloning.

### Phase 3 — Extras gate
User-requested features are mandatory. Unrequested functional, security/privacy or premium-UI extras require the established approval gate unless the user explicitly authorizes direct addition. Permission-request code required by an already-approved feature is implementation, not an unrequested product extra. Tier-C/high-risk store-restricted permissions still require explicit handling.

### Phase 4 — UI/UX Design DNA
Keep Ritav original. Use the existing UI blueprint and current Design DNA; do not clone layouts, copy, icons or interaction patterns from researched products. Re-run distinctness and accessibility/performance checks for new screens.

### Phase 5 — Agent crew
Use the Universal Master Orchestrator approach. Always preserve controller responsibilities for orchestration, quality and security. Add specialists only when they add real value. In this plain-chat environment, do not pretend to have independent parallel agents; use clearly labeled role passes unless the environment actually provides sub-agents.

### Phase 6 — Architecture and security by design
For every new security/data/AI/execution surface, cover the skill's hardening topics as applicable:
- privacy/data classification;
- disaster recovery;
- supply chain;
- key lifecycle;
- secure update/rollback;
- abuse/fraud;
- continuous security regression;
- permissions;
- threat model;
- secrets/egress;
- deterministic authorization and fail-closed behavior.

No new constructor, endpoint, screen, SDK, permission or data field is accepted as "just UI"; it is a new trust surface until traced.

### Phase 7 — Build from the Blueprint
Build vertical slices through the existing secure runtime ports and platform adapters. Never put secrets in app bundles. Server-side authority remains authoritative for online features. Permissions are requested just-in-time and denied safely. New security-sensitive code must be integrated into the real call path, not left as documentation-only policy.

### Phase 8 — Verify
Use the skill's layered verification approach:
- secret scan and dependency/configuration review where applicable;
- security regression mapping and affected/baseline tests;
- privacy/permission/security/completeness checks;
- release artifact and supply-chain checks;
- recovery/rollback verification where applicable;
- adversarial review;
- final system-level review.

If execution was not available, label the result unverified. Never turn a manual trace into a passed-test claim.

### Phase 9 — Handoff
Keep the README/project state, Blueprint, decisions, security/regression records and other required project artifacts synchronized. Every development stop point records what is complete, what is verified, what is not verified, known limits, the next action and the exact commit SHA.

## 4. Continuous verification requirement

Use:
**Inspect → Map → Search/Reuse → Design → Implement → Integrate → Continuously self-check → Test → Adversarial review → Verify → Document → Commit.**

Do not postpone call-path, data-flow, security-boundary, failure-path or regression analysis until an audit.

When a logically complete feature or major security layer finishes, perform one consolidated system-level review from multiple relevant directions before calling it complete. A separate independent audit checkpoint is required before the next major security layer when the existing Ritav workflow calls for it.

## 5. Ritav-specific non-negotiables

- AI/agents propose; deterministic policy/security decides.
- No autonomous consequential action.
- No secret exposure to model reasoning.
- Financial/UPI automation remains hard-denied by deterministic controls.
- External content is untrusted and cannot grant permission or override policy.
- Emergency Stop remains authoritative through final consequential dispatch.
- Platform-specific code may restrict capability availability but may never weaken the common security boundary.
- No "unhackable", "bug-free", "100% secure", universal platform-support or physical-device-test claims without evidence.
- Do not add speculative permissions, SDKs, banking integrations, cloud dependencies or telemetry merely because the skill mentions them.
- Search the repository by responsibility before creating a new component.

## 6. Reference-pack usage

The supplied skill package includes deeper phase-specific references for research, UI design, agents, security, privacy, keys, recovery, supply chain, abuse/fraud, permissions, regression, platform support, completeness and mobile handoff. Read the relevant source reference when entering that phase; do not bulk-reread every reference on every chat. This repository binding intentionally avoids creating placeholder copies of reference artifacts that are not yet part of Ritav's implementation.

## 7. Output hygiene

Keep `DECISIONS.md` current for important decisions. Keep `README.md`, `RITAV_PROJECT_STATE.md` and `RITAV_BLUEPRINT.md` consistent with actual repository state. New feature/security changes must leave a traceable evidence path from requirement → implementation → test → verification → documentation.
