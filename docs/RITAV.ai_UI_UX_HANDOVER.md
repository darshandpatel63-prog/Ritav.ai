# RITAV.ai — UI/UX IMPLEMENTATION HANDOVER

**Purpose:** Authoritative handoff for implementing the UI/UX blueprint without losing decisions between chats.

## Current repository baseline

- Repository: `darshandpatel63-prog/Ritav.ai`
- Baseline main commit for this documentation package: `27942d726f3e1b491454c1f511d17334a393ef49`
- Existing draft UI PR: **#31 — UI shell**
- PR #31 is intentionally **not** the final UI design. Its conversational shell is a temporary implementation slice and must be evaluated/reworked against `RITAV.ai_UI_UX_BLUEPRINT.md`.
- Current security architecture must not be weakened while implementing UI.

## Required reading at the beginning of a new implementation chat

Read once:
1. `docs/RITAV_COMMON_AI_WORKFLOW.md`
2. `docs/RITAV_ELITE_SECURITY_ADDENDUM.md`
3. `README.md`
4. `RITAV_PROJECT_STATE.md`
5. `RITAV_BLUEPRINT.md`
6. `RITAV.ai_UI_UX_BLUEPRINT.md`
7. this handover
8. relevant source/build/test files

Then inspect current `main`, current UI branch/PR state, latest CI, and actual source before modifying anything.

## UI implementation mission

Implement the complete UI described in `RITAV.ai_UI_UX_BLUEPRINT.md`.

The UI must be:
- attractive;
- premium;
- friendly;
- futuristic but usable;
- privacy-first;
- responsive;
- accessible;
- smooth;
- lightweight;
- hardware/resource-conscious;
- truthful about unavailable capabilities.

The blueprint is the authority. The implementation may improve details but must not silently contradict locked requirements.

## Global navigation — highest-priority UI system

Implement one global adaptive floating navigation system.

Required behavior:
- one global circular control;
- arbitrary user positioning;
- drag/touch/mouse support;
- position persistence;
- Free and Fixed modes;
- user can Fix and Release at any time;
- radial mode when spatially appropriate;
- horizontal/vertical linear mode near edges;
- adaptive geometry based on viewport and safe area;
- circular scrolling/rotation for many items;
- keyboard/accessibility support;
- no clipping/overlap;
- responsive to orientation, resize and IME;
- preserves position preference;
- lightweight implementation.

Settings must expose:
- Free/Fixed;
- reposition;
- reset;
- navigation appearance/preferences where appropriate.

Do not hard-code only nine positions. Nine positions are test/reference zones, not the positioning model.

## UI customization

Create a dedicated Settings → Appearance/UI branch when appropriate.

It must include at minimum:
- Light / Dark / System;
- color theme selection;
- button presentation style;
- animation Auto / On / Off;
- density;
- typography/display preferences supported by platform;
- global navigation preferences.

User-selected presentation preferences never modify security behavior.

Do not create dozens of unrelated button components. Use a consistent semantic button component system with selectable visual variants.

## Resource/performance requirements

Treat hardware efficiency as a first-class product requirement.

Prefer:
- Compose/platform primitives;
- vectors/system icons;
- semantic color tokens;
- reusable components;
- lazy lists;
- bounded state;
- event-driven layout recalculation;
- transform/alpha/simple animations;
- local preference storage.

Avoid:
- video backgrounds;
- particle systems;
- continuous canvas redraw;
- unnecessary sensors;
- polling;
- large image bundles;
- heavy animation libraries for simple effects;
- unnecessary dependencies;
- duplicated assets for themes;
- persistent high-frequency recomposition.

The UI should not keep the CPU/GPU busy when idle.

## Security boundary

The UI is never a security authority.

Use:
- `SecurityControlPort`
- `SecureExecutionPort`
- other already-approved safe facades.

Do not directly expose or instantiate:
- ActionAuthorizationService;
- permission stores;
- SecureLocalStore;
- SecureAuditLog;
- execution adapters;
- model-provider construction;
- network egress authority;
- raw authentication/authorization authorities.

Never allow a UI-only state variable to override real Emergency Stop/security state.

## Conversation/model behavior

The current model provider is intentionally unavailable/fail-closed.

Until a real approved provider is integrated:
- do not fabricate AI answers;
- do not silently call an external provider;
- show truthful unavailable/offline state;
- preserve the model security boundary.

## Security-sensitive UI behavior

Protected actions must show:
- what will happen;
- target;
- risk/impact where relevant;
- required capability;
- confirmation;
- result.

Emergency Stop must be globally accessible and reflect authoritative state.

External app/web/document/OCR content is untrusted data. It must never visually or semantically become a Ritav system instruction.

## Screen implementation order

Recommended order:
1. Design system foundation
2. App shell
3. Global Adaptive Floating Navigation
4. Settings/Appearance customization
5. Welcome/onboarding
6. Main AI Home
7. Conversation UI
8. Live Task UI
9. Security/Permission Center integration
10. Privacy Center
11. Memory/Training
12. Voice/Identity
13. Connected Services
14. Activity/Audit
15. Model/Device Capability
16. Emergency Stop
17. About/Licenses
18. Privacy/Terms
19. responsive/accessibility hardening
20. visual/performance/security QA

## Verification requirements

During implementation continuously verify:
- callers/callees;
- state flow;
- lifecycle behavior;
- security state freshness;
- data flow;
- recomposition;
- accessibility semantics;
- failure paths;
- viewport geometry;
- persistence;
- device resource use.

At the end of each logically complete feature, perform a consolidated system-level review.

For the global navigation feature specifically test:
- center;
- top-left;
- top-center;
- top-right;
- left-center;
- right-center;
- bottom-left;
- bottom-center;
- bottom-right;
- arbitrary intermediate coordinates;
- Free mode;
- Fixed mode;
- release/reset;
- 3, 5, 6, 8, 9+ destinations;
- touch;
- mouse;
- keyboard;
- rotation;
- resize;
- keyboard/IME;
- small and large windows;
- no clipping;
- no overlap;
- correct adaptive direction;
- circular rotation;
- active destination;
- navigation integration.

## Visual QA

Do not trust mathematical geometry alone.

Render and inspect actual screens on representative viewport sizes.

If available, use browser/device automation or screenshots for visual QA. For Android, use emulator/device UI tests and screenshots where practical.

Fix:
- awkward spacing;
- clipping;
- unreadable labels;
- poor edge behavior;
- excessive animation;
- accidental drag;
- confusing focus order;
- theme contrast;
- visual hierarchy issues.

## Testing layers

Required as applicable:
- unit tests for layout calculations;
- UI/component tests;
- accessibility tests;
- navigation tests;
- state-machine tests;
- security boundary regression tests;
- device/emulator tests;
- performance/resource checks;
- visual QA.

Manual physical-device validation must be reported as unverified until actually performed.

## Do not claim

Do not claim:
- fully supported on every device;
- fully secure;
- production-ready;
- all-device verified;
- real-device verified;

unless concrete evidence exists.

## Git workflow

UI implementation should normally use a dedicated branch and PR. Do not merge directly to `main) merely because the code compiles.

Before merge:
- exact-head CI must pass;
- review comments must be addressed;
- consolidated UI/security/performance review must be clean;
- docs/state/handoff must be updated where the work changes project status.

## Relationship to PR #31

PR #31 is a temporary conversational shell. Do not assume its current visual structure is approved.

When implementation resumes:
- inspect its latest CI state;
- preserve useful security-facade integration;
- redesign the visible UI according to the authoritative blueprint;
- avoid carrying over placeholder layout decisions simply because they already exist.

## Handoff rule

At every major UI milestone, update:
- `RITAV.ai_UI_UX_BLUEPRINT.md` if a design decision changes;
- this file if implementation status/order changes;
- project state/handoff documents when a major feature reaches a verified checkpoint.

A new chat should be able to continue from repository state + these documents without relying on prior conversation memory.
