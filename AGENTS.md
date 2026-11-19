# AGENTS.md

Vox is the backend for evaluating oral assessments of high school students (exams, class tests, practice
sessions, AI grading, appeals, school subscriptions/quota/billing). Spring Boot 4.1, Java 25, Gradle, PostgreSQL,
Kafka, Redis, gRPC, GraphQL + REST. Package root: `com.sep.vox`.

## Refactoring stage notice

This repository is in a refactoring stage, so this file is the agreed boundary of what agents may do. If a task asks for
something this file does not cover, or something that goes against it (a different structure, naming, layer rule or
convention, or work in an area it does not describe), do not start. Say which part of the task falls outside
`AGENTS.md`, and ask the user to confirm before you continue. A confirmation applies to that task only, not to later ones.

## Commands

```bash
./gradlew compileJava                       # fastest "does it compile" check
./gradlew test                              # unit suite: excludes tests tagged "integration"
./gradlew integrationTest                   # only tests tagged "integration" (Docker: Testcontainers Postgres 18)
./gradlew check                             # both suites
./gradlew test --tests '*CarryQuota*'       # run a subset
./gradlew test --tests '*LayeredArchitectureTests'
./gradlew clean build --no-daemon           # exactly what CI runs (.github/workflows/ci-build.yml)
./gradlew bootRun                           # needs .env / env vars, Postgres, Redis, Kafka (docker-compose.yml)
```

- Always use the wrapper `./gradlew`, never a system Gradle. JDK 25 is enforced by the toolchain.
- Env vars are listed in `README.md`; every `${...}` in `src/main/resources/application.yaml` must be set. Never commit `.env`
  or secrets.
- Local UIs: Swagger `/swagger-ui/index.html`, GraphiQL `/graphiql`.
- Tests run with `user.timezone=Asia/Ho_Chi_Minh` (set in `build.gradle`; the Dockerfile sets `TZ` to match). Do not
  write code or tests that depend on the machine's default timezone.

## Architecture (hexagonal, enforced by ArchUnit)

```
interfaces (rest, graphql, grpc, kafka) --\
                                           >--> application --> domain
infrastructure (persistence, security...) -/
```

| Layer | Path under `src/main/java/com/sep/vox` | Holds |
|---|---|---|
| domain | `domain/{model,repository,service,valueobject,dto,common}` | Plain-Java models and rules, repository **interfaces**. No Spring/JPA. |
| application | `application/port/input/{usecase,command,query,service}`, `application/port/output`, `application/query`, `application/event`, `application/response` | One use case per class, commands/queries, ports the app needs from the outside world |
| infrastructure | `infrastructure/{persistence,security,config,worker,event,client,service}` | JPA entities, repository implementations, Kafka/Redis/S3/PayOS/Firebase adapters, scheduled jobs |
| interfaces | `interfaces/{rest,graphql,grpc,kafka}` | Controllers, request DTOs, consumers |

Hard rules (`src/test/.../architecture/LayeredArchitectureTests.java` fails the build on violation):

- `domain` imports nothing from `application`, `infrastructure`, `interfaces`.
- `application` imports nothing from `infrastructure`, `interfaces`. To reach the outside world, declare a port in
  `application/port/output` and implement it in `infrastructure` (`QuotaPricingPort` / `QuotaPricingService` is the model example).
- `interfaces` never imports `infrastructure`. Controllers go through use cases.

### Where things go when adding a feature

1. Domain model / value object / repository interface -> `domain/...`.
2. `XxxCommand` (write) or `XxxQuery` (read) record -> `application/port/input/command|query`.
3. `XxxUseCase implements IUseCase<Input, Output>` -> `application/port/input/usecase/<feature>/`. Logic shared by several
   use cases goes in `application/port/input/service/`.
4. JPA entity (`XxxJpaEntity`), Spring Data repository (`SpringDataXxxRepository`), mapper, and adapter
   (`XxxRepositoryImpl implements XxxRepository`) -> `infrastructure/persistence/{entity,repository,mapper,adapter}`.
   Read-heavy/reporting queries are separate: interface in `application/query/repository`, implementation in
   `infrastructure/persistence/query`.
5. Entry point: REST controller + request record (`interfaces/rest`) and/or GraphQL controller + schema
   (`interfaces/graphql`, `src/main/resources/graphql/*.graphqls`).
6. Schema change -> a new Flyway migration (see Database).

Look at an existing feature end to end before writing a new one (e.g. `CreateExamUseCase`, `ExamController`,
`ExamBlueprintRepositoryImpl`), and follow the rules in Scope when the pattern is unclear.

## Conventions that are easy to get wrong

- **Pagination is 1-based at every boundary.** Only `infrastructure/persistence/adapter` and `.../query` convert to Spring
  Data's 0-based pages (`PageRequest.of(page - 1, ...)`, `setFirstResult((page - 1) * size)`). `PagingConventionTests` scans
  source text for this; a wrong offset passes every behavioural test.
- **Authorization** is `@PreAuthorize("hasRole(...)")` on controller methods. Roles in use: `SYSTEM_ADMIN`, `SCHOOL_ADMIN`,
  `TEACHER`, `STUDENT`. A new endpoint without it is a security bug. School-scoped data must also be checked against
  the caller's school in the use case, not only by role.
- **Errors**: throw the exceptions in `application/exception` (`NotFoundException`, `ForbiddenException`,
  `DuplicatedException`, `QuotaExceededException`, ...). REST maps them in `interfaces/rest/exception/GlobalExceptionHandler`.
  Do not return error payloads by hand.
- **REST responses** are `ResponseEntity<ApiResponse<T>>`. Use cases are `@Service` + `@Transactional` and take one
  command/query object.
- **Events**: payload classes in `application/event` are versioned (`...PayloadV1`); change a payload by adding a `V2`,
  not by  editing `V1`. Outgoing events are written to an outbox table and relayed to Kafka by `OutboxEventScheduler`
  (disabled in tests via `app.outbox.scheduler.enabled=false`). The topic is set with the `@ExternalEventTopic` annotation
  on the event class. Kafka consumers live in `interfaces/kafka/consumer`.
- **Dependency injection**: constructor injection with explicit constructors; fields are `private final`.
- **Types**: prefer explicit types over `var` in new code.
- **Language**: everything is English: identifiers, comments, Javadoc, test documentation, commit messages. Existing
  comments are largely Vietnamese and are being migrated to English. Write every new or edited comment in English. When a
  Vietnamese comment sits in code you are already changing, translate it faithfully in the same change: keep its meaning and
  its "why", do not shorten or delete it. Do not translate comments in files you are not otherwise touching unless the task
  is the translation itself. Leave string literals that are user-facing text (messages, email templates) as they are.
- **Do not hand-edit** `build/`, `.gradle/`, `logs/`.

## Database

- Flyway migrations: `src/main/resources/db/migration/V<n>__snake_case_description.sql`, next number = highest + 1.
- **Never edit a migration that has been merged**; add a new one. `ddl-auto` is `validate`, so entity and migration must agree
  or the app will not start.
- IDs are `UUID` generated by the database (`uuidv7()`); entities use `@Generated(event = EventType.INSERT)` with
  `insertable = false`. Follow an existing entity.
- Postgres 18 in CI, docker-compose and Testcontainers; keep it consistent.

## Testing

- Pure logic (services, use cases): `@ExtendWith(MockitoExtension.class)` with `@Mock` fields and `@InjectMocks` on the
  class under test, plus AssertJ, no Spring context (see `LoginUseCaseTests`). Strict stubs are on, so an unused stub
  fails the test: remove it or move it into the tests that use it, never hide it with `lenient()`. If a constructor is
  awkward for `@InjectMocks`, construct the class explicitly in `@BeforeEach`. Tests that still use hand-written
  `mock(...)` in `@BeforeEach` are migrated gradually (per feature package, starting with `application/usecase/auth`);
  do not convert them as a drive-by.
- REST controllers: `@RestControllerTest(XxxController.class)` (`support/`), a `@WebMvcTest` slice driven with
  `MockMvcTester`, with use cases and ports as `@MockitoBean`. It needs no Docker and runs in `test`. Its security is the
  test-only `config/WebMvcTestSecurityConfiguration`, not `SecurityConfig`, so CSRF, CORS and the JWT filter are not covered.
  See `AuthControllerTests`. Every role-protected endpoint group needs an authorization test: 401 with
  `@WithAnonymousUser`, 403 with a wrong role via `@WithMockUser(roles = ...)`, and success with the right role. Send
  a valid body in the 403 test, because `@Valid` runs before `@PreAuthorize` and an invalid body would answer 400.
- GraphQL controllers: direct-call unit tests stay as they are. Where authorization matters, use
  `@GraphQlControllerTest(XxxController.class)` (`support/`), which runs the real schema, `GlobalExceptionResolver`
  and `@PreAuthorize` with no Docker. See `MyClassAuthorizationGraphQlTests`.
- Repository / query code: annotate the class with `@RepositoryTest` (`support/`), a `@DataJpaTest` against the real Postgres
  from `config/TestcontainersConfiguration`. Full-context tests use `@IntegrationTest` (`support/`). Do not use raw
  `@DataJpaTest` / `@SpringBootTest`, and do not extend a base class for the database.
- Suites are split by the JUnit tag `integration`: `@RepositoryTest` and `@IntegrationTest` carry it, so those tests run in
  `integrationTest` and `./gradlew test` needs no Docker. A test that starts its own container or otherwise needs Docker
  without these annotations must carry `@Tag("integration")` itself. `TestSuiteSeparationTests` fails the build if a
  `@SpringBootTest` or `@DataJpaTest` class lacks the tag.
- `application-test.yaml` deliberately turns off Kafka listeners, the admin client and a fixed gRPC port so tests cannot
  touch a developer's running app or broker. Do not undo this; if a test needs Kafka, give it its own container.
- A bug fix needs a regression test that fails without the fix. Name test classes `XxxTests` in the mirrored package.
- Tests marked as architecture rules (`src/test/.../architecture`) are guardrails: fix the code, never loosen the rule to get green.

## Scope

- You may read any file in this repository, and modify only what the task requires (if the task asks to read outside this repository, you can do that but don't modify anything on it).
- Do what is asked and no more. Keep each change small and focused on the request. No drive-by refactors, renames,
  reformatting, or "clean up" of code you were not asked to touch. The one exception is extracting duplicated logic (see
  Coding rules). Report problems you notice instead of fixing them.
- Read a file before editing it, and look at neighbouring code before writing new code. Follow an established pattern when it
  is clear and match its naming (`CreateXxxUseCase`, `XxxJpaEntity`, `XxxRepositoryImpl`, `XxxDto`). If the pattern is
  ambiguous, ask instead of copying the nearest file. Parts of this repository are inconsistent, so "nearest file" is not
  proof of "the pattern".
- When the task itself is a repo-wide rename, change every occurrence (identifiers, files, DB/GraphQL/Kafka/URL names,
  config keys, docs), then report cross-system impact (topics, event types, env vars, DB names, URL paths).

## Coding rules

- **Small functions.** A function or method is at most 25 lines, counted from its signature to its closing brace. A very
  large feature may use up to 30.
- **One place for each piece of logic.** Before writing logic, search the other files for the same or nearly the same
  logic. If it exists, extract it into one shared method that all callers use, so a change is made in one place. Extract
  only logic that really exists in more than one place, never for hypothetical future reuse, unless the task asks for that.
- **Names say what the code does.** Function, method and variable names describe what they do. Long names are fine.
  Avoid abbreviations and vague words (`data`, `info`, `handle`, `process`, `manager`).
- **Self-explaining code.** Do not write comments that restate what a line does. Add a doc comment to a function, method
  or class only when its contract, side effects or failure cases cannot be read from its name and signature.
- **No N+1 queries.** Never query inside a loop or per element of a collection. Fetch in bulk (`IN` queries, joins, fetch
  graphs, batch methods on the repository). This also applies to GraphQL field resolvers, so use batch mapping there.
- **Read the docs for the version in use.** Before using a package or framework API, read the official documentation
  for the version this project includes (Spring Boot 4.1 and Spring Framework 7 differ from older tutorials). Do not use
  deprecated APIs.
- **New dependencies** must be the latest release, or the version managed by the Spring Boot BOM (leave the version out when
  the BOM manages it). Adding one needs approval first (see Ask first).

## Ask first

Stop and ask the user before you:

- add a dependency, or change the Spring Boot version (or any pinned version in `build.gradle`);
- change a public API contract: REST/GraphQL schema, gRPC proto, Kafka topics and event payloads, JWT claims, or anything
  else consumed by the frontend, the AI grading service or the desktop app;
- create a file that does not fit the pattern `X + Command`, `X + Query`, `X + Controller`, `X + UseCase`,
  `X + Repository`, or the files directly related to them (request/response records, entity, adapter, mapper, migration,
  tests for those);
- implement something whose pattern is ambiguous (much of this repository is, so expect to ask);
- delete or rewrite a Flyway migration, touch security config or JWT/OAuth code, or change anything that moves money
  (orders, payments, quota, school balance);
- install anything, or do anything outward-facing or hard to reverse (push, open a PR, publish, delete data or files,
  call external services, change shared infrastructure).

When you ask, state the options you see and recommend one.

## Finishing a task

- Verify with `./gradlew compileJava` and the narrowest relevant `--tests` filter. Run the full build only if feasible
  (needs Docker, via `integrationTest`). Say plainly what you did not run.
- Final report: what changed, what you verified, and the problems you noticed but did not fix.
- Never log or print secrets, tokens, OTPs, or student recordings/PII. Never commit `.env`.
- Commits: short imperative subject (`fix school balance query`). PRs target `default`. Only commit or push when asked.

## Pointers

- `README.md` - env vars and local UIs
- `docker-compose.yml` - local Postgres, Redis, Kafka
- `.github/workflows/` - CI (`ci-build.yml`), CD, CodeQL
