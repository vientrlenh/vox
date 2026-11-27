# Runs: k6, sample data and probes

Everything here runs only after the owner approved it in Step 3 of SKILL.md, against the local machine only. All
files go into the review folder, `<scratchpad>/review/<run id>/`, and nothing goes into the repository.

## A. Runs and the cells they settle

| Run | How | Cells it settles |
|---|---|---|
| `journeyWalk` | k6, 1 actor, `WALK_ITERATIONS` iterations (20 by default, 5 when a wait is over 60 s), fresh data each iteration, each path in turn | J1, J5, SPD1, SPD3, TRU4, NSP3, NSP7 |
| `probes` | k6, one iteration, one check per cell, named with the cell id | SAF1 to SAF5, SAF7, TRU1 to TRU3, NSP1, J4 |
| `statementCount` | k6 walk with markers, at 1x and at 2x volume, plus the counting snippet (section F) | SPD5 |
| `EXPLAIN` | psql in the Postgres container (section G) | SPD6 |
| `warmUp,steadyLoad` or `burst` | k6, one action at a time at the expected concurrency, with pool sampling (section H) | SPD2, SPD4, SPD7 (response size) |
| `racingRequests` | k6, many VUs and one iteration each, then the invariant in SQL (section I) | NSP2, TRU2 (at the same moment) |
| `sessionContinuity` | k6 against an app with a short token lifetime (section J) | SAF8 |
| Restart probe | kill and restart the app during a wait (section K) | TRU5 |
| Log count | PowerShell over the app logs (section L) | SAF6 |

## B. What the Step 3 "Runs" question must state

- What gets started: `docker compose up -d postgres redis kafka` for the containers that are not running, and the
  app with `./gradlew bootRun` from the current working tree. Reuse an app already on port 8081 only if the owner
  confirms that it runs the code under review. A reused app is never killed or restarted, so the restart probe and the
  SAF8 run then need the skill's own app.
- The scenarios, the expected concurrency and the reference input, with their source (the owner or the defaults in
  `references/journey.md`, section 6).
- The sample data: two schools, every role, the feature data, and that all of it is removed at the end of the run.
- The SAF8 restart with `JWT_EXPIRATION_MS=60000`, when SAF8 applies.
- Every side effect on the path and how it is neutralized: email (SMTP; seeded addresses end in `.invalid` and
  cannot be delivered), push (Firebase, off unless `FIREBASE_ENABLED`), payments (PayOS), S3 uploads, AI grading
  over Kafka or gRPC, and outbox events. Never load-test a real payment provider or real mail recipients. If a side
  effect cannot be neutralized, leave that operation out, and its cells become UNVERIFIED with the reason.

## C. Prepare

1. Check the containers with `docker ps` (`vox-db-postgres`, `vox-redis`, `vox-kafka`) and start what is missing.
2. Start the app in the background with the `.env` values loaded into that one process and SQL logging on, so that
   statements carry timestamps in `logs/vox.log` (section F). Nothing is printed:

   ```powershell
   Get-Content .env | ForEach-Object {
       if ($_ -match '^\s*([A-Za-z_][A-Za-z0-9_]*)\s*=\s*(.*)$') {
           [Environment]::SetEnvironmentVariable($Matches[1], $Matches[2].Trim().Trim('"'), 'Process')
       }
   }
   ./gradlew bootRun --console=plain "--args=--logging.level.org.hibernate.SQL=debug" *> "<review folder>/app-1.log"
   ```

   Run it with `run_in_background`, and wait with Monitor (never sleep) until the log shows the "Started" line or
   `GET /api/v1/status` answers. Note the JVM and pool settings it runs with. Every later start (sections J and K)
   writes to the next file (`app-2.log`, `app-3.log`), so each start keeps its own output.
3. Read the current schema of every table you will seed or count (the latest migrations, then the entity). The
   skeletons below match the schema at the time of writing; trust the migrations over them.
4. Build the fixtures the probes need in `<review folder>/fixtures/` (clean file, file with errors, file at the
   multipart limit, zip bomb, external-entity xlsx, the TRU4 values). Use a tool that is already installed. If none
   can write the format, ask before installing one.

## D. Sample data, created and removed in the same run

Order, for every k6 run:

1. `seed.sql` creates the schools and the accounts (SQL only for what the API cannot create).
2. k6 `setup()` logs in and creates the feature data through the API, as the seeded accounts.
3. The scenario runs.
4. k6 `teardown()` deletes through the API what `setup()` created.
5. `cleanup.sql` deletes the accounts, the schools and anything else tagged with the run id.
6. The zero-rows check confirms that nothing with the run id remains.

Run step 5 even when k6 fails: if `setup()` throws, k6 skips `teardown()`.

**Accounts.** The API cannot create users with a known password (registration goes through an OTP email), and
passwords are Argon2 hashes that SQL cannot compute. So the seeded accounts copy `password_hash` from the system
admin that `SystemAdminUserInitializer` creates, and they log in with `SYS_ADMIN_PASSWORD`. If the copied hash does
not match that password, `setup()` fails on the first login; ask the owner then.

Emails are `<run id>-<school>-<role>-<n>@perf.invalid`, where `<school>` is `a`, `b`, or `x` for the system admin, who
belongs to no school. `.invalid` is a reserved top-level domain, so nothing can ever be delivered. The probes need at
least: one account of every role in school A, two students in school A (A2 must not see A1's data), and one account
of every school role in school B.

Skeleton for `seed.sql` (check every column against the current schema first):

```sql
\set ON_ERROR_STOP on
BEGIN;
INSERT INTO schools (name, code, address, contact_email, contact_phone, student_count, is_active,
                     created_at, updated_at, created_by, updated_by)
SELECT format('%s school %s', :'run_id', s), format('%s-%s', :'run_id', s), 'review address',
       format('%s-school-%s@perf.invalid', :'run_id', lower(s)), '0000000000', 1000, true,
       now(), now(), 'System', 'System'
FROM unnest(ARRAY['A', 'B']) AS s;

WITH admin AS (
    SELECT u.password_hash
    FROM users u
    JOIN user_roles ur ON ur.user_id = u.id
    JOIN roles r ON r.id = ur.role_id AND r.code = 'SYSTEM_ADMIN'
    ORDER BY u.created_at
    LIMIT 1
), accounts AS (
    SELECT a.school, a.role_code, n
    FROM (VALUES ('X', 'SYSTEM_ADMIN', 1),
                 ('A', 'SCHOOL_ADMIN', :actor_count), ('A', 'TEACHER', :actor_count), ('A', 'STUDENT', :student_count),
                 ('B', 'SCHOOL_ADMIN', 1), ('B', 'TEACHER', 1), ('B', 'STUDENT', 1)) AS a(school, role_code, account_count),
         LATERAL generate_series(1, a.account_count) AS n
), inserted_users AS (
    INSERT INTO users (email, full_name, password_hash, status, date_of_birth, created_at, updated_at, created_by, updated_by)
    SELECT format('%s-%s-%s-%s@perf.invalid', :'run_id', lower(accounts.school), lower(accounts.role_code), accounts.n),
           format('%s %s %s %s', :'run_id', accounts.school, accounts.role_code, accounts.n),
           admin.password_hash, 'ACTIVE', DATE '2000-01-01', now(), now(), 'System', 'System'
    FROM accounts, admin
    RETURNING id, email
), account_users AS (
    SELECT inserted_users.id AS user_id, accounts.school, accounts.role_code
    FROM inserted_users
    JOIN accounts ON inserted_users.email = format('%s-%s-%s-%s@perf.invalid', :'run_id',
                                                   lower(accounts.school), lower(accounts.role_code), accounts.n)
), inserted_roles AS (
    INSERT INTO user_roles (user_id, role_id, created_at)
    SELECT account_users.user_id, roles.id, now()
    FROM account_users JOIN roles ON roles.code = account_users.role_code
    RETURNING user_id
)
INSERT INTO school_users (school_id, user_id, start_date)
SELECT schools.id, account_users.user_id, now() - interval '1 day'
FROM account_users JOIN schools ON schools.code = format('%s-%s', :'run_id', account_users.school);
-- TODO: classes, class membership and anything else the feature needs, the way the current schema links them
COMMIT;
```

Skeleton for `cleanup.sql`. First find every table that references `users` or `schools`:
`SELECT conrelid::regclass, confrelid::regclass FROM pg_constraint WHERE confrelid IN ('users'::regclass, 'schools'::regclass) AND contype = 'f';`
Logging in alone creates rows (refresh tokens, device sessions) that must go before the users.

```sql
\set ON_ERROR_STOP on
BEGIN;
CREATE TEMP TABLE run_users ON COMMIT DROP AS
    SELECT id FROM users WHERE email LIKE (:'run_id' || '-%@perf.invalid');
CREATE TEMP TABLE run_schools ON COMMIT DROP AS
    SELECT id FROM schools WHERE code LIKE (:'run_id' || '-%');
-- TODO: feature tables first, children before parents, matched by school_id IN (SELECT id FROM run_schools),
--       by created_by IN (SELECT id::text FROM run_users), or by the run id in a name column
-- TODO: every other table that references users or schools (query above)
DELETE FROM school_users WHERE user_id IN (SELECT id FROM run_users);
DELETE FROM user_roles WHERE user_id IN (SELECT id FROM run_users);
DELETE FROM users WHERE id IN (SELECT id FROM run_users);
DELETE FROM schools WHERE id IN (SELECT id FROM run_schools);
COMMIT;
SELECT (SELECT count(*) FROM users WHERE email LIKE (:'run_id' || '-%@perf.invalid')) AS remaining_accounts,
       (SELECT count(*) FROM schools WHERE code LIKE (:'run_id' || '-%')) AS remaining_schools;
-- TODO: one count per feature table cleaned above; every count must be 0
```

Run SQL through the container. It uses the container's own `POSTGRES_USER` and `POSTGRES_DB`, so no credential is
read or printed:

```powershell
Get-Content "<review folder>/seed.sql" -Raw |
    docker exec -i vox-db-postgres sh -c 'psql -v run_id=<run id> -v actor_count=3 -v student_count=2 -U "$POSTGRES_USER" -d "$POSTGRES_DB"'
```

**Feature data.** Create it in `setup()` through the API, with names that start with the run id, at the reference
volume (`references/journey.md`, section 6). If the right volume is unknown, ask.

## E. Script and run commands

Copy `scripts/k6-feature-template.js` into the review folder and fill in every `TODO`: `PATHS` from the confirmed
journey (one entry per step, with its budget), `ENTRY_POINTS` from the inventory, the feature data, and the extra
probes. k6 v2 rules:

- Executors: `constant-arrival-rate` for a steady rate (an open model, which suits APIs), `per-vu-iterations` with
  many VUs and one iteration each for a burst or a race, `shared-iterations` with one VU for a walk. No `sleep()` at
  the end of an iteration in an arrival-rate executor.
- Tag every request with `name` (the route with `:id` placeholders) and `step` (the step id), through
  `requestParams()`, so per-step thresholds and summaries work.
- REST responses are `ApiResponse` (`{ message, data }`). GraphQL is `POST /graphql` with `{ query, variables }` and
  answers 200 even on errors; `outcomeOf()` maps `errors[0].extensions.classification` (`UNAUTHORIZED`, `FORBIDDEN`,
  `NOT_FOUND`, `BAD_REQUEST`, `INTERNAL_ERROR`) to 401, 403, 404, 400 and 500. GraphQL has no 409 or 422: a refused
  state change is `BAD_REQUEST`.
- Access tokens last `JWT_EXPIRATION_MS` (15 minutes by default) and `setup()` logs in once, so keep every run but
  `sessionContinuity` shorter than that.
- Removed in v2, never use: the `externally-controlled` executor, `--no-summary` (now `--summary-mode=disabled`),
  `--summary-mode=legacy`, and `k6/experimental/redis`. Leave out `handleSummary`: the built-in summary is enough, and
  the jslib `textSummary` would download code. When `--scenario` leaves a scenario out, k6 drops the thresholds tagged
  with it and prints a warning; that warning is expected.

The password goes from `.env` into a variable of the same PowerShell call, and `-e PERF_PASSWORD` with no value
passes it into the container without putting it on the command line. Every run uses `--summary-mode=full` and saves
its output, so per-check results can be read back:

```powershell
$env:PERF_PASSWORD = (Select-String -Path .env -Pattern '^SYS_ADMIN_PASSWORD=(.*)$').Matches[0].Groups[1].Value.Trim().Trim('"')
docker run --rm -i --add-host=host.docker.internal:host-gateway `
    -v "<review folder>:/scripts" `
    -e BASE_URL=http://host.docker.internal:8081 -e RUN_ID=<run id> -e PERF_PASSWORD `
    -e ACTOR_ROLE=school_admin -e ACTOR_COUNT=3 -e WALK_ITERATIONS=20 `
    grafana/k6:2.4.0 run --summary-mode=full --scenario journeyWalk /scripts/review.js *> "<review folder>/k6-journeyWalk.log"
```

| Scenario | Extra variables |
|---|---|
| `journeyWalk` | `WALK_ITERATIONS`, `PATHS` (comma-separated path ids, all by default) |
| `probes` | none |
| `statementCount` | `VOLUME` (child rows per parent): run once at the reference volume and once at twice that |
| `warmUp,steadyLoad` | `LOAD_STEP`, `LOAD_BUDGET_MS`, `RATE` (requests per second), `DURATION` |
| `burst` | `LOAD_STEP`, `LOAD_BUDGET_MS`, `CROWD` (VUs, one request each) |
| `racingRequests` | `RACE_VUS` |
| `sessionContinuity` | `CONTINUITY_PATH`, `REFRESH_BEFORE` (the step id before which the token has expired), `TOKEN_LIFETIME_SECONDS` |

- Pin the image tag. 2.4.0 was the latest release when this skill was written; check Docker Hub and update this file
  when a newer v2 release is out.
- Exit code 99 means a threshold failed. The run itself worked, so read the summary.
- Read the per-step `review_step_duration{scenario:journeyWalk,step:...}` lines and every check from the saved
  output. The probe helper also prints `REVIEW-FAIL <cell id> expected <...> got <...>` for every failed probe, with no
  response body.
- While a run goes, watch `app.log` for errors and pool warnings.
- Each run seeds and cleans its own data. Run `cleanup.sql` and the zero-rows check after every run, whatever its
  result.

## F. SQL statements per request (SPD5)

The app runs with `org.hibernate.SQL` at DEBUG (section C), so each statement is a timestamped line in
`logs/vox.log`. `statementCount` walks the paths with markers around every action:
`REVIEW-MARK BEGIN <step id> <ISO time>` and `REVIEW-MARK END <step id> <ISO time>`, after an `IDLE` window of 10 s
that measures the background noise (schedulers such as the outbox relay query every second).

Run it at the reference volume and again at twice the child rows (`VOLUME`), then count. The snippet prints counts
only, never SQL text:

```powershell
param([string] $MarksLog, [string] $AppLog = 'logs/vox.log', [int] $MarginMs = 250)
$marks = Select-String -Path $MarksLog -Pattern 'REVIEW-MARK (BEGIN|END) (\S+) (\S+Z)' | ForEach-Object {
    [pscustomobject] @{ Kind = $_.Matches[0].Groups[1].Value; Step = $_.Matches[0].Groups[2].Value
                        At = [DateTimeOffset]::Parse($_.Matches[0].Groups[3].Value) } }
$statementTimes = @(Select-String -Path $AppLog -Pattern '^(\S+)\s+DEBUG\b.*\borg\.hibernate\.SQL\b' | ForEach-Object {
    [DateTimeOffset]::Parse($_.Matches[0].Groups[1].Value) })
function Measure-Window($from, $to) {
    @($statementTimes | Where-Object { $_ -ge $from.AddMilliseconds(-$MarginMs) -and $_ -le $to.AddMilliseconds($MarginMs) }).Count
}
$idleBegin = ($marks | Where-Object { $_.Step -eq 'IDLE' -and $_.Kind -eq 'BEGIN' }).At
$idleEnd = ($marks | Where-Object { $_.Step -eq 'IDLE' -and $_.Kind -eq 'END' }).At
$noisePerMs = (Measure-Window $idleBegin $idleEnd) / (($idleEnd - $idleBegin).TotalMilliseconds + 2 * $MarginMs)
$begins = @($marks | Where-Object { $_.Kind -eq 'BEGIN' -and $_.Step -ne 'IDLE' })
$ends = @($marks | Where-Object { $_.Kind -eq 'END' -and $_.Step -ne 'IDLE' })
$windows = for ($index = 0; $index -lt $begins.Count; $index++) {
    $windowMs = ($ends[$index].At - $begins[$index].At).TotalMilliseconds + 2 * $MarginMs
    [pscustomobject] @{ Step = $begins[$index].Step
                        Statements = (Measure-Window $begins[$index].At $ends[$index].At) - $noisePerMs * $windowMs }
}
$windows | Group-Object Step | ForEach-Object {
    [pscustomobject] @{ Step = $_.Name; Requests = $_.Count
                        StatementsPerRequest = [math]::Round(($_.Group | Measure-Object Statements -Average).Average, 1) } } |
    Format-Table -AutoSize
```

Save it as `<review folder>/count-statements.ps1` and run it with
`-MarksLog "<review folder>/k6-statementCount-1x.log"` right after each run, before `logs/vox.log` rolls over. SPD5
passes when the 2x figure is at most the 1x figure plus 2 for the step. Remember that SQL logging slows the app a
little, so say so next to the load numbers.

## G. Query plans (SPD6)

Take the step's main statements from `logs/vox.log` (they hold `?` placeholders, not values), fill in seeded ids, and
run `EXPLAIN (ANALYZE, BUFFERS)` through the container at the reference volume. `EXPLAIN ANALYZE` executes the
statement, so wrap anything that writes in `BEGIN; ... ROLLBACK;`.

## H. Pool and executor sampling (SPD4)

During a load run, sample `GET /actuator/metrics/hikaricp.connections.pending` and
`GET /actuator/metrics/hikaricp.connections.active` every 5 s from PowerShell into `<review folder>/pool.csv`. Use
Monitor for the loop, never a sleep in the foreground. Afterwards, count `RejectedExecutionException`, `Could not
submit` and HikariCP timeout lines in `app.log` (counts only).

## I. Race probe (NSP2)

Run `racingRequests` on its own: `RACE_VUS` VUs, one iteration each, all hitting the same resource at once. Before
cleanup, check the invariant with SQL: no negative balance or quota, exactly one active row, a counter equal to the
number of successful responses, and so on. A broken invariant confirms the race. A holding one does not prove there
is no race, so the cell also needs the `Static` mechanism (lock, version, conditional update, unique constraint).

## J. Session continuity (SAF8)

After the other runs, stop the skill's own app (section K shows how) and start it again as in section C, with
`$env:JWT_EXPIRATION_MS = '60000'` set in the same call. Run `sessionContinuity` with `TOKEN_LIFETIME_SECONDS=60`: the
VU logs in by itself (the refresh cookie lives in its own cookie jar), walks `CONTINUITY_PATH`, waits for the token to
expire before `REFRESH_BEFORE`, checks that the expired token is refused, refreshes (`POST /api/v1/auth/refresh`
with the `refresh_token` cookie and the `X-XSRF-TOKEN` header), finishes the path with the new token, then logs out and
checks that the refresh is refused. Restart the app with the normal settings afterwards if more runs follow.

## K. Restart probe (TRU5)

Only when approved, and only on the skill's own app. Start the path up to the wait, and while the work is in progress
(the status shows it), kill the app process to simulate a crash:

```powershell
Get-NetTCPConnection -LocalPort 8081 -State Listen | Select-Object -ExpandProperty OwningProcess -Unique |
    ForEach-Object { Stop-Process -Id $_ -Force }
```

Then stop the background `bootRun` task, start the app again as in section C, and poll the status until it reaches a
terminal state or the worst case from config has passed. Record the time and the final status.

## L. Log privacy count (SAF6)

After the last run, count over the app output of this review only (`app-*.log` holds every log line of each start,
and `logs/vox.log` also holds older runs), without printing any line:

```powershell
$logs = Get-ChildItem "<review folder>/app-*.log"
$patterns = [ordered] @{
    password     = [regex]::Escape($env:PERF_PASSWORD)
    jwt          = 'eyJ[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{10,}'
    seeded_email = 'perf\.invalid'
    seeded_name  = [regex]::Escape('<run id>') + ' [ABX] (SYSTEM_ADMIN|SCHOOL_ADMIN|TEACHER|STUDENT) \d+'
}
foreach ($name in $patterns.Keys) {
    $count = @(Select-String -Path $logs -Pattern $patterns[$name]).Count
    "{0}: {1}" -f $name, $count
}
```

Add a pattern for refresh tokens and OTPs when the journey has them. Set `$env:PERF_PASSWORD` in the same call, as in
section E. The k6 markers are in the k6 logs, not in `app-*.log`, so every match is the app logging a secret or
seeded personal data.

## M. What goes in the report

- One row per scenario: profile, requests, RPS, p50, p95, p99, error rate and the threshold result.
- The per-step times for the Journey table, and the statements per request at 1x and 2x.
- The invariants checked and their values.
- Cleanup proof: the remaining-row counts (all zero), or exactly what was left and why.
- Caveat: a single local instance on Docker Desktop with SQL logging on, so the numbers compare runs and are not
  production capacity.
