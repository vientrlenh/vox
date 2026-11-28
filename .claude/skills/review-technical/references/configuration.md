# Configuration: rows, effective values and lenses

The CFG checks in `references/checks.md` run on the config key rows built here. A config key row whose effective value
differs from the library default is also a decision row (`references/decisions.md`, section 1, "Config value").

## 1. Config key rows

| Target | Config key rows |
|---|---|
| A feature | Every key the inventory reads (`@Value`, `@ConfigurationProperties`, `Environment`), every key that sets a budget, limit, lifetime or interval on its paths and background paths, and every key that pairs with one of them (CFG4) |
| A configuration file | Every key in the file (for `build.gradle`: each plugin, each pinned version, each task setting; for a workflow: each action, permission and secret use) |
| A diff | Every changed key, plus the keys that pair with it (CFG4) |

The files: `src/main/resources/application.yaml`, `src/test/resources/application-test.yaml`, `.env.example` and the
README env table, `docker-compose.yml`, `Dockerfile`, `build.gradle`, and `.github/workflows/*.yml`.

## 2. The effective value (CFG1)

Resolve each key for each place it runs: a local `bootRun`, the tests, docker-compose, and CI. Later sources override
earlier ones:

1. The code default: a field initializer in a `@ConfigurationProperties` class, or the default in
   `@Value("${key:default}")`.
2. The library default (section 3).
3. `application.yaml`, then a profile file. The tests use `application-test.yaml`; it deliberately turns off Kafka
   listeners, the admin client and a fixed gRPC port, so never count that as a defect (`AGENTS.md`, Testing).
4. Environment variables, by relaxed binding: dots become underscores, dashes are dropped, and the name is upper case
   (`spring.datasource.hikari.maximum-pool-size` is `SPRING_DATASOURCE_HIKARI_MAXIMUMPOOLSIZE`). A `${VAR}` placeholder
   in `application.yaml` reads one directly, and `AGENTS.md` requires every placeholder to be set. Spring does not read
   `.env` by itself: the shell or the IDE loads it, and docker-compose passes it with `env_file` or `environment`.
5. Java system properties, then command-line arguments (`--args` of `bootRun`).
6. In tests only: `@TestPropertySource`, `@DynamicPropertySource` and the `properties` of a test annotation.

Write the chain into the key's cell, for example "default 10 (HikariConfig) -> application.yaml 20 -> no env
override -> 20 everywhere".

## 3. The library default, for the version in use

- **Spring Boot properties.** The `META-INF/spring-configuration-metadata.json` inside the jar that declares the
  property gives its type, default and description for that exact version. Find the version with
  `./gradlew dependencyInsight --dependency <artifact> --configuration runtimeClasspath`, then the jar under
  `~/.gradle/caches/modules-2/files-2.1/<group>/<artifact>/<version>/`, and read the file with `unzip -p <jar>
  META-INF/spring-configuration-metadata.json` (Git Bash). Copy what you need into the review folder; never change
  the cache.
- **Properties without metadata** (a Hikari, Kafka client or Hibernate setting, a field of a library class): the
  library's docs for that version, or its source in the `-sources.jar` when Gradle has it.
- **Never** from memory, a tutorial or the docs of another version. Spring Boot 4.1 and Spring Framework 7 differ from
  older releases (`AGENTS.md`, Coding rules).

## 4. Lenses by file (CFG5 to CFG7)

- `application.yaml`: secrets only through `${ENV}` with no real secret as a default, new variables listed in the
  README env table and `.env.example`, and insecure defaults (SSL off, wide CORS, actuator `exposure.include: "*"`).
  Check what `SecurityConfig` actually protects before calling any of these a vulnerability.
- `Dockerfile`: base images pinned to a version, the runtime user (root?), `TZ` matching the tests' Asia/Ho_Chi_Minh,
  and no secrets baked into layers.
- `docker-compose.yml`: no `:latest` images, local-only services bound to localhost when possible, credentials from
  `.env`, and Postgres 18 as in CI and Testcontainers (`AGENTS.md`, Database).
- CI workflows: actions pinned, secrets never echoed, minimal `permissions`, and the same build command that
  `AGENTS.md` names (`./gradlew clean build --no-daemon`).
- `build.gradle`: only trusted repositories, and versions that are the latest release or managed by the Spring Boot
  BOM (`AGENTS.md`, New dependencies). A version change is "Ask first": propose it, never apply it.
