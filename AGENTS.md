# AGENTS.md

Vox is the backend for evaluating oral assessments of high school students (centralized assessments, class tests, practice
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
| interfaces | `interfaces/{rest,graphql,grpc,kafka}` | Controllers (`RestXxxController`, `GraphQLXxxController`), REST request DTOs in `rest/dto/request/<feature>/`, GraphQL inputs in `graphql/input/<feature>/`, DataLoader configs in `graphql/dataloader`, loader keys in `graphql/key`, scalars in `graphql/scalar`, consumers |

Hard rules (`src/test/.../architecture/LayeredArchitectureTests.java` fails the build on violation):

- `domain` imports nothing from `application`, `infrastructure`, `interfaces`.
- `application` imports nothing from `infrastructure`, `interfaces`. To reach the outside world, declare a port in
  `application/port/output` and implement it in `infrastructure` (`QuotaPricingPort` / `QuotaPricingService` is the model example).
- `interfaces` never imports `infrastructure`. Controllers go through use cases.
- `domain` is plain Java: it depends only on `java..` and on `domain` itself. No Spring, Lombok, Jackson or any other
  library or framework type.
- `application` depends only on plain Java, `domain`, `application`, slf4j, `@Service`, `@Transactional` (with its enums)
  and the Lombok annotations `@RequiredArgsConstructor` and `@Slf4j`. Jackson, POI, Spring DAO/scheduling types and the like
  go behind a port in `application/port/output` (`JsonSerializationPort` for JSON). The classes that still break this
  are listed in `APPLICATION_EXEMPTIONS` (`LayeredArchitectureTests`) and `LombokUsageConventionTests`; those lists may
  only shrink, so remove a class from them when you fix it, and never add to them.
- `infrastructure` and `interfaces` have no restriction on libraries.

### Where things go when adding a feature

1. Domain model / value object / repository interface -> `domain/...`.
2. `XxxCommand` (write) or `XxxQuery` (read) record -> `application/port/input/command/<feature>/` or `.../query/<feature>/`
   (never directly in `command` or `query`; the feature is the one of the use case with the same name).
3. `XxxUseCase implements IUseCase<Input, Output>` -> `application/port/input/usecase/<feature>/`. Logic shared by several
   use cases goes in `application/port/input/service/`.
4. JPA entity (`XxxJpaEntity`), Spring Data repository (`SpringDataXxxRepository`), mapper, and adapter
   (`XxxRepositoryImpl implements XxxRepository`) -> `infrastructure/persistence/{entity/<feature>,repository,mapper,adapter}`.
   Read-heavy/reporting queries are separate: interface in `application/query/repository`, implementation in
   `infrastructure/persistence/query`.
5. Entry point: `RestXxxController` + request record (`interfaces/rest/controller`, `interfaces/rest/dto/request/<feature>/`)
   and/or `GraphQLXxxController` + schema (`interfaces/graphql`, `src/main/resources/graphql/*.graphqls`). The request
   record turns itself into the command or query with a static `toCommand` or `toQuery` (a record with several
   conversions names each `to<CommandClassName>`; a list is `toCommands`). A separate mapper class is only for
   conversions that hold logic (multipart files, `ViewMyAssessmentsQueryMapper`, Kafka mappers);
   the allowed classes are listed in `InterfacesStructureConventionTests` and the list may only shrink.
   A GraphQL input field the client may omit is an `ArgumentValue<T>`, never a `Map`. An update field whose column is
   nullable carries a `<field>Provided` flag in its command: omitted keeps the stored value, an explicit null clears it,
   any other value behaves as before. An explicit null on a field whose column is required means "not given".
6. Schema change -> a new Flyway migration (see Database).

Look at an existing feature end to end before writing a new one (e.g. `CreateAssessmentUseCase`, `RestAssessmentController`,
`AssessmentBlueprintRepositoryImpl`), and follow the rules in Scope when the pattern is unclear.

## Conventions that are easy to get wrong

- **Pagination is 1-based at every boundary.** Two shared classes are the only places that handle it. Controllers validate
  every `page`/`size` argument with `PageArguments.validate(page, size)` (`interfaces/shared`; rejects a null or `< 1` page and
  a size outside 1..200), and never write their own page check. `OneBasedPaging` (`infrastructure/persistence/paging`) is the
  only code that subtracts or adds 1: adapters and query repositories use `toPageRequest(...)`, `toOffset(...)` and
  `toPageResult(...)` and never write `PageRequest.of(page - 1, ...)`, `(page - 1) * size` or `getNumber()` by hand.
  `PagingConventionTests` scans source text for this; a wrong offset passes every behavioural test.
- **Authorization** is `@PreAuthorize("hasRole(...)")` on controller methods. Roles in use: `SYSTEM_ADMIN`, `SCHOOL_ADMIN`,
  `TEACHER`, `STUDENT`. A new endpoint without it is a security bug. School-scoped data must also be checked against
  the caller's school in the use case, not only by role.
- **Errors**: throw the exceptions in `application/exception` (`ResourceNotFoundException`, `ForbiddenException`,
  `ResourceDuplicatedException`, `BusinessRuleViolationException`, ...). REST maps them in
  `interfaces/rest/exception/GlobalExceptionHandler`. Do not return error payloads by hand. Pick the type by meaning:
  `IllegalArgumentException` is a bad request (400); `IllegalStateException` is an internal fault the caller cannot
  cause or fix (500); `BusinessRuleViolationException` is a valid request that a business rule forbids (422, for
  example quota or plan limits). In `application` and `domain`, use `IllegalStateException` only for such an internal
  fault (corrupt stored data, wiring), and give the reason in the PR or plan notes.
- **REST responses** are `ResponseEntity<ApiResponse<T>>`. Use cases are `@Service` + `@Transactional` and take one
  command/query object.
- **Events**: payload classes in `application/event` are versioned (`...PayloadV1`); change a payload by adding a `V2`,
  not by  editing `V1`. Outgoing events are written to an outbox table and relayed to Kafka by `OutboxEventScheduler`
  (disabled in tests via `app.outbox.scheduler.enabled=false`). The topic is set with the `@ExternalEventTopic` annotation
  on the event class. Kafka consumers live in `interfaces/kafka/consumer`.
- **GraphQL DataLoaders.** One config per schema file (`graphql/dataloader/<Schema>GraphQLDataLoaderConfig`) holds every
  loader whose result type is defined in that file. A loader name is `<result>By<KeyField>` (`userByUserId`), declared as
  a `public static final String` constant of its config and registered with `forName(CONSTANT)`. A controller looks a
  loader up only through `GraphQLDataLoaders.require(environment, Config.CONSTANT)`, never `getDataLoader(` and never a
  string literal. A loader that can return more than 20 rows per parent takes a key record (in `graphql/key`) with page
  and size, and a repository method that windows per parent. A slice test imports the config that owns the loader.
- **Dependency injection**: constructor injection with `private final` fields. A constructor that only assigns its
  parameters is written as `@RequiredArgsConstructor` in `application`, `infrastructure` and `interfaces` (never in
  `domain`). An explicit constructor stays only when it does more than assign, when a parameter carries `@Value` or
  `@Qualifier`, or when the class has several constructors. Parameter order is field declaration order.
- **Logging**: loggers use `@Slf4j` and are named `log`; never write `LoggerFactory.getLogger` (domain has no logger).
- **Types**: prefer explicit types over `var` in new code.
- **Models and builders.** Every concrete class in `domain/model` has a nested `Builder` and
  `public static Builder builder()`, and is built with it. There are no all-args constructors; the public no-argument
  constructor stays because the builder uses it. A model is built with its builder only inside the model (static factories
  and behaviour methods named for the situation they build, e.g. `ImportSession.previewed(...)`), in infrastructure
  mappers and in tests; `application` never calls `Model.builder()`. Value objects, DTOs, commands and queries do not
  get builders.
- **Audit columns.** A model with `id`, `createdAt`, `updatedAt`, `createdBy` and `updatedBy` extends `BaseModel`, and its
  entity extends `BaseEntity` (`infrastructure/persistence/entity`). Spring Data JPA auditing fills the four audit values
  when the entity is saved, so use cases, services, factories and workers never call
  `setCreatedBy/setUpdatedBy/setCreatedAt/setUpdatedAt` and never pass a user id or `now` only to stamp a row.
  `createdBy`/`updatedBy` are `String`: the user id text for a logged-in caller, `ActorConstant.SYSTEM` (`"System"`) when
  no user is authenticated (schedulers, Kafka consumers, anonymous endpoints), or `null` on rows written before this
  rule. Read them only through `createdByUserId()`, `updatedByUserId()` and `isCreatedBy(UUID)`; never parse them as a
  `UUID` by hand. DTOs and GraphQL keep `UUID`, with `"System"` and `null` shown as `null`. Background work that acts for a
  user (import commits) runs inside `AuditActorScopePort.callAs(actor, supplier)`. A class that lacks one of the audit
  fields does not extend the base classes and is not given the missing columns. `@Modifying` JPQL and native
  statements bypass auditing, so their adapter still sets `updated_at`/`updated_by` (the user id as text), taking the time
  and the actor from the same source as the auditing hook. Such a statement never receives `updatedAt` or `updatedBy`
  from its caller.
- **Entities.** Each entity is in `entity/<feature>/` (only `BaseEntity` sits directly in `entity/`) and uses
  `@Getter @Setter @SuperBuilder @NoArgsConstructor(access = AccessLevel.PROTECTED)`; mappers and tests build it with
  `builder()`. Give a field that has an initializer `@Builder.Default`. Never put `@Data`, `@EqualsAndHashCode` or
  `@ToString` on an entity.
- **Language**: everything is English: identifiers, comments, Javadoc, test documentation, log, exception, validation and
  API response messages, email, push notification and spreadsheet texts, and commit messages. Vietnamese stays only as
  data: seed data, import header aliases and accepted values, the 'đ' char literals, and Vietnamese names and transcripts
  in test fixtures. `LanguageConventionTests` fails on Vietnamese in a comment or a string literal in any Java file that
  is not listed in `src/test/resources/architecture/language-keep-list.txt`; that list may only shrink, so never add to
  it. When you edit a comment, write it in English.
- **Do not hand-edit** `build/`, `.gradle/`, `logs/`.

## Database

- Flyway migrations: `src/main/resources/db/migration/V<n>__snake_case_description.sql`, next number = highest + 1.
- **Never edit a migration that has been merged**; add a new one. `ddl-auto` is `validate`, so entity and migration must agree
  or the app will not start.
- IDs are `UUID` version 7 generated by Hibernate: `@UuidGenerator(style = UuidGenerator.Style.VERSION_7)` (already on
  `BaseEntity`; declare it on an entity that does not extend it). The insert is sent at flush, not at `save()`, and a new
  entity must have a null id. Every table keeps `DEFAULT uuidv7()` as a safety net, but an entity never mentions `uuidv7()`.
  Every entity gets its id from `BaseEntity`, `@UuidGenerator(style = VERSION_7)` or, when another system supplies the
  id, `@AssignedOrUuidVersion7`; never call `UUID.randomUUID()` to make a row id. Follow an existing entity.
- A new audited table has `created_at`, `updated_at` (`NOT NULL`) and `created_by`, `updated_by` as `varchar(64)`.
- Postgres 18 in CI, docker-compose and Testcontainers; keep it consistent.

## Testing

- Pure logic (services, use cases): `@ExtendWith(MockitoExtension.class)` with `@Mock` fields and `@InjectMocks` on the
  class under test, plus AssertJ, no Spring context (see `LoginUseCaseTests`). Strict stubs are on, so an unused stub
  fails the test: remove it or move it into the tests that use it, never hide it with `lenient()`. If a constructor is
  awkward for `@InjectMocks`, construct the class explicitly in `@BeforeEach`. Tests that still use hand-written
  `mock(...)` in `@BeforeEach` are migrated gradually (per feature package, starting with `application/usecase/auth`);
  do not convert them as a drive-by.
- REST controllers: `@RestControllerTest(RestXxxController.class)` (`support/`), a `@WebMvcTest` slice driven with
  `MockMvcTester`, with use cases and ports as `@MockitoBean`. It needs no Docker and runs in `test`. Its security is the
  test-only `config/WebMvcTestSecurityConfiguration`, not `SecurityConfig`, so CSRF, CORS and the JWT filter are not covered.
  See `RestAuthControllerTests`. Every role-protected endpoint group needs an authorization test: 401 with
  `@WithAnonymousUser`, 403 with a wrong role via `@WithMockUser(roles = ...)`, and success with the right role. Send
  a valid body in the 403 test, because `@Valid` runs before `@PreAuthorize` and an invalid body would answer 400.
- GraphQL controllers: direct-call unit tests stay as they are. Where authorization matters, use
  `@GraphQLControllerTest(GraphQLXxxController.class)` (`support/`), which runs the real schema, `GlobalExceptionResolver`
  and `@PreAuthorize` with no Docker. See `MyClassAuthorizationGraphQLTests`.
- Auditing: a `@RepositoryTest` already imports the auditing configuration. Test it with a `XxxAuditingTests`
  (see `SupportedLanguageAuditingTests`): set the principal through `SecurityContextHolder` with a `CustomUserDetails` for
  the user case and leave it empty for the `"System"` case, and flush and clear the `EntityManager` before each reload.
  Remember that the object returned by `save()` of an update shows the new `updatedAt` only after a flush.
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
- `ModelAndEntityConventionTests` guards the model and entity rules above (builder on every model, no `UUID`
  `createdBy`/`updatedBy`, no `uuidv7()` in an entity, every entity with a `UUID` `@Id` declares a version 7 generator,
  no `.id(UUID.randomUUID())` or `.setId(UUID.randomUUID())` in `src/main/java`, entities in feature sub-packages, no
  manual audit setters in `application/` and `infrastructure/worker/`). Its exemption lists name the classes that do not own the full audit set; an
  exemption that stops applying fails the test.
- `LombokUsageConventionTests` guards the Lombok rule above by scanning source text, because Lombok annotations leave no
  trace in the bytecode that `LayeredArchitectureTests` reads. It also fails on any `LoggerFactory.getLogger` under
  `src/main/java` and on any assignment-only constructor in `application`, `infrastructure` and `interfaces`; the
  constructor exemption set may only shrink.
- `LanguageConventionTests` guards the Language rule above by scanning source text: no Vietnamese letter in a comment or
  a string literal under `src/main/java` and `src/test/java`, except in the files of `language-keep-list.txt`, and an
  entry that no longer holds Vietnamese fails the test. It has self-tests with violating snippets.
- `InterfacesStructureConventionTests` guards the interfaces layout: `Rest`/`GraphQL` controller prefixes, the spelling
  `GraphQL`, no class directly in `port/input/command`, `port/input/query` or a `dto/request` or `graphql/input` package, the mapper
  allow-list, and the return type of `toCommand`/`toQuery`.
- `GraphQLDataLoaderConventionTests` guards the loader rules above by scanning source text: `getDataLoader(` only in
  `GraphQLDataLoaders`, every `forName(` argument a constant of the same config, no name registered twice, and every name
  matching `<result>By<KeyField>`. Each rule has a self-test with a violating snippet.
- `ModelCreationConventionTests` fails when a file in `application` calls `Model.builder()` on a domain model;
  `RepositoryAuditParameterConventionTests` fails when a `domain/repository` method takes `updatedAt`, `updatedBy`,
  `modifiedAt` or `modifiedBy`; `MethodSizeConventionTests` fails when a method is over 25 lines. Their exemptions
  (`model-creation-exemptions.txt`, a set in the test, `method-size-exemptions.txt`) may only shrink and never get new
  entries; each rule has a self-test with a violating snippet.
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

## Memory and sources of truth

- Do not rely on local agent memory (for example `~/.claude/projects/*/memory/`, `MEMORY.md`, or any per-machine notes).
  It is not shared between people or machines, it can be stale, and it is not reviewed. Do not read it to decide how to
  work, and do not write to it.
- Use git as the only source of project context: the working tree, `git log`, `git diff`, `git blame`, and branches.
  When you need to know why something changed or what was decided, look there first.
- If something must be remembered for later tasks (a convention, a decision, a rename, a known problem), put it in a file
  tracked by git (this `AGENTS.md`, `README.md`, or `docs/`) and tell the user in the final report, instead of saving it
  to local memory. Changing `AGENTS.md` still needs the user's confirmation (see Refactoring stage notice).
- If local memory and the repository disagree, the repository wins.

## Coding rules

- **Small functions.** A function or method is at most 25 lines, counted from its signature to its closing brace. A very
  large feature may use up to 30. `MethodSizeConventionTests` enforces 25.
- **One place for each piece of logic.** Before writing logic, search the other files for the same or nearly the same
  logic. If it exists, extract it into one shared method that all callers use, so a change is made in one place. Extract
  only logic that really exists in more than one place, never for hypothetical future reuse, unless the task asks for that.
- **Names say what the code does.** Function, method and variable names describe what they do. Long names are fine.
  Avoid abbreviations and vague words (`data`, `info`, `handle`, `process`, `manager`).
- **Self-explaining code.** Do not write comments that restate what a line does. Add a doc comment to a function, method
  or class only when its contract, side effects or failure cases cannot be read from its name and signature. When you
  edit a method, delete the comments in it that only restate the code (`// 1. Validate the user`); keep, and translate to
  English, those that record a reason (a race, a security rule, a workaround).
- **Import types, never write packages inline.** Refer to a type by its simple name and add an `import` for it. Never
  write a fully qualified name in code (`java.util.List<String> names`, `new com.sep.vox.domain.model.Foo()`), in
  annotations, generics, casts or Javadoc `{@link}`. The only exception is two types with the same simple name used in
  one file: import one and qualify the other.
- **No N+1 queries.** Never query inside a loop or per element of a collection. Fetch in bulk (`IN` queries, joins, fetch
  graphs, batch methods on the repository). This also applies to GraphQL field resolvers, so use batch mapping there.
- **Read the docs for the version in use.** Before using a package or framework API, read the official documentation
  for the version this project includes (Spring Boot 4.1 and Spring Framework 7 differ from older tutorials). Do not use
  deprecated APIs.
- **New dependencies** must be the latest release, or the version managed by the Spring Boot BOM (leave the version out when
  the BOM manages it). Adding one needs approval first (see Ask first).

## Unused code and imports

Before reporting a task done, check every file you created or edited for unused code, and remove what you find.

- **Check** imports, local variables, parameters, private fields and private methods that your change left without a
  reader. `./gradlew compileJava compileTestJava` plus the IDE or `-Xlint` warnings are the first source; also search
  for the name when the compiler cannot tell (a private method whose last caller you removed).
- **Remove** each unused import, variable, parameter, field and method, including the ones your own change orphaned
  (an import left by a deleted line, a helper whose only caller was rewritten).
- **Keep** an unused element only when the code needs it to run or compile: a parameter required by an interface, an
  override or a framework callback signature, a field or method that reflection, JPA, Jackson or a test scan reaches by
  name, or a fixture that a test inspects but never calls (for example the static methods of `SampleRequest` in
  `InterfacesStructureConventionTests`, which carry `@SuppressWarnings("unused")`). Mark it with the narrowest
  `@SuppressWarnings("unused")` and say why in a short comment when the reason is not obvious.
- Do not remove unused code in files you are not otherwise touching, and do not add `@SuppressWarnings` to hide code that
  can simply be deleted. Report unused code you notice elsewhere instead of fixing it (see Scope).

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
