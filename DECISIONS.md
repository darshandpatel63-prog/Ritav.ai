# Ritav.ai — Decision Log

## 2026-10-03 — Adopt Ultra Secure App Builder process

### Decision
Adopt the user-provided `ultra-secure-app-builder.skill` as a mandatory additive development process for Ritav.ai app creation, feature development, UI/UX, privacy, security and release work.

### Evidence
- User explicitly instructed that the skill be used for building Ritav.ai and that the instructions be added to the repository.
- The supplied skill requires secure-by-design implementation, Blueprint-led planning, feature intelligence, extras gating, original UI, specialist review, permissions, privacy/data classification, disaster recovery, supply-chain and update hardening, abuse/fraud controls, and continuous security regression.

### Ritav-specific decisions
1. Preserve `docs/RITAV_COMMON_AI_WORKFLOW.md`, `docs/RITAV_ELITE_SECURITY_ADDENDUM.md` and the active cross-platform architecture as higher-priority existing contracts.
2. Use `RITAV_BLUEPRINT.md` as the canonical Blueprint; do not create a duplicate `BLUEPRINT.md`.
3. Treat Ritav as Tier 3 for process depth.
4. Use the skill's existing-app change path instead of restarting the whole project.
5. Keep extras behind the existing approval gate unless the user explicitly authorizes direct addition.
6. Keep deterministic security boundaries below models/agents; the skill never grants execution authority to AI.
7. Do not claim that adoption itself makes the product secure, complete or production-ready.

### Current adoption checkpoint
- Current development branch: `ui/conversational-shell`.
- PR #31 remains open/draft and unmerged.
- Exact current PR head at adoption time: `f92e84d6e9898a9d93a632b9e09b288cad7b5831`.
- Exact current PR-head Android CI Run #680 is SUCCESS.
- This adoption change is documentation/process-only; it does not alter the existing security execution boundary.

### Next-use rule
For future `Start/Continue`, apply the repository-local skill together with the existing startup workflow, then choose the next logically complete work package from the live repository state.
