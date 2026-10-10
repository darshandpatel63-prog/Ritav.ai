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


## 2026-10-03 — Bound conversational history and IME submission

### Decision
Keep the current conversational shell intentionally local/fail-closed while adding bounded UI-state retention and keyboard-friendly submission.

### Rationale
- The message list previously grew without a bound during a session.
- A hard bound of 100 messages limits memory growth without changing the product's current non-persistent conversation semantics.
- IME Send reuses the existing send path and therefore does not introduce a new execution or model authority.
- Input remains bounded at 4096 characters.

### Security / privacy impact
No new permission, network path, provider, SDK, privileged capability, or persistent data surface was introduced.

## 2026-10-10 — Responsive conversation, IME and settings regression coverage

### Decision
Keep the conversation context and bounded message history in one scrollable viewport while keeping the message composer available above the IME. Trigger latest-message scrolling from a separate send revision so it remains correct after the message-history bound is reached.

### Implementation boundaries
- Retain the 100-message history, 4096-character input cap and 8192-character stored-message cap.
- Keep IME submission and the Send button on the same presentation-only path.
- Give Permission Center a bounded scroll region above its navigation actions on short/IME-reduced screens.
- Respect IME insets in the main shell and floating-navigation viewport; declare `adjustResize` for reliable viewport resizing.
- Verify persisted appearance preferences, blocked/stopped task-state copy, and the Emergency Stop recovery-to-fresh-authentication path with regression tests.

### Security and privacy
No model/provider, action execution, permission, capability, network/egress path or new runtime dependency is added. Conversation output remains honest that the production model/provider is unavailable. These changes do not give the UI authorization or adapter authority.

### Verification
- The first implementation iteration at `dd49935ca8759746dc3ed175a8e9788edc552f93` failed Android instrumentation compilation because the test used the unavailable `assertExists()` API. The test was corrected to the supported `assertIsDisplayed()` API in `d709aaa153e7aaae6a87a778407f8669ffa5272e`.
- Exact implementation/test HEAD `d709aaa153e7aaae6a87a778407f8669ffa5272e`: Android unit/instrumentation Run #702 — SUCCESS; all 21 managed-device tests completed. https://github.com/darshandpatel63-prog/Ritav.ai/actions/runs/38043832147
- Exact implementation/test HEAD `d709aaa153e7aaae6a87a778407f8669ffa5272e`: Android release validation Run #34 — SUCCESS, including security configuration, source secret scan, release tests/lint, signed CI APK/AAB verification, non-debug APK check, R8 mapping and artifact upload. https://github.com/darshandpatel63-prog/Ritav.ai/actions/runs/38043832163
- CI uses an ephemeral validation signing key. Physical-device visual/touch/keyboard/accessibility QA and production signing/distribution remain unverified.

## 2026-10-10 — Adaptive navigation linear-capacity correction

### Finding and correction
- Source review found that the linear navigation menu counted capacity on the viewport width when the menu actually stacked items vertically, and on height when items formed a horizontal row.
- Capacity calculation now uses the same axis as the selected placement strategy and is covered by pure JVM regression tests for vertical stack, horizontal row and minimum-viable-axis fallback.

### Boundary
- The correction changes presentation geometry only. Navigation destinations, user-controlled positioning, security state, authorization and task execution paths remain unchanged.
- Exact-head Android CI must pass before this work package is marked verified.

### Placement-boundary follow-up
- Review of the placement formulas also found the first item's top-left coordinate was calculated from an item-center coordinate and clamped per item. Near a viewport boundary, that could compress multiple items into the same clamp value.
- Linear placement now constrains the complete item group before laying out positions, and JVM tests assert items remain within margins and separated along their active axis.
- Geometry regressions use narrow/tall and wide/short viewports so per-item clamping that compresses the final spacing cannot pass only because the menu is centered on a square screen.

## 2026-10-10 — Adaptive navigation viewport geometry checkpoint

### Exact-head verification
- Production layout correction at `994ef4849fd202930f1a6861483af9e5d51024cd`: Android release validation Run #38 — SUCCESS.
- Full implementation/test HEAD `4cf90e240b507ff83297652812a0aa3acfbdc9ff`: Android release validation Run #39 — SUCCESS; Android unit/instrumentation Run #707's JVM tests and instrumentation compilation passed, while managed-device instrumentation was still in progress at checkpoint time.
- Run #39: https://github.com/darshandpatel63-prog/Ritav.ai/actions/runs/38044841684
- Run #707: https://github.com/darshandpatel63-prog/Ritav.ai/actions/runs/38044841687

### Remaining verification
- Do not mark the full current unit/instrumentation workflow verified until Run #707 reports its final managed-device result.
- Physical-device visual/touch/keyboard/TalkBack QA remains deferred until whole-product completion.


## 2026-10-10 — Adaptive navigation capacity and Back-priority hardening

### Decision and behavior
- Calculate menu item capacity on the actual stack/row axis and count only required inter-item gaps; no trailing gap is reserved after the final menu item.
- Expose the global-navigation toggle state with Compose semantics (`Expanded` / `Collapsed`).
- When the menu is expanded, Android Back closes it first rather than executing the destination/screen-level Back behavior.

### Regression coverage and correction history
- JVM geometry coverage verifies correct axis selection, minimum viable slot, bounded/non-overlapping layout at constrained widths/heights, and four-item stack fit at the exact margin/spacing boundary.
- The initial accessibility test head `84f6b763d90abd5cf5cbc8cd420447f67abce014` failed instrumentation-test compilation because a Compose `assert` extension import was missing.
- Commit `3d1baa3a422a21f1efa6c5326e54864d3d70ab59` added the import; Run #711 passed on that intermediate source/test head.
- The final regression then navigated to Security / Permission Center, opened global navigation, dispatched Android Back, and asserted that the menu collapsed while Permission Center remained displayed.
- Exact-head Android unit/instrumentation Run #713 passed: 22 tests on `pixel2api30`, 0 failed / 0 skipped. Exact-head Android release Run #45 passed. Links: https://github.com/darshandpatel63-prog/Ritav.ai/actions/runs/38046331913 and https://github.com/darshandpatel63-prog/Ritav.ai/actions/runs/38046331909.

### Security/system boundary
- Geometry, semantics and menu Back handling are presentation-only; they cannot resume/stop tasks or grant protected-action authority.
- No execution, authorization, permission, identity, trusted-app, provider, egress, audit or result-verification authority changed.
- No new permission, production dependency, or privileged capability.
- Emergency Stop remains controlled by the security facade and its final dispatch guard is independent of menu Back handling.

### Open limits / next step
- Physical-device visual/touch/keyboard/TalkBack QA remains deferred until whole-product completion.
- Launcher icon wiring remains untouched.
- Continue Phase 1 responsive/accessibility review; Phase 2 Policy Engine only after Phase 1 closure.
- Exact source/test HEAD: `703a6892deabdc11731d36ca1dc955f10a7b6743`; `main`: `81396a4fe7fdd21b20a19b69774d407b2a302508`; PR #31 open/draft/unmerged, 162 ahead / 4 behind.
- Informal planning estimates: Phase 1 UI ~90–91%, overall project ~58%, security foundation ~99%. These are not formal metrics.
