# Decisions: find them, ask why, test the reason

A technical review questions the engineer. For each decision in the code and the configuration it asks why it was
made, and whether that reason holds. This file fixes which decisions count, so two reviews ask about the same ones.
It also says where to look for reasons before anyone is asked, how to ask, and how a reason is tested. DEC1 to DEC3 in
`references/checks.md` settle the cells.

## 1. Decision kinds

Every instance of these kinds in the inventory files and config key rows (not one hop out) is one decision row,
`DEC<n>`, numbered in inventory order. A FAIL cell of another check whose cause is an instance of a kind joins that
instance's row; it never starts a second row for the same decision.

| Kind | An instance is | Governed checks | Stressors (section 5) |
|---|---|---|---|
| Transaction | a `@Transactional` with a non-default attribute (`propagation`, `isolation`, `readOnly`, `timeout`, `rollbackFor`, `noRollbackFor`), or a write path that runs outside any transaction | SPD8, TRU6, TRU8 | crash, duplicate, concurrent |
| Concurrency control | a lock (`@Lock`, `FOR UPDATE`, `SKIP LOCKED`), `@Version`, a conditional update, a Redis lock or `synchronized`, or a write on shared state with none of these | NSP8, NSP4 | concurrent, second instance, crash |
| Executor or scheduler | an executor's pool size, queue capacity or rejection policy, or a `@Scheduled` kind (`fixedDelay`, `fixedRate`, cron), interval or initial delay | SPD4, NSP4 | load, crash, second instance |
| Time or retry value | a timeout, lease, TTL, expiry window, retry count or backoff, literal or from config | CFG4, NSP4 | load, crash, time boundary |
| Messaging | a topic, partition key, listener concurrency, ack mode, error handler, dead-letter topic or payload version | NSP4, TRU6 | duplicate, crash, load |
| Data model | in a migration row: a type or length, nullability, a constraint, an index, a cascade rule, or a denormalized column (a counter, a copied value) | TRU8, SPD6, MIG1 | load, concurrent, input |
| Cache or Redis key | what is stored, its key, TTL, eviction and invalidation | SAF13 when it holds a token or an OTP, CFG4 otherwise | concurrent, time boundary, crash |
| External call | a client to HTTP, gRPC, S3, SMTP, Firebase or PayOS: its timeout, retry and fallback | SPD8, CFG4, SAF11 | load, crash, duplicate |
| Config value | a config key row whose effective value differs from the library default | CFG1, CFG4 | load, time boundary |
| Dependency version | a version in `build.gradle` that the Spring Boot BOM does not manage, when `build.gradle` is in scope or a CVE leads there | SAF10 | none |
| Rule exception | an entry in an architecture exemption list, or a `@SuppressWarnings`, on an inventory class, or code that departs from the pattern `AGENTS.md` names | AGT | none |

The governed checks set the order of the questions (the most severe on failure first) and the finding that a failed
DEC2 or DEC3 cell joins.

## 2. Find the reason before asking (DEC1)

Go in this order and stop at the first step that gives a reason:

1. **Self-evident.** The value equals the library default for the version in use (`references/configuration.md`,
   section 3), an `AGENTS.md` rule requires it (quote the rule), or it follows the example that `AGENTS.md` names as
   the model. DEC1 is PASS, and DEC2 does not apply. DEC3 still does, because a default can be wrong for the case.
2. **Git.** Find the commit that introduced the current lines with `git blame -L <start>,<end> --porcelain -- <file>`,
   read it with `git show -s --format='%h %ad%n%s%n%n%b' --date=short <commit>`, and read the earlier versions of the
   same lines with `git log -L <start>,<end>:<file>` when the value changed over time. When the commit came in through
   a merge commit, read that one too:
   `git log --merges --ancestry-path --reverse --format='%h %s' <commit>..default` (the first line).
3. **Tracked files.** The plans under `docs/` whose text names the file, the class or the key (their Decisions
   section), `README.md`, `AGENTS.md`, and the Javadoc or a comment next to the decision.
4. **Pull requests**, only when Step 3 approved it: `gh pr view <n> --json title,body` for the `(#n)` in the subject
   of the commit or of its merge commit.
5. **Otherwise** the decision is open, and Step 7 asks the engineer (section 3).

A record counts as a reason only when it says why, not what. "add import dispatcher" does not explain "every 5 s".
Quote the reason and its source (`a1b2c3d`, `docs/shared-paging-plan` D2, PR #118) in the reasoning record.

## 3. Ask why

- **Batches.** One `AskUserQuestion` holds up to four open decisions. Order them by the most severe governed check on
  failure (Critical first), then by decision number, and keep asking until every open decision has been asked.
- **One decision per question.** State it in one line with its `file:line` or key and the effective value. Say what
  git and the docs gave ("it came in with a1b2c3d 'add import dispatcher', which gives no reason; no plan names it")
  and the evidence that makes it matter (a measured value, or the counter-scenario of section 5). End with "Why?".
- **Header**: the kind, in at most 12 characters ("Transaction", "Lock", "Executor", "Schedule", "Timeout",
  "Messaging", "Data model", "Cache", "Client", "Config", "Dependency", "Exception").
- **Options**: up to three candidate reasons that the evidence suggests, worded neutrally. Examples: a measurement ("the
  longest import takes under 5 minutes"), a constraint of another system ("PayOS retries webhooks for 24 hours"), a
  copied pattern with its file ("copied from `OutboxEventScheduler`"), or "kept the library default". Add "No
  deliberate reason", described as "it was a default, a copy or a guess". Never mark one "(Recommended)". The engineer
  types any other reason under "Other".
- **Code in view.** When the answer needs the code, put the excerpt (at most 15 lines, never a secret) in the
  `preview` of every option, so it shows whichever option has the focus.
- **Record** each answer verbatim in the reasoning record, with "engineer" and the date as its source.
- **Follow up** when DEC2 finds that the evidence contradicts a premise of the answer. Ask once more, showing the
  evidence: "You said <premise>; <evidence> shows <fact>. Which holds?" Options: "The evidence holds", "The run is not
  representative" and "There is another reason". Ask at most two follow-up rounds per decision, then judge with what
  is known.

Example (the commit, line and values are illustrative; take the real ones from the code and git):

```
Header: Schedule
Question: ImportJobDispatcher.poll runs every 5 s (fixedDelay = 5000, ImportJobDispatcher.java:41), with a 5-minute
lease and 3 attempts. It came in with a1b2c3d "add import dispatcher", which gives no reason, and no plan names it. A
crash mid-import leaves the session in progress for up to 5 minutes per attempt. Why these values?
Options:
- "Sized to the import length": the longest import takes under 5 minutes, so a shorter lease would re-run live work
- "Copied from the outbox relay": the same pattern as OutboxEventScheduler, not tuned for imports
- "No deliberate reason": it was a default, a copy or a guess
```

## 4. Reasoning record (DEC2)

Write one record per decision row in `<review folder>/decisions.md`:

```
### DEC4  Executor or scheduler  fileImportExecutor: 2 threads, no queue, AbortPolicy  (AsyncConfig.java:31)
- Reason: "<the reason, verbatim>" (<source: commit, plan, PR, engineer and date, or "none">)
- Strongest form: <the reason restated at its best, before it is tested>
- Premises:
  1. <a factual claim the reason rests on> -> <holds | false | assumed> (<Run or Static evidence>)
  2. ...
- Counter-scenarios (DEC3): <stressor>: <the concrete scenario> -> <outcome> (<evidence>)
- Alternatives: <at least one other design that meets the same need, and its cost on the Step 1 context>
- Holds only while: <the Step 1 facts the PASS depends on, such as "one instance runs", or "nothing">
- Cells: DEC4:DEC1 <status>, DEC4:DEC2 <status>, DEC4:DEC3 <status>
```

- **Premises.** Split the reason into the factual claims it rests on: a load, a duration, a size, a framework
  behaviour, an ordering, "only one caller does this". Each one is settled with evidence: a run, a `file:line`, or,
  for a framework behaviour, the configuration metadata or docs of the version in use. A premise about another system
  that cannot be checked locally (AI grading, `vox-streaming`, PayOS) is "assumed" and goes under "Unverified" with
  what would settle it.
- **Strongest form first.** Restate the reason at its best before you test it, so you do not knock down a weaker
  version of it.
- **Alternatives** feed the "Options" of a finding. They never make a cell FAIL by themselves.
- **Holds only while** keeps the conditions visible: a decision that is sound for one instance stops being sound when
  a second one starts, and the report says so.

## 5. Counter-scenarios (DEC3)

Apply every stressor that the decision's kind lists in section 1, within the Step 1 context:

| Stressor | The scenario |
|---|---|
| load | 2x the Step 1 load or data volume |
| second instance | the same code on two instances at once; only when Step 1 says several or not decided |
| crash | the process dies, or a call times out, at each hop of the decision's path: after the write and before the commit, after the commit and before the side effect, in the middle of a batch |
| duplicate | the same message, request or job arrives twice, or out of order |
| concurrent | two callers on the same row or key at the same moment |
| time boundary | one second before, at and after an expiry, a lease end, or midnight in Asia/Ho_Chi_Minh |
| input | empty, and at the configured maximum |

For each stressor, write the concrete scenario (inputs, interleaving, timings), then its outcome with evidence: `Run`
when the runs can reproduce it, otherwise `Static`, by walking the code and the thread map
(`references/concurrency.md`). The cell passes when every outcome stays correct, or fails visibly (a terminal status
with a reason, an error the caller can act on) and recoverably (a retry, a sweeper, a reconciler) within the worst
case from config. A FAIL carries the scenario.

## 6. In the report

The "Decisions" table of the report has one row per decision. "Reason" and "Source" come from the record. "Holds?" is
"yes" when DEC2 and DEC3 pass (or do not apply), "no" when either fails, and "unverified" otherwise. "Holds only
while" is copied from the record. The decisions explained only in this chat go under "To write down", each with the
git-tracked file where its reason belongs.
