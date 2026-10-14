# Ritav.ai — Ultra Secure App Builder adoption

## Why this was adopted

The user has directed that the supplied `ultra-secure-app-builder.skill` be used as an additional process for building and hardening Ritav.ai. The skill is a process layer, not a replacement security architecture.

## Locked interpretation for Ritav

- Product target remains Android, iOS/iPadOS, Windows, macOS, Linux and supported ChromeOS runtimes.
- Existing Ritav security invariants and the Elite Security Addendum remain authoritative.
- Ritav is handled at Tier 3 process depth.
- `RITAV_BLUEPRINT.md` is the canonical Blueprint; do not introduce a duplicate `BLUEPRINT.md`.
- Ritav is an existing app, so the skill's "Updating an existing app" workflow is the normal path.
- New product/UI features use feature-scoped research and intelligence, an extras gate, Design DNA preservation, threat/data/permission updates and security regression.
- New security-sensitive surfaces are treated as new trust boundaries until the complete call path is traced.
- Plain chat does not imply independent parallel agents. Role-based passes are used honestly unless real sub-agent execution is available.
- No speculative permission, SDK, provider, banking/UPI, telemetry or cloud integration is introduced just because the generic skill mentions it.
- Any conflict is resolved in favor of the stricter deterministic Ritav security behavior.

## Current artifact mapping

| Skill artifact/process | Ritav artifact or rule |
| --- | --- |
| `BLUEPRINT.md` | `RITAV_BLUEPRINT.md` |
| Change log / decisions | `RITAV_BLUEPRINT.md` + `DECISIONS.md` |
| Security checklist | `docs/RITAV_COMMON_AI_WORKFLOW.md` + `docs/RITAV_ELITE_SECURITY_ADDENDUM.md` + existing security implementation/tests |
| Platform matrix | `docs/RITAV_CROSS_PLATFORM_ARCHITECTURE.md` + `docs/MASTER_REQUIREMENTS_MATRIX.md` |
| Security regression | Existing continuous-regression workflow and tests; extend coverage when new surfaces are added |
| Privacy/data map | Existing local-first/data-handling design; create/update a dedicated map when a new data surface requires it |
| Permissions | Existing Android/security permission boundary and `PermissionCenter`; update permission documentation when new permissions are introduced |
| Recovery / supply chain / keys / abuse | Apply the skill's phase-specific checks when the corresponding subsystem exists or changes |
| Design DNA | Existing UI/UX blueprint and current product UI direction |

## Existing UI work

The first conversational UI/navigation slice was already started before this skill was adopted. Adoption does not invalidate that work or restart it. Future UI slices must use the skill's feature-scoped workflow and must continue consuming safe UI/security facades rather than internal security authorities.

## Completion rule

A work package is complete only after implementation, real integration, relevant tests, adversarial review, consolidated system-level review, documentation/state update and available CI verification. Unavailable evidence remains explicitly unverified.

## Source note

The canonical source for this binding is the user-provided `ultra-secure-app-builder.skill` package from this development session. The repository stores the durable Ritav-specific process contract here and in `skills/ultra-secure-app-builder/SKILL.md`; deep reference material should be read phase-by-phase when supplied/available rather than copied into the repo as stale duplicates.
