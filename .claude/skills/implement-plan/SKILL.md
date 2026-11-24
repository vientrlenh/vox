---
name: implement-plan
description: Implement an agreed plan or task step by step, safely and in scope. Use when asked to "implement the plan", "build this", "execute the plan", or after a plan is approved and coding starts. Gates on AGENTS.md boundaries and plan approval, asks open decisions in chat, works in small verified slices layer by layer, keeps the status of the plan, its phases and jobs, and the user's answers current in the plan's HTML file, reuses earlier answers instead of asking the same question again in later phases or jobs, handles deviations.
---

# Implement a plan

Purpose: turn an agreed plan into working code without drifting from it. The plan is the contract. This skill covers
the work between "plan approved" and "ready to verify".

Rules for the whole skill:

- Do what the plan says and no more. Unrequested refactors, renames, reformatting and cleanup are out of scope.
  Report problems you notice instead of fixing them (AGENTS.md, Scope).
- One slice at a time. Compile after every slice, so a failure points at the last small change.
- Never guess a pattern. If the pattern is ambiguous, ask.
- Only commit or push when the user asks.
- Every open decision goes to the user before implementation (see "Decisions: ask before implementing").
- Never ask what the user already answered in this plan. Check the recorded answers first (see "Reuse answered
  decisions").

## Decisions: ask before implementing

The user reviews plans as HTML in a browser and cannot edit that file there. So an "Open questions", "Options",
"TBD" or "Decision needed" block in the plan can never be answered inside the plan. The only way to get an answer is
to ask in the chat with the `AskUserQuestion` tool. Never assume an answer, never pick silently, and never tell the
user to edit the plan file.

What counts as a decision the user must answer:

- an open question, option list, "TBD", "?" or "A or B" left in the plan;
- anything on the AGENTS.md "Ask first" list, or outside what `AGENTS.md` covers (see Step 1);
- an ambiguous pattern: two existing files disagree on how to do it;
- a choice that changes behavior, a public contract, data, naming that leaves the codebase, or money-related logic;
- a plan gap or impossible item found while working (see Step 8).

What does not count: choices that have a conventional default or that you can verify in the code. Pick those,
and list them under "Decisions made by default" in the final report.

How to ask:

0. Check first. Run "Reuse answered decisions" below before every question. Ask only what no recorded answer covers.
1. Collect first. Before the first edit, scan the whole plan for open decisions and gather all of them. Merge
   questions on the same topic that come from different phases or jobs into one question, and ask it once with the
   scope that covers all of them. Ask them in one batch, so the user is not interrupted again and again.
   `AskUserQuestion` takes up to 4 questions per call; use more calls if needed, but finish asking before you write
   code.
2. Make each question stand alone. The user may see only the dialog, so name the item, say why it matters, and do not
   write "the plan" or "as above".
3. Give 2-4 distinct options with the trade-off of each. Put your recommendation first and end its label with
   "(Recommended)". Use `preview` when options are code, schema or layout the user must compare. The user can
   always type their own answer through "Other".
4. Do not start any item that depends on an unanswered decision. Do not start implementing at all until the
   up-front batch is answered.
5. A decision found mid-work: stop that item, ask the same way, and continue only after the answer. Items that do
   not depend on it may continue.
6. Apply the answer exactly. If a free-text answer conflicts with `AGENTS.md`, say which rule, and ask again.
7. Write every answer into the plan file right away (see "Tracking status in the plan file"), with the date, its
   scope and the user's own words if they typed them. Also list it in the final report under "Decisions from you". An
   answer applies to this plan only, never to later tasks, and inside the plan it follows its recorded scope.

## Reuse answered decisions

The plan file is the memory of what the user already decided: every `answered` block under Decisions, both the
plan's own (`d1`, `d2`, ...) and the ones you added while implementing (`a1`, `a2`, ...). A conversation that was
cleared or resumed has lost those answers, so read the file, not your memory. If the plan is not a file, keep the
same list in the task list.

Before every `AskUserQuestion` (the up-front batch, a mid-work question, a deviation):

1. Load the answers: `grep -o 'id="[da][0-9]*" data-decision-state="answered"' docs/<name>-plan/index.html`, then
   read those blocks. Do it at Step 1, at the start of every phase, and before any mid-work question.
2. Match by topic, not by wording. An answer covers the new question when it settles the same choice (for example
   where mappers live, which existing feature is the pattern, how a class is named) and its scope includes the
   current job: `plan` covers every phase and job, `phase-N` covers the jobs of that phase, `job-N-M` covers only
   that job. An answered `dN` has scope `plan` unless its text says otherwise.
3. Covered: do not ask. Apply the answer, write `applies A1` (or `D2`) in the job's status log, and list it under
   "Decisions reused" in the final report. Add the job id to the block's "Reused by" list.
4. Partly covered, or the new case differs in a way the answer did not consider (another layer, a public contract,
   money, security, data): ask, but quote the earlier answer and ask only about the difference.
5. A confirmation for an `AGENTS.md` boundary or an "Ask first" item covers only the item it names (the same file,
   endpoint, migration or area), not every item of the same kind. Each new item needs its own confirmation, unless
   the answer explicitly says it covers them.
6. When a question is about a pattern or rule that will come up again in later jobs (naming, which file is the
   pattern, where a class goes, test style), say so in the question: "I will apply your answer to every later job in
   this plan with the same choice". Record that answer with scope `plan`. Answers tied to one item (behavior,
   contract, data, money) get the scope of that job or phase. The user can change the scope through "Other".
7. When the user later contradicts a recorded answer, do not edit the old block. Add a new block that says
   "Supersedes A1" and apply the newest one from then on. If it conflicts with finished work, say so before changing
   anything.

How to record an answer that the plan did not ask for. Append the block at the end of the Decisions section, after
the last `dN` block, with the next free id (`a1`, `a2`, ...). Never renumber or delete one:

```html
<div class="decision" id="a1" data-decision-state="answered" data-scope="plan" data-topic="mapper-location">
  <h3>A1. REPLACE: the question, standing alone <span class="status answered">Answered</span></h3>
  <p class="meta">Asked while implementing job 2.1. Scope: whole plan. Reused by: none yet.</p>
  <div class="answer"><strong>Answer (YYYY-MM-DD):</strong> REPLACE: option &ndash; the user's own words</div>
</div>
```

`data-scope` is `plan`, `phase-N` or `job-N-M`. `data-topic` is a short kebab-case label that makes the block easy
to find with `grep data-topic`. When you reuse an answer whose topic you are unsure about, grep the topics first.

## Tracking status in the plan file

Applies when the plan is a `write-plan` file (`docs/<name>-plan/index.html`). `write-plan` defines the format: its
`SKILL.md` and `template.html`. If the plan came only from the chat, track it in the task list and skip this section.

The user follows progress in the browser and cannot edit the file, so the file must always match reality. Keep it
current at the moments below. Never wait until the end to write status.

Rules:

- Status, decision answers (including new `aN` answer blocks and their "Reused by" lists), status-log lines and
  implementation notes are the only things you write in the plan file. Never edit its content (phases, jobs, scope,
  options). If the content must change, that is a deviation: ask, and
  use `write-plan` to reopen the plan as Draft.
- A status change always edits all three together: the `data-status` (or `data-plan-status`) attribute, the pill's
  class, and the pill's label (Not started, In progress, Done, Blocked, Deferred; Approved, Draft). Use `Edit` with
  exact strings, for example `id="job-1-2" data-status="todo"`.
- Every change also appends one line to that element's `status-log`:
  `<li>YYYY-MM-DD &middot; <Label> &middot; <note>. <evidence></li>`. Never rewrite an old line.
- Evidence is a `file:line`, a test name, or a command with its result. Mark `done` only with evidence.
- A phase is `progress` once any of its jobs starts and not all are finished, `done` when every job is `done` or
  `skipped` and the phase's verify command passes, and `blocked` when a job is blocked and no other job can continue.

| Moment | Write in the plan file |
|---|---|
| Gate, plan is `approved` and the user wants it built | Plan -> `in-progress`, before the first code edit |
| Gate, plan is `draft` | Do not implement. Ask the user to approve or to revise with `write-plan`. Only an explicit approval sets `approved` (and a Change log row "Approved") |
| Gate, plan is `done` or `blocked` | Ask the user whether to reopen with `write-plan`, or what unblocks it |
| User answers a decision the plan lists | Decision -> `answered`, pill "Answered", answer text with date and the user's words |
| User answers a question the plan did not list | Append an `aN` block (format in "Reuse answered decisions") with scope, topic, date and the user's words |
| Before asking any question | Re-read the `answered` blocks and reuse what covers it; log `applies A1` on the job |
| Before the first edit of a phase | Phase -> `progress` |
| Before the first edit of a job | Job -> `progress` |
| A job's checks pass | Job -> `done`, log line with evidence |
| A job cannot continue | Job -> `blocked`, log line with the reason and the decision or fix it waits for |
| User defers a job or phase | `skipped`, log line with the user's words |
| A job is done differently from the plan | Log line on the job (how and why), plus a line in Implementation notes |
| All jobs of a phase are done and its verify passes | Phase -> `done` |
| Every phase is `done` or `skipped` and the final checks pass | Plan -> `done` |
| You stop with phases remaining | Plan stays `in-progress`; every status current |

Resuming: when the plan is already `in-progress`, start from the first job that is not `done` or `skipped`. Check
the file against `git status` and `git diff --stat` first. If a job says `done` but the diff has no trace of it, or
the reverse, say so and ask before continuing.

## Step 1: Gate before touching code

1. Find the plan: the approved plan in this conversation, the task description, or the file the user pointed to. If
   there is none, stop and ask. Do not invent one. If the plan is not approved yet, plan first and get approval.
2. Read `AGENTS.md` in full now, not from memory. It is the only source of rules and may have changed.
3. Check the plan against it, and collect every open decision in the plan. Remove the ones an answered `dN` or `aN`
   block already covers ("Reuse answered decisions"), then ask the rest before implementing, as described in
   "Decisions: ask before implementing". Ask if any item:
   - is outside `AGENTS.md` (a different structure, naming or layer rule, or an area it does not describe);
   - is on the "Ask first" list: new dependency, public API contract change (REST/GraphQL/gRPC/Kafka/JWT claims),
     a file that fits no standard pattern, an ambiguous pattern, Flyway migration deletion or rewrite, security or
     JWT/OAuth config, anything that moves money (orders, payments, quota, school balance), or anything outward-facing
     or hard to reverse.
   - A confirmation covers that plan only, not later ones, and only the item it names (see "Reuse answered
     decisions", rule 5).
4. Check the memory index for notes that affect this work (ongoing renames, known unrelated compile errors, open
   decisions). Verify a note against the code before relying on it.
5. If the plan is a `write-plan` file, read its status first:
   `grep -o 'id="[^"]*" data-status="[^"]*"' docs/<name>-plan/index.html` and `data-plan-status`. Follow the Gate rows
   in "Tracking status in the plan file". Do not edit code while the plan is `draft`.

## Step 2: Turn the plan into an ordered checklist

1. Split the plan into atomic items (P1, P2, ...), each with one observable done-condition, for example
   "`GET /x` returns 403 for a TEACHER of another school".
2. Add the implicit items the plan assumes: tests for new behavior, a Flyway migration for a schema change, a
   regression test for a bug fix, a role check and school-scope check for a new endpoint.
3. Order the items from the inside out, so each slice compiles on its own:
   domain -> application (command/query, use case, port) -> infrastructure (entity, repository, adapter, mapper,
   migration) -> interfaces (controller, request DTO, GraphQL schema, consumer) -> tests for the new surface.
4. Track the items with a task list and mark one in progress at a time. When the plan file already has phases and
   jobs, use its ids (`1.1`, `1.2`, ...) instead of making new ones, so the task list and the file say the same thing.

## Step 3: Record a baseline

```bash
git status --short
./gradlew compileJava
```

Note any existing compile or test failure before you change anything. A failure that is already there is not yours:
do not fix it, and do not let it hide a new one. Compare against this baseline after every slice.

## Step 4: Read before you write

For each slice, before writing:

1. Read every file you will edit, in full around the edit.
2. Read one similar feature end to end (for example `CreateExamUseCase`, `ExamController`,
   `ExamBlueprintRepositoryImpl`) and match its naming: `CreateXxxUseCase`, `XxxJpaEntity`, `XxxRepositoryImpl`,
   `XxxDto`. Parts of the repo are inconsistent, so the nearest file is not proof of the pattern. If two files
   disagree, ask.
3. Search for the logic you are about to write. If the same or nearly the same logic exists in more than one place,
   extract one shared method that all callers use. Extract only logic that really exists twice, never for imagined
   future reuse.
4. Before using a framework or library API, read the docs for the version in use (Spring Boot 4.1, Spring Framework
   7, Java 25). Do not use deprecated APIs.
5. Find all call sites of any signature you change, and update them in the same slice.

## Step 5: Implement one slice

First mark the job (and its phase, if this is the phase's first job) In progress in the plan file. Then write the
smallest change that satisfies the item, and apply the rules below.

Code rules:

- A function is at most 25 lines, signature to closing brace (30 for a very large feature).
- Names say what the code does. No `data`, `info`, `handle`, `process`, `manager`.
- No comments that restate a line. Add a doc comment only for a contract, side effect or failure case the signature
  does not show. Write every new or edited comment in English. Translate Vietnamese comments in code you are
  already changing, keeping meaning and the "why". Leave user-facing string literals as they are.
- Constructor injection with explicit constructors, `private final` fields. Prefer explicit types over `var`.
- No query inside a loop or per element. Fetch in bulk. In GraphQL field resolvers use batch mapping.

Architecture and conventions:

- `domain` imports nothing from the other layers. `application` imports nothing from `infrastructure` or
  `interfaces`: declare a port in `application/port/output` and implement it in `infrastructure`. `interfaces`
  never imports `infrastructure`.
- Pagination is 1-based everywhere. Convert to 0-based only in `infrastructure/persistence/adapter` and `.../query`.
- Every new endpoint has `@PreAuthorize("hasRole(...)")`, and school-scoped data is also checked against the caller's
  school in the use case.
- Throw the exceptions in `application/exception`. Do not return error payloads by hand. REST returns
  `ResponseEntity<ApiResponse<T>>`.
- Use cases are `@Service` + `@Transactional` and take one command or query object.
- Events: add a `V2` payload class instead of editing `V1`. Set the topic with `@ExternalEventTopic`.
- Database: add a new Flyway migration `V<highest+1>__snake_case.sql`. Never edit a merged one. Entity and migration
  must agree because `ddl-auto` is `validate`. IDs are database-generated `uuidv7()` UUIDs: copy an existing entity.
- Do not write code that depends on the machine timezone. Never log or print secrets, tokens, OTPs, or student
  recordings and PII.
- Do not hand-edit `build/`, `.gradle/`, `logs/`.

## Step 6: Test with the code

Write the tests in the same slice as the code they cover, not at the end.

- Bug fix: write the regression test first, run it, and watch it fail for the right reason. Then fix the code and
  watch it pass.
- Use cases and services: `@ExtendWith(MockitoExtension.class)`, `@Mock` + `@InjectMocks`, AssertJ, no Spring
  context. Strict stubs are on: remove an unused stub, never hide it with `lenient()`.
- REST controllers: `@RestControllerTest`. Every role-protected endpoint group needs 401 (`@WithAnonymousUser`),
  403 with a wrong role (send a valid body, because `@Valid` runs first) and success with the right role.
- GraphQL where authorization matters: `@GraphQLControllerTest`.
- Repository or query code: `@RepositoryTest`. Full context: `@IntegrationTest`. Never raw `@DataJpaTest` or
  `@SpringBootTest`. Keep `application-test.yaml` as it is.
- Name classes `XxxTests` in the mirrored package. Assert the behavior in the done-condition, not only that the code
  runs without throwing.
- Do not convert old hand-written `mock(...)` tests as a drive-by.
- Architecture rules in `src/test/.../architecture` are guardrails: fix the code, never loosen the rule.

## Step 7: Check after every slice

```bash
./gradlew compileJava
./gradlew test --tests '*<ChangedClassOrFeature>*'
./gradlew test --tests '*LayeredArchitectureTests'     # when imports or packages changed
./gradlew test --tests '*PagingConventionTests'        # when pagination or repositories changed
```

- Compare with the Step 3 baseline. A new failure is yours: fix it before the next slice.
- Do not move to the next item while the current one fails.
- Run `integrationTest` only if Docker is available. If it is not, say so in the report. Never claim a check passed
  if you did not run it.
- When the slice passes, mark the item done and name the evidence (`file:line`, test name), in the task list and in
  the plan file. When it fails and you cannot fix it inside the plan, mark it Blocked with the reason.

## Step 8: Handle deviations

The plan will sometimes be wrong or incomplete. Do not silently work around it. Before asking in any row below,
check the recorded answers ("Reuse answered decisions"): a gap the user already settled for this plan is not asked
again.

| Situation | Action |
|---|---|
| Small implementation detail differs, same behavior and contract | Continue. Mark the item CHANGED, and say how and why in the report, the job's status log and Implementation notes |
| An item is impossible or the plan has a gap that changes behavior | Stop that item and ask with `AskUserQuestion` (options, recommendation first), wait for the answer |
| The fix touches an Ask-first area or a public contract | Stop and ask with `AskUserQuestion`, even if it looks small |
| You find an unrelated bug or smell | Do not fix it. Add it to "Problems noticed" |
| An unrelated test or compile error blocks you | Confirm it exists without your change, report it, work around it only if the plan allows |

## Step 9: Finish

1. Run the final checks: `./gradlew compileJava` and the narrowest relevant `--tests` filters. Run the full build
   only if feasible.
2. Look at `git status --short` and `git diff --stat`. Every changed file must trace to a plan item. Read each new
   untracked file. Revert accidental edits (whitespace, import reordering, IDE or formatter changes).
3. Do not commit or push unless the user asked.

## Final report

Keep it short and factual.

```
## Implemented: <plan name>

### What changed
- P1 <item> -> `path/File.java`, `FooTests`
- P2 ... (CHANGED: <how and why>)

### Progress
- Phases done: <list>. Phases remaining: <list, or "none: this was the last phase">
- Plan file: `docs/<name>-plan/index.html`, plan status <in-progress | done>, statuses current: <yes | no: why>

### Verified
- `./gradlew compileJava` -> pass
- `./gradlew test --tests '*Foo*'` -> N passed

### Decisions from you
- <question -> your answer (recorded as D2 / A3, scope), or "none">

### Decisions reused
- <earlier answer (D2 / A3) -> jobs where it was applied instead of asking again, or "none">

### Decisions made by default
- <conventional choices I made without asking, or "none">

### Not run
- <what and why, e.g. integrationTest needs Docker>

### Problems noticed but not fixed
- <unrelated issues, or "none">
```
