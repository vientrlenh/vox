---
name: write-plan
description: Create a new implementation plan, or update an existing one that is still a Draft, as an HTML file under docs/ with a tracked status on the plan, every phase and every job. Use when asked to "write a plan", "plan this", "create a plan", "update the plan", or "revise the plan". Measures the codebase first, turns every open question into a decision asked in chat, and hands the approved plan to implement-plan.
---

# Write or update a plan

Purpose: produce a plan that can be reviewed, approved, executed phase by phase, and tracked. The plan is an HTML
file the user reads in a browser and cannot edit there. So every question is asked in chat, every answer is written
into the file by you, and every phase and job carries a status that `implement-plan` keeps current.

Rules for the whole skill:

- A plan is a contract. Write facts you measured, not guesses. Never leave `TBD` or a bare question in the file.
- The plan changes only while its status is Draft. After approval only statuses, decision answers, the status log
  and the implementation notes change, and those are written by `implement-plan` (see Step 6).
- Planning writes only the plan file. Do not edit source code while planning.
- Write everything in English, using absolute dates (`YYYY-MM-DD`).
- Respect `AGENTS.md`: a plan must not schedule work that `AGENTS.md` forbids or that needs approval first. Make such
  work a decision instead.

## File and format

- Path: `docs/<kebab-name>-plan/index.html`, like `docs/test-restructuring-plan/index.html`. One folder per plan.
- Start every new plan from `.claude/skills/write-plan/template.html` (copy it, then replace each `REPLACE`). It holds
  the styles, the section order, and a small script that builds the Progress table from the status attributes.
- Status lives in attributes, so tools can read and edit it with exact strings:

| Where | Attribute | Allowed values |
|---|---|---|
| Whole plan | `data-plan-status` on `#plan-status` | `draft`, `approved`, `in-progress`, `blocked`, `done` |
| Phase `.phase#phase-N` | `data-status` | `todo`, `progress`, `done`, `blocked`, `skipped` |
| Job `.job#job-N-M` | `data-status` | `todo`, `progress`, `done`, `blocked`, `skipped` |
| Decision `.decision#dN` | `data-decision-state` | `open`, `answered` |

- The visible pill next to each status repeats it (`class="status <value>"` and its label). The attribute and the pill
  must always change together. Labels: Not started, In progress, Done, Blocked, Deferred; Draft, Approved.
- Ids are stable once the plan is approved. Never renumber, reuse, or delete one.

## Step 1: Decide create or update

1. Look in `docs/` for an existing plan on the same topic. If one exists, update it instead of creating a second.
2. Read its `data-plan-status`.
   - `draft` -> update in place (Step 5).
   - anything else -> do not edit the content. Ask the user with `AskUserQuestion` whether to reopen it as Draft
     (recommended when the scope changes) or to put the new work in a separate plan. Reopening is only done on a
     "yes" (Step 5, reopening).
3. No plan and the task is unclear -> ask what the goal is before exploring.

## Step 2: Learn before you plan

1. Read `AGENTS.md` in full, and the memory index for ongoing work that affects the task.
2. Read the code the task touches. Find an existing feature built the same way and note its files.
3. Measure instead of estimating: count files, call sites, usages (`grep -r`, `git ls-files`, `wc`). Put the numbers
   and commands in "Current state". If you did not look, say "not checked".
4. Run `./gradlew compileJava` once and note failures that already exist, so the plan does not take them on.
5. Read official docs for the framework version in use (Spring Boot 4.1, Spring Framework 7) when the plan depends
   on an API. Note the version.

## Step 3: Draft the content

Fill the template sections in order: goal, Progress (script fills it), Current state, Decisions, Phases, Risks, Out of
scope, Verification, Change log.

Phases and jobs:

- A phase is independently reviewable and leaves the build green. If a phase breaks the build until the next one, merge
  them or reorder.
- Order by dependency, inside out: domain -> application -> infrastructure (with migration) -> interfaces -> tests for
  the new surface. Tests belong to the job that creates the behavior, not to a last "tests" phase.
- A job is atomic and has one observable done-condition ("`GET /x` returns 403 for a TEACHER of another school", not
  "authorization works"), the files it touches, and the verify command. A job fits in one slice of work.
- Name the check for each phase: the narrowest `./gradlew ... --tests` filter, and `LayeredArchitectureTests` or
  `PagingConventionTests` where the phase touches them.
- Add the implicit jobs: a Flyway migration for a schema change, a regression test for a bug fix, `@PreAuthorize` and
  school-scope checks for a new endpoint, English translation of Vietnamese comments in code the plan edits.
- State size and risk in the tag (`small`, `medium`, `large`; `mechanical`, `needs D2`).
- No drive-by work. Anything not needed for the goal goes under "Out of scope".

Decisions:

- Every open choice becomes a decision block with 2-3 options, the trade-off of each, and one marked
  `(recommended)`. Use "decided" blocks, with the answer filled in, for choices the user already made.
- Anything on the `AGENTS.md` "Ask first" list that the plan needs is always a decision: new dependency, public API
  contract change, a file that fits no pattern, ambiguous pattern, migration rewrite, security or JWT/OAuth config,
  money logic (orders, payments, quota, school balance), anything outward-facing or hard to reverse.
- A phase that depends on a decision says which one ("needs D2").
- Do not ask what has a conventional default or can be verified in the code. Decide it and write it down.

All initial statuses are `todo`, decisions are `open`, and the plan is `draft`.

## Step 4: Check the draft

Before showing it, check the file itself:

```bash
grep -n "REPLACE" docs/<name>-plan/index.html        # must print nothing
grep -o 'id="[^"]*" data-status="[^"]*"' docs/<name>-plan/index.html
grep -o 'id="d[0-9]*" data-decision-state="[^"]*"' docs/<name>-plan/index.html
```

- No `REPLACE`, `TBD` or `TODO` text. Ids unique, every phase and job has `id` then `data-status`, pill matches
  attribute, every `needs Dn` points to a real decision.
- Every job has a done-condition and a verify command. Every phase ends with a green-build check.
- The plan does not contradict `AGENTS.md`. Quote the section if it is close to the edge.
- HTML is well formed (every tag closed). The Progress table lists every phase and job when opened in a browser.

## Step 5: Ask, record, and get approval

The user cannot edit the file, so finish the draft in chat.

1. Tell the user the path and how to open it (`open docs/<name>-plan/index.html`), with a three-line summary: goal,
   number of phases, number of open decisions.
2. Ask every open decision with `AskUserQuestion` (at most 4 per call, several calls if needed). Each question stands
   alone, offers 2-4 options with the trade-off, and puts the recommendation first with "(Recommended)".
3. Write each answer into its decision block: set `data-decision-state="answered"`, change the pill to
   `<span class="status answered">Answered</span>`, and replace the `.answer` text with
   `<strong>Answer (YYYY-MM-DD):</strong> <option> &ndash; <the user's words if they typed their own>`.
   If an answer changes the plan, edit the affected phases and jobs now, and add a Change log row.
4. When all decisions are answered, ask for approval with `AskUserQuestion`: "Approve the plan as written",
   "Request changes". On approval set `data-plan-status="approved"` and the pill to `approved` / "Approved", and add a
   Change log row "Approved". On changes, edit, re-run Step 4, and ask again.
5. Never set `approved` yourself, and never approve because the user said "looks good" about something else.

Updating a Draft plan: edit in place, keep ids stable where possible, re-run Step 4, append a Change log row
(date, what, why), and re-ask only the decisions that changed or are new. Decisions already answered stay answered
unless the change invalidates them: say so and ask again.

Reopening a non-draft plan (only after the user said yes): set `draft`, add a Change log row with the reason, and keep
every job already `done` exactly as it is. New work is added as new jobs or a new phase with new ids. Do not rewrite
history. After the edit, run Steps 4-5 again.

## Step 6: Hand off

- Do not implement. Offer `implement-plan`.
- `implement-plan` owns every status change after approval: plan status, phase and job status, status log entries,
  decision answers, and implementation notes. Do not change them here once the plan is approved.
- Do not commit or push unless asked. The plan folder is a normal file change in the working tree.

## Final report

```
## Plan: <name>   (<create | update | reopen>)
File: docs/<name>-plan/index.html   Status: <draft | approved>

### Summary
- <goal, phases, jobs>

### Decisions
- D1 <question> -> <answer, or "open">

### Measured
- <the numbers and commands used, or "not checked: <why>">

### Needs your attention
- <open decisions, Ask-first items, existing failures found, or "none">
```
