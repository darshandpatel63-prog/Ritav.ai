# Ritav.ai — AI Model & Training Preparation Plan

**Status:** Planning only — no model selected, no dataset collected, no training executed, no model weights or runtime dependency added.  
**Last reviewed:** 2026-10-14  
**Working branch:** `ui/conversational-shell`  
**UI PR at review:** [#31](https://github.com/darshandpatel63-prog/Ritav.ai/pull/31), open/draft/unmerged.

This document supplements — and does not replace — `RITAV_BLUEPRINT.md`, `docs/RITAV_CROSS_PLATFORM_ARCHITECTURE.md`, `docs/RITAV_COMMON_AI_WORKFLOW.md` and `docs/RITAV_ELITE_SECURITY_ADDENDUM.md`. If this plan conflicts with the existing security boundary or phase order, the stricter existing requirement wins.

## 1. Decision for the current waiting period

Use the time before GitHub Actions storage/billing access is restored to prepare the AI strategy on paper. Do **not** try to simulate training by writing dummy model files, and do not run a training workflow on GitHub Actions.

The current repository has security/runtime contracts and a UI branch, but the production model/provider runtime is intentionally unavailable/fail-closed. This preparation is not evidence that Ritav already has a trained model.

Work permitted now:
- define target tasks and languages;
- define data provenance, consent, privacy and license gates;
- design a repeatable baseline benchmark and acceptance criteria;
- create a model/provider comparison scorecard;
- map the future training/evaluation pipeline and its security boundaries;
- document choices and unresolved questions.

Actual inference integration, dataset ingestion and any model training remain gated by the product Blueprint's development order and a separate review of the selected model, hardware and data.

## 2. Keep four different ideas separate

1. **Personalization / deterministic local memory.** Store user-approved aliases, preferences and routines in inspectable, editable, deletable local memory. This is not neural-model training and must not be represented as such.
2. **Retrieval/context.** Supply a small, authorized, relevant set of documents/examples at inference time. This is useful for changing knowledge without retraining the model; retrieval does not authorize actions.
3. **Fine-tuning a licensed base model.** Consider a small, parameter-efficient adaptation (for example, an adapter-based LoRA-style approach) only if measured evaluations show a real gap that cannot be addressed adequately by prompt/schema changes, retrieval, or deterministic code.
4. **Pretraining from scratch.** Treat this as a distant research option, not the first implementation. It needs a large, lawfully usable corpus, substantial compute, repeatable evaluation and sustained maintenance. No cost, quality or capability claim should be made before a measured feasibility study.

**Initial recommended strategy:** evaluate existing candidate models without changing their weights; keep the provider abstraction replaceable; use deterministic local personalization and retrieval where appropriate; only propose fine-tuning after a baseline, quality gap, lawful data supply and feasible compute path have been demonstrated. No provider or model is approved by this document.

## 3. Architecture and order of work

The canonical Blueprint already describes a `LocalModelProvider` abstraction and separates speech, language/reasoning, vision, embeddings and speaker-verification workloads. Do not bind Ritav directly to one model or duplicate an existing runtime responsibility.

Respect the current Blueprint order:
- close the current Phase 1 UI work and its required checks;
- proceed through Phase 2 Policy Engine and the intervening phases in order;
- design/integrate the Phase 4 Local AI runtime only when its gates are reached;
- address memory and training/personalization under Phase 6, without making training a prerequisite for a safe first model integration.

Planning and literature/architecture work can happen in parallel now. This does **not** authorize implementation out of sequence, a new provider, an online endpoint, a new permission or a merge/rebase of PR #31.

## 4. Requirements before any model choice

Build a model scorecard for each candidate using evidence collected at selection time:

- **Task fit:** Gujarati, Hindi and English support (including code-switching); instruction following; structured/JSON output reliability; summarization; intent/planning quality; calibrated uncertainty and safe refusal.
- **Runtime fit:** supported runtime and CPU/GPU/NPU architectures; RAM/VRAM and package footprint; quantization compatibility; startup latency, steady-state latency, battery/thermal impact and cancellation/resource limits.
- **Privacy/egress:** genuinely local inference behavior, network requirements, telemetry, crash/log behavior, model-download path and whether prompts or training samples can leave the device.
- **License and provenance:** exact license/version, model card, origin of weights, intended commercial use, modification/fine-tuning rights, redistribution of weights/adapters, attribution/notice obligations, usage restrictions and all relevant dataset licenses. “Open weights” does not by itself mean unrestricted use or redistribution.
- **Operational quality:** maintenance, reproducibility, known limitations, vulnerability/supply-chain review, quantized model integrity and upgrade/rollback approach.

Do not select a model based on its name, a benchmark headline or claimed parameter count alone. Record the evidence and unresolved risks before approval.

## 5. Dataset rules

Every future dataset must have a documented purpose and provenance manifest. Record the source, license/consent basis, creation date, language/task labels, transformation steps, filtering, deduplication and dataset hash/version.

Mandatory exclusions and controls:
- Never use OTPs, passwords, UPI PINs, CVVs, recovery codes, access tokens, private keys, biometric raw data or equivalent secrets as training examples.
- Do not automatically turn private Ritav conversations, device content, files, contacts, notifications, screenshots or audit logs into a training corpus.
- Do not upload real user data or prompts to a third-party trainer/provider without a separately designed, explicit consent and data-egress review. Local-first and zero-egress-by-default remain authoritative.
- Do not scrape or copy a corpus merely because it is publicly reachable. Verify dataset rights and permitted processing/use.
- Remove or safely transform personal data where it is not necessary; keep a deletion/correction process and preserve source attribution and licenses.
- Separate train, validation and held-out test examples by source/task where practical; deduplicate to reduce memorization and leakage. The held-out test set must not be used to tune the model.
- Treat synthetic examples as untrusted until reviewed for correctness, privacy, bias, duplication and license/provenance concerns.

Use a small, human-reviewed **evaluation set** before considering any training set. Keep evaluation examples and expected outcomes versioned so candidate models can be compared fairly.

## 6. Evaluation plan before training

Establish baseline scores on the unmodified candidate first. Define a fixed task suite and a review rubric before examining tuned-model results. Include:

- Gujarati/Hindi/English and mixed-language requests;
- intent extraction and schema-valid structured responses;
- factuality, unsupported claims, uncertainty and appropriate abstention;
- prompt injection in user messages, webpages and supplied documents;
- secret-seeking requests and sensitive-information leakage attempts;
- finance/UPI boundary requests, unauthorized external actions and Emergency Stop scenarios;
- malformed, very long and adversarial inputs;
- latency, memory, startup, cancellation, resource exhaustion and offline behavior;
- regression checks that model output never changes policy, grants permission, authorizes itself or calls a privileged adapter directly.

Measure the same held-out tasks before and after any adaptation. Do not proceed if quality improvements are not reproducible or if safety, privacy, language coverage, resource limits or regression performance deteriorate. Numerical acceptance thresholds must be agreed from baseline measurements, not invented in advance.

## 7. Future training/evaluation pipeline (design only)

1. Freeze task scope, exclusions, license rules and measurable acceptance criteria.
2. Benchmark legally compatible base candidates without training.
3. Approve dataset sources and prepare a reproducible, versioned corpus only if the measured gap justifies it.
4. Split data; audit provenance, secrets, privacy, duplication, quality and leakage.
5. Select the least-complex intervention that solves the measured gap: deterministic rules, prompt/schema, retrieval, then a narrowly scoped adapter/fine-tune if justified.
6. Run training in a separately approved, resource-bounded environment. Do not use GitHub Actions as a training cluster and do not commit large model weights, datasets or secrets to this repository.
7. Evaluate on held-out and adversarial tests; record exact model revision, tokenizer/config revision, data-manifest hashes, seed, training parameters, software versions, hardware class, metrics and known failures.
8. Export/quantize only after quality gates pass; compare the exported artifact to the evaluated model.
9. Add integrity/provenance checks, cancellation/resource controls and rollback before inference integration.
10. Integrate through the existing model-provider/security composition path, then run the required unit, integration, privacy, egress, resource-bound, prompt-injection and system-level regression tests.

A model response remains an untrusted proposal. The existing deterministic policy, authorization, sensitive-information/finance controls, confirmation requirements, Emergency Stop and approved adapter boundary remain authoritative.

## 8. Fifteen-session paper plan

These are work sessions, not a promise that a trained model will exist by the end.

1. Write concrete user tasks and non-goals; include Gujarati-first and code-switch cases.
2. Separate personalization, retrieval, inference and fine-tuning requirements.
3. Define what “Ritav's own AI” means for ownership, runtime control, model weights, branding and licensing.
4. Prepare the vendor/model-neutral candidate scorecard.
5. Define dataset categories and data classification.
6. Establish source, license, consent, privacy, deletion and provenance rules.
7. Draft a reviewed, non-sensitive evaluation-set specification.
8. Specify multilingual, structured-output and uncertainty tests.
9. Specify prompt-injection, secret-isolation, finance-boundary and Emergency Stop tests.
10. Define clean train/validation/test separation and leakage checks.
11. Map the model-provider interface to the existing Blueprint without adding implementation.
12. Set resource-profiling and graceful-degradation criteria for target device classes.
13. Complete a model, dataset and tooling license/supply-chain checklist.
14. Specify reproducibility, evaluation reports, artifact integrity and rollback.
15. Run a readiness review: either record “not ready” and the missing evidence, or make a separately reviewed recommendation. Training is not automatic at session 15.

## 9. Training-readiness gate

No actual training or production-model integration until all applicable items are evidenced:

- [ ] The planned capability and measurable gap are specific.
- [ ] Current phase order permits the implementation work.
- [ ] Candidate model/runtime works on the intended device class and its license/redistribution terms have been reviewed.
- [ ] Dataset source rights/consent, privacy, secret exclusion, provenance and deletion are established.
- [ ] Baseline and held-out evaluation suite exists with acceptance criteria.
- [ ] Resource bounds, cancellation and offline/failure behavior are designed.
- [ ] Deterministic security boundaries, egress rules and model-output handling are preserved.
- [ ] Supply-chain integrity, reproducibility, rollback and documentation are planned.
- [ ] The required independent/system-level security and model-evaluation review is complete before release.

## 10. Current truth / limitations

- No base model is chosen.
- No training corpus is collected or approved.
- No training or fine-tuning has been run.
- No performance, accuracy, cost or release-readiness results exist.
- The production provider/model runtime remains unavailable/fail-closed.
- This plan does not close UI Phase 1, implement the Policy Engine, grant a model execution authority, alter PR #31, or replace later testing and review.
