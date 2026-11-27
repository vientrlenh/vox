---
name: review-feature
description: Review a Vox feature, configuration or its tests against the owner's target. Asks two plain questions (what do you want to do, what is the actual target), maps all use cases onto one user journey (steps to the target, time per step, too long or not), then applies one fixed check catalogue to every step and entry point - speed (k6 latency, throughput), trust (reliability, data integrity), no surprises (predictable state, concurrency), safety (security, privacy, session continuity, OWASP dependency-check CVEs) - plus config lenses and an AGENTS.md audit, so reviews of the same context report the same findings. Use it to review, audit or check a feature, endpoint, use case, import, worker, scheduler, Kafka consumer, GraphQL resolver, application.yaml, build.gradle, Dockerfile, docker-compose, CI workflow or their tests, and for "ready to merge?", "fast enough?", "any CVEs?", "load test this" or "thread-safe?". Prefer it over code-review for a named feature or config. Not for verifying a plan (verify-plan).
---

# Review a feature or configuration

Purpose: tell the owner, with evidence, whether the feature gets its real user to the target the owner named, how
many steps and how much waiting that takes, and where it fails on speed, trust, predictability or safety. The yardstick
is the owner's target. The code is what gets judged, never the reference for what is right.

The intent-first flow, the configuration lenses and the "verify every finding" rule come from the owner's
`feature-config-review` plugin. This skill adds the journey, the fixed catalogue and the Vox specifics.

## Rules for the whole skill

- **Target first.** No review starts before the two questions in Step 1 are answered. "The code does X" is not a
  finding. A finding is a gap between what a catalogue check requires and what the feature was seen to do.
- **Observe, do not infer.** Exercise every behaviour that can be exercised locally (a k6 run, a probe request, a
  test) and cite the run. Read code to find entry points, to settle a check that cannot be run, and to explain a
  failure you saw. A check settled from code alone carries the evidence kind `Static`.
- **Same context, same findings.** Apply the whole catalogue in `references/checks.md` to every row it covers, with
  its own applicability rules, pass criteria and severities. There is no finding cap, no sampling, no "top N", no
  "the others are similar", no trimming of real findings to shorten the report, and no style opinions. A problem the
  catalogue does not cover goes under "Catalogue gaps", never under Findings.
- **Report only when complete.** Write the report only after the completeness gate in Step 8 passes.
- **Read-only.** Report findings and do not fix them. A fix comes after the report, when the owner asks, as a normal
  task under `AGENTS.md` (or through `implement-plan`). The only exception is the scratch material this skill creates
  and removes itself: the review folder, k6 scripts, fixtures, seed SQL and sample rows in the local database.
- **`AGENTS.md` is the only source of repo rules.** Read it in full at the start, every time. It changes during the
  refactoring stage, so never work from a remembered copy.
- **Ask before anything heavy or outward-facing**: the CVE scan (it downloads vulnerability data), starting
  containers or the app, seeding data, k6 runs, and restarting the app. Step 3 asks for all of it at once.
- **Local only.** Load tests, probes and seed data target only `localhost` or `host.docker.internal` and the
  docker-compose Postgres container (`vox-db-postgres`). Refuse a remote or shared host even when asked, and say why.
- **Never print secrets**, tokens, OTPs, passwords or student PII, in commands, logs, scripts or the report. When you
  search logs for them, print counts, never the matching lines.
- **Long reviews keep going.** Context is summarized automatically. The matrix lives in a file (Step 4), so a
  summary cannot lose it.

## Step 1: Ask the two questions

Before reading any code, ask the owner in chat, in plain words, and wait for the answer:

1. **What do you want to do?** The job, in the words of the person who uses the feature.
   Example: "As a school admin, I want to put the student list I already have in Excel into Vox."
2. **What is the actual target you are really aiming for?** The end state that means success, beyond the feature
   itself, and how that person sees it.
   Example: "On the first day every student can log in and is already in the right class, and I never type a name by
   hand."

- Ask both in one message, with no technical terms, and do not suggest answers taken from the code. The point is to
  learn the target before the code can bias it.
- If it is unclear which feature, configuration or diff is meant, ask that in the same message.
- If the request already answers both questions, restate the answers and ask the owner to confirm them.
- If the answers do not say who acts, where it starts, or how the actor knows the target is reached, ask one plain
  follow-up about the missing part only.
- If the owner mentions a time ("within a minute", "before the exam starts at 8:00") or a crowd ("a whole grade at
  once"), record it. It overrides the defaults in `references/journey.md`.
- For a configuration target, ask the same two questions about what the configuration must achieve. For a tests
  target, ask them about the feature the tests protect.
- Write the answers into the intent record (`references/journey.md`, section 1). Never go on with an assumed answer.

## Step 2: Map the journey

Follow `references/journey.md`:

1. Inventory every entry point of the feature, for all of its use cases at once (section 2), and read the inventory
   files plus one hop out.
2. Build the paths: P0 from the start state to the success signal, plus every alternate path the feature offers
   (section 3). Every step is an action or a wait, with its requests.
3. Put every entry point on a path, or on the off-path list with the reason.
4. Count the steps, and work out each wait's worst case from config (section 4).
5. Build the thread map when the target is multi-threaded (`references/concurrency.md`). Its rows become background
   path rows of the matrix.

## Step 3: Confirm the journey and approve the runs, in one question

Ask one `AskUserQuestion` with up to four questions. Each one must stand alone, with the recommendation first:

1. **Journey.** "Is this the journey from <start> to <target>?" Put the compact journey table (paths, steps,
   entry points, budgets, worst-case waits) in the `preview` of the first option, because the owner may see only the
   dialog. Options: "Matches (Recommended)" and "Needs changes".
2. **CVE scan.** Run `dependencyCheckAnalyze` now? Say that it downloads NVD and CISA data and takes 5 to 20 minutes
   on the first run (much longer without an API key), and whether `NVD_API_KEY` is set. Check that with
   `[bool]$env:NVD_API_KEY` and never print the value.
3. **Runs.** Start the missing containers and the app, seed the sample data (two schools, every role), and run the k6
   scenarios? State the scenarios, the expected concurrency and reference input (the owner's or the defaults), the
   short-token app restart for SAF8 when it applies, the data that is created and removed, and every side effect on
   the path with how it is neutralized (`references/performance-k6.md`, section B).
4. **Restart probe**, only when the journey has a wait: kill the app while a background step is in progress, start it
   again, and watch where the work ends (TRU5). It is a separate question because it kills the app mid-work.

If the journey needs changes, fix it and ask again. A declined run leaves its cells UNVERIFIED (reason "declined")
unless a `Static` method settles them. Never run anything heavy that was not approved.

## Step 4: Build the matrix

1. Pick a run id, `review-<yyyyMMddHHmmss>`, and create the review folder `<scratchpad>/review/<run id>/`. The
   matrix, the k6 script, fixtures, SQL and outputs all go there. Nothing goes into the repository.
2. Write `matrix.md` in the format at the end of `references/checks.md`: one block per row (the review, each path,
   each step, each entry point, each background path, each config key, each test class, each `AGENTS.md` rule), and
   in each block one line per check whose "Applies to" rule covers the row, with status `TODO`. Write the expected cell
   count at the top.
3. Create one task per row group (journey, each path, entry points, background paths, config, tests, `AGENTS.md`
   audit, CVEs), so the task list mirrors the matrix.

## Step 5: Collect the evidence

Do the static work while the long runs go. The order:

1. Start the CVE scan in the background when approved (`references/security.md`, part C).
2. The build gate: GATE1 to GATE3. When a test fails, run it on the default branch in a temporary worktree under the
   review folder (`git worktree add`, removed afterwards) to tell a pre-existing failure from one the target causes.
3. Containers, the app and the sample data (`references/performance-k6.md`, sections C and D).
4. k6, in this order, each run from the same script (`references/performance-k6.md`, section E): `journeyWalk`,
   `probes`, `statementCount` at 1x and 2x volume, the load scenarios, `racingRequests` when NSP2 applies, and
   `sessionContinuity` when SAF8 applies. Then the restart probe if it was approved.
5. `EXPLAIN (ANALYZE, BUFFERS)` of the main queries at the reference volume (SPD6).
6. The log privacy count (SAF6), after the last run.
7. Cleanup of the sample data and the zero-rows check, whatever the k6 results were.
8. The `Static` cells: thread map (`references/concurrency.md`), configuration chain (CFG), test mapping (TRU7, TST)
   and the `AGENTS.md` audit (AGT).
9. Triage of the CVEs when the scan ends (`references/security.md`, part C).

## Step 6: Settle every cell

For each cell, record the status, the evidence and, for a FAIL, the failure scenario. Update `matrix.md` as you go.

- The status comes from the check's pass criterion and nothing else.
- PASS needs evidence too.
- N/A only by the check's "Applies to" rule; quote the rule.
- UNVERIFIED only when the run was declined or cannot be done locally, and no `Static` method settles the cell. Write
  exactly what would settle it.
- Before you write a FAIL, look for an existing guard, validation or test that already handles the case. A FAIL
  needs the behaviour, not only the absence of the code you expected.

## Step 7: Turn the FAIL cells into findings

- **One finding per check and root cause.** The root cause is the `file:line` or config key whose change fixes it.
  Several cells that fail the same check for the same root cause form one finding that lists every cell. When the
  root cause is unknown (seen only in a run), make one finding per check and step.
- **Severity** comes from the catalogue row, with its escalation conditions. CVEs use the rule in
  `references/security.md`, part C.
- **Numbering** is fixed: by severity (Critical first), then by check id in catalogue order, then by step order.
  F1 is always the most severe.
- Each finding has: id, severity, check, cells, where, problem, failure scenario, evidence (`Run` or `Static`) and
  the smallest suggested fix. A fix that falls under "Ask first" in `AGENTS.md` is proposed, never applied.

## Step 8: Completeness gate

Write the report only when all of these hold. Otherwise go back and finish:

1. Every cell in `matrix.md` is PASS, FAIL, N/A or UNVERIFIED. No `TODO` is left, and the settled count equals the
   expected count.
2. Every entry point from the inventory is on a path or on the off-path list.
3. Every thread-map row reaches all of its end states (`references/concurrency.md`).
4. Every `AGENTS.md` rule row is settled.
5. Every FAIL cell belongs to exactly one finding, and every finding cites at least one FAIL cell.
6. The zero-rows check after cleanup returned 0, or the report lists exactly what remains and why.
7. **Second pass.** Walk the catalogue from top to bottom once more, row kind by row kind, and derive again which
   checks apply to each row. Any check that applies but has no cell is added and settled before you go on.

## Step 9: Report

Use this shape. Keep it factual, and drop a section only when it cannot apply (say which in Scope).

```
## Review: <target> -> <READY | READY WITH FIXES | NOT READY>
<one line: the most severe findings, and any Critical or High catalogue gap>

### Intent
- Wants to: <answer 1, the owner's words>
- Target: <answer 2>; success signal: <...>
- Actor: <roles>; time expectation: <the owner's | default budgets>; expected concurrency: <the owner's | defaults>

### Journey
| Step | Kind | Work in plain words | Requests | Time to the next step (p50 / p95, or max for a wait) | Budget | Too long? |
- Steps to the target: <n> actions, <m> waits, <r> requests on P0
- System time to target: p50 <x>, p95 <y>; worst case from config <z> (human time not included)
- Alternate paths: <path: +k actions, +j waits, +t time>
- Wasted effort: <steps done before a refusal that could have come earlier, or "none">

### Pillars
| Pillar | Cells | PASS | FAIL | N/A | UNVERIFIED |
| Journey | | | | | |
| Speed | | | | | |
| Trust | | | | | |
| No surprises | | | | | |
| Safety | | | | | |
| Configuration | | | | | |
| Build gate, tests and AGENTS.md | | | | | |

### Findings
| # | Severity | Check | Cells | Where | Problem | Failure scenario | Evidence | Suggested fix |

### Library CVEs
| Library:version | CVE | CVSS | KEV | Classpath | Real / false positive | Fixed in | Proposed fix |
- Scan: <date, NVD data age, analyzers off> or "not run: <reason>"

### Performance
| Scenario | Profile | Requests | RPS | p50 | p95 | p99 | Errors | Thresholds |
- Statements per request (1x / 2x volume): <per action>
- Sample data removed: <yes, 0 rows left | no: what remains>
- Caveat: one local instance on a laptop, with SQL logging on; compare runs, do not read it as production capacity

### Thread map (multi-threaded only)
| Path | Starts at | Hand-offs | Shared state | Ends at (success / failure) | Cells |

### Checks run
- `./gradlew compileJava compileTestJava` -> pass

### Unverified
| Cell | Why | What would settle it |

### Questions for the owner
- <only what the owner can answer, such as a choice between two readings of the target>

### Catalogue gaps
- <problem with evidence, and the proposed row for references/checks.md>

### Outside the target
- <problems noticed in code that is not part of the target, not fixed>

### Matrix
- <full path of matrix.md>; <expected cells> cells, all settled
```

Verdict, decided only by these rules:

- **NOT READY**: any Critical or High FAIL.
- **READY WITH FIXES**: the FAILs are Medium or Low only, or an UNVERIFIED cell sits on a check that is Critical or
  High on failure (list how to settle each one).
- **READY**: no FAIL above Low, and no UNVERIFIED cell on a check that is Critical or High on failure.

After the report, offer in one line to publish it as a page for sharing. If there are catalogue gaps, offer to add
the proposed rows to `references/checks.md`, which changes the skill and needs the owner's approval.

## Pitfalls

- **Reviewing the code instead of the journey.** A clean class that leaves the actor three steps short of the target
  is a failed review of the feature.
- **Stopping at the use case the owner named.** The target decides where the journey ends (see the example in
  `references/journey.md`).
- **Assuming defaults.** Look up the real default for the version in use (Spring Boot 4.1, Spring Framework 7,
  Hibernate, Kafka client), and check units and precedence in config.
- **Vague findings.** "Might be slow" is not a finding. Name the cell, the measured value, the budget and the
  consequence for the actor.
- **Fixing instead of reviewing.** Suggest the fix and leave the decision to the owner.
