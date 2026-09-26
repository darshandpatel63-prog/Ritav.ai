# RITAV.ai — UI / UX BLUEPRINT

**File:** `RITAV.ai_UI_UX_BLUEPRINT.md`  
**Status:** Authoritative UI/UX specification for implementation  
**Product:** Ritav.ai  
**Scope:** Cross-platform product UI; Android is the current executable UI target.  
**Relationship:** This document extends `RITAV_BLUEPRINT.md`; it does not weaken any security invariant or platform constraint.  
**Primary goals:** premium, friendly, futuristic, privacy-first, accessible, adaptive, smooth, resource-efficient.

> **Authoritative UI rule:** When implementing UI, use this document as the source of truth for visual language, interaction behavior, navigation, states, accessibility, performance, privacy presentation, and user customization. If an implementation conflicts with this document, update the blueprint first or explicitly record the decision in the Change Log.

---

## 1. Purpose & Design Philosophy

Ritav.ai UI must make a sophisticated security-first AI assistant feel simple and trustworthy.

The experience is:
- futuristic without looking like noisy science fiction;
- premium without requiring heavy graphics;
- friendly without becoming childish;
- privacy-first without exposing internal security complexity;
- adaptive to the user's device, input method, screen size and preferences;
- fast enough for low-to-mid-range supported hardware;
- understandable to first-time users and efficient for experienced users.

The UI communicates **control, clarity and calm**. Ritav is an assistant, not an owner.

Primary interaction philosophy:

`USER → INTENT → ORCHESTRATOR → POLICY → PERMISSION → CONFIRMATION → EXECUTION → VERIFICATION → AUDIT`

The UI may explain this flow in human language, but it must never imply that an AI response itself grants authority.

---

## 2. UI/UX Non-Negotiable Principles

1. User control comes first.
2. Security state must never be visually hidden when it affects an action.
3. No fake AI responses, fake execution, fake success or fake online state.
4. Never bypass `SecurityControlPort` or the deterministic security boundary.
5. Never expose protected secrets through UI, logs, previews or accessibility descriptions.
6. Default to local processing and local UI state where practical.
7. No unnecessary telemetry or remote UI state.
8. Every consequential action has a clear state: pending, confirmed, executing, verified, failed, blocked or stopped.
9. Emergency Stop remains visually and interactionally accessible.
10. Motion is purposeful and user-controllable.
11. Accessibility is built into components, not added later.
12. UI must degrade gracefully on constrained hardware.
13. Avoid large image/video assets when vectors, shapes or system icons are sufficient.
14. Avoid persistent high-frequency animation.
15. Do not create duplicate navigation systems.
16. Existing security/admin functionality remains integrated through approved public facades.
17. UI customization must not alter security policy.
18. User-selected themes/buttons/animations are presentation preferences only.
19. Any ambiguous security-critical state must fail closed in the underlying system and be clearly communicated.
20. Claims shown in UI must reflect verified runtime state.

---

## 3. Product Identity / Visual Language

### 3.1 Personality

Ritav should feel:
- intelligent;
- calm;
- capable;
- private;
- modern;
- approachable;
- technically advanced but not intimidating.

### 3.2 Visual language

Use:
- clean surfaces;
- restrained gradients;
- subtle depth;
- rounded geometry;
- strong hierarchy;
- generous but efficient spacing;
- high-quality iconography;
- small amounts of controlled glow/accent treatment.

Avoid:
- excessive neon;
- full-screen animated backgrounds;
- glassmorphism everywhere;
- decorative particle systems;
- constant pulsing;
- visually noisy dashboards;
- tiny text;
- too many cards.

### 3.3 Brand motif

The Ritav identity should be recognizable through a consistent combination of:
- circular/ring geometry;
- a restrained AI/security accent;
- rounded containers;
- clear status indicators;
- a distinctive but simple app icon.

---

## 4. Color System

The system uses semantic color tokens rather than hard-coded colors.

Required modes:
- Light
- Dark
- System/default

Required user-selectable theme families:
- Ritav Default
- Ocean/Blue
- Emerald
- Violet
- Sunset/Warm
- Graphite/Monochrome
- Additional themes may be added later without changing component contracts.

Every theme must define:
- background;
- surface;
- elevated surface;
- primary;
- secondary;
- accent;
- text primary;
- text secondary;
- border/divider;
- success;
- warning;
- error;
- security/protected;
- disabled.

Accessibility rules:
- semantic status cannot rely on color alone;
- contrast must remain readable;
- user themes cannot remove required error/warning distinction;
- security-critical states retain recognizable shape/icon/text indicators.

Theme switching should not recreate the entire app unnecessarily. Prefer stable composition with token changes.

---

## 5. Typography

Use a platform-appropriate, highly legible system-first typeface.

Rules:
- large title hierarchy;
- readable body text;
- compact metadata;
- clear labels;
- monospaced typography only for technical identifiers/code-like data;
- support dynamic font scaling;
- avoid text embedded in images.

Typography scale must adapt to screen size and user accessibility settings.

Never truncate security-critical messages in a way that changes meaning.

---

## 6. Iconography

Use one coherent icon family.

Icons must:
- have consistent stroke/weight;
- have predictable semantic meaning;
- support light/dark themes;
- remain recognizable at small sizes;
- include accessible labels.

Do not make every button visually unique.

The product uses a **small standardized button system**. Users may choose among supported button presentation styles globally from UI Settings, while the underlying component semantics remain identical.

---

## 7. Spacing / Grid / Shape / Elevation

Use a consistent spacing scale based on small platform-independent increments.

Recommended baseline:
- compact: 4–8dp;
- standard: 12–16dp;
- comfortable: 20–24dp;
- section: 28–40dp.

Shapes:
- compact controls: moderately rounded;
- cards/surfaces: medium/large radius;
- primary global navigation: circular;
- dialogs: large but not excessive radius.

Elevation should be subtle. Prefer tonal/surface separation over large shadows.

No screen should become visually dense merely to avoid scrolling.

---

## 8. Motion & Animation

Motion is optional, purposeful and resource-aware.

Allowed examples:
- radial navigation expansion;
- menu rotation;
- task progress transition;
- listening indicator;
- subtle security-state transition;
- voice waveform when actively listening;
- success/failure confirmation;
- screen transition.

Rules:
- short durations;
- no continuous animation unless actively communicating live state;
- pause/stop animation when off-screen;
- respect system reduced-motion settings;
- user can disable non-essential live animation globally;
- animation setting has at least: **Auto / On / Off**;
- critical security status must remain understandable without animation.

Performance:
- prefer transform/alpha/simple shape animation;
- avoid expensive blur/particle effects;
- avoid continuously redrawing large areas;
- avoid animated videos as UI backgrounds;
- use lazy composition where appropriate.

---

## 9. Accessibility

Minimum requirements:
- keyboard navigation where platform supports it;
- screen-reader names;
- logical focus order;
- visible focus;
- touch targets of appropriate minimum size;
- dynamic text scaling;
- content descriptions for icon-only actions;
- semantic grouping;
- Escape/back dismissal where applicable;
- reduced motion;
- high-contrast-friendly themes;
- no color-only meaning;
- error messages associated with the relevant control.

Accessibility labels must describe the actual action, not internal implementation details.

---

## 10. Responsive / Device Adaptation

The UI is adaptive across:
- phone;
- foldable;
- tablet;
- desktop window;
- large monitor;
- landscape/portrait;
- keyboard-visible states.

Use available window size rather than device model assumptions.

Adapt:
- navigation density;
- content columns;
- panel widths;
- radial/linear menu geometry;
- dialog size;
- touch/mouse affordances;
- typography density.

Do not duplicate the entire UI per device class unless platform behavior genuinely differs.

---

## 11. Global App Shell

The shell contains:
- global adaptive navigation;
- persistent security status affordance;
- main content region;
- global Emergency Stop access;
- transient feedback/toast region where appropriate;
- modal/dialog host.

The shell must not own privileged security logic. It observes approved facade state.

The shell should remain lightweight and mounted consistently to avoid navigation flicker.

---

## 12. Navigation Architecture

### 12.1 Global navigation model

Ritav uses a **Global Adaptive Floating Navigation** system.

The primary control is a circular menu button that:
- is available throughout the app;
- can be dragged by the user;
- can be positioned at arbitrary coordinates within safe bounds;
- remembers its position locally;
- can be switched between Free and Fixed positioning;
- can be released from Fixed mode at any time.

### 12.2 Position modes

**Free**
- user can drag the circle;
- preferred position is persisted locally;
- system may make a temporary safe adjustment for keyboard, rotation or impossible viewport constraints;
- user preference is preserved.

**Fixed**
- user selects and confirms a preferred location;
- the position remains stable relative to the usable viewport;
- orientation/window changes re-map the anchor proportionally rather than blindly using stale pixels;
- system may perform only the minimum accessibility/safe-area correction required.

Settings:
- Navigation Position: Free / Fixed
- Reposition Navigation
- Reset Navigation Position

### 12.3 Closed state

Only the circular control is visible.

It must look like part of Ritav, not a generic floating action button.

### 12.4 Opening modes

The layout engine chooses:
- radial;
- horizontal linear;
- vertical linear;
- constrained/hybrid arrangement when required.

Inputs:
- circle center;
- viewport/safe-area dimensions;
- keyboard/inset state;
- menu item count;
- item measured size;
- minimum spacing;
- active destination;
- accessibility/touch target constraints.

Outputs:
- mode;
- direction;
- item positions;
- visible item count;
- rotation range;
- overflow strategy.

### 12.5 Radial behavior

Near corners/central areas, options expand toward available space.

The algorithm must:
- avoid viewport clipping;
- avoid system bars/insets;
- avoid keyboard obstruction;
- avoid overlap;
- preserve readable labels;
- bias expansion away from the nearest blocking edge.

### 12.6 Linear behavior

Near an edge, a linear menu may be preferable.

Examples:
- left edge → expand right;
- right edge → expand left;
- top edge → expand down;
- bottom edge → expand up.

Direction is determined by actual available space, not merely a hard-coded zone.

### 12.7 Circular overflow

For many destinations:
- do not shrink labels below usability;
- show a comfortable visible subset;
- allow swipe/drag/rotation;
- support mouse wheel where appropriate;
- expose accessible next/previous controls;
- provide a clear indication that more destinations exist;
- preserve a continuous circular model where it remains understandable.

### 12.8 Interaction

Tap/click:
- opens/closes.

Drag:
- repositions only after deliberate movement threshold.

Small movement:
- remains a tap.

Keyboard:
- focus opens;
- arrow/tab navigation follows logical order;
- Escape closes;
- Enter/Space activates.

### 12.9 Persistence

Persist only the minimum preference:
- mode;
- normalized position;
- optional preferred menu behavior.

Do not upload navigation position.

### 12.10 Performance

The global menu must:
- avoid polling;
- avoid sensors;
- avoid background work;
- calculate layout only on relevant state changes;
- use lightweight primitives;
- avoid large assets;
- avoid unnecessary recomposition;
- pause animation when not visible.

---

## 13. Welcome / Onboarding

Onboarding is progressive, not overwhelming.

Stages:
1. Ritav introduction.
2. Privacy/local-first explanation.
3. Core UI preferences.
4. Optional voice setup.
5. Optional permissions.
6. Security/Emergency Stop explanation.
7. Finish.

Never request every permission at startup merely for convenience.

Users can skip optional configuration and revisit it later.

---

## 14. Main AI Home

The home screen is the primary calm workspace.

It should communicate:
- Ritav status;
- local/connected mode;
- listening state;
- active task;
- security state;
- current conversation;
- quick safe actions.

Do not turn the home into a dashboard full of technical metrics.

The global navigation remains available.

---

## 15. Conversation UI

Conversation is the primary interaction surface.

Requirements:
- clear user/assistant message distinction;
- readable message width;
- timestamps only where useful;
- streaming state only when a real model is producing output;
- explicit unavailable/offline state when no model exists;
- copy/share actions only when permitted;
- sensitive content display follows classification rules.

Never fabricate an AI response when a model runtime is unavailable.

Conversation history:
- lightweight;
- locally managed where applicable;
- bounded;
- user-deletable;
- never treated as unrestricted training data.

---

## 16. Live Task UI

When Ritav performs a multi-step task, show:
- task name;
- current step;
- overall state;
- requested/active capabilities;
- confirmation state;
- progress where measurable;
- Stop control;
- final verification result.

Do not expose hidden chain-of-thought.

Use human-readable summaries such as:
- Planning
- Waiting for permission
- Waiting for confirmation
- Executing
- Verifying
- Completed
- Blocked
- Stopped
- Failed safely

---

## 17. Permission Center

The Permission Center is an administration surface, not a casual chat screen.

Show:
- capability;
- scope;
- risk;
- active/inactive;
- duration;
- confirmation behavior;
- revocation;
- relevant app scope.

Protected capabilities must clearly show when they are unavailable by policy.

The UI must use `SecurityControlPort`; raw permission stores/authorization authorities remain internal.

---

## 18. Security Center

Show the current security posture in understandable terms.

Examples:
- Security boundary active.
- Emergency Stop active.
- Model unavailable.
- Online mode active.
- Protected capability blocked.
- Device authentication required.

Provide access to:
- Emergency Stop;
- active sessions;
- protected capability information;
- security events/status;
- trusted-app state where applicable.

Never expose internal cryptographic material.

---

## 19. Privacy Center

Sections:
- Data on device;
- Network/online access;
- Memory;
- Conversation retention;
- Voice;
- Vision;
- Activity/audit;
- deletion controls.

Use plain language first, technical detail second.

Every data category should answer:
- What?
- Why?
- Where?
- How long?
- Can I delete/disable it?

---

## 20. Memory & Training

Separate:
- Memory;
- Personalization;
- User training.

Controls:
- view;
- edit;
- delete;
- clear all;
- export only where safe and explicitly supported.

Protected security policy cannot be changed through training.

Show whether a preference is:
- temporary;
- persistent;
- app-specific;
- global.

---

## 21. Voice & Identity

Voice UI includes:
- wake phrase;
- spoken name;
- voice recognition state;
- speaker verification;
- confirmation phrase;
- lock-screen voice policy;
- microphone permission;
- recording/processing status.

Visual microphone indicator must be unambiguous.

Do not imply that voice alone is sufficient for protected financial/secret actions.

Voice recordings are not retained by default beyond immediate operation unless the user explicitly enables a supported feature.

---

## 22. Connected Services

Each service has:
- connected/disconnected;
- scope;
- data category;
- permissions/capabilities;
- last-used state where appropriate;
- revoke/disconnect control.

Never display access tokens.

Connected services must not silently become available to AI.

---

## 23. Activity / Audit

Activity UI shows safe summaries:
- timestamp;
- task;
- action category;
- target app/service;
- permission decision;
- confirmation;
- result;
- policy reason.

Avoid raw sensitive content.

Users should be able to filter by:
- date;
- task;
- category;
- result.

Audit UI must not become a secret storage viewer.

---

## 24. Model & Device Capability

Show:
- local model availability;
- model class/name only where safe;
- device capability summary;
- resource mode;
- estimated availability;
- online fallback state.

Do not expose low-level hardware telemetry continuously.

Provide user-facing modes such as:
- Battery Saver;
- Balanced;
- Performance;
- Auto.

The actual model runtime remains authoritative.

---

## 25. Emergency Stop

Emergency Stop is a global safety control.

Requirements:
- accessible from global shell/menu;
- visually distinct;
- one clear activation action;
- immediate state feedback;
- protected execution paths observe it;
- user can release/reset only through the existing security authority;
- stopped tasks clearly report stopped/blocked state.

The UI must never claim an action stopped unless the underlying security/execution path confirms the relevant state.

---

## 26. Settings

Settings is organized into branches:

### Appearance
- Light/Dark/System;
- theme;
- accent;
- button style;
- navigation style;
- animation mode;
- density;
- font/display scaling where supported.

### Navigation
- Global Adaptive Navigation;
- Free/Fixed;
- position;
- reset position;
- radial/linear preferences where exposed;
- menu behavior.

### Conversation
- message density;
- text size;
- response presentation;
- history/retention preferences.

### Voice
- wake phrase;
- spoken name;
- voice confirmation;
- microphone behavior.

### Vision
- camera behavior;
- visual processing;
- sensitive-content handling;
- permission state.

### Privacy
- memory;
- history;
- audit;
- connected services;
- deletion.

### Security
- Emergency Stop;
- authentication;
- permission center;
- protected-action confirmation;
- trusted app/security state.

### Performance
- resource profile;
- animations;
- battery-conscious mode;
- background behavior.

### Accessibility
- font scaling;
- contrast;
- reduced motion;
- screen-reader behavior;
- touch target preferences.

### About
- version;
- build;
- licenses;
- open-source notices;
- privacy/terms.

UI customization is deliberately centralized so users can personalize appearance without changing security behavior.

---

## 27. About / Licenses

Must include:
- Ritav version;
- build information;
- open-source notices;
- third-party licenses;
- security/privacy documentation entry points;
- project attribution.

Do not expose secrets or internal endpoints.

---

## 28. Privacy Policy / Terms

Use readable legal documents with:
- headings;
- table of contents where useful;
- version/date;
- language selection if supported.

Never make absolute security promises.

---

## 29. Common Components

Create reusable components for:
- buttons;
- icon buttons;
- cards;
- status chips;
- dialogs;
- bottom sheets;
- navigation items;
- text fields;
- toggles;
- segmented controls;
- sliders;
- progress indicators;
- empty states;
- error states;
- confirmation panels;
- task timeline;
- security banners.

One component contract may support multiple user-selected visual styles.

---

## 30. Security Status Components

Standard statuses:
- Secure/normal;
- Attention;
- Protected;
- Blocked;
- Emergency Stop;
- Offline;
- Online;
- Model unavailable;
- Authentication required;
- Confirmation required.

Every state has:
- icon/shape;
- text;
- optional color;
- optional motion;
- accessible description.

---

## 31. Confirmation Components

Confirmation UI must state:
- what will happen;
- target;
- important data involved;
- risk/impact;
- required capability;
- confirmation action;
- cancel/stop action.

Never use ambiguous buttons such as “Continue” for consequential operations when a specific action name is possible.

---

## 32. Error / Offline / Unavailable States

Errors are calm and actionable.

Pattern:
1. What happened.
2. What Ritav did not do.
3. What the user can do next.

Example:
> Model runtime unavailable. No external model bypass was attempted. You can retry later or continue with available local features.

Never show raw stack traces to ordinary users.

---

## 33. Loading / Empty States

Loading indicators must communicate real work.

Do not animate indefinitely when the operation has already failed.

Empty states explain:
- why the area is empty;
- what the user can do;
- whether setup is optional.

Use lightweight skeletons only where they improve perceived performance.

---

## 34. Permission & Authentication UX

Permission requests must be:
- contextual;
- minimal;
- explainable;
- revocable.

Authentication prompts must identify:
- why authentication is needed;
- what action it protects;
- whether it is device authentication or Ritav-specific confirmation.

Do not request sensitive permissions before they are needed.

---

## 35. SecurityControlPort UI Boundary

UI accesses security capabilities only through safe public facade interfaces such as `SecurityControlPort`.

The UI must not directly instantiate or mutate:
- authorization services;
- permission stores;
- secure storage authorities;
- audit storage authorities;
- execution adapters;
- model providers;
- network egress authorities.

The UI can request a safe operation; deterministic security code decides whether it is allowed.

---

## 36. UI → Security → Execution Data Flow

Conceptual flow:

`UI event → safe command/request → security facade → policy/permission/confirmation → execution pipeline → adapter → verification → audit → UI state`

UI must not:
- construct privileged authorization tokens;
- invent policy decisions;
- call adapters directly;
- mark execution successful before verification;
- disable Emergency Stop locally.

---

## 37. UI → Model Data Flow

`User input → UI validation/bounds → context/privacy boundary → model runtime → model output boundary → safe UI presentation`

If the model is unavailable:
- show unavailable state;
- do not silently switch to an unapproved provider;
- do not claim inference occurred.

Model output is data, not authorization.

---

## 38. Sensitive Data Display Rules

Never knowingly display:
- OTP;
- UPI PIN;
- password;
- CVV;
- private key;
- access/refresh token;
- recovery code;
- equivalent secret.

Where a protected value must be referenced:
- mask it;
- minimize it;
- avoid copying;
- avoid accessibility leakage;
- avoid screenshots where possible.

Sensitive visual content must remain subject to the existing classification/filtering boundary.

---

## 39. Prompt-Injection / Untrusted Content UX

External content is visually marked as untrusted where the distinction matters.

Examples:
- web content;
- app content;
- messages;
- documents;
- OCR;
- imported files.

Use language such as:
> “This content is information, not an instruction to Ritav.”

Do not expose technical attack terminology unless useful.

Never let external content visually imitate a trusted Ritav system instruction.

---

## 40. Emergency Stop UX

When activated:
- global shell reflects stopped state;
- protected action controls become unavailable as appropriate;
- active task shows stopped/blocked;
- user receives concise confirmation;
- no animation is required for safety.

Reactivation/release follows the actual security state.

---

## 41. Voice UX

Voice states:
- Idle;
- Listening;
- Processing;
- Confirming;
- Speaking;
- Finished;
- Blocked;
- Error.

Use a subtle waveform or ring only while it communicates a live state.

User can disable decorative voice animation while retaining essential microphone/listening indication.

Never imply recording is happening when it is not.

---

## 42. Vision UX

Vision is visibly opt-in/task-scoped.

When active:
- clear camera/screen-reading indicator;
- task purpose;
- stop control;
- privacy state.

Prefer structured UI/accessibility information over screenshots.

No silent camera activation.

---

## 43. Automation UX

Automation is represented as a task, not as invisible magic.

Show:
- planned high-level action;
- required capability;
- confirmation;
- current step;
- verification;
- final result.

Never expose hidden chain-of-thought.

For risky actions, require the actual configured confirmation path.

---

## 44. Memory UX

Memory should feel like user-owned preferences, not surveillance.

Every persistent memory should have:
- source/context;
- value summary;
- scope;
- delete control.

No secret or prohibited security material enters ordinary memory.

---

## 45. Online/Offline UX

Always make the effective mode understandable:
- Local/Offline;
- Connected;
- Connecting;
- Offline fallback;
- Online feature unavailable.

Network use should never be visually hidden when relevant to privacy.

Core local features remain usable without network wherever technically possible.

---

## 46. Android-specific behavior

Respect Android:
- window insets;
- system bars;
- keyboard/IME;
- lifecycle;
- accessibility;
- permission dialogs;
- background execution limits;
- foreground service rules;
- configuration changes;
- device-specific safe areas.

Do not depend on undocumented OEM behavior.

---

## 47. Back navigation

Back behavior hierarchy:
1. close active dialog/sheet;
2. close open global menu;
3. cancel transient interaction;
4. navigate to previous destination;
5. exit only according to platform conventions.

Do not accidentally exit during an active confirmation.

---

## 48. Background/foreground behavior

On background:
- pause nonessential animation;
- stop unnecessary work;
- preserve safe UI state;
- maintain only supported task execution;
- never continue prohibited work silently.

On foreground:
- refresh security state;
- refresh Emergency Stop state;
- refresh task status;
- avoid stale UI claims.

---

## 49. Lock-screen behavior

Only supported safe actions may appear while locked.

Sensitive controls are hidden/disabled according to policy.

Do not expose protected content through notifications or lock-screen UI.

---

## 50. Notifications

Notifications must be:
- minimal;
- user-controlled;
- privacy-aware.

Never include:
- OTPs;
- passwords;
- full sensitive message content;
- private task context unnecessarily.

Notification actions must still pass through the same security boundary.

---

## 51. UI State Machine

Core global states:

`BOOTING → READY → ACTIVE → WAITING_CONFIRMATION → EXECUTING → VERIFYING → COMPLETED`

Alternate states:
- BLOCKED;
- STOPPED;
- ERROR;
- OFFLINE;
- MODEL_UNAVAILABLE;
- AUTH_REQUIRED.

State transitions must be driven by actual system events, not UI assumptions.

---

## 52. Navigation State Machine

Global menu states:

`CLOSED → OPENING → OPEN → ROTATING/SCROLLING → SELECTING → NAVIGATING → CLOSED`

Drag path:

`IDLE → DRAG_PENDING → DRAGGING → POSITION_COMMITTED`

Fixed/free:

`FREE ↔ FIXED`

The navigation system must survive configuration/viewport changes without losing its persisted preference.

---

## 53. Failure & Recovery Matrix

| Failure | UI behavior | Security behavior |
|---|---|---|
| Model unavailable | Explain unavailable state | No provider bypass |
| Permission denied | Explain required capability | Deny |
| Confirmation expired | Ask again | Do not execute |
| Emergency Stop active | Show stopped state | Block execution |
| App target unavailable | Explain | Fail closed |
| Verification fails | Report not verified | Treat as failure/uncertainty |
| Network unavailable | Offline state | No hidden egress |
| Storage failure | Safe error | Fail closed where security state depends on it |
| Theme invalid | Fallback to safe theme | No security impact |
| Viewport too small | Reflow/linear menu | Preserve access |
| Accessibility failure | Maintain text alternatives | Never rely on motion |

---

## 54. UI Testing Requirements

Test:
- onboarding;
- navigation;
- conversation;
- task states;
- permissions;
- confirmation;
- security center;
- Emergency Stop;
- settings;
- themes;
- button styles;
- animation on/off;
- offline/unavailable states.

Global navigation must test:
- center;
- all four corners;
- all edge centers;
- intermediate positions;
- free/fixed;
- rotation;
- keyboard;
- touch;
- mouse;
- 3/5/6/8/9+ menu items;
- no clipping;
- no overlap;
- viewport changes;
- IME/keyboard appearance.

---

## 55. Accessibility Testing

Verify:
- TalkBack/screen reader;
- keyboard navigation where supported;
- focus order;
- focus visibility;
- dynamic font scaling;
- reduced motion;
- contrast;
- touch targets;
- semantic labels;
- Escape/back behavior.

Icon-only global navigation items must expose meaningful names.

---

## 56. Security UI Testing

Attempt:
- stale security state;
- stale Emergency Stop state;
- UI-only permission manipulation;
- direct navigation to protected screens;
- rapid repeated confirmations;
- expired confirmation;
- model-unavailable bypass;
- notification action bypass;
- background/foreground race;
- configuration-change race;
- external/untrusted content presented as trusted instruction.

The UI must never create a second authority path.

---

## 57. Device Testing

Representative validation should include:
- low-memory device;
- mid-range device;
- high-end device;
- small phone;
- large phone/foldable;
- tablet;
- desktop-sized window where applicable;
- portrait/landscape;
- hardware keyboard where applicable.

Physical-device results must be recorded as verified only when actually tested.

---

## 58. Design Do / Don't

### Do
- calm hierarchy;
- meaningful motion;
- clear security state;
- consistent components;
- lightweight visuals;
- user customization;
- responsive layout;
- honest unavailable states.

### Don't
- fake AI;
- fake progress;
- fake security;
- excessive neon;
- constant animation;
- giant asset packs;
- hidden network state;
- icon-only ambiguous controls;
- duplicate navigation;
- UI-side permission bypass.

---

## 59. Implementation Rules

1. Inspect current code before changing it.
2. Search the repository before creating a new UI component by responsibility.
3. Reuse existing security/admin UI and safe facades.
4. Do not replace PR #31's temporary shell with assumptions; rework it to this blueprint when implementation resumes.
5. Keep navigation, theme, settings and common components reusable.
6. Prefer platform/system APIs and vector assets.
7. Keep persisted UI preferences local.
8. Bound all user-controlled text and collections.
9. Avoid unbounded animation/recomposition.
10. Test actual rendered layouts.
11. Verify security state at lifecycle boundaries.
12. Never let UI state claim success before authoritative verification.
13. Update documentation and handoff state with meaningful UI milestones.
14. Do not merge UI work merely because it compiles; perform functional, accessibility, performance and security review.

### 59.1 User customization contract

The following are presentation-only preferences:
- theme;
- light/dark/system;
- button style;
- navigation appearance;
- animation;
- density;
- typography scale;
- certain layout preferences.

They must never change:
- policy;
- authorization;
- capability scope;
- Emergency Stop authority;
- financial hard-deny;
- sensitive-data boundaries.

### 59.2 Resource budget philosophy

UI must be designed to remain small:
- vector/system icons preferred;
- no large decorative video;
- no large image library solely for UI;
- lazy-load heavy optional features;
- release builds use appropriate shrinking/minification;
- avoid duplicate assets for themes when tinting/tokenization works;
- store only preferences required to reproduce UI state.

---

## 60. Blueprint Definition of Done

UI/UX work is complete only when:
- every implemented screen follows this blueprint;
- global navigation works across destinations;
- themes work;
- button style selection works;
- animation preference works;
- accessibility works;
- responsive behavior works;
- security boundary remains intact;
- no fake model/provider behavior exists;
- offline/unavailable states are truthful;
- performance/resource behavior is acceptable on representative hardware;
- tests pass;
- visual QA is performed on actual rendered screens;
- documentation/handoff is updated;
- no known critical UI/security defect remains.

---

## 61. Future Decisions / Open Questions

These are deliberately implementation-time decisions unless later locked by the user:
- exact final brand colors/hex values;
- exact button style catalog;
- exact theme count;
- exact typography family per platform;
- final global navigation icon;
- exact radial geometry constants;
- final animation durations;
- exact desktop navigation adaptations;
- model/device capability visualization depth;
- advanced personalization options.

Decision rule: choose the simplest design that preserves usability, accessibility, performance, privacy and security. Record significant decisions in the Change Log rather than relying on chat memory.

---

## 62. Change Log

### 2026-09-26 — Initial authoritative UI/UX blueprint
Locked:
- global adaptive floating navigation;
- user-positioned and draggable navigation;
- Free/Fixed navigation modes;
- local position persistence;
- radial + linear adaptive opening;
- circular overflow/rotation;
- light/dark/system modes;
- selectable color themes;
- selectable button presentation styles;
- user-controlled animation mode;
- dedicated UI customization Settings branch;
- resource-conscious visual design;
- accessibility-first interaction;
- security-facade-only UI boundary;
- truthful model/offline/unavailable states;
- cross-device adaptive behavior;
- performance/resource constraints.

This document intentionally gives the implementation team latitude to improve details while preserving the locked principles above.
