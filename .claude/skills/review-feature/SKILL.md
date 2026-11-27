---
name: review-feature
description: Review a Vox feature, configuration or its tests end to end. Covers correctness against AGENTS.md, security (code and config checks plus an OWASP dependency-check CVE scan of every library), performance (static checks plus a k6 load test in Docker, with sample data created and removed in the same run, asking before the app is started), and, for multi-threaded code, a trace of every thread path to its end. Use it whenever the user asks to review, audit or check a feature, endpoint, use case, worker, scheduler, Kafka consumer, GraphQL resolver, application.yaml, build.gradle, Dockerfile, docker-compose or CI workflow, or their tests. Also use it for "is this ready to merge", "any CVEs in our libraries", "load test this" or "is this thread-safe", even without the word "review". Prefer it over the generic code-review when the target is a named feature or configuration, not only the current diff. Not for checking a plan's implementation (use verify-plan).
---

# Review a feature or configuration

Purpose: give the user an honest, evidence-backed verdict on one feature or configuration (production code, its
config and its tests) across four tracks: correctness, security, performance and, when it applies, concurrency.

Rules for the whole skill:

- **Read-only.** Report findings and do not fix them. A fix happens only after the report, when the user asks, as a
  normal task under `AGENTS.md` (or through `implement-plan`). The one exception is the scratch material this skill
  creates and removes itself: k6 scripts, seed SQL and sample rows in the local database.
- **Evidence for every finding**: a `file:line`, a command and its output, or k6 numbers. Mark each finding
  **Confirmed** (reproduced, or the code leaves no doubt) or **Suspected** (reasoned but not reproduced). Do not
  present a suspicion as a fact.
- **`AGENTS.md` is the only source of repo rules.** Read it in full at the start. Do not work from a remembered copy,
  because it changes during the refactoring stage.
- **Ask before anything heavy or outward-facing**: the CVE scan (it downloads vulnerability data), starting
  containers or the app, seeding data, and running k6. Batch these into as few `AskUserQuestion` calls as possible,
  with the concrete plan in each question, so the user can approve once (see Step 3).
- **Local only.** Load tests and seed data target only `localhost` / `host.docker.internal` and the docker-compose
  Postgres container. Refuse a remote or shared host even if asked, and say why.
- **Never print secrets**, tokens, OTPs, passwords or student PII, whether in commands, logs, scripts or the report.
- **Multi-threaded means "to the end".** If the feature is multi-threaded (Step 2), the concurrency track is
  mandatory and the review is not finished until every thread path in its thread map reaches its terminal state.
  There is no finding cap, no sampling and no "and similar for the rest". A long review keeps going, because context
  is summarized automatically. Read `references/concurrency.md` before starting that track.

## Step 1: Pin down the target

1. Work out what the user wants reviewed: a feature name ("quota carry"), paths, a configuration file, a branch or PR,
   or the current diff. If two readings are plausible, ask. Otherwise go on.
2. Build an **inventory** of every file that belongs to the target, layer by layer. Search by the names the feature
   uses (use case, model, entity, table, endpoint path, GraphQL type, topic, config key):
   - `domain/` models, value objects and repository interfaces
   - `application/` commands, queries, use cases, shared services, ports and event payloads
   - `infrastructure/` entities, adapters, mappers, query repositories, workers, clients and config classes
   - `interfaces/` REST and GraphQL controllers, request records, DataLoader configs and Kafka consumers
   - resources: `graphql/*.graphqls`, `db/migration/V*.sql` and the `application.yaml` keys it reads
   - tests: every test class for the files above, including architecture and convention tests that name them
   - for a configuration target: the file, every class that reads the keys it touches (`@Value`,
     `@ConfigurationProperties`, `Environment`), and the tests or CI steps that depend on it
   - for a diff: `git diff default...HEAD --stat`, `git status --short`, and every untracked file
3. Read every inventory file in full, plus one hop out: the callers of each entry point and the callees that do I/O.

## Step 2: Classify the target

Decide which tracks apply and record why:

| Track | Applies when |
|---|---|
| Correctness and conventions | Always |
| Tests | Always (the tests are part of the target) |
| Security: code and config | Always |
| Security: library CVEs | Always, unless the user scoped it out |
| Performance: static | Always |
| Performance: k6 | The target has an HTTP or GraphQL entry point, or a path a request triggers |
| Concurrency (to the end) | The target is multi-threaded, per the criteria in `references/concurrency.md` |

"Multi-threaded" here includes `@Async`, executors (`AsyncConfig`), `CompletableFuture`, `@Scheduled` workers, Kafka
consumers, the outbox relay, async GraphQL resolvers and gRPC streams. It also includes any logic whose correctness
depends on concurrent requests touching the same rows or keys (quota, balance, counters, status transitions, "only
one active X"). Money, quota and billing paths always count.

Create one task per track that applies, so no track is dropped silently. Add one task per thread path once the thread
map exists.

## Step 3: Ask once for the heavy steps

After the inventory and classification, and before running anything heavy, ask in one `AskUserQuestion` call
(each question must stand alone, recommendation first):

- **CVE scan**: run `dependencyCheckAnalyze` now? Say that it downloads NVD and CISA data, takes 5 to 20 minutes on
  the first run (much longer without an API key), and whether `NVD_API_KEY` is set. Check it with
  `[bool]$env:NVD_API_KEY` and never print the value.
- **Load test**: start the needed containers and the app, seed sample data, and run k6? State the endpoints, the load
  profile, the data that will be created and removed, and every side effect found on the path (email, push,
  payment, S3, AI grading). Details are in `references/performance-k6.md`, section B.

If the user declines a step, skip it and list it under "Not run" with the reason. Run the CVE scan in the background
(`run_in_background`) and do the static tracks while it runs.

## Step 4: Correctness and conventions

- Business rules: invariants, state transitions, boundaries (null, empty, first/last page, zero, max), and time
  handling (`Asia/Ho_Chi_Minh` in tests; no machine-timezone dependence).
- Exceptions by meaning (`AGENTS.md`, Errors): 400 vs 422 vs 500. Look for a 500 that a caller can trigger, which is a
  bug.
- Transactions: the use case owns `@Transactional`; read-only queries; no external call inside a transaction that
  holds locks.
- Data access: no query per element (N+1), shared paging classes only, migrations that agree with entities, and a new
  migration rather than an edited one.
- Events: payload versioning (`V2` instead of editing `V1`) and the outbox for outgoing events.
- `AGENTS.md` audit: list the touched areas, then check each one against every section that applies (Architecture,
  Conventions, Database, Testing, Scope, Coding rules). Quote the rule and the line for each violation.
- Run the checks. They are local and need no approval:
  ```bash
  ./gradlew compileJava
  ./gradlew test --tests '*<Feature>*'
  ./gradlew test --tests '*LayeredArchitectureTests'   # plus the convention tests the target touches
  ./gradlew integrationTest --tests '*<Feature>*'       # when Docker is running
  ```
  A failure that also exists without the target (check `git stash` or the default branch) is reported as
  pre-existing, not blamed on the target.

## Step 5: Tests

Review the tests as part of the product:

- Map each behaviour of the target to the test that would fail if the behaviour broke. A behaviour with no such
  test is a finding (Medium, or High for security, money or concurrency behaviour).
- Check the `AGENTS.md` Testing rules: the right annotation per layer, the 401 / 403 (with a valid body) / success trio
  for every role-protected endpoint group, a regression test for every bug fix, strict stubs with no `lenient()`, and
  the `integration` tag.
- Check the quality of each assertion: it asserts the outcome (state, response body, emitted event), not only "no
  exception". No `Thread.sleep` coordination, no dependence on test order or the machine timezone, and no data shared
  between tests.
- A test that mocks the repository proves nothing about locking, SQL or races. Say so when the risk lives there.
- When the target itself is a test or a test configuration, review what it claims to cover against what it really
  covers.

## Step 6: Security

Read `references/security.md` and do both parts:

- **A. Code and configuration**: access control and school scope, input handling, data exposure and logging,
  auth and session, money paths, and configuration files.
- **B. Library CVEs**: the OWASP dependency-check run, a summary with
  `scripts/summarize-dependency-check.ps1`, and a triage of each CVE (classpath, origin, real or false positive,
  reachability, fixed version, proposed fix). Upgrades are "Ask first" in `AGENTS.md`, so propose them and never
  apply them.

## Step 7: Performance

Read `references/performance-k6.md`:

- **A. Static**: queries and indexes, unbounded reads, transaction length, payload size, executor sizing, caching.
- **B. k6** (only after approval in Step 3): start what is missing, seed the sample data, run the scenarios with
  `grafana/k6` in Docker, remove the data, and verify that it is gone. All of it happens within one run, from
  `scripts/k6-feature-template.js`, in the session scratchpad.

## Step 8: Concurrency (mandatory when multi-threaded)

Follow `references/concurrency.md`: build the thread map, trace every path hop by hop to its end (success and
failure), answer the checklist for each hop, and back suspected races with a k6 race probe when the path is reachable
over HTTP. The track is done only when every row in the thread map has a terminal state and evidence.

## Step 9: Report

Severity:

| Level | Meaning |
|---|---|
| Critical | Exploitable security hole, data loss or corruption, wrong money, quota or balance, or a race on those |
| High | Wrong behaviour in a realistic case, a race under normal load, a runtime CVE with CVSS 7 or more |
| Medium | Edge-case bug, missing test for important behaviour, slowdown under expected load, test-only CVE |
| Low | Convention or maintainability issue not caught by the architecture tests |
| Nit | Wording, naming, style |

Use this shape. Keep it factual. Drop a section only if its track did not apply, and say so in "Scope".

```
## Review: <target> -> <READY | READY WITH FIXES | NOT READY>

### Scope
- Inventory: <n files: layers covered>; tracks: <list>; multi-threaded: <yes (why) | no>

### Findings
| # | Severity | Confidence | Track | Where | Finding | Suggested fix |
|---|---|---|---|---|---|---|
| 1 | High | Confirmed | Security | `path/File.java:42` | ... | ... |

### Library CVEs
| Library:version | CVE | CVSS | KEV | Classpath | Real / false positive | Fixed in | Proposed fix |
- Scan: <date, NVD data age, analyzers off> or "not run: <reason>"

### Performance
| Scenario | Profile | Requests | RPS | p50 | p95 | p99 | Errors | Thresholds |
- Invariants checked: <...>; sample data removed: <yes, 0 rows left | no: what remains>
- Caveat: one local instance on a laptop; compare runs, do not read it as production capacity

### Thread map (multi-threaded only)
| Path | Starts at | Hand-offs | Shared state | Ends at (success / failure) | Findings |

### Tests
- Behaviours without a test: <...>; test quality issues: <...>

### Checks run
- `./gradlew compileJava` -> pass

### Not run / not verified
- <what and why>

### Outside the target
- <problems noticed in code that is not part of the target, not fixed>
```

Verdict: NOT READY if any Critical or High finding is Confirmed. READY WITH FIXES if the open findings are Medium or
lower, or Suspected Highs with a clear check to settle them. READY only if nothing above Low is open and every track
that applies ran or the user scoped it out.

After the report, offer in one line to publish it as a page, for when the user wants to share it.
