---
name: verify-plan
description: Verify that an implemented plan is actually complete and correct. Use after finishing implementation of an agreed plan or task, before reporting done, or when asked to "verify", "check the plan", or "double-check what you did". Checks every plan item with evidence, detects scope creep, runs real compile and test checks, and audits the diff against the Vox AGENTS.md rules.
---

# Verify a plan after implementation

Purpose: prove the work matches the plan, instead of assuming it does. Never report "done" from memory of what
you intended to do. Re-read the plan and the real diff, then collect evidence for every claim.

Rules for the whole skill:

- Evidence beats belief. A claim like "item 3 is done" needs a `file:line`, a test name, or command output.
- Report failures as they are. Do not soften a failed check, and do not hide a skipped one.
- Verification is read-only by default. If you find a problem, list it. Fix it only if the fix is inside the plan's
  scope (see Step 7), and then re-run the affected checks.

## Step 1: Rebuild the plan as a checklist

1. Find the plan: the approved plan in this conversation, the task description, or the plan file the user pointed to.
   If there is none, say so and ask the user for it. Do not invent one.
2. Break it into atomic, checkable items. Each item gets an id (P1, P2, ...) and one observable done-condition, e.g.
   "`GET /x` returns 403 for a TEACHER of another school", not "authorization works".
3. Add the implicit items the plan assumes: tests for new behavior, a Flyway migration for a schema change, a
   regression test for a bug fix (AGENTS.md: it must fail without the fix).

## Step 2: Read the real change

```bash
git status --short
git diff --stat
git diff            # tracked changes; add `git diff --cached` if staged
```

New untracked files do not show in `git diff`. Read each one listed under `??` in `git status`. Read every changed
file in full around the edit, not just the hunk, so you see what the change interacts with.

## Step 3: Plan coverage (plan -> diff)

For each checklist item, find the code that satisfies it and record one of:

| Status | Meaning |
|---|---|
| DONE | Code exists and evidence shows it works (cite `file:line` and the check from Step 5) |
| PARTIAL | Some of the done-condition is met; say exactly what is missing |
| MISSING | Nothing in the diff addresses it |
| CHANGED | Done differently from the plan; say how and why |

Check the easy-to-forget parts: every call site of a changed signature, every layer the feature crosses
(domain -> use case -> persistence -> controller/schema), config keys, GraphQL schema files, migrations.

## Step 4: Scope creep (diff -> plan)

Walk the diff the other way. For every changed hunk, name the plan item that requires it.

- A hunk with no plan item is unrequested. AGENTS.md forbids drive-by refactors, renames, reformatting and cleanup.
  Revert it, or report it and ask.
- Check for accidental edits: whitespace-only changes, import reordering, files touched by the IDE or formatter,
  edited `build/`, `.gradle/`, `logs/`, or a committed `.env`.
- The one allowed extra is extracting logic that was duplicated in more than one place.

## Step 5: Run real checks

Run the narrowest check first, then widen. Never claim a check passed if you did not run it.

```bash
./gradlew compileJava                                   # always
./gradlew test --tests '*<ChangedClassOrFeature>*'      # narrowest relevant tests
./gradlew test --tests '*LayeredArchitectureTests'      # when imports or packages changed
./gradlew test --tests '*PagingConventionTests'         # when pagination or repositories changed
./gradlew integrationTest                               # only if feasible (needs Docker)
```

Rules:

- A bug fix needs a regression test that fails without the fix. Prove it: stash or revert the fix, run the test,
  confirm it fails, then restore the fix. If you cannot do this, say so.
- A new test that passes proves little. Check that it asserts the behavior in the done-condition, not just that the
  code runs without throwing.
- If a failure is unrelated to your change (for example the known compile errors noted in memory), confirm that by
  checking the same failure exists without your change, then report it as a problem you noticed. Do not fix it.
- For REST or GraphQL behavior that tests do not cover, exercise it for real if the app can run (use the `run`
  skill). If it cannot run here, list it under "Not verified".
- Tests run with `user.timezone=Asia/Ho_Chi_Minh`. Flag any new code or test that depends on the machine timezone.

## Step 6: Audit against AGENTS.md

`AGENTS.md` is the only source of rules. Do not rely on a remembered or copied list, because it may be outdated.

1. Read `AGENTS.md` in full now.
2. List the diff's touched areas (layers, endpoints, persistence, events, migrations, tests, comments).
3. For each touched area, check the diff against every `AGENTS.md` section that applies: Architecture, Conventions,
   Database, Testing, Scope, Coding rules, and Ask first. Do not skip a section because it seems unrelated; skip it
   only after you confirm the diff does not touch what it covers.
4. For each violation, quote the line and cite the rule's section in `AGENTS.md`.
5. Confirm no "Ask first" action happened without the user's approval.
6. Confirm nothing in the diff leaks secrets, tokens, OTPs, or student recordings/PII.

Rules enforced by an architecture test (`src/test/.../architecture`) are already covered by Step 5. Do not loosen
those tests to get green.

## Step 7: Resolve or report

- Fix a gap only when it is inside the plan's scope (a MISSING or PARTIAL item, a failing check caused by your
  change). Then re-run the checks that cover it.
- Do not fix problems outside the plan. Report them.
- If a fix would change the plan, a public contract, or anything on the ask-first list, stop and ask.

## Step 8: Final report

Use this shape. Keep it short and factual.

```
## Verification result: <PASS | PASS WITH GAPS | FAIL>

### Plan coverage
| Item | Status | Evidence |
|---|---|---|
| P1 <done-condition> | DONE | `path/File.java:42`, `FooTests.bar` passes |
| P2 ... | PARTIAL | missing X |

### Unrequested changes
- <hunk or file, or "none">

### Checks run
- `./gradlew compileJava` -> pass
- `./gradlew test --tests '*Foo*'` -> 12 passed, 1 failed (<name>, reason)

### Not run / not verified
- <what and why, e.g. integrationTest needs Docker>

### AGENTS.md findings
- <rule, file:line, or "none">

### Problems noticed but not fixed
- <unrelated issues>
```

Result rules: PASS only if every item is DONE, no unrequested changes remain, and every check you ran passed.
PASS WITH GAPS if the only open issues are checks you could not run, and you listed them. Otherwise FAIL.
