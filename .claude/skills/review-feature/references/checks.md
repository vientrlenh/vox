# Check catalogue

A review applies every check below to every matrix row the check covers. Nothing is sampled or capped, and a check
is skipped only when its own "Applies to" rule leaves the row out. The catalogue is the only source of findings. A
problem it does not cover goes to "Catalogue gaps" in the report (SKILL.md, Step 9) as a proposed new row, never into
Findings. That is how two reviews of the same context end with the same findings.

## Terms

### Matrix rows

| Row | One per | Id |
|---|---|---|
| Review | review | `R` |
| Path | path in the confirmed journey (`references/journey.md`) | `P0`, `P1`, ... |
| Action step | action (A) on a path | `P0.S2` |
| Wait step | wait (W) on a path | `P0.W1` |
| Entry point | entry point in the inventory, on or off a path | `E4` |
| Background path | row of the thread map (`references/concurrency.md`) | `B2` |
| Config key | key the target touches, or one that sets a budget, limit, lifetime or interval on the journey | `K3` |
| Test class | test class in the inventory | `T5` |
| Rule | `AGENTS.md` bullet (see AGT below) | `AGT-Database-3` |

### Words with a fixed meaning

- **Write**: an action or entry point that changes a row, publishes an event, sends a message or calls an external
  service. Everything else is a **read**. Classify by effect, not by HTTP method.
- **Protected value**: money (orders, payments, PayOS, school balance, quota, subscription) or grades (scores,
  results, appeals). Some checks rate higher when one is involved.
- **Cell id**: `<row id>:<check id>`, plus `:<variant>` when a check runs once per role, state or input
  (`E4:SAF2:TEACHER`, `P0.S2:NSP1:COMPLETED`). Every k6 check name starts with its cell id, so the k6 summary maps
  straight onto the matrix.
- **Variants**: when a feature has several variants of one step (13 import types, for example), a check is shared
  by the variants only when every variant runs the same class for the part the check judges. Cite that class.
  Otherwise each variant gets its own cell.

### Status of a cell

| Status | Meaning | Must carry |
|---|---|---|
| PASS | The pass criterion holds | Evidence |
| FAIL | The pass criterion does not hold | Evidence and a failure scenario: this input or state leads to this wrong outcome |
| N/A | The check's "Applies to" rule leaves this row out | The rule, quoted |
| UNVERIFIED | The method could not run (declined, or not reproducible locally) and no `Static` method settles it | Exactly what would settle it |

Evidence is either `Run` (k6 scenario plus the check or metric and its value, a probe response, a test result, or
command output) or `Static` (a `file:line` or config key that leaves no doubt). Use `Static` only where the check's
method allows it. "Looks fine" is not evidence, and neither is a remembered fact about the code.

### Severity

Severity is fixed by the row. An "escalates" condition is part of the rule, not a judgement call, so never raise or
lower a severity by feel.

| Severity | Meaning |
|---|---|
| Critical | Security hole, data loss or corruption, or a wrong protected value |
| High | The actor is blocked, misled or exposed in a realistic case |
| Medium | Slower, harder or less safe than the target allows, or a risky behaviour without a test |
| Low | A maintainability or convention problem with no effect the actor can see |

## J: Journey

| Id | Check | Applies to | Method | PASS when | Severity on FAIL |
|---|---|---|---|---|---|
| J1 | The path reaches its end | Path rows | Run: `journeyWalk` on the path, with check `<path>:J1` on its end (the success signal for paths that end at the target) | The end is observed in every iteration | High |
| J2 | Every action is needed | Path rows | Static, from the journey table: what uses each action's output, and which values each action asks for | Every action's output is used by a later step or the success signal, and no value is asked for twice | Low |
| J3 | Time to target fits the owner's expectation | `P0`, only when the owner stated a time | Run: system time to target, p95 over the walk iterations. Static: the worst case from config | Both within the stated time | Medium. High when the worst case misses a deadline the target depends on (the start of an assessment, for example) |
| J4 | Refusals come at the earliest step | Path rows, once per refusal the path can produce (rule limit, quota, permission, deadline, duplicate) | Static: the step where the rule is checked, and the earliest step whose input already decides it. Run: the probe that triggers it | The refusal fires at that earliest step | Medium |
| J5 | The actor can leave and come back | Path rows with a wait over 10 s or at least 3 actions | Run: after each action and during each wait, a fresh login of the same actor finds the item and its current status through the API | Found, with the current status, at every point | Medium |
| J6 | An off-path entry point has a user | Entry point rows that are on no path | Static: the actor or other journey that needs it | One is named | Low |

## SPD: Speed (latency and throughput)

| Id | Check | Applies to | Method | PASS when | Severity on FAIL |
|---|---|---|---|---|---|
| SPD1 | Step time for one actor | Action rows | Run: `journeyWalk`, `review_step_duration{step:<id>}` p95 | Within the step's budget (`references/journey.md`, section 5) | Medium. High above 3x the budget |
| SPD2 | Step time at the expected concurrency | Action rows | Run: `steadyLoad` or `burst` on the step, at the expected concurrency (`references/journey.md`, section 6) | p95 within the budget and `http_req_failed` under 1% | Medium. High above 3x the budget, or at 1% errors or more |
| SPD3 | Wait time and its feedback | Wait rows | Run: `journeyWalk`, the measured max. Static: the worst case from config, and the feedback the actor gets | Measured max and worst case within the wait's band, and the band's feedback exists | Medium. High when the worst case has no bound |
| SPD4 | Throughput holds | Action rows with an expected concurrency above 1 | Run: during the load run, `dropped_iterations` is 0, `hikaricp.connections.pending` (sampled every 5 s) stays 0, and the app log has no `RejectedExecutionException` or pool warning | All three hold | Medium. High when requests fail |
| SPD5 | SQL statements per request do not grow with the data | Action rows | Run: `statementCount` at the reference volume and at twice the child rows (`references/performance-k6.md`, section F) | Statements per request at 2x is at most the 1x figure plus 2 | Medium |
| SPD6 | Main queries are indexed | Action and wait rows that query a table that grows per school, student, session or attempt | Static: an index in `db/migration` covers each `WHERE`, `JOIN` and `ORDER BY` column. Run: `EXPLAIN (ANALYZE, BUFFERS)` at the reference volume | Indexed, and no `Seq Scan` with a filter on a table above 10,000 rows | Medium |
| SPD7 | Responses are bounded | Read actions that return lists, and wait rows that produce results | Static: paged with `PageArguments` or capped by a stated limit. Run: the response size at the reference volume | Bounded, and at most 1 MB at the reference volume | Medium |
| SPD8 | No outside call inside a transaction | Write actions and wait rows | Static: from each `@Transactional` boundary, trace the calls to HTTP, gRPC, S3, SMTP, Firebase and Kafka sends | None inside a transaction | Medium. High when that transaction holds a row lock |

## TRU: Trust (reliability and data integrity)

| Id | Check | Applies to | Method | PASS when | Severity on FAIL |
|---|---|---|---|---|---|
| TRU1 | All or nothing | Write actions and wait rows that change more than one row or table | Run: make the step fail at its last write (an input that passes validation but breaks a constraint, or a last item that is invalid), then count the rows it touched, outbox included | No row from the failed attempt remains | High. Critical with a protected value |
| TRU2 | Safe to repeat | Write actions | Run: the same request twice in a row, and twice at the same moment | One effect, or the second is refused with 409 or 422 | High. Critical with a protected value |
| TRU3 | A mistake the user can fix gets a 4xx that says what to fix | Action rows, once per input field and once per file rule | Run: missing, empty, too long, wrong type, out of range and unknown enum values. For files: wrong type, empty, header only, missing column, extra column, duplicate rows, bad date | 400 or 422 naming the field (row and column for files), with the state unchanged | High for a 500. Medium for a message that does not name the field |
| TRU4 | What goes in comes out | Write actions that store text, dates, numbers or codes | Run: fixtures with Vietnamese names in NFC and NFD (`Nguyễn Thị Ánh`), leading zeros (`0901234567`, `00123`), a time one minute before and one after midnight in Asia/Ho_Chi_Minh, a score of `7.25`, and text at the column's maximum length. Read them back through the API | Read back equal to what was sent (NFD may come back as NFC, if it always does) | High |
| TRU5 | Background failures end visibly | Wait rows | Run: an input that makes the job fail, and the restart probe when approved. Static: what sets the failed state for each way the work can end (exception in the handler, pool rejection, lease expiry, retries exhausted, shutdown) | Every way ends in a terminal status with a reason the actor can read, within the stated worst case, and the actor can retry | High when a status can stay in progress forever. Medium when the failed status gives no reason |
| TRU6 | Side effects follow the commit | Write actions and wait rows that publish events or send email or push | Static: outbox or `AFTER_COMMIT`. Run: after the TRU1 failure, no outbox row and no message | Nothing leaves before the commit or after a rollback | High |
| TRU7 | Behaviour is protected by a test | Action and wait rows | Static: for each behaviour another check exercised on this row (PASS or FAIL), the test that fails if it breaks | Every behaviour has one | Medium. High when the untested behaviour belongs to a SAF check, a protected value or NSP2 |

## NSP: No surprises (predictable state and concurrency)

| Id | Check | Applies to | Method | PASS when | Severity on FAIL |
|---|---|---|---|---|---|
| NSP1 | Actions in the wrong state are refused | Write actions, once per state of the resource in which the action is not allowed (from the status enum) | Run: drive the resource into the state, then send the action | 409 or 422 (GraphQL: `BAD_REQUEST`) with a message, and the state unchanged | High |
| NSP2 | The same resource at once | Write actions on shared state (counters, statuses, quota, balance, "only one active" rules, unique business keys) | Run: `racingRequests`, then the invariant in SQL. Static: the lock, version, conditional update or unique constraint | The invariant holds and the mechanism is named | Critical with a protected value. High otherwise |
| NSP3 | A read sees the write that just succeeded | Action rows followed by a read on the same path | Run: `journeyWalk` reads right after the write | Every iteration sees it | Medium |
| NSP4 | Background work runs once per item | Background path rows | Static: the thread map (`references/concurrency.md`): duplicate delivery, two instances, retries | The path names its dedupe or claim mechanism and reaches every end state | High. Critical with a protected value |
| NSP5 | Time rules flip exactly on time | Path rows, once per time rule (deadline, window, expiry, "today") | Run: one second before, at and one second after the boundary, or a test with a fixed clock in Asia/Ho_Chi_Minh | The outcome changes exactly at the boundary | High |
| NSP6 | Lists are stable | Read actions that page | Static: `ORDER BY` ends with a unique column. Run: page through and compare with the total | No duplicates or gaps, and the total matches the content | Medium |
| NSP7 | Statuses and counters tell the truth | Path rows, once per status or counter the actor sees | Run: after the walk, compare with SQL counts | Equal | High |

## SAF: Safety (security, privacy and session continuity)

| Id | Check | Applies to | Method | PASS when | Severity on FAIL |
|---|---|---|---|---|---|
| SAF1 | No access without login | Entry point rows, except the paths in `SecurityConfig` `PERMITTED_PATTERNS` that are public by design (quote the reason; `/graphql` is listed there but its operations are not public) | Run: no token | 401 (GraphQL: `UNAUTHORIZED`, no data) | Critical |
| SAF2 | A wrong role is refused | Entry point rows, once per role that is not allowed | Run: a valid body with that role's token | 403 (GraphQL: `FORBIDDEN`, no data) | Critical |
| SAF3 | Another school or another owner is refused | Entry point rows that take an id or return school- or owner-scoped data | Run: the allowed role of school B on school A's ids, and student A2 on student A1's data, for reads, writes and GraphQL nested fields | 403 or 404, and no data of A in the body | Critical |
| SAF4 | Hostile input is contained | Entry point rows that take input | Run: a body and a file above `spring.servlet.multipart.max-file-size`, a row count above the stated limit, 10,000-character strings, SQL metacharacters in filters and sort fields, a deeply nested or heavily aliased GraphQL query, an xlsx zip bomb, an xlsx with an external entity, and exported cells starting with `=`, `+`, `-` or `@` | Refused with a 4xx within the step budget, no 500, no hang, and formulas escaped in exports | High. Critical when an injection or the external entity works |
| SAF5 | Responses show only what the actor may see | Entry point rows that return data | Run: every response body from the walk and the probes | No password hash, token, OTP, other school's or other student's data, or internal-only field | High. Critical for credentials |
| SAF6 | Logs keep secrets and PII out | Review row | Run: after all runs, count the matches in `logs/vox.log` and the bootRun output for the run's password, JWTs (`eyJ`), refresh tokens, OTPs, seeded emails (`perf.invalid`) and seeded names (the run id). Print counts only | Zero for each | High |
| SAF7 | Errors reveal no internals | Entry point rows | Run: the error responses from the TRU3 and SAF4 probes | No stack trace, SQL, class or package name | Medium |
| SAF8 | The session lasts the journey | Path rows whose worst case, or any one step or wait, can last longer than the access-token lifetime (`jwt.expiration-ms`, 15 minutes by default) | Run: `sessionContinuity` against an app started with a short `JWT_EXPIRATION_MS`: the token expires before the longest step, the actor refreshes and goes on; then a logout ends the session | The actor goes on after the refresh and loses nothing, and the logged-out session is refused | High |
| SAF9 | Costly actions are limited | Entry point rows that send email or OTP, call AI grading, upload to storage or move a protected value | Static: a rate limit or quota per actor. Run: a burst of 20 requests in 10 s | A limit exists and holds | Medium. High for login and OTP |
| SAF10 | No exploitable known CVE in the libraries | Review row | Run: dependency-check and triage (`references/security.md`, part C) | No real CVE on the runtime classpath | Per CVE, by the rule in `references/security.md` part C, with one finding per CVE |
| SAF11 | Money moves correctly | Entry point rows that move money (orders, payments, PayOS webhooks, balance) | Static and Run (`references/security.md`, part B) | The signature is checked first, the amount is checked against the order, repeats are idempotent, and the state transition is guarded | Critical |

## CFG: Configuration

These come from the configuration lenses of the `feature-config-review` plugin. They apply to config key rows, which
also exist when the target is a feature: every key that sets a budget, limit, lifetime or interval on the journey.

| Id | Check | Applies to | Method | PASS when | Severity on FAIL |
|---|---|---|---|---|---|
| CFG1 | The effective value is the intended one | Config key rows | Static: resolve the chain (code default, `application.yaml`, `application-test.yaml` in tests, environment variable, command line) for local runs, tests and docker-compose. Look the defaults up in the docs for the version in use; never assume them | The value that applies is the one the target needs | Medium |
| CFG2 | Units are right | Config key rows that hold a time or a size | Static: the unit the reader expects (`ms` or `s`, MB or MiB, `Duration` syntax) | They match | Medium |
| CFG3 | Defined and read | Config key rows | Static: every key that is read (`@Value`, `@ConfigurationProperties`, `Environment`) is defined or has a safe default, and every key that is defined is read (a typo is silently ignored) | Both hold | Low. Medium when a typo leaves a limit unset |
| CFG4 | Limits agree with each other | Config key rows that form a pair (timeouts, pool sizes, leases, lifetimes, upload limits) | Static: a client timeout under the upstream timeout; Kafka listener concurrency, executor pools and `hikari.maximum-pool-size` under Postgres `max_connections`; a lease longer than the longest job; the token lifetime against the longest wait; the multipart limit against the reference input | All consistent | Medium |
| CFG5 | The same meaning in every environment | Config key rows | Static: `application.yaml`, `application-test.yaml`, `docker-compose.yml`, `Dockerfile`, the CI workflows and the README env table | Every difference is intended and says why | Low |
| CFG6 | No secret or insecure default | Config key rows | Static: a real secret only through `${ENV}` with no default. An insecure default (actuator exposure, SSL off, wide CORS, `cookie.secure: false`) is judged only after checking what `SecurityConfig` protects | None | High for a real secret or an unprotected sensitive endpoint. Medium otherwise |
| CFG7 | Versions are pinned | Config key rows for images and build plugins | Static | A fixed version, never `latest` | Low |
| CFG8 | A wrong value can be undone | Config key rows | Static: whether changing it needs a migration, a data fix or a coordinated deploy | Reverting is a redeploy of the previous value | Medium |

## GATE: Build gate

| Id | Check | Applies to | Method | PASS when | Severity on FAIL |
|---|---|---|---|---|---|
| GATE1 | It compiles | Review row | `./gradlew compileJava compileTestJava` | Pass | High |
| GATE2 | The feature's tests pass | Review row | `./gradlew test --tests '*<Feature>*'`, and `./gradlew integrationTest --tests '*<Feature>*'` when Docker runs | Pass. A failure that also happens on the default branch is listed as pre-existing and is not this cell's FAIL | High |
| GATE3 | The guardrails pass | Review row | `./gradlew test --tests '*LayeredArchitectureTests' --tests '*ConventionTests' --tests '*TestSuiteSeparationTests'` | Pass | Medium |

## TST: Test quality

| Id | Check | Applies to | Method | PASS when | Severity on FAIL |
|---|---|---|---|---|---|
| TST1 | Tests assert the outcome | Test class rows | Static | Every test asserts a state, a response body or an emitted event, not only "no exception" | Low |
| TST2 | Tests are deterministic | Test class rows | Static | No `Thread.sleep` coordination, no dependence on test order or the machine timezone, and no data shared between tests | Medium |
| TST3 | The test level fits the risk | Test class rows that cover SQL, locking, races or auditing | Static | They run against the real Postgres (`@RepositoryTest` or `@IntegrationTest`). A mocked repository does not count | Medium |

## AGT: AGENTS.md audit

The owner chose to keep a manual audit against `AGENTS.md`. Its rows are built at run time, because `AGENTS.md`
changes during the refactoring stage:

1. **Rows.** From the `AGENTS.md` read at the start of the review, every bullet and numbered item under
   "Architecture" (the hard rules and "Where things go when adding a feature"), "Conventions that are easy to get
   wrong", "Database", "Testing", "Coding rules" and "Unused code and imports" becomes one row,
   `AGT-<section>-<n>`, numbered in document order. Add the "Scope" bullets when the target is a diff. One row per
   bullet, however many sentences it holds.
2. **Applies to.** The inventory files inside the bullet's own scope (layer, package or file kind). A bullet whose
   scope holds no inventory file is N/A.
3. **Method.** Static: read those files against the bullet. When a guardrail test that passed in GATE3 enforces the
   bullet, the cell is PASS with that test as its evidence, and no reading is needed.
4. **FAIL** carries the `file:line` and the quoted sentence it breaks.
5. **Severity.** Low. Medium for bullets about authorization, errors, pagination, migrations and audit columns. When
   the violation also fails another check, cite that check's finding and do not open a second one.

## Catalogue gaps

When you see a problem that no row covers, write it under "Catalogue gaps" in the report with its evidence and a
proposed row in the same columns as above. It is not a finding and does not change the verdict. If it would rate
Critical or High, say so in the first line of the report so it is not missed. Adding the row to this file is a change
to the skill, so it needs the owner's approval.

## Matrix file

`<review folder>/matrix.md` holds every cell of the review. Generate it in Step 4 of SKILL.md, with one block per row
and one line per applicable check, and update it as cells are settled:

```
# Review matrix: <target>    run id: <run id>
Expected cells: 214    Settled: 214    TODO: 0

## P0.S1  action, write  POST /api/v1/schools/{schoolId}/users/import/preview  (E3)
- P0.S1:SPD1  PASS  Run: journeyWalk review_step_duration{step:P0.S1} p95 = 420 ms <= 10 s
- P0.S1:TRU1  FAIL  Run: probes check "P0.S1:TRU1 no rows left" saw 37 rows -> finding F4
- P0.S1:NSP3  N/A   rule: "Action rows followed by a read on the same path" (no read follows)
- P0.S1:SPD2  UNVERIFIED  load run declined; settle with steadyLoad at 3 actors on P0.S1
```
