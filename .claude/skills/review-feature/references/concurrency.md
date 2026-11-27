# Concurrency track: review every thread path to its end

## When the target is multi-threaded

Any one of these makes the track mandatory:

- It hands work to another thread: `@Async`, an executor bean (see `infrastructure/config/AsyncConfig`; at the time
  of writing `fileImportExecutor`, `practiceGenerationExecutor` and `pushExecutor`), `CompletableFuture`,
  `parallelStream`, `new Thread`, virtual threads, or an async `@EventListener` / `@TransactionalEventListener`.
- It runs on a scheduler thread: `@Scheduled` jobs in `infrastructure/worker` (`OutboxEventScheduler`,
  `ImportJobDispatcher`, `PendingOrderReconciler`, ...).
- It is driven by messages: `@KafkaListener` consumers in `interfaces/kafka/consumer` and
  `infrastructure/event/internal/consumer`, plus the outbox relay that feeds them.
- It resolves GraphQL fields asynchronously (DataLoaders, `CompletableFuture` or `Mono` returns), or serves a gRPC
  stream.
- Its correctness depends on concurrent requests touching the same rows or keys. This covers check-then-act on
  quota, balance, counters or status, "only one active X" rules, unique business keys, Redis keys, and queries with
  `@Version`, `@Lock`, `FOR UPDATE` or `SKIP LOCKED`.
- It runs on every app instance at once. Schedulers and consumers do when the app is scaled out.

Money, quota and billing paths are always in this track.

## What "to the end" means

1. **Build the thread map first**, before judging anything. One row per path:

   | Path | Starts at (`file:line`, thread or executor) | Hand-offs (each hop: thread, executor, transaction) | Shared state (tables and rows, Redis keys, fields) | Ends at |
   |---|---|---|---|---|

   "Ends at" lists every terminal state, not only the happy one: commit and response, offset committed, event
   published, exception in the worker, rejection by a full pool, timeout, retries exhausted or dead-lettered,
   duplicate delivery, a second instance running the same job, and app shutdown mid-task.
2. **Follow each path through every class it touches**, including framework behaviour between your code: proxies,
   transaction synchronization, the listener container's error handler, the executor's rejection policy, and
   graceful shutdown. Read the config that governs it: `AsyncConfig`, the Kafka listener settings in
   `application.yaml`, and the scheduler pool size.
3. **A path is done only when** its end is reached and every checklist item below has been answered for each hop,
   either with evidence (`file:line`) or with "N/A: <reason>". "Probably fine" is not an answer.
4. **No cap, no sampling.** Twelve consumers means twelve rows. Never write "the others are similar". If the review
   gets long, keep going, because context is summarized automatically. Keep the map in the task list (one task per
   path) so a summary cannot lose it.
5. **Do not hand back with open rows.** A path that leaves the repository (the AI grading service, `vox-streaming`,
   PayOS) ends at the boundary: write "ends outside the repo at <topic / endpoint>" and state the contract you
   assumed (delivery guarantee, idempotency, ordering).

## Checklist for each hop

### Transactions

- `@Transactional` does not follow work onto another thread. Work started inside a transaction can run before the
  commit (it reads missing or stale rows) or after a rollback (it acts on data that no longer exists). Look for
  `execute`, `submit` or `@Async` calls inside a transaction that do not go through an after-commit hook
  (`TransactionSynchronization#afterCommit`, `@TransactionalEventListener(phase = AFTER_COMMIT)`) or the outbox.
- Self-invocation: `@Async` or `@Transactional` on a method called from the same class does nothing.
- Row locks held while calling HTTP, gRPC, S3, SMTP or Firebase.
- Entities passed across threads: pass ids and reload in the new thread. A lazy load outside its session throws.
- `@Modifying` statements skip the persistence context (stale entities in the same session) and auditing (the
  adapter must set `updated_at` / `updated_by`, per `AGENTS.md`).

### Thread context

- `SecurityContext` is thread-local. Check which executors are wrapped
  (`DelegatingSecurityContextAsyncTaskExecutor`); schedulers and consumers run without a user. Auditing then writes
  `"System"`, and work done on a user's behalf must run in `AuditActorScopePort.callAs` (`AGENTS.md`, Audit columns).
- MDC or other `ThreadLocal` values set in a pooled thread must be cleared in `finally`.

### Executors

- Which executor actually runs it? `CompletableFuture.supplyAsync` without an executor uses the shared
  `ForkJoinPool.commonPool`, which has no security context and few threads.
- Pool size, queue capacity and rejection policy: what happens to the task when the pool is full? `AbortPolicy`
  throws, so check that the caller leaves recoverable state (the `fileImportExecutor` Javadoc relies on this).
  `CallerRunsPolicy` runs the task on the caller's thread (a request or scheduler thread), which can block it.
- Errors: is every future joined or handled with `exceptionally` / `handle`? An exception in an `@Async void` method
  is only logged, so a lost error can leave a row stuck in an in-progress state forever. Who recovers it (a sweeper,
  a reconciler, nobody)?
- Shutdown: `waitForTasksToCompleteOnShutdown`, `awaitTerminationSeconds`, `server.shutdown: graceful`. What state
  does an interrupted task leave behind?

### Schedulers

- The default Spring scheduler has a single thread unless `spring.task.scheduling.pool.size` says otherwise, so a
  slow job delays all the others.
- Overlap: `fixedRate` against a run longer than the interval; `fixedDelay` against catch-up after downtime.
- Several instances: each instance runs the job. It needs a database-level claim (`FOR UPDATE SKIP LOCKED`, a
  conditional `UPDATE ... WHERE status = ?` that checks the affected row count, or a unique constraint). Name the
  mechanism, or report that there is none.
- Batch size: what happens with 10,000 pending rows?

### Kafka consumers

- Delivery is at least once, so the handler must be idempotent (dedupe on an event id or a natural key, backed by a
  unique constraint).
- The order of offset commit and database commit: what happens if the process dies between them, or the database
  commit fails after the side effects?
- Error handling: retries, backoff, dead-letter topic, a poison message blocking the partition, deserialization
  errors.
- Ordering exists only per partition key. Does the logic assume more? Is listener concurrency set higher than the
  partition count?
- `application-test.yaml` turns listeners off, so consumer tests call the handler directly. Note what that leaves
  untested.

### Outbox

- The event row is written in the same transaction as the state change.
- The relay claims rows safely when several instances run it, and downstream tolerates duplicates.

### Shared state and races

- Check-then-act on rows (read, decide, write) with no lock or version gives lost updates and double spending.
  Look for a conditional update (`SET balance = balance - :amount WHERE balance >= :amount`, then check the row
  count), `@Version` (is `OptimisticLockException` retried or mapped?), or `@Lock(PESSIMISTIC_WRITE)` (lock order and
  deadlocks, lock timeout).
- "Only one" rules need a unique constraint as the last line of defence, and its violation must map to
  `ResourceDuplicatedException`, not a 500.
- Spring beans are singletons: look for mutable fields, non-thread-safe types shared between calls (`HashMap`,
  `ArrayList`, `SimpleDateFormat`, POI workbooks), unsynchronized lazy initialization, and home-made caches.
- Redis: use atomic operations (`INCR`, `SET NX PX`). A lock needs a TTL longer than the work and an owner token, and
  only its owner may release it.
- Virtual threads, if used: since JDK 24 `synchronized` no longer pins a carrier thread, but native calls still do,
  and per-thread caches multiply.

## Tests for concurrent behaviour

- A race needs a test that runs the competing operations at the same moment: an `ExecutorService` with a
  `CountDownLatch` start gate, in an `@IntegrationTest` or `@RepositoryTest` against the real Postgres, because mocks
  cannot show locking. It must assert the invariant afterwards. Coordination uses latches and bounded waits, never
  `Thread.sleep`.
- A unit test with mocked repositories proves nothing about a race. If that is the only test, report the gap.

## Dynamic proof

When a suspected race is reachable over HTTP, propose a k6 race probe (`references/performance-k6.md`, "Race
probe"): many VUs fire the same operation at the same resource at once, then SQL checks the invariant. A failing
probe confirms the race. A passing probe does not prove there is none, so say that in the report.

## In the report

Put the full thread map in the report, one row per path, with both success and failure ends and the findings for each
path. A row whose end is outside the repository says so.
