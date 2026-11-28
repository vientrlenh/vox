# Technical check catalogue

A review applies every check below to every matrix row the check covers. Nothing is sampled or capped, and a check
is skipped only when its own "Applies to" rule leaves the row out. The catalogue is the only source of findings. A
problem it does not cover goes to "Catalogue gaps" in the report (SKILL.md, Step 10) as a proposed new row, never into
Findings. That is how two reviews of the same context end with the same findings.

Check ids share one id space with the catalogue of `review-feature`
(`.claude/skills/review-feature/references/checks.md`). An id lives in one catalogue only and is never reused, so a
gap in the numbering is a check of the other skill: J1 to J6, SPD1 to SPD3, SPD7, TRU1 to TRU5, NSP1 to NSP3, NSP5 to
NSP7, SAF1 to SAF3, SAF5 and SAF8 are there.

## Terms

The words **write**, **protected value**, **reachable by a user**, **cell id** and **variants** mean what the
`review-feature` catalogue says in "Words with a fixed meaning". In this skill, `journey.md` and `performance-k6.md`
are the files of that name in `.claude/skills/review-feature/references/`, which both reviews share.

### Matrix rows

| Row | One per | Id |
|---|---|---|
| Review | review | `R` |
| Action step | action (A) on a path (SKILL.md, Step 2) | `P0.S2` |
| Wait step | wait (W) on a path | `P0.W1` |
| Entry point | entry point in the inventory | `E4` |
| Background path | row of the thread map (`references/concurrency.md`) | `B2` |
| Config key | key from `references/configuration.md`, section 1 | `K3` |
| Migration | migration that creates or changes a table of the target | `M1` |
| Test class | test class in the inventory | `T5` |
| Rule | `AGENTS.md` bullet (see AGT below) | `AGT-Database-3` |
| Decision | decision in the register (`references/decisions.md`, section 1) | `DEC4` |

A **write row** is a write action, an entry point that writes, or a background path that writes. **Shared state** is a
counter, a status, quota, balance, an "only one active" rule or a unique business key.

### Status of a cell

| Status | Meaning | Must carry |
|---|---|---|
| PASS | The pass criterion holds | Evidence |
| FAIL | The pass criterion does not hold | Evidence and a failure scenario: this input, interleaving or state leads to this wrong outcome |
| N/A | The check's "Applies to" rule leaves this row out | The rule, quoted |
| UNVERIFIED | The method could not run (declined, or not reproducible locally) and no `Static` method settles it | Exactly what would settle it |

Evidence is either `Run` (k6 scenario plus the check or metric and its value, a probe response, a test result, an
`EXPLAIN` plan, or command output) or `Static` (a `file:line`, config key or commit that leaves no doubt, or, for a
framework behaviour, the configuration metadata or docs of the version in use). Use `Static` only where the check's
method allows it. "Looks fine" is not evidence, and neither is a remembered fact about the code or a framework.

### Severity

Severity is fixed by the row. An "escalates" condition is part of the rule, not a judgement call, so never raise or
lower a severity by feel.

| Severity | Meaning |
|---|---|
| Critical | Security hole, data loss or corruption, or a wrong protected value |
| High | A caller is blocked, misled or exposed in a realistic case |
| Medium | Slower, harder or less safe than the context allows, or a risky behaviour without a test |
| Low | A maintainability or convention problem with no effect a caller can see |

## Data access and transactions

| Id | Check | Applies to | Method | PASS when | Severity on FAIL |
|---|---|---|---|---|---|
| SPD4 | Throughput holds | Action rows with an expected concurrency above 1 | Run: during the load run, `dropped_iterations` is 0, `hikaricp.connections.pending` (sampled every 5 s) stays 0, and the app log has no `RejectedExecutionException` or pool warning | All three hold | Medium. High when requests fail |
| SPD5 | SQL statements per request do not grow with the data | Action rows | Run: `statementCount` at the reference volume and at twice the child rows (`performance-k6.md`, section F) | Statements per request at 2x is at most the 1x figure plus 2 | Medium |
| SPD6 | Main queries are indexed | Action and wait rows that query a table that grows per school, student, session or attempt | Static: an index in `db/migration` covers each `WHERE`, `JOIN` and `ORDER BY` column. Run: `EXPLAIN (ANALYZE, BUFFERS)` at the reference volume (`performance-k6.md`, section G) | Indexed, and no `Seq Scan` with a filter on a table above 10,000 rows | Medium |
| SPD8 | No outside call inside a transaction | Write rows and wait rows | Static: from each `@Transactional` boundary, trace the calls to HTTP, gRPC, S3, SMTP, Firebase and Kafka sends | None inside a transaction | Medium. High when that transaction holds a row lock |
| TRU6 | Side effects follow the commit | Write rows and wait rows that publish events or send email or push | Static: outbox or `AFTER_COMMIT`. Run: the failing-write probe (an input that passes validation but breaks a constraint), then count outbox rows and messages | Nothing leaves before the commit or after a rollback | High |
| TRU8 | The database enforces the rules the code relies on | Entry point and background path rows that enforce a rule on stored data: a unique business key, "only one active", a non-negative amount, an allowed status transition, a required reference | Static: a constraint in `db/migration` (`UNIQUE`, a partial unique index, `CHECK`, `FOREIGN KEY`, `NOT NULL`) backs each rule, and its violation maps to the exception its meaning calls for (`ResourceDuplicatedException` for a duplicate, never a 500) | Every rule has a constraint and a mapping, or a decision row whose reason holds explains why not | Medium. High with a protected value |

## Concurrency and background work

| Id | Check | Applies to | Method | PASS when | Severity on FAIL |
|---|---|---|---|---|---|
| NSP4 | Background work runs once per item | Background path rows | Static: the thread map (`references/concurrency.md`): duplicate delivery, retries, and two instances when Step 1 says several or not decided | The path names its dedupe or claim mechanism and reaches every end state | High. Critical with a protected value |
| NSP8 | The mechanism behind a shared-state rule holds | Write rows on shared state | Static: name the lock, version, conditional update or constraint, then walk two callers through it (read, decide, write) on one instance and, when Step 1 says several or not decided, on two. Run: `racingRequests` and the invariant in SQL (`performance-k6.md`, section I) support it | The mechanism is named and no interleaving breaks the invariant | Critical with a protected value. High otherwise |

## Security internals

| Id | Check | Applies to | Method | PASS when | Severity on FAIL |
|---|---|---|---|---|---|
| SAF4 | Hostile input is contained | Entry point rows that take input | Run: a body and a file above `spring.servlet.multipart.max-file-size`, a row count above the stated limit, 10,000-character strings, SQL metacharacters in filters and sort fields, a deeply nested or heavily aliased GraphQL query, an xlsx zip bomb, an xlsx with an external entity, and exported cells starting with `=`, `+`, `-` or `@` (`references/security.md`, part A) | Refused with a 4xx within the budget of `journey.md`, section 5, no 500, no hang, and formulas escaped in exports | High. Critical when an injection or the external entity works |
| SAF6 | Logs keep secrets and PII out | Review row | Run: after all runs, count the matches in the app output for the run's password, JWTs (`eyJ`), refresh tokens, OTPs, seeded emails (`perf.invalid`) and seeded names (the run id), printing counts only (`performance-k6.md`, section L). Static: log statements in the inventory that print a whole object, a request body or a token | Zero for each count, and no such log statement | High |
| SAF7 | Errors reveal no internals | Entry point rows | Run: the error responses from the SAF4 probes and every other error response of the runs | No stack trace, SQL, class or package name | Medium |
| SAF9 | Costly actions are limited | Entry point rows that send email or OTP, call AI grading, upload to storage or move a protected value | Static: a rate limit or quota per actor. Run: a burst of 20 requests in 10 s | A limit exists and holds | Medium. High for login and OTP |
| SAF11 | Money moves correctly | Entry point rows that move money (orders, payments, PayOS webhooks, balance) | Static and Run (`references/security.md`, part B) | The signature is checked first, the amount is checked against the order, repeats are idempotent, and the state transition is guarded | Critical |
| SAF12 | Internal callers are authenticated and their payload is checked | Entry point rows of internal REST endpoints (`interfaces/rest/controller/internal`), gRPC services and Kafka consumers | Static: how the caller is authenticated (a service token, mTLS, or network isolation shown in the deployment config), and whether each write checks the ids and state in the payload against stored data before acting (a score is applied only to a session that awaits grading) | The authentication is named with evidence, and no write trusts a payload blindly | High. Critical with a protected value |
| SAF13 | Tokens and OTPs are checked, rotated and revoked | Entry point rows that issue, refresh, revoke or check a token or an OTP | Static: the JWT signature, algorithm and expiry are verified; a refresh token is rotated on use and a reused one is refused; logout revokes; an OTP expires, has an attempt limit and is compared in constant time. Run: the refresh-after-logout check of `sessionContinuity`, when it ran | All hold | Critical |
| SAF14 | Tokens never travel in URLs | Entry point rows that issue a token or redirect after a login | Static: no access token, refresh token, OTP or password set-up token in a query string or path (redirect targets such as the OAuth2 success redirect, links in emails); use a fragment, a cookie or a one-time code exchanged by POST. Run: follow the redirect of the login probes and check its `Location` | No token in any URL | Medium. High for a long-lived token |
| SAF15 | The client IP comes only from a trusted proxy | Entry point rows that record or act on the client IP (sessions, audit, rate limits) | Static: `X-Forwarded-For` / `X-Real-IP` are read only when `server.forward-headers-strategy` and the proxy in front overwrite them, and no code reads them by hand. Run: send a spoofed `X-Forwarded-For` and read the stored IP | The stored IP is the proxy-reported one | Low. Medium when the IP drives a rate limit or an allow-list |
| SAF16 | API metadata is not public unless intended | Config key rows that expose a schema or service catalogue (GraphQL introspection, GraphiQL, gRPC reflection, OpenAPI docs) | Static: the key and the paths `SecurityConfig` permits; the intent recorded in git, a tracked file or the engineer's answer. Run: anonymous request for the schema | Anonymous access matches the recorded intent | Low |
| SAF17 | Key material is derived the same way on every JVM | Code that turns a configured secret into a key (HMAC, AES) | Static: `getBytes` with an explicit charset (UTF-8), the same derivation for every key of the same kind | Explicit and consistent | Low |

## Library CVEs

| Id | Check | Applies to | Method | PASS when | Severity on FAIL |
|---|---|---|---|---|---|
| SAF10 | No exploitable known CVE in the libraries | Review row | Run: dependency-check and triage (`references/security.md`, part C) | No real CVE on the runtime classpath | Per CVE, by the rule in `references/security.md` part C, with one finding per CVE |

## Configuration

These come from the configuration lenses of the `feature-config-review` plugin. They apply to config key rows, which
exist for every target (`references/configuration.md`, section 1).

| Id | Check | Applies to | Method | PASS when | Severity on FAIL |
|---|---|---|---|---|---|
| CFG1 | The effective value is the intended one | Config key rows | Static: resolve the chain for local runs, tests, docker-compose and CI (`references/configuration.md`, section 2), with the defaults read for the version in use (section 3) | The value that applies is the one the target needs | Medium |
| CFG2 | Units are right | Config key rows that hold a time or a size | Static: the unit the reader expects (`ms` or `s`, MB or MiB, `Duration` syntax) | They match | Medium |
| CFG3 | Defined and read | Config key rows | Static: every key that is read (`@Value`, `@ConfigurationProperties`, `Environment`) is defined or has a safe default, and every key that is defined is read (a typo is silently ignored) | Both hold | Low. Medium when a typo leaves a limit unset |
| CFG4 | Limits agree with each other | Config key rows that form a pair (timeouts, pool sizes, leases, lifetimes, upload limits) | Static: a client timeout under the upstream timeout; Kafka listener concurrency, executor pools and `hikari.maximum-pool-size` under Postgres `max_connections`; a lease longer than the longest job; the token lifetime against the longest wait; the multipart limit against the reference input | All consistent | Medium |
| CFG5 | The same meaning in every environment | Config key rows | Static: `application.yaml`, `application-test.yaml`, `docker-compose.yml`, `Dockerfile`, the CI workflows and the README env table | Every difference is intended and says why | Low |
| CFG6 | No secret or insecure default | Config key rows | Static (`references/configuration.md`, section 4): a real secret only through `${ENV}` with no default. An insecure default (actuator exposure, SSL off, wide CORS, `cookie.secure: false`) is judged only after checking what `SecurityConfig` protects | None | High for a real secret or an unprotected sensitive endpoint. Medium otherwise |
| CFG7 | Versions are pinned | Config key rows for images, build plugins and CI actions | Static | A fixed version, never `latest` | Low |
| CFG8 | A wrong value can be undone | Config key rows | Static: whether changing it needs a migration, a data fix or a coordinated deploy | Reverting is a redeploy of the previous value | Medium |

## Migrations

| Id | Check | Applies to | Method | PASS when | Severity on FAIL |
|---|---|---|---|---|---|
| MIG1 | A migration does not block the table it changes | Migration rows that change a table that grows per school, student, session or attempt | Static: for each statement, the lock it takes and whether its work grows with the rows. A column type change, or a column added with a volatile default, rewrites the table under an `ACCESS EXCLUSIVE` lock; `SET NOT NULL` scans the table unless a validated `CHECK (column IS NOT NULL)` exists; `CREATE INDEX` without `CONCURRENTLY` blocks writes; a new `FOREIGN KEY` or `CHECK` validates every row unless it is added `NOT VALID` and validated later. Read the Postgres 18 and Flyway docs for the version in use before proposing a fix | No statement blocks writes while doing work that grows with the rows, or a plan under `docs/` names the maintenance window | Medium |

## Build gate, tests and AGENTS.md

| Id | Check | Applies to | Method | PASS when | Severity on FAIL |
|---|---|---|---|---|---|
| GATE1 | It compiles | Review row | `./gradlew compileJava compileTestJava` | Pass | High |
| GATE2 | The target's tests pass | Review row | `./gradlew test --tests '*<Feature>*'`, and `./gradlew integrationTest --tests '*<Feature>*'` when Docker runs | Pass. A failure that also happens on the default branch is listed as pre-existing and is not this cell's FAIL | High |
| GATE3 | The guardrails pass | Review row | `./gradlew test --tests '*LayeredArchitectureTests' --tests '*ConventionTests' --tests '*TestSuiteSeparationTests'` | Pass | Medium |
| TRU7 | Behaviour is protected by a test | Action, wait and background path rows | Static: for each behaviour a check exercised on this row (PASS or FAIL) in this review, or in a `review-feature` matrix of the same target in the scratchpad's review folder, the test that fails if it breaks | Every behaviour has one | Medium. High when the untested behaviour belongs to a SAF check, a protected value, NSP2 or NSP8 |
| TST1 | Tests assert the outcome | Test class rows | Static | Every test asserts a state, a response body or an emitted event, not only "no exception" | Low |
| TST2 | Tests are deterministic | Test class rows | Static | No `Thread.sleep` coordination, no dependence on test order or the machine timezone, and no data shared between tests | Medium |
| TST3 | The test level fits the risk | Test class rows that cover SQL, locking, races or auditing | Static | They run against the real Postgres (`@RepositoryTest` or `@IntegrationTest`). A mocked repository does not count | Medium |
| TRU9 | Security gates are protected by a test | Entry point rows that are gates: a filter chain, servlet filter or interceptor that authenticates or authorizes | Static: a test that drives the real `SecurityConfig` chain or the real filter (not `WebMvcTestSecurityConfiguration`) and fails if the gate answers wrongly: 401 without credentials, 403 with the wrong ones, success with the right ones, and an encoded or non-normalized path (`%73`, `//`, `;`) for every path-matched gate | Every gate has one | High |
| GATE4 | No unused code in the target | Review row | Static: `./gradlew compileJava compileTestJava` warnings with `-Xlint`, then a search for each private member, parameter and import of the inventory files that has no reader; an exemption that only exists because of unused code counts | None found | Low |

### AGT: AGENTS.md audit

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

## Decisions

The method is in `references/decisions.md`. These checks apply to decision rows only.

| Id | Check | Applies to | Method | PASS when | Severity on FAIL |
|---|---|---|---|---|---|
| DEC1 | The reason is known and written down | Decision rows | Static: `references/decisions.md`, section 2 (a default, an `AGENTS.md` rule, git, docs, a pull request); otherwise the engineer's answer (section 3) | The decision is self-evident, or its reason is recorded in git or in a tracked file | Low when only the engineer's answer gives the reason. Medium when nobody can say why. All Low rows form one finding and all Medium rows another, because each group has one root cause: the missing record |
| DEC2 | The reason's premises are true | Decision rows whose reason came from git, a tracked file, a pull request or the engineer (not the self-evident ones) | Static or Run, per premise (`references/decisions.md`, section 4) | Every premise is confirmed | Joins the finding of the cell that fails because of the false premise. Low when no cell fails |
| DEC3 | The decision survives its counter-scenarios | Decision rows whose kind lists stressors (`references/decisions.md`, section 1) | `references/decisions.md`, section 5: every stressor the decision kind lists, within the Step 1 context | Every outcome stays correct, or fails visibly and recoverably within the worst case from config | Joins the finding of the governed check whose pass criterion the scenario breaks. Otherwise by the Severity table: lost or corrupted data or a wrong protected value is Critical, stuck or blocked work is High, slower or harder work is Medium |

## Catalogue gaps

When you see a problem that no row covers, write it under "Catalogue gaps" in the report with its evidence and a
proposed row in the same columns as above. It is not a finding and does not change the verdict. If it would rate
Critical or High, say so in the first line of the report so it is not missed. Adding the row to this file is a change
to the skill, so it needs the owner's approval. A problem the user would meet on the journey belongs to
`review-feature`: list it under "Outside the target" with that pointer.

## Matrix file

`<review folder>/matrix.md` holds every cell of the review. Generate it in Step 4 of SKILL.md, with the context record,
one block per row and one line per applicable check, and update it as cells are settled:

```
# Technical review matrix: <target>    run id: <run id>
Context: scope <...>; why now <...>; runs as <...>; load and data <...>
Expected cells: 187    Settled: 187    TODO: 0

## P0.S2  action, write  POST /api/v1/schools/{schoolId}/users/import/{sessionId}/accept  (E4)
- P0.S2:SPD5  FAIL  Run: statementCount 14.0 per request at 1x, 26.0 at 2x -> finding F3
- P0.S2:SPD8  PASS  Static: <UseCase>.java:48, the transaction holds no outside call

## DEC4  executor  fileImportExecutor: 2 threads, no queue, AbortPolicy  (AsyncConfig.java:31)
- DEC4:DEC1  FAIL  engineer only (see decisions.md DEC4) -> finding F9
- DEC4:DEC2  PASS  Run: <the measurement that confirms each premise>
- DEC4:DEC3  UNVERIFIED  load run declined; settle with burst at 3 admins on P0.S2
```
