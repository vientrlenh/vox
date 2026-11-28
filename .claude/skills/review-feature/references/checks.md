# Check catalogue

A review applies every check below to every matrix row the check covers. Nothing is sampled or capped, and a check
is skipped only when its own "Applies to" rule leaves the row out. The catalogue is the only source of findings. A
problem it does not cover goes to "Catalogue gaps" in the report (SKILL.md, Step 10) as a proposed new row, never into
Findings. That is how two reviews of the same context end with the same findings.

Every check here judges something the user does, waits for, sees or is protected from. Check ids share one id space
with the catalogue of `review-technical` (`.claude/skills/review-technical/references/checks.md`). An id lives in one
catalogue only and is never reused, so a gap in the numbering is a check of the other skill: SPD4, SPD5, SPD6, SPD8,
TRU6 to TRU8, NSP4, NSP8, SAF4, SAF6, SAF7, SAF9 to SAF13, and the CFG, MIG, GATE, TST, AGT and DEC rows are there.

## Terms

### Matrix rows

| Row | One per | Id |
|---|---|---|
| Path | path in the confirmed journey (`references/journey.md`) | `P0`, `P1`, ... |
| Action step | action (A) on a path | `P0.S2` |
| Wait step | wait (W) on a path | `P0.W1` |
| Entry point | entry point in the inventory, on or off a path | `E4` |

### Words with a fixed meaning

- **Write**: an action or entry point that changes a row, publishes an event, sends a message or calls an external
  service. Everything else is a **read**. Classify by effect, not by HTTP method.
- **Protected value**: money (orders, payments, PayOS, school balance, quota, subscription) or grades (scores,
  results, appeals). Some checks rate higher when one is involved.
- **Reachable by a user**: a REST endpoint or a GraphQL operation or field. Internal endpoints
  (`interfaces/rest/controller/internal`), gRPC services and Kafka consumers are called by other systems, and their
  callers are `review-technical`'s (SAF12).
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

Evidence is either `Run` (k6 scenario plus the check or metric and its value, a probe response, or command output)
or `Static` (a `file:line` or config key that leaves no doubt). Use `Static` only where the check's method allows it.
"Looks fine" is not evidence, and neither is a remembered fact about the code.

### Severity

Severity is fixed by the row. An "escalates" condition is part of the rule, not a judgement call, so never raise or
lower a severity by feel.

| Severity | Meaning |
|---|---|
| Critical | Someone gets at data or actions that are not theirs, data is lost or corrupted, or a protected value is wrong |
| High | The user is blocked, misled or exposed in a realistic case |
| Medium | Slower, harder or less safe than the target allows |
| Low | Extra effort the user can live with: a needless step, a value asked twice, an entry point nobody uses |

## J: Journey

| Id | Check | Applies to | Method | PASS when | Severity on FAIL |
|---|---|---|---|---|---|
| J1 | The path reaches its end | Path rows | Run: `journeyWalk` on the path, with check `<path>:J1` on its end (the success signal for paths that end at the target) | The end is observed in every iteration | High |
| J2 | Every action is needed | Path rows | Static, from the journey table: what uses each action's output, and which values each action asks for | Every action's output is used by a later step or the success signal, and no value is asked for twice | Low |
| J3 | Time to target fits the owner's expectation | `P0`, only when the owner stated a time | Run: system time to target, p95 over the walk iterations. Static: the worst case from config | Both within the stated time | Medium. High when the worst case misses a deadline the target depends on (the start of an assessment, for example) |
| J4 | Refusals come at the earliest step | Path rows, once per refusal the path can produce (rule limit, quota, permission, deadline, duplicate) | Static: the step where the rule is checked, and the earliest step whose input already decides it. Run: the probe that triggers it | The refusal fires at that earliest step | Medium |
| J5 | The user can leave and come back | Path rows with a wait over 10 s or at least 3 actions | Run: after each action and during each wait, a fresh login of the same user finds the item and its current status through the API | Found, with the current status, at every point | Medium |
| J6 | An off-path entry point has a user | Entry point rows that are on no path | Static: the user or other journey that needs it | One is named | Low |

## SPD: Speed (step times, waits and the crowd)

| Id | Check | Applies to | Method | PASS when | Severity on FAIL |
|---|---|---|---|---|---|
| SPD1 | Step time for one user | Action rows | Run: `journeyWalk`, `review_step_duration{step:<id>}` p95 | Within the step's budget (`references/journey.md`, section 5) | Medium. High above 3x the budget |
| SPD2 | Step time at the expected concurrency | Action rows | Run: `steadyLoad` or `burst` on the step, at the expected concurrency (`references/journey.md`, section 6) | p95 within the budget and `http_req_failed` under 1% | Medium. High above 3x the budget, or at 1% errors or more |
| SPD3 | Wait time and its feedback | Wait rows | Run: `journeyWalk`, the measured max. Static: the worst case from config, and the feedback the user gets | Measured max and worst case within the wait's band, and the band's feedback exists | Medium. High when the worst case has no bound |
| SPD7 | Lists and results come in a size the user can handle | Read actions that return lists, and wait rows that produce results | Static: paged with `PageArguments` or capped by a stated limit. Run: the response size at the reference volume | Bounded, and at most 1 MB at the reference volume | Medium |

## TRU: Trust (what the user can rely on)

| Id | Check | Applies to | Method | PASS when | Severity on FAIL |
|---|---|---|---|---|---|
| TRU1 | All or nothing | Write actions and wait rows that change more than one row or table | Run: make the step fail at its last write (an input that passes validation but breaks a constraint, or a last item that is invalid), then count the rows it touched, outbox included | No row from the failed attempt remains | High. Critical with a protected value |
| TRU2 | Safe to repeat | Write actions | Run: the same request twice in a row, and twice at the same moment | One effect, or the second is refused with 409 or 422 | High. Critical with a protected value |
| TRU3 | A mistake the user can fix gets a message that says what to fix | Action rows, once per input field and once per file rule | Run: missing, empty, too long, wrong type, out of range and unknown enum values. For files: wrong type, empty, header only, missing column, extra column, duplicate rows, bad date | 400 or 422 naming the field (row and column for files), with the state unchanged | High for a 500. Medium for a message that does not name the field |
| TRU4 | What goes in comes out | Write actions that store text, dates, numbers or codes | Run: fixtures with Vietnamese names in NFC and NFD (`Nguyễn Thị Ánh`), leading zeros (`0901234567`, `00123`), a time one minute before and one after midnight in Asia/Ho_Chi_Minh, a score of `7.25`, and text at the column's maximum length. Read them back through the API | Read back equal to what was sent (NFD may come back as NFC, if it always does) | High |
| TRU5 | Background failures end visibly | Wait rows | Run: an input that makes the job fail, and the switch-off test when approved. Static: what sets the failed status for each way the work can end (exception in the handler, pool rejection, lease expiry, retries exhausted, shutdown) | Every way ends in a terminal status with a reason the user can read, within the stated worst case, and the user can retry | High when a status can stay in progress forever. Medium when the failed status gives no reason |

## NSP: No surprises (predictable state)

| Id | Check | Applies to | Method | PASS when | Severity on FAIL |
|---|---|---|---|---|---|
| NSP1 | Actions in the wrong state are refused | Write actions, once per state of the resource in which the action is not allowed (from the status enum) | Run: drive the resource into the state, then send the action | 409 or 422 (GraphQL: `BAD_REQUEST`) with a message, and the state unchanged | High |
| NSP2 | Two people doing the same thing at once | Write actions on shared state (counters, statuses, quota, balance, "only one active" rules, unique business keys) | Run: `racingRequests`, then the invariant in SQL (`references/performance-k6.md`, section I) | The invariant holds in the run. The report says that a held invariant is not proof; `review-technical` NSP8 names the mechanism | Critical with a protected value. High otherwise |
| NSP3 | A read sees the write that just succeeded | Action rows followed by a read on the same path | Run: `journeyWalk` reads right after the write | Every iteration sees it | Medium |
| NSP5 | Time rules flip exactly on time | Path rows, once per time rule (deadline, window, expiry, "today") | Run: one second before, at and one second after the boundary, or a test with a fixed clock in Asia/Ho_Chi_Minh | The outcome changes exactly at the boundary | High |
| NSP6 | Lists are stable | Read actions that page | Static: `ORDER BY` ends with a unique column. Run: page through and compare with the total | No duplicates or gaps, and the total matches the content | Medium |
| NSP7 | Statuses and counters tell the truth | Path rows, once per status or counter the user sees | Run: after the walk, compare with SQL counts | Equal | High |

## SAF: Safety as the user sees it (access, privacy and staying logged in)

| Id | Check | Applies to | Method | PASS when | Severity on FAIL |
|---|---|---|---|---|---|
| SAF1 | No access without login | Entry point rows reachable by a user, except the paths in `SecurityConfig` `PERMITTED_PATTERNS` that are public by design (quote the reason; `/graphql` is listed there but its operations are not public) | Run: no token | 401 (GraphQL: `UNAUTHORIZED`, no data) | Critical |
| SAF2 | A wrong role is refused | Entry point rows reachable by a user, once per role that is not allowed | Run: a valid body with that role's token | 403 (GraphQL: `FORBIDDEN`, no data) | Critical |
| SAF3 | Another school or another owner is refused | Entry point rows reachable by a user that take an id or return school- or owner-scoped data | Run: the allowed role of school B on school A's ids, and student A2 on student A1's data, for reads, writes and GraphQL nested fields | 403 or 404, and no data of A in the body | Critical |
| SAF5 | Responses show only what the user may see | Entry point rows that return data | Run: every response body from the walk and the probes | No password hash, token, OTP, other school's or other student's data, or internal-only field | High. Critical for credentials |
| SAF8 | The session lasts the journey | Path rows whose worst case, or any one step or wait, can last longer than the access-token lifetime (`jwt.expiration-ms`, 15 minutes by default) | Run: `sessionContinuity` against an app started with a short `JWT_EXPIRATION_MS`: the token expires before the longest step, the user refreshes and goes on; then a logout ends the session | The user goes on after the refresh and loses nothing, and the logged-out session is refused | High |

## Catalogue gaps

When you see a problem the user would meet that no row covers, write it under "Catalogue gaps" in the report with its
evidence and a proposed row in the same columns as above. It is not a finding and does not change the verdict. If it
would rate Critical or High, say so in the first line of the report so it is not missed. Adding the row to this file
is a change to the skill, so it needs the owner's approval. A problem that only an engineer can see is not a gap here:
it goes under "For the technical review".

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
- P0.S1:SPD2  UNVERIFIED  load run declined; settle with steadyLoad at 3 users on P0.S1
```
