# Performance track

## A. Static review (always; nothing runs)

Record each item with `file:line` and its likely impact under realistic volume.

- **Queries.** Watch for N+1: a repository call inside a loop or per element, a mapper that loads relations one by
  one, or a GraphQL field resolver without a DataLoader or `@BatchMapping`. Also flag unbounded reads (`findAll`,
  no `LIMIT`, an `IN` list with no size limit), and new `WHERE`, `JOIN` or `ORDER BY` columns with no index in
  `db/migration` (search for `CREATE INDEX`). Check fetch joins combined with paging (Hibernate then pages in memory),
  and count queries on large tables.
- **Transactions.** Long transactions, external calls (HTTP, gRPC, S3, SMTP) inside a transaction, and queries
  that are not read-only.
- **Payloads and memory.** Lists returned without paging, and whole files loaded into memory (POI `XSSFWorkbook`
  holds the entire workbook).
- **Threads.** Executor pool and queue sizes against the expected load (`AsyncConfig`). Details are in the
  concurrency track.
- **Caching.** Redis TTLs, cache stampede on expiry, unbounded key growth.
- **Hot-path logging.** `info`-level logs per request or per row.

## B. Load test with k6

### Ask before running (Step 3 of SKILL.md)

Put the whole plan in the question, so one answer approves it:

- What gets started: `docker compose up -d postgres redis kafka` if they are not running, and the app with
  `./gradlew bootRun` from the current working tree. Reuse an app already listening on 8081 only if the user
  confirms that it runs the code under review.
- The operations under test, the load profile (executor, rate or VUs, duration) and the thresholds.
- The sample data: what is seeded and how, and that it is removed at the end of the same run.
- The side effects found on the path, and how each is neutralized: email (SMTP), push (Firebase, off unless
  `FIREBASE_ENABLED`), payments (PayOS), S3 uploads, AI grading over Kafka or gRPC, outbox events. Never load-test
  against a real payment provider or real mail recipients. If a side effect cannot be neutralized, leave that
  operation out and say so.
- The target is local only: `localhost` / `host.docker.internal` and the `vox-db-postgres` container.

If the user says no, report the static part and write "k6 not run (declined)".

### Prepare

1. Choose a run id, `perf-<yyyyMMddHHmmss>`, and a run folder, `<scratchpad>/k6/<run id>/`. The script, seed SQL,
   cleanup SQL and outputs all go there. Nothing goes into the repository.
2. Check the infrastructure with `docker ps` (`vox-db-postgres`, `vox-redis`, `vox-kafka`) and start what is missing.
3. Start the app with `./gradlew bootRun` in the background, and wait for the "Started" log line or for any public
   endpoint to answer (use Monitor, not sleep). Note the JVM and pool settings it runs with.
4. Read the current schema for every table you will seed (the latest migrations, then the entity). The skeletons
   below match the schema when this skill was written; trust the migrations over them.

### Sample data: created and removed in the same run

One run, in this order:

1. `seed.sql` creates the accounts (SQL is needed only for what the API cannot create).
2. k6 `setup()` logs in and creates the feature data through the API, as the seeded accounts.
3. The scenarios run.
4. k6 `teardown()` deletes, through the API, what `setup()` created.
5. `cleanup.sql` deletes the accounts and anything else tagged with the run id.
6. A check query confirms that zero rows with the run id remain.

Run step 5 even when k6 fails: if `setup()` throws, k6 skips `teardown()`.

**Accounts.** The API cannot create users with known passwords (registration goes through OTP email), and
passwords are Argon2 hashes, which SQL cannot compute. So seeded accounts copy `password_hash` from the system
admin row that `SystemAdminUserInitializer` creates, and they log in with `SYS_ADMIN_PASSWORD`. If the copied
hash does not match that password (someone changed it), `setup()` fails on the first login. Ask the user then.
Emails are `<run id>-<role>-<n>@perf.invalid`; `.invalid` is a reserved TLD and can never be delivered.

**Feature data.** Create it in `setup()` through the API, with names starting with the run id. Seed enough volume
for the measured query to behave as it would in production (realistic page sizes and row counts). If the right
volume is unknown, ask.

Skeleton for `seed.sql` (check every column against the current schema first):

```sql
\set ON_ERROR_STOP on
BEGIN;
INSERT INTO users (email, full_name, password_hash, status, date_of_birth, created_at, updated_at, created_by, updated_by)
SELECT format('%s-%s-%s@perf.invalid', :'run_id', lower(:'role_code'), n),
       format('%s %s %s', :'run_id', :'role_code', n),
       admin.password_hash, 'ACTIVE', DATE '2000-01-01', now(), now(), 'System', 'System'
FROM (SELECT u.password_hash
      FROM users u
      JOIN user_roles ur ON ur.user_id = u.id
      JOIN roles r ON r.id = ur.role_id AND r.code = 'SYSTEM_ADMIN'
      ORDER BY u.created_at
      LIMIT 1) AS admin,
     generate_series(1, :account_count) AS n;
INSERT INTO user_roles (user_id, role_id, created_at)
SELECT u.id, r.id, now()
FROM users u JOIN roles r ON r.code = :'role_code'
WHERE u.email LIKE (:'run_id' || '-' || lower(:'role_code') || '-%@perf.invalid');
-- TODO: link the accounts to a school (and class) the way the current schema does
COMMIT;
```

Skeleton for `cleanup.sql`. Find every table that references `users` first:
`SELECT conrelid::regclass FROM pg_constraint WHERE confrelid = 'users'::regclass AND contype = 'f';`
Logging in alone creates rows (refresh tokens, device sessions) that must be deleted before the users.

```sql
\set ON_ERROR_STOP on
BEGIN;
CREATE TEMP TABLE run_users ON COMMIT DROP AS
  SELECT id FROM users WHERE email LIKE (:'run_id' || '-%@perf.invalid');
-- TODO: feature tables first, children before parents, matched by created_by IN (SELECT id::text FROM run_users)
--       or by the run id prefix in a name column
-- TODO: every other table that references users (query above)
DELETE FROM user_roles WHERE user_id IN (SELECT id FROM run_users);
DELETE FROM users WHERE id IN (SELECT id FROM run_users);
COMMIT;
SELECT count(*) AS remaining_accounts FROM users WHERE email LIKE (:'run_id' || '-%@perf.invalid');
```

Run SQL through the container. It uses the container's own `POSTGRES_USER` and `POSTGRES_DB`, so no credential is
read or printed:

```powershell
Get-Content <run folder>/seed.sql -Raw |
  docker exec -i vox-db-postgres sh -c 'psql -v run_id=<run id> -v role_code=TEACHER -v account_count=5 -U "$POSTGRES_USER" -d "$POSTGRES_DB"'
```

### Script

Copy `scripts/k6-feature-template.js` into the run folder and fill in every `TODO`. k6 v2 rules:

- Executors: `constant-arrival-rate` for latency at a fixed request rate (an open model, which suits APIs);
  `ramping-arrival-rate` to find where latency bends; `per-vu-iterations` with many VUs and one iteration each for a
  race probe. Do not put `sleep()` at the end of an iteration in an arrival-rate executor.
- Group URLs that contain ids with `tags: { name: 'GET /api/v1/things/:id' }`, so the metrics stay readable.
- REST responses are `ApiResponse` (`{ message, data }`). GraphQL is `POST /graphql` with `{ query, variables }`, and
  it answers 200 even on errors, so check that `errors` is absent.
- Log in once per account in `setup()` and pass the tokens on through the returned data (JSON only). Access tokens
  last `JWT_EXPIRATION_MS` (15 minutes by default), so keep each run shorter than that.
- Start with these thresholds: `http_req_failed` `rate<0.01`, `http_req_duration` `p(95)<500` and `p(99)<1500`,
  and `checks` `rate>0.99`. Apply them only to the measured scenario. Use the user's targets if they gave any.
- Removed in v2, so never use them: the `externally-controlled` executor, `--no-summary` (now
  `--summary-mode=disabled`) and `k6/experimental/redis`. Leave out `handleSummary`: the default end-of-test
  summary is enough, and the jslib `textSummary` would download code from jslib.k6.io.

### Run

The password comes from `.env` into a variable of the same PowerShell call (state does not carry over between
calls). `-e PERF_PASSWORD` with no value passes it into the container without putting it on the command line:

```powershell
$env:PERF_PASSWORD = (Select-String -Path .env -Pattern '^SYS_ADMIN_PASSWORD=(.*)$').Matches[0].Groups[1].Value.Trim().Trim('"')
docker run --rm -i --add-host=host.docker.internal:host-gateway `
  -v "<run folder>:/scripts" `
  -e BASE_URL=http://host.docker.internal:8081 -e RUN_ID=<run id> -e ACCOUNT_ROLE=teacher -e ACCOUNT_COUNT=5 `
  -e RATE=20 -e DURATION=2m -e PERF_PASSWORD `
  grafana/k6:2.4.0 run --scenario warmUp,steadyLoad /scripts/load-test.js
```

- Pin the image tag. 2.4.0 was the latest when this skill was written; check Docker Hub and update this file when a
  newer v2 release is out.
- Exit code 99 means a threshold failed. The run itself worked, so read the summary.
- While it runs, watch the app log for errors and pool warnings. Afterwards, run `EXPLAIN (ANALYZE, BUFFERS)` on the
  feature's main queries with the seeded volume, and read `/actuator/metrics/hikaricp.connections.pending` if it is
  reachable.
- Then run `cleanup.sql` and the zero-rows check, whatever the k6 result.

### Race probe (multi-threaded targets)

Run the `racingRequests` scenario on its own (`--scenario racingRequests`): many VUs, one iteration each, all hitting
the same resource at once. Afterwards, check the invariant with SQL before cleanup: no negative balance or quota,
exactly one active row, a counter equal to the number of successful responses, and so on. A failing invariant
confirms the race. A passing one does not prove there is none, so say so in the report. Each run (load or race)
seeds and cleans its own data.

### What goes in the report

- One row per scenario: profile, requests, RPS, p50, p95, p99, error rate, and the threshold result.
- The invariants checked and their values.
- Cleanup proof: the remaining-row counts (all zero), or exactly what was left and why.
- Caveat: a single local instance on Docker Desktop, so the numbers compare runs and are not production capacity.
