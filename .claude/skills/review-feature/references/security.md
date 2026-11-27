# Safety: probes, static checks and library CVEs

Part A runs the SAF checks against the local app. Part B is the static side, for cells whose method allows `Static`
and for explaining a probe that failed. Part C is the OWASP dependency-check scan for SAF10.

## A. Probes (Run)

All probes live in the `probes` scenario of the k6 script. Each check is named with its cell id, and a failed probe
prints `REVIEW-FAIL <cell id> expected <...> got <...>` without the response body.

### Accounts the probes use

From the seed (`references/performance-k6.md`, section D): one account of every role in school A, students A1 and A2,
and every school role in school B. The system admin account belongs to no school.

### SAF1, SAF2, SAF3: who may call what

`probes()` generates these from `ENTRY_POINTS`, so no entry point is skipped:

- SAF1: the request with no token. REST answers 401; GraphQL answers an `UNAUTHORIZED` error and no data.
- SAF2: the same valid request with the token of every role that `@PreAuthorize` does not allow. REST answers 403;
  GraphQL answers `FORBIDDEN`. Send a valid body, because `@Valid` runs before `@PreAuthorize` and an invalid body
  would answer 400.
- SAF3: for every entry point that takes an id or returns school-scoped data, the allowed role of school B on school
  A's ids, and student A2 on student A1's data. Expect 403 or 404 and no data of A in the body. Cover reads and writes,
  and for GraphQL also the nested fields (`@SchemaMapping`, `@BatchMapping`) that expose another entity.
- A path in `SecurityConfig` `PERMITTED_PATTERNS` is public by design only when its reason says so. `/graphql` is
  listed there, but its operations are protected by `@PreAuthorize` on the resolvers, so SAF1 still applies to each of
  them.

### SAF4: hostile input

One check per entry point that takes input, and per kind of input it takes:

- A body and a file just above `spring.servlet.multipart.max-file-size` (5 MB at the time of writing) and the import
  row limit, if there is one.
- 10,000-character strings in every text field.
- SQL metacharacters (`'`, `--`, `;`, `%`, `_`) in filters, search terms and sort fields. A sort or column name taken
  from input must be refused unless it is on an allow-list.
- GraphQL: a query nested ten levels deep, and one with 100 aliases of the same field.
- Files: an xlsx zip bomb (a small file that expands to gigabytes), an xlsx whose XML declares an external entity,
  a CSV with a formula in a cell, and a file with the wrong content type for its extension.
- Exports: seed a value that starts with `=`, `+`, `-` or `@`, export it, and check that the cell is escaped.

Pass: a 4xx within the step budget, no 500, no hang, and the app still answers `GET /api/v1/status` afterwards.

### SAF5: what responses show

The k6 helper `exposedFieldsIn()` walks every JSON response of the walk and the probes and fails the cell when it
finds a key named like a secret (`password`, `hash`, `secret`, `otp`, `refreshToken`, `token` outside the login and
refresh responses) or an id that belongs to the other school. Add the internal-only fields of the feature (for example
fields meant for admins only) to its list.

### SAF7: what errors reveal

Every error response from the TRU3 and SAF4 probes is checked for a stack trace, SQL, or a Java class or package name
(`com.sep.vox`, `org.hibernate`, `java.`, `Exception`). `GlobalExceptionHandler` and `GlobalExceptionResolver` decide
what an error shows; `INTERNAL_ERROR` should show only "An error occurred".

### SAF8: session continuity

See `references/performance-k6.md`, section J. Refresh uses the `refresh_token` cookie and CSRF: the first protected
call without the header answers 403 and sets the `XSRF-TOKEN` cookie, which then goes back as the `X-XSRF-TOKEN`
header (`SecurityConfig`, `CSRF_PROTECTED_API_PATHS`). If refresh keeps answering 403, read
`SecurityConfig#apiCsrfTokenRepository` before calling it a finding.

### SAF9: limits on costly actions

A burst of 20 requests in 10 s from one actor against each entry point that sends email or OTP, calls AI grading,
uploads to storage or moves a protected value. Neutralize the side effect first (section B of
`references/performance-k6.md`). If it cannot be neutralized, the cell is `Static` only.

## B. Static checks

Each item names the cells it supports.

### Access control (SAF1 to SAF3)

- Every REST and GraphQL entry point has `@PreAuthorize("hasRole(...)")` with the right roles (`SYSTEM_ADMIN`,
  `SCHOOL_ADMIN`, `TEACHER`, `STUDENT`).
- School scope: follow every id from the request to the query. The use case must compare the resource's school with
  the caller's (`AGENTS.md`, Authorization); the role alone is not enough.
- Internal endpoints (`interfaces/rest/controller/internal`), gRPC services and Kafka consumers: how is the caller
  authenticated (a service token, the network only)? Does the code trust the payload blindly, for example by applying a
  score from a message to any session id?

### Input handling (SAF4, TRU3)

- `@Valid` on request bodies, size limits on strings and lists, `PageArguments.validate` for paging, and safe enum
  parsing.
- SQL injection: `@Query(nativeQuery = true)`, `createNativeQuery`, JPQL built by string concatenation, and `ORDER BY`
  or column names taken from input.
- File imports (POI, commons-csv): file size and row limits, zip-bomb and XXE protection, and formula injection in
  exported spreadsheets.
- Input used as a URL or object key (S3, HTTP clients): SSRF and path traversal. Redirect targets (the OAuth start):
  open redirect.

### Data exposure (SAF5 to SAF7)

- Logs: no secrets, tokens, OTPs, passwords, student PII or recordings (`AGENTS.md`). Watch for whole objects logged
  with `{}`: a record's `toString` prints every field.
- Error responses: no stack traces or SQL.

### Auth and session (SAF8; review only, because changes here are "Ask first")

- JWT validation and expiry, refresh-token rotation, and revocation on logout.
- OTP expiry, attempt limits, constant-time comparison, and rate limits on OTP and login.

### Money (SAF11)

- PayOS webhooks: the signature is verified before anything else, repeats are idempotent, the amount is checked
  against the order, and the state transition is guarded.
- Quota and balance never go negative, including under concurrent requests (NSP2).

### Configuration (CFG6, CFG7)

- `application.yaml`: secrets only through `${ENV}` with no real secret as a default; new variables listed in the
  README env table; insecure defaults (SSL off, wide CORS, actuator `exposure.include: "*"`). Check what
  `SecurityConfig` actually protects before calling any of these a vulnerability.
- `Dockerfile`: base images pinned to a version, the runtime user (root?), and no secrets baked into layers.
- `docker-compose.yml`: no `:latest` images, local-only services bound to localhost when possible, credentials from
  `.env`.
- CI workflows: actions pinned, secrets never echoed, minimal `permissions`.
- `build.gradle`: only trusted repositories, and versions that are the latest release or managed by the Spring Boot
  BOM (`AGENTS.md`, New dependencies).

## C. Library CVEs with OWASP dependency-check (SAF10)

### Setup (already in `build.gradle`)

- Plugin `org.owasp.dependencycheck` (13.0.0 when this skill was written), configured in the `dependencyCheck {}`
  block.
- The scan covers test dependencies too (`skipTestGroups = false`), because they run in CI with Docker access.
- Reports: JSON and HTML in `build/reports/` (`dependency-check-report.json`, `dependency-check-report.html`).
- NVD API key from the `NVD_API_KEY` environment variable. Without a key the scan still works, but the first download
  is very slow. A free key comes from https://nvd.nist.gov/developers/request-an-api-key.
- The OSS Index analyzer needs a Sonatype token and is on only when `OSS_INDEX_TOKEN` is set. The .NET assembly
  analyzer is off, since this is a JVM project.
- `failBuildOnCVSS` keeps its default, so the scan never fails the build. This skill reads the report instead.

### Run (after the owner approves it in Step 3)

```bash
./gradlew dependencyCheckAnalyze --console=plain     # with run_in_background; do other work meanwhile
```

Then summarize:

```bash
powershell -NoProfile -File .claude/skills/review-feature/scripts/summarize-dependency-check.ps1
# optional: -ReportPath <path to dependency-check-report.json>
```

The script prints one row per CVE, sorted by CVSS: package, CVE, severity, score, KEV flag and the Gradle
configurations. If it fails (for example, because the report schema changed), read the HTML report and say so in the
report.

### Triage every CVE (do not paste the raw list)

1. **Classpath.** Run `./gradlew dependencyInsight --dependency <artifact> --configuration runtimeClasspath`, then
   with `testRuntimeClasspath`. Is it production, test-only or build-only?
2. **Origin.** Is it a direct dependency, managed by the Spring Boot BOM, or pulled in transitively, and by what?
3. **Real or false positive.** dependency-check matches by CPE, so it misreports: a CVE for another product with a
   similar name, or a version range that does not include ours. Read the CVE's product and affected versions, and mark
   a false positive with the reason.
4. **Reachable.** Does the journey use the vulnerable module, class or feature? Being unreachable lowers the priority
   but does not remove the finding.
5. **Fix.** Name the fixed version and the smallest change: bump the direct dependency, override the BOM-managed
   version (check the Spring Boot 4.1 docs for the version property), or wait for a Boot patch release. A version
   change is "Ask first" in `AGENTS.md`: propose it, never apply it.
6. **KEV.** A CVE in CISA's Known Exploited Vulnerabilities list moves up one severity level.

Severity, one finding per real CVE: CVSS 9 or more on the runtime classpath and reachable is Critical; 7 or more on
the runtime classpath is High; anything else on the runtime classpath is Medium; test-only or build-only is at most
Medium; unreachable is one level lower, with the reason stated. A false positive is not a finding; list it in the CVE
table with the reason.

Do not create a suppression file yourself. For confirmed false positives, propose suppression entries and ask, since
that is a new file.

### Troubleshooting

- `NoSuchMethodError` during analysis involving Jackson, commons-lang3 or commons-text: a buildscript classpath
  conflict that the plugin README documents. Report it and do not edit `build.gradle` without asking.
- NVD HTTP 403 or 429: the key is missing, wrong or rate-limited. Retry later, or ask the owner to set `NVD_API_KEY`.
- OSS Index 401: the token is missing or expired.

### Container images

dependency-check does not scan container images. List the images from `Dockerfile` and `docker-compose.yml` with
their tags, flag unpinned tags (CFG7), and write "image CVEs not scanned" under "Unverified".
