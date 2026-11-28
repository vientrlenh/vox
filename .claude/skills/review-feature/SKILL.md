---
name: review-feature
description: Review a Vox feature from the side of the person who will use it. Asks, with AskUserQuestion in this chat and in plain words, who uses it, what they want to do and the real target. Maps every use case onto one user journey (steps to the target, time per step, too long or not), then applies one fixed check catalogue to every step and entry point. Speed covers step and wait times and a crowd at once. Trust covers all or nothing, safe to repeat, error messages that say what to fix, what goes in comes out, and background work that ends visibly. No surprises covers refusals in the wrong state, two people at once, truthful statuses, stable lists and time rules. Safety as the user sees it covers login, roles, other schools, what responses show and staying logged in. Reviews of the same context report the same findings, in plain words. Use it to review, check or try out a feature, endpoint, use case, import, wait or user flow before users get it, and for "is it ready for users?", "can a school admin do X?", "how many steps?", "is it fast enough for a whole grade at once?" or "load test this". Not for configuration, CVEs, code internals, tests or AGENTS.md rules (review-technical), and not for verifying a plan (verify-plan).
---

# Review a feature from the user's side

Purpose: tell the owner, with evidence and in plain words, whether the feature gets its real user to the target the
owner named, how many steps and how much waiting that takes, and where that user would be slowed down, let down,
surprised or exposed. Review it as the person who is about to use the system will meet it, not as the engineer who
built it. The yardstick is the owner's target. The code is what gets judged, never the reference for what is right.

The intent-first flow and the "verify every finding" rule come from the owner's `feature-config-review` plugin. This
skill adds the journey, the fixed catalogue and the Vox specifics. Configuration, library CVEs, code internals, tests
and the `AGENTS.md` audit belong to `review-technical`, which shares this skill's inventory, runs and check ids.

## Rules for the whole skill

- **The user's view.** Every check judges something the user does, waits for, sees or is protected from. How the code
  does it matters only to explain a failure the user would meet. A problem that only an engineer can see goes under
  "For the technical review" in the report, never under Findings.
- **Plain words.** Questions, the journey table and the main text of the report say what the user does, waits for and
  sees, in the user's words. No class names, endpoints, status codes, config keys or library names there. Technical
  detail (entry point, `file:line`, status code) goes only in the columns marked "for the engineer" and in `matrix.md`.
- **Every question goes through `AskUserQuestion`, in this chat.** Never ask in plain text, never leave a question for
  the report, and never go on with an assumed answer. One call holds up to four questions, each with two to four
  options (labels of one to five words, a header of at most 12 characters). The owner can always type their own
  answer under "Other", so never add an "Other" option. Make each question stand alone, because the owner may see only
  the dialog: say what it is about and why it matters. Put an option first with "(Recommended)" only when there is a
  real recommendation. Show a table the owner must judge in the `preview` of an option (single-select questions only).
  Ask what belongs together in one call. Write every answer, in the owner's words, into the intent record or
  `matrix.md` before going on.
- **Target first.** No review starts before the questions in Step 1 are answered. "The code does X" is not a finding.
  A finding is a gap between what a catalogue check requires and what the feature was seen to do.
- **Observe, do not infer.** Try every behaviour that can be tried locally (a k6 run, a probe request) and cite the
  run. Read code to find entry points, to settle a check that cannot be run, and to explain a failure you saw. A check
  settled from code alone carries the evidence kind `Static`.
- **Same context, same findings.** Apply the whole catalogue in `references/checks.md` to every row it covers, with
  its own applicability rules, pass criteria and severities. There is no finding cap, no sampling, no "top N", no
  "the others are similar", no trimming of real findings to shorten the report, and no style opinions. A problem the
  user would meet that the catalogue does not cover goes under "Catalogue gaps", never under Findings.
- **Report only when complete.** Write the report only after the completeness gate in Step 9 passes.
- **Read-only.** Report findings and do not fix them. A fix comes after the report, when the owner asks, as a normal
  task under `AGENTS.md` (or through `implement-plan`). The only exception is the scratch material this skill creates
  and removes itself: the review folder, k6 scripts, fixtures, seed SQL and sample rows in the local database.
- **`AGENTS.md` is the only source of repo rules.** Read it in full at the start, every time: it sets what the review
  may do (Scope, Ask first, secrets). It changes during the refactoring stage, so never work from a remembered copy.
- **Ask before anything heavy**: starting containers or the app, seeding data, k6 runs and restarting the app. Step 3
  asks for all of it at once.
- **Local only.** Load tests, probes and seed data target only `localhost` or `host.docker.internal` and the
  docker-compose Postgres container (`vox-db-postgres`). Refuse a remote or shared host even when asked, and say why.
- **Never print secrets**, tokens, OTPs, passwords or student PII, in commands, logs, scripts or the report.
- **Long reviews keep going.** Context is summarized automatically. The matrix lives in a file (Step 4), so a
  summary cannot lose it.

## Step 1: Ask who, what and what for

Before reading any code, ask one `AskUserQuestion` in plain words, so the code cannot bias the target:

1. **Who will use it?** (`multiSelect`) One option per role, each described by what that person does in a school:
   "School admin", "Teacher", "Student", "System admin".
2. **What do you want to do?** The job, in the words of the person who uses the feature.
   Example: "As a school admin, I want to put the student list I already have in Excel into Vox."
3. **What is the actual target you are really aiming for?** The end state that means success, beyond the feature
   itself, and how that person sees it.
   Example: "On the first day every student can log in and is already in the right class, and I never type a name by
   hand."
4. **Which feature?** Only when the request does not say which feature, flow or change is meant.

- The options of questions 2 to 4 are at most three phrasings built from the owner's request, in the user's words.
  Never build them from the code (no class, endpoint, status or table names). Say in the question that they are only
  starting points and that "Other" takes the owner's own words. When the request gives too little, phrase plain jobs a
  user would have with a feature of that name.
- If the request already answers a question, make the restated answer the first option, marked "(Recommended)".
- If the answers do not say where the journey starts or how the user knows the target is reached, ask one more
  `AskUserQuestion` about the missing part only.
- If the owner mentions a time ("within a minute", "before the exam starts at 8:00") or a crowd ("a whole grade at
  once"), record it. It overrides the defaults in `references/journey.md`.
- If the request is about configuration, CVEs, code internals or tests, ask with `AskUserQuestion` whether to run
  `review-technical` instead, and stop here if the owner agrees.
- Write the answers into the intent record (`references/journey.md`, section 1).

## Step 2: Map the journey

Follow `references/journey.md`:

1. Inventory every entry point of the feature, for all of its use cases at once (section 2), and read the inventory
   files plus one hop out.
2. Build the paths: P0 from the start state to the success signal, plus every alternate path the feature offers
   (section 3). Every step is an action or a wait, with its requests.
3. Put every entry point on a path, or on the off-path list with the reason.
4. Count the steps, and work out each wait's worst case from config (section 4).

## Step 3: Confirm the journey and approve the runs, in one call

Ask one `AskUserQuestion`, in plain words:

1. **Journey.** "Is this how <the user> gets from <start> to <target>?" Put the plain journey table
   (`references/journey.md`, section 7) in the `preview` of the first option, because the owner may see only the
   dialog. Options: "Matches (Recommended)" and "Needs changes".
2. **Crowd and size.** "How many people at once, and how big an input?" State the figures in words, the owner's or the
   defaults (`references/journey.md`, section 6), for example "one class of 45 students starting together" and "an
   Excel file of 500 rows". Options: "Use these (Recommended)" and "Plan for twice as many".
3. **Trial runs.** "May I try the feature out on this computer?" Say in plain words what happens: Vox and its database
   start here, sample schools and users are made up and deleted at the end, a test user walks the journey, a crowd
   uses the busiest steps, people of another school and of the wrong role try to get in, and a short login lifetime
   checks that the user stays logged in. Nothing reaches real people or payment services. Put the exact list in the
   first option's `preview` (`references/performance-k6.md`, section B). Options: "Run all (Recommended)",
   "Walk-through only" (the walk and the probes, no crowd, race or login-lifetime runs) and "No runs".
4. **Switch-off test**, only when the journey has a wait: "May I switch Vox off while <the wait, in words> is running,
   and on again, to see whether the user's work survives?" It is a separate question because it kills the app
   mid-work. Options: "Yes (Recommended)" and "No".

If the journey needs changes, fix it and ask again. A declined run leaves its cells UNVERIFIED (reason "declined")
unless a `Static` method settles them. Never run anything heavy that was not approved.

## Step 4: Build the matrix

1. Pick a run id, `review-<yyyyMMddHHmmss>`, and create the review folder `<scratchpad>/review/<run id>/`. The
   matrix, the k6 script, fixtures, SQL and outputs all go there. Nothing goes into the repository.
2. Write `matrix.md` in the format at the end of `references/checks.md`: one block per row (each path, each step and
   each entry point), and in each block one line per check whose "Applies to" rule covers the row, with status `TODO`.
   Write the expected cell count at the top.
3. Create one task per row group (the journey, each path, the entry points), so the task list mirrors the matrix.

## Step 5: Collect the evidence

In this order:

1. Containers, the app and the sample data (`references/performance-k6.md`, sections C and D).
2. k6, each run from the same script (`references/performance-k6.md`, section E): `journeyWalk`, `probes`, the load
   scenarios, `racingRequests` when NSP2 applies, and `sessionContinuity` when SAF8 applies. Then the switch-off test
   if it was approved (section K).
3. Cleanup of the sample data and the zero-rows check, whatever the k6 results were.
4. The `Static` cells: J2, the step where each rule of J4 is checked, J6, the worst cases of SPD3, the bounds of SPD7,
   the failed-status code of TRU5 and the ordering of NSP6, plus every cell whose run was declined and whose method
   allows `Static`.

## Step 6: Settle every cell

For each cell, record the status, the evidence and, for a FAIL, the failure scenario. Update `matrix.md` as you go.

- The status comes from the check's pass criterion and nothing else.
- PASS needs evidence too.
- N/A only by the check's "Applies to" rule; quote the rule.
- UNVERIFIED only when the run was declined or cannot be done locally, and no `Static` method settles the cell. Write
  exactly what would settle it.
- Before you write a FAIL, look for an existing guard or validation that already handles the case. A FAIL needs the
  behaviour, not only the absence of the code you expected.

## Step 7: Turn the FAIL cells into findings

- **One finding per check and root cause.** The root cause is the `file:line` or config key whose change fixes it.
  Several cells that fail the same check for the same root cause form one finding that lists every cell. When the
  root cause is unknown (seen only in a run), make one finding per check and step.
- **Severity** comes from the catalogue row, with its escalation conditions.
- **Numbering** is fixed: by severity (Critical first), then by check id in catalogue order, then by step order.
  F1 is always the most severe.
- Each finding has: id, severity, what the user runs into (plain words), the step where it happens, an example in the
  user's terms ("a school admin who clicks Accept twice gets every student twice"), the evidence (`Run` or `Static`),
  and for the engineer: the check, the cells, where (`file:line`) and the smallest suggested fix. A fix that falls
  under "Ask first" in `AGENTS.md` is proposed, never applied.
- A suspected internal cause (a slow step that looks like a query per row, for example) goes under "For the technical
  review" with the finding it explains. It does not change the finding.

## Step 8: Ask what only the owner can answer

Before the report, put every question that only the owner can answer into one `AskUserQuestion` (more calls if there
are more than four): a target that can be read two ways, or a behaviour the target does not settle ("Rows that are
already in Vox are skipped without a word. Should the user be told?"). Never leave such a question for the report.
Record each answer in the intent record. When an answer changes the target or the success signal, settle every cell
it affects again before going on.

## Step 9: Completeness gate

Write the report only when all of these hold. Otherwise go back and finish:

1. Every cell in `matrix.md` is PASS, FAIL, N/A or UNVERIFIED. No `TODO` is left, and the settled count equals the
   expected count.
2. Every entry point from the inventory is on a path or on the off-path list.
3. Every FAIL cell belongs to exactly one finding, and every finding cites at least one FAIL cell.
4. The zero-rows check after cleanup returned 0, or the report lists exactly what remains and why.
5. No question is open: each one went through `AskUserQuestion`, and its answer is recorded.
6. **Second pass.** Walk the catalogue from top to bottom once more, row kind by row kind, and derive again which
   checks apply to each row. Any check that applies but has no cell is added and settled before you go on.

## Step 10: Report

Use this shape and these plain words. Keep it factual, and drop a section only when it cannot apply (say which at the
top).

```
## Review: <the feature, in the user's words> -> <READY | READY WITH FIXES | NOT READY>
<one line in plain words: what affects the user most, and any Critical or High catalogue gap>

### What the user wants
- Who: <roles>
- Wants to: <answer 2, the owner's words>
- Target: <answer 3>; we know it worked when: <the success signal, in plain words>
- Time and crowd: <the owner's figures | the defaults, in words>

### The journey
| Step | What the user does or waits for | Typical time | Slow time (95 of 100 are faster), or longest wait | Allowed | Too long? |
- Steps to the target: <n> things to do and <m> waits
- Time to the target, not counting the user's own thinking: typical <x>, slow <y>, worst case <z>
- Other ways through: <each path in words: +k things to do, +j waits, +t time>
- Wasted effort: <work the user does before a refusal that could have come earlier, or "none">

### How it went
| Area | Checked | Fine | Problem | Does not apply | Not checked |
| The journey | | | | | |
| Speed | | | | | |
| Trust | | | | | |
| No surprises | | | | | |
| Safety | | | | | |

### Problems
| # | Severity | What the user runs into | Step | Example | Evidence | For the engineer: check, cells, where, smallest fix |

### With a crowd
| What was tried | How many at once | Typical time | Slow time | Failed | Within what is allowed? |
- Sample data removed: <yes, nothing left | no: what remains>
- Caveat: one copy of Vox on one computer; good for comparing runs, not a promise for production

### Not checked
| What | Why | What would settle it |

### Answers from the owner
- <question -> answer, and what it changed>

### For the technical review
- <internal problems noticed, and suspected causes of the problems above, for review-technical>

### Catalogue gaps
- <a problem the user would meet that no check covers, its evidence, and the proposed row for references/checks.md>

### Outside the target
- <problems noticed in other features, not reviewed>

### Matrix
- <full path of matrix.md>; <expected cells> cells, all settled
```

Verdict, decided only by these rules:

- **NOT READY**: any Critical or High FAIL.
- **READY WITH FIXES**: the FAILs are Medium or Low only, or an UNVERIFIED cell sits on a check that is Critical or
  High on failure (list how to settle each one).
- **READY**: no FAIL above Low, and no UNVERIFIED cell on a check that is Critical or High on failure.

After the report, ask one `AskUserQuestion` with only the questions that apply: whether to publish the report as a
page for sharing; whether to add the proposed rows of "Catalogue gaps" to `references/checks.md`, which changes the
skill; and whether to run `review-technical` on the items under "For the technical review".

## Pitfalls

- **Reviewing the code instead of the journey.** A clean class that leaves the user three steps short of the target
  is a failed review of the feature.
- **Stopping at the use case the owner named.** The target decides where the journey ends (see the example in
  `references/journey.md`).
- **Talking like an engineer.** "The accept endpoint answers 500" becomes "Accepting the file ends with 'An error
  occurred', and the admin cannot tell what to fix". The status code goes in the engineer's column.
- **Asking in plain text, or leaving a question in the report.** Every question is an `AskUserQuestion` in this chat.
- **Drifting into internals.** SQL counts, indexes, thread pools, transactions, config, CVEs and tests are
  `review-technical`'s. Note a suspicion under "For the technical review" and move on.
- **Assuming defaults.** When a wait's worst case depends on a default, look up the real default for the version in
  use (Spring Boot 4.1, Spring Framework 7, Hibernate, Kafka client).
- **Vague findings.** "Might be slow" is not a finding. Name the step, the measured time, the allowed time and what it
  means for the user.
- **Fixing instead of reviewing.** Suggest the fix and leave the decision to the owner.
