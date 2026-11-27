# Security track

Do both parts. In part A, record each item as checked (with evidence) or "N/A: <reason>".

## A. Code and configuration

### Access control

- Every REST and GraphQL entry point in the inventory has `@PreAuthorize("hasRole(...)")` with the right roles
  (`SYSTEM_ADMIN`, `SCHOOL_ADMIN`, `TEACHER`, `STUDENT`). For GraphQL, check field resolvers
  (`@SchemaMapping`, `@BatchMapping`) that expose another entity's data, not only queries and mutations.
- School scope (IDOR): follow every id from the request to the query. Can a `TEACHER` of school A pass an id that
  belongs to school B and get or change it? The role alone is not enough; the use case must compare the resource's
  school with the caller's (`AGENTS.md`, Authorization).
- Students reach only their own attempts, results and recordings.
- Internal endpoints (`interfaces/rest/controller/internal`), gRPC services and Kafka consumers: how is the caller
  authenticated (service token, network only)? Does the code trust the payload blindly (for example, applying a
  score from a message to any session id)?
- Authorization tests exist for each role-protected group (401, 403, success).

### Input handling

- `@Valid` on request bodies, size limits on strings and lists, `PageArguments.validate` for paging, and safe enum
  parsing.
- SQL injection: `@Query(nativeQuery = true)`, `createNativeQuery`, or JPQL built by string concatenation, and
  `ORDER BY` or column names taken from input.
- File imports (POI, commons-csv): file size and row limits, zip-bomb and XXE protection, and formula injection in
  exported spreadsheets (cells starting with `=`, `+`, `-` or `@`).
- Input used as a URL or object key (S3, HTTP clients): SSRF and path traversal. Redirect targets (OAuth start): open
  redirect.

### Data exposure

- Responses do not leak password hashes, tokens, other schools' data or internal-only fields.
- Logs contain no secrets, tokens, OTPs, passwords, student PII or recordings (`AGENTS.md`). Watch for whole objects
  logged with `{}`: a record's `toString` prints every field.
- Error responses do not leak stack traces or SQL (`GlobalExceptionHandler`, `GlobalExceptionResolver`).

### Auth and session (review only: changes here are "Ask first")

- JWT validation and expiry, refresh-token rotation and revocation on logout.
- OTP expiry, attempt limits, constant-time comparison, and rate limiting of OTP and login.

### Money

- PayOS webhooks: signature verified before anything else, idempotent on repeats, amount checked against the order,
  state transition guarded.
- Quota and balance never go negative, including under concurrent requests (concurrency track).

### Configuration (when the target includes config)

- `application.yaml`: secrets only through `${ENV}` with no real secret as a default; new variables listed in the
  `README.md` env table; insecure defaults (SSL off, wide CORS, actuator `exposure.include: "*"`). Check what
  `SecurityConfig` actually protects before calling any of these a vulnerability.
- `Dockerfile`: base images pinned to a version, the runtime user (root?), no secrets baked into layers.
- `docker-compose.yml`: no `:latest` images, local-only services bound to localhost when possible, credentials from
  `.env`.
- CI workflows: actions pinned, secrets never echoed, minimal `permissions`.
- `build.gradle`: only trusted repositories, and versions that are the latest release or managed by the Spring Boot
  BOM (`AGENTS.md`, New dependencies).

## B. Library CVEs with OWASP dependency-check

### Setup (already in `build.gradle`)

- Plugin `org.owasp.dependencycheck` (13.0.0 when this skill was written), configured in the `dependencyCheck {}`
  block.
- The scan covers test dependencies too (`skipTestGroups = false`), because they run in CI with Docker access.
- Reports: JSON and HTML in `build/reports/` (`dependency-check-report.json`, `dependency-check-report.html`).
- NVD API key from the `NVD_API_KEY` environment variable. Without a key the scan still works, but the first
  download is very slow. A free key comes from https://nvd.nist.gov/developers/request-an-api-key.
- The OSS Index analyzer needs a Sonatype token and is on only when `OSS_INDEX_TOKEN` is set. The .NET assembly
  analyzer is off, since this is a JVM project.
- `failBuildOnCVSS` keeps its default, so the scan never fails the build. This skill reads the report instead.

### Run (after the user approves in Step 3)

```bash
./gradlew dependencyCheckAnalyze --console=plain     # with run_in_background; do other tracks meanwhile
```

Then summarize:

```bash
powershell -NoProfile -File .claude/skills/review-feature/scripts/summarize-dependency-check.ps1
# optional: -ReportPath <path to dependency-check-report.json>
```

The script prints one row per CVE, sorted by CVSS: package, CVE, severity, score, KEV flag and the Gradle
configurations. If it fails (for example, the report schema changed), read the HTML report and say so in the report.

### Triage each CVE (do not paste the raw list)

1. **Classpath.** Run `./gradlew dependencyInsight --dependency <artifact> --configuration runtimeClasspath`, then
   with `testRuntimeClasspath`. Is it production, test-only or build-only?
2. **Origin.** Is it a direct dependency, managed by the Spring Boot BOM, or pulled in transitively, and by what?
3. **Real or false positive.** dependency-check matches by CPE, so it misreports: a CVE for another product with a
   similar name, or a version range that does not include ours. Read the CVE's product and affected versions, and
   mark a false positive with the reason.
4. **Reachable.** Does the project use the vulnerable module, class or feature? Being unreachable lowers the
   priority but does not remove the finding.
5. **Fix.** Name the fixed version and the smallest change: bump the direct dependency, override the BOM-managed
   version (check the Spring Boot 4.1 docs for the version property), or wait for a Boot patch release. A version
   change is "Ask first" in `AGENTS.md`: propose it, never apply it.
6. **KEV.** A CVE in CISA's Known Exploited Vulnerabilities list moves up one severity level.

Severity: CVSS 9 or more on the runtime classpath and reachable is Critical; 7 or more on the runtime classpath is
High; test-only or build-only is at most Medium; unreachable is one level lower, with the reason stated.

Do not create a suppression file yourself. For confirmed false positives, propose suppression entries and ask, since
that is a new file.

### Troubleshooting

- `NoSuchMethodError` during analysis involving Jackson, commons-lang3 or commons-text: a buildscript classpath
  conflict that the plugin README documents. Report it and do not edit `build.gradle` without asking.
- NVD HTTP 403 or 429: the key is missing, wrong or rate-limited. Retry later, or ask the user to set
  `NVD_API_KEY`.
- OSS Index 401: the token is missing or expired.

### Container images

dependency-check does not scan container images. List the images from `Dockerfile` and `docker-compose.yml` with
their tags, flag unpinned tags, and write "image CVEs not scanned" under "Not run".
