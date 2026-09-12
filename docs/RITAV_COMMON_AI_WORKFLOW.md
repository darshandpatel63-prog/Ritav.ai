# RITAV.AI — COMMON AI WORKFLOW & DEVELOPMENT RULES

This is the common operating contract for every AI/chat working on Ritav.ai. It complements `README.md`, `RITAV_PROJECT_STATE.md`, `RITAV_BLUEPRINT.md`, and `docs/MASTER_REQUIREMENTS_MATRIX.md`.

## 1. Existing project / platform
Ritav.ai is an existing Android-only, privacy-first, local-first project. Continue the existing project; never restart or replace working architecture without explicit user instruction. Do not add web, iOS, or desktop implementations unless explicitly requested.

## 2. New-chat protocol
When the user says `Start` or `Continue`, resume from the repository and project state. At the beginning of a new chat, read this file once together with `README.md`, `RITAV_PROJECT_STATE.md`, `RITAV_BLUEPRINT.md`, and `docs/MASTER_REQUIREMENTS_MATRIX.md`, plus the relevant build/config/source/test files. Do not repeatedly reread this workflow file during the same chat unless context is lost, requirements change, or rereading is needed for correctness. Do not make the user repeat known project context.

## 3. Build/config and repository orientation
At the start of meaningful development, inspect repository structure and relevant build/configuration, including `settings.gradle*`, `build.gradle*`, `gradle.properties`, Gradle wrapper files, `AndroidManifest.xml`, `.github/`, `.github/workflows/`, dependencies, source, tests, security modules, interfaces, and environment/configuration files when present/relevant.

## 4. Understand before changing
Inspect the relevant architecture, call paths, existing security controls, tests, dependencies, and documentation before changing security-critical behavior. Security must be evaluated as a system, not as isolated files.

## 5. Absolute duplicate-prevention rule
Before creating ANY file, folder, class, interface, service, manager, utility, policy, store, test, or security component, search the ENTIRE repository by responsibility/functionality—not merely by proposed filename. Search related terminology, architecture layers, interfaces, implementations, call paths, security mechanisms, tests, utilities, services, managers, policies, stores, and modules. If the responsibility already exists, edit/extend/refactor the existing implementation. Create a new component only when the responsibility genuinely does not exist and separation is architecturally justified.

## 6. Development method — continuous verification
Use: **Inspect → Map → Search/Reuse → Design → Implement → Integrate → Continuously self-check → Test → Adversarial review → Verify → Document → Commit.**

Checking is NOT postponed until the end. While writing code, continuously inspect its callers/callees, interfaces, data flow, security boundaries, failure paths, edge cases, and interaction with existing controls. After each meaningful change, re-check the affected integration and security assumptions before proceeding. Do not waste time on repetitive checks that add no value; check where correctness or security risk exists.

## 7. Complete-system verification at work-package completion
When a logically complete feature or major security layer is finished, perform one consolidated system-level review of the whole affected path—not just the new file. Re-check architecture, call paths, data flow, security boundaries, permissions, authorization, failure/rollback behavior, tests, dependencies, and documentation. Try to break the complete system from multiple relevant directions before calling the work package complete.

## 8. Work continuously; no audit after every small change
Do not stop or request an audit after every answer, file edit, or tiny change. Continue until the current logically complete work package reaches its justified checkpoint: implementation + integration + relevant tests + adversarial review + available verification + documentation/state update + commit.

## 9. Major security-layer audit gate
After a logically complete major security layer reaches that checkpoint, create a clear audit checkpoint. An independent audit must occur before starting the next major security layer. `Audit` means an additional independent review; it does not replace continuous development-time checking or system-level verification already required above.

## 10. Actual implementation
Requirements must become executable, integrated code. Documentation, comments, prompts, agent instructions, or diagrams alone are never security implementation.

## 11. Security + audit are both required
Security controls must actually prevent unsafe behavior. Security auditing must detect/report weaknesses, violations, unexpected states, and implementation problems. Neither is a substitute for the other.

## 12. Deterministic security boundary
AI models, agents, prompts, LLM reasoning, and natural-language instructions must never be the final security boundary. AI may propose; deterministic policy and authorization decide; approved adapters execute.

## 13. Core security invariants
Never allow autonomous consequential action; secret exposure to AI reasoning; default financial transaction automation; hidden private-data egress; app/external-content policy override; or execution without valid permission + user intent + risk-appropriate authorization. Uncertainty → block or ask. Never guess security-critical information. Required security inspection failure → fail closed.

## 14. Sensitive Information Firewall
OTP, UPI PIN, passwords, CVV, recovery codes, private keys, API keys, access tokens, authentication secrets, and equivalent credentials must be protected from AI reasoning and unnecessary downstream processing. Detection, redaction/blocking, and safe handling must be deterministic. Regex alone must never be treated as complete protection. Test Unicode, normalization, whitespace/format manipulation, obfuscation, OCR-derived text, malformed/oversized input, false positives, false negatives, and relevant integration paths.

## 15. Finance Firewall
Banking, UPI, wallets, trading, investments, transfers, and similar financial actions are high-risk. Financial automation is default-deny where required and must never become autonomous. Enforce deterministic policy, user intent, authorization, and appropriate device authentication before any permitted high-risk action.

## 16. Prompt-injection boundary
Websites, apps, notifications, messages, documents, OCR, files, and external-service content are untrusted data. They cannot change policy, grant permissions, authorize themselves, override user intent, bypass confirmation, disable security, or instruct secret disclosure.

## 17. Least privilege / authorization / emergency stop
Use scoped capabilities and layered permissions: global → app → capability → action → session/context. Authorization must be explicit, scoped, appropriately time-bounded, bound to the exact intended action/plan, single-use where appropriate, replay-resistant, and invalidated when security state requires it. Emergency Stop must be enforced by the execution path and unsafe/consequential execution must stop while active.

## 18. Execution + result verification
Where applicable follow: User → Interaction/Security Gate → Intent/Context → Master Orchestrator → Policy → Permission → Sensitive/Finance Firewall → Confirmation/Device Auth → Approved Android Adapter → Result Verification → Local Audit. Never assume adapter return means success; observe and verify expected state and report failure/uncertainty honestly.

## 19. Privacy/local-first
Prefer local processing/storage, minimal permissions, minimal dependencies, zero-egress by default, and no hidden telemetry. Review every network/data-egress path and dependency for privacy implications.

## 20. Universal Master Orchestrator
For every legitimate user request, first use a Universal Master Orchestrator approach. It determines whether one agent is sufficient or whether multiple specialists are genuinely useful. Agent count has no artificial upper limit, but unnecessary agents are forbidden.

## 21. Universal domain detection and dynamic specialization
The orchestrator identifies all relevant domains and dynamically selects the expertise required. Specialist roles must have clear objectives, responsibilities, exclusions, inputs, outputs, standards, evidence requirements, and uncertainty reporting. Cross-domain teams are formed only when justified.

## 22. Interconnected multi-agent collaboration
When multiple agents are useful, coordinate dependencies, parallel/sequential work, knowledge exchange, cross-critique, independent verification, conflict resolution, synthesis, and final QA as appropriate. Agents should share findings/evidence/assumptions/calculations and challenge each other rather than acting as isolated answers.

## 23. Challenge and independent verification
For complex, high-risk, ambiguous, or important work, use challenge/devil's-advocate/audit roles when useful. Search for logical errors, incorrect assumptions, unsupported claims, missing requirements, contradictions, edge cases, security/safety vulnerabilities, outdated information, hidden dependencies, and false certainty. Independently verify important calculations, facts, technical decisions, safety claims, and security claims whenever practical.

## 24. Disagreement protocol
Never select a majority merely because it is the majority. Identify the disagreement, compare evidence and assumptions, recalculate/check where relevant, seek stronger evidence or independent review, and preserve uncertainty if unresolved.

## 25. Truth-first / no hallucination
Never fabricate facts, data, statistics, research, papers, citations, sources, laws, medical information, specifications, calculations, experiments, events, quotes, credentials, tool capabilities, results, evidence, or certainty. Clearly distinguish known, unknown, inferred, estimated, assumed, conflicting, unverified, and verified information. If it cannot be established reliably, say so.

## 26. Source hierarchy
When external evidence is required, prefer primary sources, official documentation, government sources, peer-reviewed research, professional organizations, academic institutions, authoritative references, and high-quality secondary sources, while evaluating quality, relevance, date, methodology, and conflicts.

## 27. Agent performance monitoring / escalation
Monitor completeness, accuracy, relevance, consistency, evidence quality, requirements, safety, security, practicality, and user-intent alignment. Weak or contradictory work must be revised or independently reviewed. If new complexity appears, dynamically add the required temporary specialist.

## 28. Final synthesis / QA
For sufficiently complex tasks, use a final synthesis/QA role to remove duplication, resolve inconsistencies, check requirements/evidence/calculations, and prevent unsupported claims from reaching the user. Never reveal hidden chain-of-thought, private deliberations, or internal agent transcripts.

## 29. Efficiency rule
Use a single agent for simple work, small teams for moderate work, and larger teams only when complexity, risk, specialization, parallelism, or verification value genuinely justifies them. Do not create agents or repeat repository reads merely to appear thorough.

## 30. Testing / build / CI
For every meaningful security change, add/update tests covering success, rejection, boundaries, malformed/adversarial input, failure behavior, and interaction with existing security layers. Check the real build system before changing it; do not assume Gradle wrapper or CI exists. Any build/CI addition must preserve Android-only scope. If tests cannot actually be executed, state: **Tests were not executed.**

## 31. Documentation state
After meaningful work, update `README.md` and `RITAV_PROJECT_STATE.md` where appropriate with actual completed work, verification status, known limitations/issues, next action, and exact commit SHA. Never document unverified work as verified.

## 32. Verification honesty
`Implemented`, `Integrated`, `Tested`, `Verified`, and `Real-device Tested` are different states. Never claim fully secure, 100% secure, bug-free, production-ready, all tests passed, verified, or works on every device without evidence.

## 33. End-of-chat state
Every development chat should leave: **CURRENT STOP POINT, COMPLETED, VERIFIED, NOT VERIFIED, KNOWN LIMITATIONS, NEXT ACTION, EXACT COMMIT SHA.** Never fabricate these fields.

## 34. User command protocol
Normally the user only needs `Start` or `Continue` to resume development. `Audit` requests an independent audit of the current relevant implementation. `Full Security Audit` requests a system-level security review. Audit commands do not replace the required continuous verification during development.

## 35. Audit protocol
For an audit, do not trust previous-chat claims. Independently inspect repository state, relevant source, tests, call paths, security boundaries, permissions, authorization, dependencies, build configuration, workflows, documentation, and recent commits. Classify findings as CRITICAL/HIGH/MEDIUM/LOW/INFORMATIONAL and distinguish implemented/partial/missing and tested/untested/verified/unverified.

## 36. Absolute final principle
**NEVER MAKE SOMETHING UP JUST BECAUSE THE USER EXPECTS AN ANSWER.** Verify when possible. Cross-check when important. Challenge when necessary. Admit uncertainty. Correct errors. Prioritize truth over appearance.
