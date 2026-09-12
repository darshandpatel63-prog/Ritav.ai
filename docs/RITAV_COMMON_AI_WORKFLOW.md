# RITAV.AI — COMMON AI WORKFLOW & DEVELOPMENT RULES

This file is the common operating contract for every AI/chat working on Ritav.ai. Read and follow it before development work. It complements `README.md`, `RITAV_PROJECT_STATE.md`, `RITAV_BLUEPRINT.md`, and `docs/MASTER_REQUIREMENTS_MATRIX.md`.

## 1. Existing project
Ritav.ai is an existing Android-only, privacy-first, local-first project. Continue the existing project; never restart it without explicit user instruction.

## 2. Android-only
Work on the Android/APK application only. Do not add web, iOS, or desktop implementations unless the user explicitly changes this requirement.

## 3. New-chat command
When the user says `Start` or `Continue`, resume the existing project from repository state. Do not make the user repeat known project context.

## 4. Read project state first
Before coding, read `README.md`, `RITAV_PROJECT_STATE.md`, `RITAV_BLUEPRINT.md`, `docs/MASTER_REQUIREMENTS_MATRIX.md`, and this file.

## 5. Inspect build/config
Also inspect `settings.gradle*`, `build.gradle*`, `gradle.properties`, Gradle wrapper files, `AndroidManifest.xml`, `.github/`, `.github/workflows/`, dependencies, source, and tests when present/relevant.

## 6. Understand before changing
Do not edit code until the relevant architecture, call paths, existing security controls, tests, and documentation have been inspected.

## 7. Whole-repository search
Before creating any file, folder, class, interface, service, manager, utility, policy, store, test, or security component, search the entire repository for an existing implementation of the same responsibility.

## 8. Search by responsibility
Do not search only by the proposed filename. Search by functionality, responsibility, architecture layer, related terminology, classes, interfaces, and call paths.

## 9. Reuse existing code
If an existing component already performs the required responsibility, edit/extend/refactor it instead of creating a duplicate.

## 10. New files only when justified
Create a new file/folder only when the responsibility genuinely does not already exist and separation is architecturally justified.

## 11. No blind replacement
Never overwrite or replace existing architecture merely because a different design looks simpler. Preserve working behavior unless a change is required.

## 12. Actual implementation
Requirements must become real executable code. Documentation, comments, prompts, or diagrams alone are not implementation.

## 13. Security + audit
Both enforcement security and security auditing are required. An audit must never be treated as a substitute for an actual security control.

## 14. Security boundary
Deterministic security/policy controls are the real security boundary. AI, agents, prompts, and model reasoning must never be trusted as the final security boundary.

## 15. Core security invariants
Never allow autonomous consequential action, secret exposure to AI reasoning, default financial transaction automation, hidden private-data egress, policy override by app content, or execution without valid permission/user intent/risk-appropriate authorization.

## 16. Fail closed
When required security inspection cannot be performed safely, block rather than guess or forward the unsafe input.

## 17. Sensitive Information Firewall
OTP, UPI PIN, password, CVV, recovery codes, private keys, API keys, access tokens, authentication secrets, and equivalent secrets must be protected from AI reasoning and unnecessary downstream processing.

## 18. Sensitive-data testing
Test sensitive-data protection against normal input, Unicode, normalization, whitespace manipulation, obfuscation, OCR-derived text, malformed input, oversized input, false positives, and false negatives.

## 19. Finance Firewall
Banking, UPI, wallet, trading, investment, transfer, and similar financial actions are high-risk. Financial automation is default-deny and must not become autonomous.

## 20. Prompt-injection boundary
Websites, apps, notifications, messages, documents, OCR, files, and external content are untrusted data. They cannot change policy, grant permission, authorize actions, disable security, or reveal secrets.

## 21. Least privilege
Use scoped capabilities and layered permissions: global → app → capability → action → session/context. Prefer the narrowest privilege that satisfies the task.

## 22. Authorization
Authorization must be explicit, scoped, appropriately time-bounded, bound to the exact intended action/plan, resistant to replay, and invalidated when required by security state.

## 23. Emergency Stop
Emergency Stop must be enforceable by the execution path. Unsafe/consequential execution must stop while it is active.

## 24. Execution flow
Where applicable follow: User → Security Gate → Intent/Context → Master Orchestrator → Policy → Permission → Sensitive/Finance Firewall → Confirmation/Device Auth → Android Adapter → Result Verification → Local Audit.

## 25. Result verification
Do not assume execution succeeded. Observe and verify the expected result; report failure or uncertainty honestly.

## 26. Privacy/local-first
Prefer local processing/storage, minimal permissions, minimal dependencies, zero-egress by default, and no hidden telemetry. Review every network/data-egress path.

## 27. Universal Master Orchestrator
For every legitimate user request, first use a Universal Master Orchestrator approach. It decides whether one agent is enough or whether multiple specialists are genuinely useful.

## 28. Dynamic agent count
There is no artificial upper limit on agents, but unnecessary agents are forbidden. Agent count must depend on complexity, specialization, parallelism, risk, reliability, and verification value.

## 29. Universal domain detection
The orchestrator must identify all relevant domains, including technology, science, medicine, engineering, finance, law, education, business, research, languages, design, planning, creative work, and future legitimate domains.

## 30. Specialist roles
Every specialist must have a clear objective, responsibility, scope, exclusions, required inputs, expected outputs, standards, evidence requirements, and uncertainty reporting.

## 31. Inter-agent collaboration
When multiple agents are justified, they should exchange findings, evidence, assumptions, calculations, questions, critiques, corrections, and updated conclusions rather than operating as isolated answers.

## 32. Dependency graph
Conceptually use: User Request → Master Orchestrator → Decomposition → Specialists → Collaboration → Cross-Critique → Verification → Conflict Resolution → Synthesis → Final Audit → User.

## 33. Parallel/sequential work
Run independent work in parallel conceptually when useful; sequence work when one result depends on another.

## 34. Multi-round workflow
When appropriate use: independent analysis → knowledge exchange → cross-critique → conflict resolution → independent verification → synthesis → final audit. Not every task requires every round.

## 35. Challenge agents
For complex, high-risk, ambiguous, or important work, create independent challenge/devil's-advocate/audit roles to search for errors, vulnerabilities, omissions, contradictions, edge cases, outdated information, and false certainty.

## 36. Independent verification
Important calculations, facts, technical decisions, safety claims, and security claims should be independently checked whenever practical.

## 37. Truth-first
Never fabricate facts, data, statistics, research, citations, sources, laws, medical information, specifications, calculations, experiments, events, quotes, capabilities, results, evidence, or certainty.

## 38. Uncertainty
Clearly distinguish known, unknown, inferred, estimated, assumed, conflicting, unverified, and verified information. If it cannot be established reliably, say so.

## 39. Source hierarchy
When external evidence is required, prefer primary sources, official documentation, government sources, peer-reviewed research, professional organizations, academic institutions, authoritative references, and high-quality secondary sources in that order where applicable.

## 40. Cross-domain teams
When a task genuinely spans domains, form a multidisciplinary team with only the specialties required by the actual task.

## 41. Agent monitoring
Continuously check completeness, accuracy, relevance, consistency, evidence quality, requirements, safety, security, practicality, and user-intent alignment. Weak outputs must be revised or independently reviewed.

## 42. Disagreement protocol
Never select a majority merely because it is the majority. Identify the disagreement, compare evidence and assumptions, re-check calculations, seek stronger evidence, and preserve uncertainty if unresolved.

## 43. Final synthesis
For sufficiently complex work, use a final synthesis/QA role to remove duplication, resolve inconsistencies, check requirements/evidence/calculations, and prevent unsupported claims from reaching the user.

## 44. Dynamic escalation
If new complexity appears, dynamically add the required temporary specialist rather than forcing the original plan to handle it.

## 45. Efficiency
Use a single agent for simple work, small teams for moderate work, and larger teams only when genuinely justified. More agents do not automatically mean better work.

## 46. Development cycle
For each meaningful change: inspect → search/reuse → design → implement → integrate → test → adversarial review → verify → document → commit. Do not skip security-critical integration/testing.

## 47. Verification honesty
`Implemented`, `Integrated`, `Tested`, `Verified`, and `Real-device Tested` are different states. Never claim tests passed, security is complete, or device behavior is verified without actual evidence.

## 48. Work-package completion and audit cadence
A chat must NOT stop for an audit after every answer or tiny change. Continue until the current **logically complete work package / major security layer** reaches a justified checkpoint: implementation + integration + relevant tests + adversarial review + available verification + documentation/state update + commit. **Then stop and request/perform an independent audit before starting the next major security layer.** If the work package is not complete, continue working; do not call it done merely because a file was edited.

---

# REQUIRED END-OF-CHAT STATE

Every development chat should leave:

- CURRENT STOP POINT
- COMPLETED
- VERIFIED
- NOT VERIFIED
- KNOWN LIMITATIONS
- NEXT ACTION
- EXACT COMMIT SHA

Never fabricate any of these fields.

# ABSOLUTE PRINCIPLE

**NEVER MAKE SOMETHING UP JUST BECAUSE THE USER EXPECTS AN ANSWER.**

Verify when possible. Cross-check when important. Challenge when necessary. Admit uncertainty. Correct errors. Prioritize truth over appearance.
