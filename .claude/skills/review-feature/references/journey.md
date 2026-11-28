# Journey: from the owner's answers to measured steps

The journey is the yardstick of the review. It is built from the owner's two answers, every use case of the feature
is placed on it, and the owner confirms it before anything is measured. That confirmation is what makes two reviews
of the same feature look at the same steps.

## 1. Intent record

Fill it in from the answers in SKILL.md, Step 1. Keep the owner's words.

| Field | Value |
|---|---|
| Wants to | Answer 2, verbatim |
| Target | Answer 3, verbatim |
| Success signal | The observable state that proves the target, for example "every student in the file can log in, and `myClass` returns their class". J1 checks this |
| Actors | Answer 1, the roles that act: `SYSTEM_ADMIN`, `SCHOOL_ADMIN`, `TEACHER`, `STUDENT` |
| Start state | What exists before the first step ("the school exists, the admin is logged in, an Excel file with 450 students") |
| Time expectation | The owner's words, or "default budgets" (section 5) |
| Expected concurrency | The owner's figure, or the default (section 6) |
| Reference input | The realistic size of what the actor brings (rows, MB, answers per test): the owner's figure, or the default (section 6) |

If the success signal can be read from the target in two ways, ask the owner which one with `AskUserQuestion`. Do not
pick.

## 2. Entry point inventory

Find every entry point of the feature, for all of its use cases at once. Search by the names the feature uses (use
case, model, table, endpoint path, GraphQL type, topic, config key). The search is for finding things, not for
judging them. `review-technical` builds its inventory from this section too, and adds what it needs on top.

- REST: `interfaces/rest/controller/**`, including `internal/` (`@GetMapping`, `@PostMapping`, ...)
- GraphQL: `src/main/resources/graphql/*.graphqls` (queries, mutations, type fields) and `interfaces/graphql/**`
  (`@QueryMapping`, `@MutationMapping`, `@SchemaMapping`, `@BatchMapping`)
- gRPC: `interfaces/grpc/**`
- Kafka: `interfaces/kafka/consumer/**` and `infrastructure/event/internal/consumer/**`, plus every topic the
  feature publishes through the outbox (`@ExternalEventTopic`)
- Background: `infrastructure/worker/**` (`@Scheduled`), executors in `infrastructure/config/AsyncConfig`, `@Async`,
  `@EventListener` and `@TransactionalEventListener`
- Config values that set a pick-up delay, limit, lifetime or interval for any of the above (`application.yaml`, and
  the classes that read them with `@Value` or `@ConfigurationProperties`). This review only reads them to work out the
  worst-case waits (section 4); whether they are right is `review-technical`'s question

One row per entry point:

| Id | Kind | Entry point (method and path, operation, topic or job) | Roles (`@PreAuthorize`) | Use case | Read or write | `file:line` |
|---|---|---|---|---|---|---|

Read the files of the inventory in full, plus one hop out: the callers of each entry point and the callees that do
I/O. Bugs usually live where the feature meets the code around it.

## 3. Steps and paths

### What is a step

- **Action (A)**: one thing the actor does on purpose: submit a form, upload a file, confirm, or open something they
  need to read before they can decide. List every request the API needs for it, in order. An action that needs
  several requests in a row is still one action, and its requests are listed.
- **Wait (W)**: time in which the actor cannot go on because the system is still working: a background job, a
  scheduler pick-up, a Kafka round trip, AI grading, an email or OTP on its way. A wait starts when the request that
  triggers it returns, and ends at something the actor can observe (a status change, a notification, a row that
  appears).
- **Not steps**: requests the client sends on its own (token refresh, status polling, which belong to the wait), and
  human thinking time, which the review does not measure.
- A step ends when the next step becomes possible.

### Paths

- **P0, the main path**: from the start state to the success signal, with a reference input that has no errors.
- **Alternate paths that the feature itself offers**, each starting from the step where it branches: fix and retry
  after a refusal (invalid input, a rule limit), cancel, reject or undo, expiry (something left too long), resume
  after leaving, and every variant of a step (other import types, other assessment kinds). Number them `P1`, `P2`,
  and so on.
- Paths do not include attacks; hostile input is `review-technical`'s (SAF4). The Safety checks of this skill cover
  every entry point a user reaches, on a path or not.
- Every entry point in the inventory is on at least one path, or on the off-path list with the reason (J6).

### Counting

The report gives these figures:

- **Steps to the target**: the number of actions on P0. Also give the number of waits and of requests.
- **Per alternate path**: the extra actions and waits compared with P0.
- **Wasted effort**: for each refusal the feature can produce, the step where it fires and the earliest step whose
  input already decides it (J4).

## 4. Timing

| Step | What its time is |
|---|---|
| Action | The server time of its requests in sequence. p50 and p95 from `journeyWalk` (one actor), and p95 under the expected concurrency from the load run |
| Wait | From the response that triggers it to the moment the next step is possible. `journeyWalk` polls the status every second, so the figure is accurate to one second; say so |

Split each wait into its parts. The worst case comes from config (Static) and is reported next to the measured value:

| Part | Where it comes from |
|---|---|
| Pick-up delay | The `fixedDelay`, `fixedRate` or cron of the job that picks the work up (`ImportJobDispatcher.poll` runs every 5 s, for example) |
| Queue time | A busy executor: pool size and queue capacity in `AsyncConfig` (`fileImportExecutor` has 2 threads and no queue) |
| Processing | The work itself, at the reference input |
| Retry chain | Lease, sweeper interval and maximum attempts when the work fails or the app dies (`ImportJobDispatcher`: a 5-minute lease, a sweeper every 60 s, 3 attempts) |
| Relay | The outbox relay (`OutboxEventScheduler`, every second) and consumer lag |
| Outside the repository | AI grading, `vox-streaming`, PayOS, SMTP. Measure it only if it runs locally. Otherwise the cell is UNVERIFIED, and the report states the contract that was assumed |

**System time to target** is the sum of the action times and the wait times on P0. Human time is not included, and
the report says so.

## 5. Budgets: is it too long?

The owner's time expectation wins. Without one, use these defaults. They follow the classic response-time limits:
0.1 s feels instant, 1 s keeps the flow of thought, and 10 s is the limit of attention.

| Step | Budget |
|---|---|
| Action that reads (a list, a detail, a preview opened again) | p95 at most 500 ms |
| Action that writes (submit, confirm, accept) | p95 at most 1 s |
| Action that uploads and parses a file of the reference size | p95 at most 10 s. Longer work must become a wait with a status |
| Wait the actor watches on the same screen | At most 10 s |
| Wait with a progress status the actor can see (done out of total) | At most 60 s |
| Wait the actor can leave, being told when it ends (a notification, or the result waiting where they come back) | It must end before the target needs it (the owner's deadline). Without a deadline, the worst case only has to be bounded and stated |

A step is **too long** when its measured p95 (actions) or measured max (waits) is above its budget, or when a wait
lacks the feedback its band requires: no status for a wait over 10 s, or no notification and no findable result for a
wait over 60 s. The report gives the measured value next to the budget for every step. SPD1, SPD2 and SPD3 use these
budgets.

## 6. Defaults for the crowd and the input

Use these only when the owner gave no figure, and state them in the Step 3 question so the owner can correct them.

| Actor and step | Expected concurrency |
|---|---|
| `STUDENT` in a class test or practice | 45 at once (one class) |
| `STUDENT` in a centralized assessment | 450 at once (ten classes) |
| `TEACHER` | 10 |
| `SCHOOL_ADMIN` | 3 |
| `SYSTEM_ADMIN` | 1 |
| Background work | What one school produces at once, for example 45 sessions to grade after a class test |

- "At once" for a step the crowd starts together (the start of an assessment, a submit at the deadline) means all
  of them within 10 s: the `burst` scenario. Other steps arrive at a steady rate of the crowd size per minute: the
  `steadyLoad` scenario.
- Reference input: 500 rows for an import (one grade with some margin), and the largest file the multipart limit
  allows (`spring.servlet.multipart.max-file-size`, 5 MB at the time of writing) for the hostile-input probes of
  `review-technical` (SAF4).

## 7. Journey table

Keep the full table in `matrix.md`:

| Step | Kind | Actor action or system work, in plain words | Requests (entry point ids) | Next step possible when | Budget | Worst case from config |
|---|---|---|---|---|---|---|

The owner sees it in plain words, in the Step 3 preview and in the report. Drop the Requests column, write the kind as
"does" or "waits", and give the times in seconds or minutes:

| Step | Does or waits | What the user does, or what the system is doing | Can go on when | Allowed time | Longest it can take |
|---|---|---|---|---|---|

## 8. Example: school user import

This example only shows the shape of a journey. Build the real one from the inventory every time.

- Wants to: "As a school admin, I want to put the student list I already have in Excel into Vox."
- Target: "On the first day every student can log in and is already in the right class."

P0:

| Step | Kind | Work | Requests | Next step possible when |
|---|---|---|---|---|
| P0.S1 | A | Upload the file and look at the preview | `POST /api/v1/schools/{schoolId}/users/import/preview` (multipart) | The session is `PREVIEWED` with the suggested mapping |
| P0.S2 | A | Confirm the mapping and accept | `POST /api/v1/schools/{schoolId}/users/import/{sessionId}/accept` | The session is `QUEUED` |
| P0.W1 | W | The import runs | `importSession(id)` polled; picked up by `ImportJobDispatcher.poll` | The session is `COMPLETED` |
| P0.S3 | A | Check the result | `importSession(id)` | `importedRows` is shown |

The target says "in the right class", so P0 does not end at P0.S3: it goes on with the class membership import
(`/{schoolId}/classes/users/import/...`) before the success signal can hold. How an imported student logs in for
the first time is also part of P0. Find it in the inventory; do not assume it. This is why the target question comes
first: it shows that the journey is longer than the feature the owner named.

Alternate paths: P1, a file with invalid rows (preview errors, `importRows(sessionId, status: INVALID)`, fix the file,
upload again); P2, reject the session (`POST /api/v1/imports/{sessionId}/reject`); P3, a previewed session left until
it is `EXPIRED`; P4 and on, the other `ImportType` variants.
