---
name: review-technical
description: Review the engineering behind a Vox feature, configuration or change, as if questioning the engineer who built it. Asks for the context with AskUserQuestion in this chat, finds every technical decision and its reason in git and docs, asks the engineer "why" (AskUserQuestion, same chat) for each one git does not explain, and tests each reason with deeper reasoning (premises against evidence, counter-scenarios, alternatives). Applies one fixed catalogue so reviews of the same context report the same findings - SQL per request, indexes, transactions, pool throughput, side effects after commit, the thread map and concurrency mechanisms, constraints, migration locks, hostile input, logs, error internals, rate limits, money, internal callers, tokens and OTPs, OWASP dependency-check CVEs, configuration lenses, the build gate, test quality and an AGENTS.md audit. Use it for the internals of a feature, use case, worker, scheduler, Kafka consumer, GraphQL resolver, migration, application.yaml, build.gradle, Dockerfile, docker-compose, CI workflow or tests, and for "why is it built like this?", "thread-safe?", "any CVEs?", "is this config right?" or "ready to merge?". Prefer it over code-review for a named feature or config. Not for the user's side (review-feature) or a plan (verify-plan).
---

# Review the engineering of a feature or configuration

Purpose: tell the engineer, with evidence, whether the way the feature or configuration is built holds up. That means
whether each technical decision has a reason, whether the reason is true, and whether the decision survives the worst
realistic case, plus every catalogue check of the internals, the configuration and the libraries. Review it as if
questioning the engineer who built it, so most questions are "why". The yardstick is the engineer's stated context and
the catalogue. The code is what gets judged, and a reason counts only when the evidence supports it.

The intent-first flow, the configuration lenses and the "verify every finding" rule come from the owner's
`feature-config-review` plugin. The user's side of a feature (the journey, step times, what the user sees) belongs to
`review-feature`. This skill shares that skill's entry point inventory and paths
(`.claude/skills/review-feature/references/journey.md`), its runs
(`.claude/skills/review-feature/references/performance-k6.md` and `scripts/k6-feature-template.js`) and its check id
space.

## Rules for the whole skill

- **Context first.** No review starts before the questions in Step 1 are answered.
- **Why before verdict.** A decision is judged only after its reason is known, from git and docs first and from the
  engineer second. Never judge a reason you guessed, and never accept one the evidence contradicts.
- **Every question goes through `AskUserQuestion`, in this chat.** Never ask in plain text, never leave a question for
  the report, and never go on with an assumed answer. One call holds up to four questions, each with two to four
  options (labels of one to five words, a header of at most 12 characters). The engineer can always type their own
  answer under "Other", so never add an "Other" option. Make each question stand alone, because the engineer may see
  only the dialog: name the decision, its `file:line` and the evidence that makes it matter. Technical words are fine.
  Mark an option "(Recommended)" only when there is a real recommendation. A "why" question never has one, because the
  reason is a fact only the engineer knows. Use the `preview` of an option for code or a table (single-select
  questions only). Ask what belongs together in one call. Write every answer, verbatim, into the matrix or the
  reasoning record before going on.
- **Git is the first source of reasons.** `AGENTS.md` ("Memory and sources of truth") makes git the source of project
  context: `git log`, `git log -L`, `git blame`, merge commits, and the plans under `docs/`. Ask the engineer only what
  those do not answer. Never read local agent memory.
- **Observe, do not infer.** Try every behaviour that can be tried locally (a k6 run, a probe, a test, `EXPLAIN`) and
  cite the run. A check settled from code alone carries the evidence kind `Static`.
- **Same context, same findings.** Apply the whole catalogue in `references/checks.md` to every row it covers, and
  register every decision of the kinds in `references/decisions.md`, section 1. There is no finding cap, no sampling,
  no "top N", no "the others are similar", no trimming of real findings to shorten the report, and no style opinions.
  A problem the catalogue does not cover goes under "Catalogue gaps", never under Findings. The context is the code,
  the config, git, and the answers given in this chat.
- **Deeper reasoning, written down.** Every decision gets a reasoning record (`references/decisions.md`, sections 4
  and 5) and every finding a causal chain and an escape analysis (Step 8). "Looks fine" and "probably" are not
  reasoning.
- **Report only when complete.** Write the report only after the completeness gate in Step 9 passes.
- **Read-only.** Report findings and do not fix them. A fix comes after the report, when the engineer asks, as a
  normal task under `AGENTS.md` (or through `implement-plan`). The only exception is the scratch material this skill
  creates and removes itself: the review folder, k6 scripts, fixtures, seed SQL and sample rows in the local database.
- **`AGENTS.md` is the only source of repo rules.** Read it in full at the start, every time. It changes during the
  refactoring stage, so never work from a remembered copy.
- **Ask before anything heavy or outward-facing**: the CVE scan (it downloads vulnerability data), starting containers
  or the app, seeding data, k6 runs, and reading pull requests on GitHub. Step 3 asks for all of it at once.
- **Local only.** Load tests, probes and seed data target only `localhost` or `host.docker.internal` and the
  docker-compose Postgres container (`vox-db-postgres`). Refuse a remote or shared host even when asked, and say why.
- **Never print secrets**, tokens, OTPs, passwords or student PII, in commands, logs, scripts or the report. When you
  search logs for them, print counts, never the matching lines.
- **Long reviews keep going.** Context is summarized automatically. The matrix and the reasoning records live in files
  (Step 4), so a summary cannot lose them.

## Step 1: Ask for the context

Before reading any code, ask one `AskUserQuestion`:

1. **What should I review?** (header "Scope") Options from the request: the feature by its name, a configuration file,
   or the changes of the current branch against `default`.
2. **Why this review, now?** (header "Why now") Options: "Before merging a change", "Something broke or is slow",
   "Before a big day" (an exam day, the start of the school year) and "Routine health check". The answer never changes
   which checks apply. "Before merging a change" adds the diff to the scope and the "Scope" bullets of `AGENTS.md` to
   the AGT rows. "Something broke or is slow" makes the symptom's causal chain a required part of the report.
3. **How does it run in production?** (header "Instances") Options: "One instance", "Several instances" and "Not
   decided". "Not decided" counts as several, which is the safe reading; say so in its description. It decides whether
   the second-instance stressor applies (NSP4, NSP8, DEC3).
4. **How much load and data?** (header "Load") State the defaults in words: the crowds and the reference input of
   `.claude/skills/review-feature/references/journey.md`, section 6, and 10,000 rows in each table that grows per
   school, student, session or attempt, which is where SPD6 starts to judge a sequential scan. Options: "Use the
   defaults (Recommended)" and "Plan for twice as much".

- After "Something broke or is slow", ask one more `AskUserQuestion` about the symptom: what was seen (options
  "Errors", "Slowness", "Wrong data" and "Stuck work"), and when and how often, typed under "Other".
- For a configuration target, the review rows are the keys of that file (`references/configuration.md`, section 1).
- If the request is really about the user's side (steps, waits, what the user sees), ask with `AskUserQuestion`
  whether to run `review-feature` instead, and stop here if the engineer agrees.
- Write the answers, verbatim, at the top of `matrix.md` once it exists (Step 4), as the context record.

## Step 2: Inventory, history and decisions

All of this is `Static`:

1. Read `AGENTS.md` in full.
2. Build the entry point inventory as in `.claude/skills/review-feature/references/journey.md`, section 2, and read
   the files plus one hop out. Add: the config key rows (`references/configuration.md`, section 1); every migration
   that creates or changes the target's tables (migration rows); the test classes that cover the inventory; the
   direct dependencies the inventory imports (for CVE reachability); and every entry that names an inventory class
   in an exemption list of the architecture tests (the sets in `src/test/java/com/sep/vox/architecture` and the files
   in `src/test/resources/architecture/`).
3. Build the paths as in `journey.md`, section 3: P0 and the alternate paths. Here they only drive the runs (the
   statement count marks each action), so they need no budgets and no confirmation. When a `review-feature` matrix of
   the same target exists in the scratchpad's review folder, reuse its confirmed paths.
4. Build the thread map when the target is multi-threaded (`references/concurrency.md`). Its rows become background
   path rows of the matrix.
5. Read the history: `git log --follow --date=short --format='%h %ad %s' -- <file>` for each inventory file, and the
   plans under `docs/` that name the feature (their Decisions sections).
6. Build the decision register (`references/decisions.md`, sections 1 and 2): one row per decision, with what the code
   does, where, and the reason found in a default, an `AGENTS.md` rule, git or docs, or "none recorded".

## Step 3: Approve the runs, in one call

Ask one `AskUserQuestion`, each question standing alone:

1. **CVE scan.** Run `dependencyCheckAnalyze` now? Say that it downloads NVD and CISA data and takes 5 to 20 minutes on
   the first run (much longer without an API key), and whether `NVD_API_KEY` is set. Check that with
   `[bool]$env:NVD_API_KEY` and never print the value.
2. **Runs.** Start the missing containers and the app, seed the sample data (two schools, every role), and run the
   technical scenarios: `statementCount` at 1x and 2x volume, `EXPLAIN` of the main queries, the load runs with pool
   sampling, the hostile-input, error, limit and failing-write probes, `racingRequests` when NSP8 applies, and the log
   count. State the figures from Step 1, the data that is created and removed, and every side effect with how it is
   neutralized (`.claude/skills/review-feature/references/performance-k6.md`, section B). Put that list in the first
   option's `preview`.
3. **Pull requests**, only when `gh` is installed and merge commits of the inventory files name a pull request
   (`(#118)`): read their descriptions with `gh pr view <n> --json title,body`? It is read-only, but it calls GitHub.
   Options: "Read them (Recommended)" (fewer "why" questions) and "Local git only".

A declined run leaves its cells UNVERIFIED (reason "declined") unless a `Static` method settles them. Never run
anything heavy or outward-facing that was not approved.

## Step 4: Build the matrix

1. Pick a run id, `review-<yyyyMMddHHmmss>`, and create the review folder `<scratchpad>/review/<run id>/`. The
   matrix, the reasoning records, the k6 script, fixtures, SQL and outputs all go there. Nothing goes into the
   repository.
2. Write `matrix.md` in the format at the end of `references/checks.md`: the context record, then one block per row
   (the review, each step, each entry point, each background path, each config key, each migration, each test class,
   each `AGENTS.md` rule and each decision), and in each block one line per check whose "Applies to" rule covers the
   row, with status `TODO`. Write the expected cell count at the top.
3. Write `decisions.md` in the review folder with one reasoning record skeleton per decision row
   (`references/decisions.md`, section 4).
4. Create one task per row group (entry points and steps, background paths, config, migrations, tests, `AGENTS.md`
   audit, CVEs, decisions), so the task list mirrors the matrix.

## Step 5: Collect the evidence

Do the static work while the long runs go. The order:

1. Start the CVE scan in the background when approved (`references/security.md`, part C).
2. The build gate: GATE1 to GATE3. When a test fails, run it on the default branch in a temporary worktree under the
   review folder (`git worktree add`, removed afterwards) to tell a pre-existing failure from one the target causes.
3. Containers, the app and the sample data (`performance-k6.md`, sections C and D).
4. k6, each run from the same script (`performance-k6.md`, section E): `statementCount` at 1x and 2x volume, `probes`
   with the technical cells, the load scenarios with pool sampling (section H), and `racingRequests` when NSP8
   applies.
5. `EXPLAIN (ANALYZE, BUFFERS)` of the main queries at the Step 1 data volume (SPD6, section G). Seed the extra rows
   with SQL, tagged with the run id the way `cleanup.sql` matches them (section D), so the cleanup removes them.
6. The log privacy count (SAF6, section L), after the last run.
7. Cleanup of the sample data and the zero-rows check, whatever the k6 results were.
8. The `Static` cells: the thread map ends (NSP4, NSP8), transactions and side effects (SPD8, TRU6), constraints
   (TRU8), migrations (MIG1), security code (`references/security.md`, part B), the configuration chain
   (`references/configuration.md`), test mapping (TRU7, TST) and the `AGENTS.md` audit (AGT).
9. Triage of the CVEs when the scan ends (`references/security.md`, part C).

## Step 6: Settle every check cell

For each cell outside the DEC checks, record the status, the evidence and, for a FAIL, the failure scenario. Update
`matrix.md` as you go.

- The status comes from the check's pass criterion and nothing else.
- PASS needs evidence too.
- N/A only by the check's "Applies to" rule; quote the rule.
- UNVERIFIED only when the run was declined or cannot be done locally, and no `Static` method settles the cell. Write
  exactly what would settle it.
- Before you write a FAIL, look for an existing guard, validation or test that already handles the case. A FAIL needs
  the behaviour, not only the absence of the code you expected.
- When the cause of a FAIL is an instance of a decision kind, link the cell to that decision row
  (`references/decisions.md`, section 1).

## Step 7: Ask why, and test every reason

Follow `references/decisions.md`, sections 2 to 5:

1. **Open decisions.** Every decision row whose reason no default, `AGENTS.md` rule, commit, plan or pull request
   gives.
2. **Ask** them with `AskUserQuestion`, up to four per call and as many calls as it takes, most severe governed check
   first (section 3). Do not drop a decision to save the engineer's time.
3. **Test** every reason, from git or from the engineer: split it into premises and settle each one against evidence
   (DEC2), then run its counter-scenarios (DEC3).
4. **Follow up** when the evidence contradicts a premise of the engineer's answer: ask once more, showing the
   evidence. At most two follow-up rounds per decision, then judge with what is known.
5. An answer never turns a FAIL into a PASS. The pass criteria are fixed; an answer can only explain a FAIL (an
   accepted trade-off, which the finding states) or correct a Step 1 fact. When it corrects a Step 1 fact (instances,
   load), update the context record and settle again every cell that depends on it.

## Step 8: Turn the FAIL cells into findings

- **One finding per check and root cause.** The root cause is the `file:line`, config key or decision row whose change
  fixes it. Several cells that fail the same check for the same root cause form one finding that lists every cell.
  When the root cause is unknown (seen only in a run), make one finding per check and row. A DEC cell that fails
  because a governed check fails joins that check's finding; it does not open a second one.
- **Severity** comes from the catalogue row, with its escalation conditions. CVEs use the rule in
  `references/security.md`, part C.
- **Numbering** is fixed: by severity (Critical first), then by check id in catalogue order, then by row order. F1 is
  always the most severe.
- **Deeper reasoning in every finding:**
  - *Why it happens*: the causal chain from the symptom to the root cause, one link per "why", each with evidence. It
    ends at a decision row, a missing rule or a missing test, never at "the code does X".
  - *Why it was not caught*: the test, guardrail or review step that should have caught it (TRU7, TST, GATE3, an
    `AGENTS.md` rule), and why it did not.
  - *The decision behind it*: the decision row, its reason and where that reason came from, and the DEC verdicts.
  - *Options*: at least two ways to fix it, with the trade-off of each on the Step 1 context, then the smallest fix. A
    fix that falls under "Ask first" in `AGENTS.md` is proposed, never applied, and the finding says which item.
- Each finding also has: id, severity, check, cells, where, problem, failure scenario and evidence (`Run` or
  `Static`).

## Step 9: Completeness gate

Write the report only when all of these hold. Otherwise go back and finish:

1. Every cell in `matrix.md` is PASS, FAIL, N/A or UNVERIFIED. No `TODO` is left, and the settled count equals the
   expected count.
2. Every entry point from the inventory has its rows, and every one is on a path or on the off-path list.
3. Every thread-map row reaches all of its end states (`references/concurrency.md`).
4. Every `AGENTS.md` rule row is settled.
5. Every decision row has a complete reasoning record, and every question asked has its answer recorded. No question
   is open.
6. Every FAIL cell belongs to exactly one finding, every finding cites at least one FAIL cell, and every finding has
   its causal chain and its escape analysis.
7. The zero-rows check after cleanup returned 0, or the report lists exactly what remains and why.
8. **Second pass.** Walk the catalogue and the decision kinds from top to bottom once more, row kind by row kind, and
   derive again which checks apply to each row and which decisions the inventory holds. Anything that applies but
   has no cell or row is added and settled before you go on.

## Step 10: Report

Use this shape. Keep it factual, and drop a section only when it cannot apply (say which at the top).

```
## Technical review: <target> -> <READY | READY WITH FIXES | NOT READY>
<one line: the most severe findings, and any Critical or High catalogue gap>

### Context
- Scope: <answer>; why now: <answer>; runs as: <instances>; load and data: <figures and their source>
- Symptom (only when something broke): <answer> -> <the finding that explains it, or "not found", with what was ruled out>

### Decisions
| # | Decision | Where | Reason | Source (default, rule, commit, plan, PR, engineer, none) | Holds? (DEC2 / DEC3) | Holds only while |
- <n> decisions: <a> hold, <b> do not; <c> explained only in this chat; <d> nobody could explain

### Pillars
| Area | Cells | PASS | FAIL | N/A | UNVERIFIED |
| Data access and transactions | | | | | |
| Concurrency and background work | | | | | |
| Security internals | | | | | |
| Library CVEs | | | | | |
| Configuration | | | | | |
| Migrations | | | | | |
| Build gate, tests and AGENTS.md | | | | | |
| Decisions | | | | | |

### Findings
#### F1 <Severity> <check>: <the problem in one line>
- Cells: <cell ids>
- Where: <file:line, config key or decision row>
- Failure scenario: <this input, interleaving or state -> this wrong outcome>
- Why it happens: <causal chain, each link with evidence, ending at the root cause>
- Why it was not caught: <the test, guardrail or review step, and why it missed>
- Decision behind it: <DEC row, its reason and source, DEC verdicts | none>
- Evidence: <Run | Static>
- Options: <at least two, each with its trade-off on the context>; smallest fix: <...>; Ask first: <the AGENTS.md item | no>

### Library CVEs
| Library:version | CVE | CVSS | KEV | Classpath | Real / false positive | Reachable | Fixed in | Proposed fix |
- Scan: <date, NVD data age, analyzers off> or "not run: <reason>"; container images: not scanned

### Measurements
| Run | Profile | Requests | RPS | p50 | p95 | p99 | Errors | Thresholds |
- Statements per request (1x / 2x volume): <per action>
- Query plans: <statement, top plan node, rows, time>
- Pool and executors under load: <highest pending connections, rejections, pool warnings>
- Log count: <pattern: count, all expected 0>
- Sample data removed: <yes, 0 rows left | no: what remains>
- Caveat: one local instance on a laptop, with SQL logging on; compare runs, do not read it as production capacity

### Thread map (multi-threaded only)
| Path | Starts at | Hand-offs | Shared state | Mechanism | Ends at (success / failure) | Cells |

### Checks run
- `./gradlew compileJava compileTestJava` -> pass

### Unverified
| Cell | Why | What would settle it |

### Answers from the engineer
- <question -> answer, verbatim, and the decision or cell it settled>

### To write down
- <each reason given only in this chat, and the git-tracked file where it belongs (a doc in docs/, a Javadoc, AGENTS.md)>

### Catalogue gaps
- <problem with evidence, and the proposed row for references/checks.md>

### Outside the target
- <problems noticed in code that is not part of the target, not fixed; user-side problems go to review-feature>

### Matrix
- <full path of matrix.md and decisions.md>; <expected cells> cells, all settled
```

Verdict, decided only by these rules:

- **NOT READY**: any Critical or High FAIL.
- **READY WITH FIXES**: the FAILs are Medium or Low only, or an UNVERIFIED cell sits on a check that is Critical or
  High on failure (list how to settle each one).
- **READY**: no FAIL above Low, and no UNVERIFIED cell on a check that is Critical or High on failure.

After the report, ask one `AskUserQuestion` with only the questions that apply: whether to publish the report as a
page for sharing; whether to add the proposed rows of "Catalogue gaps" to `references/checks.md`, which changes the
skill; and whether to write the reasons under "To write down" into the files named there. Changing `AGENTS.md` still
needs its own confirmation, as `AGENTS.md` says.

## Pitfalls

- **Accepting a reason because it sounds right.** Split it into premises and check each one. A reason with a false
  premise is a lucky decision, not a sound one.
- **Leading questions.** "This is for performance, right?" with one real option is not a question. Give neutral
  candidate reasons and "No deliberate reason".
- **Asking what git already answers.** Read the commit, the merge commit, the plan and, when approved, the pull request
  first.
- **Judging the decision you imagined.** Quote the line or key, and the effective value (`references/configuration.md`).
- **Assuming defaults.** Read the defaults from the configuration metadata inside the jar of the version in use, or
  from that version's docs (Spring Boot 4.1, Spring Framework 7, Hibernate, Kafka client). Check units and precedence.
- **Stopping at the symptom.** A causal chain ends at a decision, a missing rule or a missing test.
- **Vague findings.** "Might not scale" is not a finding. Name the cell, the measured value, the criterion and the
  consequence.
- **Drifting into the journey.** Step budgets, waits and the wording of messages belong to `review-feature`.
- **Fixing instead of reviewing.** Suggest the fix and leave the decision to the engineer.
