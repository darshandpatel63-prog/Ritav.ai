# Ritav.ai — Elite Security Addendum

This addendum strengthens the existing Ritav.ai security workflow. It is additive only: it does **not** weaken, replace, or override any existing Ritav.ai security invariant, firewall, authorization rule, audit gate, Android-only scope, or verification requirement.

## 1. Maximum-assurance security posture
For authentication, personal data, AI/data processing, device access, or money-adjacent functionality, apply the strongest practical security posture by default. Security is risk reduction, not a promise of perfect safety.

## 2. Secure-by-construction
Security must be designed into implementation, not postponed to review:
- Prefer strongly typed and memory-safe mechanisms already supported by the Android/Kotlin stack.
- Never execute dynamic code derived from untrusted input (`eval`, dynamic code generation, or equivalent).
- Never construct queries or commands by concatenating untrusted input; use parameterized APIs or safe typed interfaces.
- Validate untrusted input at every trust boundary; client-side validation is never the sole control.
- Encode/sanitize untrusted content before rendering or interpreting it.
- Fail closed on validation, authorization, security-inspection, parsing, or unexpected-error paths.
- New resources/capabilities are deny-by-default until explicitly authorized.

## 3. Security review lenses
For non-trivial security work, dynamically use only the specialist lenses that add real value. At minimum consider:
- Security Engineer — auth, authorization, secrets, input validation, storage, data exposure.
- QA/Adversarial Reviewer — edge cases, malformed input, race conditions, integration failures.
- Red-Team Reviewer — actively attempts to defeat the actual defense.
- Permissions/Privacy Reviewer — least privilege, Android permissions, local-first data handling.
- Platform/Integration Reviewer — Android lifecycle, adapters, IPC/accessibility boundaries, callers/callees.
- Performance/Resource Reviewer — oversized input, unbounded work, memory/CPU denial-of-service risks.

A Guardian/final-review lens must check that every required lens actually examined its responsibility, that findings do not contradict each other, and that nothing was marked complete without evidence. The Guardian can veto completion.

## 4. Minimum adversarial attack set
For each exposed security boundary, deliberately attempt applicable variants of:
- malformed, truncated, null-like, invalid-Unicode, and oversized input;
- normalization, whitespace, zero-width/format-character, encoding, case, punctuation, and script-mixing bypasses;
- injection payloads;
- missing, expired, replayed, wrong-scope, wrong-plan, wrong-role, and insufficient authorization;
- IDOR/resource-substitution attempts where identifiers exist;
- secret exposure through logs, errors, audit records, exceptions, UI, model context, or downstream adapters;
- XSS/content-injection equivalents where rendered markup or rich content exists;
- rapid-repeat/rate-limit bypasses where networked or costly endpoints exist;
- race conditions around token consumption, emergency stop, session expiry, confirmation, and asynchronous authentication;
- dependency/configuration weaknesses and unexpected failure paths.

If an attack succeeds, fix it and repeat the same attack. Do not knowingly ship a demonstrated bypass as a future improvement.

## 5. Evidence-based red-team rule
Where executable testing is available, write and actually run the attack/regression test. Where execution is unavailable, trace the complete path manually and label the result unverified. Never convert a manual trace into a claim that a test passed.

## 6. OWASP-style coverage, adapted to Android
For relevant functionality, explicitly consider the major application-security classes: injection, broken authentication, broken access control, sensitive-data exposure, insecure deserialization/parsing, security misconfiguration, client/UI content injection, vulnerable dependencies, insufficient logging/monitoring, and denial-of-service/resource exhaustion. Browser-only controls such as CSP or CSRF are not blindly added to an Android-native path; instead apply the platform-appropriate equivalent where the threat exists.

## 7. Secrets and sensitive data
- Never hardcode API keys, tokens, passwords, private keys, recovery codes, OTPs, UPI PINs, CVVs, or equivalent credentials.
- Never place secrets in logs, analytics, crash reports, audit records, exceptions, screenshots, model prompts, model context, or adapter payloads unless the architecture explicitly requires a protected secret-handling boundary.
- Sensitive-data minimization applies before, during, and after model/context processing.
- Regex detection is defense-in-depth, never the sole guarantee of secret discovery.
- Any new AI/context ingestion path must pass through the deterministic sensitive-data boundary before model exposure.

## 8. Least privilege and permissions
For every Android permission or privileged capability:
- request only what the actual feature requires;
- never add permissions speculatively for future usefulness;
- map each permission to its concrete feature and trust boundary;
- minimize capability scope and lifetime;
- require explicit user consent where the platform/feature requires it;
- treat personal-data permissions as a privacy and disclosure boundary, not merely a manifest entry;
- default to no permission when necessity is uncertain.

## 9. Dependency and configuration hygiene
Before adding or upgrading a dependency, verify that it is necessary, maintained, compatible with the existing Android/Kotlin stack, and does not introduce avoidable attack surface. Review known vulnerabilities when tooling/network access permits. Keep configuration fail-closed and never commit secrets. Security tooling or CI additions must remain within the Android-only project scope.

## 10. Logging and audit safety
Security logs must help detect failures and abuse without becoming a secret-exfiltration channel. Never log raw credentials or full sensitive payloads. Prefer safe event types, identifiers that are non-secret, reason codes, and minimal metadata. Audit failure must not silently convert a denied operation into an allowed operation.

## 11. Network and cost abuse
Any future networked or paid AI capability must have explicit egress policy, destination control, bounded requests, timeout/resource limits, and abuse/rate controls appropriate to the endpoint. Never assume a private endpoint is safe merely because it is not publicly advertised. Private user data must not be sent to an external destination unless an explicit policy and authorization path permits it.

## 12. Resource-exhaustion resistance
Security inspection itself must be bounded. Apply explicit limits to input size, parsing depth, recursion, collection growth, expensive transformations, retries, and external waits where applicable. A security transformation that cannot safely complete within its bounds must fail closed.

## 13. Asynchronous security state
Security decisions involving asynchronous authentication, confirmation, session expiry, emergency stop, or token issuance must use the time/state at the security event itself, not an earlier caller timestamp when that could extend or otherwise invalidate the intended security window. Re-check security state at the point of consequential execution.

## 14. Completion gate
A security layer is not complete merely because code exists. Completion requires, as applicable:
1. implementation;
2. integration into the real call path;
3. regression and adversarial tests;
4. execution of those tests when an executable environment exists;
5. consolidated system-level review;
6. documentation/state update;
7. independent audit checkpoint before the next major security layer.

If executable tests were not run, the layer remains unverified regardless of how convincing manual reasoning is.

## 15. No weakening rule
When this addendum conflicts with an existing Ritav.ai requirement, choose the stricter security behavior unless the conflict is with an explicit product/platform constraint; then document the conflict and preserve the strongest deterministic boundary possible. Never remove an existing control merely to satisfy this addendum.
