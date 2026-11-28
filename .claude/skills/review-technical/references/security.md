# Security internals: probes, static checks and library CVEs

Part A runs SAF4, SAF7 and SAF9 against the local app and points to the log count for SAF6. Part B is the static
side: SAF11 to SAF13, and the code behind SAF4, SAF6 and SAF7. Part C is the OWASP dependency-check scan for SAF10.
Login, roles, other schools, what responses show and staying logged in (SAF1 to SAF3, SAF5, SAF8) belong to
`review-feature` (`.claude/skills/review-feature/references/security.md`).

## A. Probes (Run)

All probes live in the `probes` scenario of the shared k6 template (`performance-k6.md`, section E). Each check is
named with its cell id, and a failed probe prints `REVIEW-FAIL <cell id> expected <...> got <...>` without the
response body. The accounts are the seeded ones that `review-feature`'s `security.md` lists under "Accounts the probes
use".

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

Pass: a 4xx within the budget of `journey.md`, section 5, no 500, no hang, and the app still answers
`GET /api/v1/status` afterwards.

### SAF7: what errors reveal

Every error response from the SAF4 probes, and every other error response of the runs, is checked with
`checkErrorDetails()` for a stack trace, SQL, or a Java class or package name (`com.sep.vox`, `org.hibernate`,
`java.`, `Exception`). `GlobalExceptionHandler` and `GlobalExceptionResolver` decide what an error shows;
`INTERNAL_ERROR` should show only "An error occurred".

### SAF9: limits on costly actions

A burst of 20 requests in 10 s from one actor against each entry point that sends email or OTP, calls AI grading,
uploads to storage or moves a protected value. Neutralize the side effect first (`performance-k6.md`, section B). If
it cannot be neutralized, the cell is `Static` only.

### SAF6: the log count

See `performance-k6.md`, section L. It runs after the last run, over this review's app output only, and prints counts,
never lines.

## B. Static checks

Each item names the cells it supports.

### Input handling (SAF4)

- `@Valid` on request bodies, size limits on strings and lists, `PageArguments.validate` for paging, and safe enum
  parsing.
- SQL injection: `@Query(nativeQuery = true)`, `createNativeQuery`, JPQL built by string concatenation, and `ORDER BY`
  or column names taken from input.
- File imports (POI, commons-csv): file size and row limits, zip-bomb and XXE protection, and formula injection in
  exported spreadsheets.
- Input used as a URL or object key (S3, HTTP clients): SSRF and path traversal. Redirect targets (the OAuth start):
  open redirect.

### Data exposure (SAF6, SAF7)

- Logs: no secrets, tokens, OTPs, passwords, student PII or recordings (`AGENTS.md`). Watch for whole objects logged
  with `{}`: a record's `toString` prints every field.
- Error responses: no stack traces or SQL.

### Internal callers (SAF12)

- Internal REST endpoints (`interfaces/rest/controller/internal`), gRPC services and Kafka consumers: how is the
  caller authenticated (a service token, mTLS, or the network only)? A "network only" answer needs evidence in the
  deployment config (`docker-compose.yml`, the CD workflow), not a belief.
- Does the code trust the payload blindly, for example by applying a score from a message to any session id, without
  checking that the session exists and awaits grading?

### Tokens and OTPs (SAF13; review only, because changes here are "Ask first")

- JWT: signature and algorithm verified (no `none`, no algorithm taken from the token), expiry checked, and the
  claims the code trusts named.
- Refresh tokens: rotated on use, a reused one refused (and ideally the family revoked), revoked on logout, bound to
  the device id.
- OTP: expiry, attempt limits, constant-time comparison, and rate limits on OTP and login (SAF9).

### Money (SAF11)

- PayOS webhooks: the signature is verified before anything else, repeats are idempotent, the amount is checked
  against the order, and the state transition is guarded.
- Quota and balance never go negative, including under concurrent requests (NSP8).

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

### Run (after the engineer approves it in Step 3)

```bash
./gradlew dependencyCheckAnalyze --console=plain     # with run_in_background; do other work meanwhile
```

Then summarize:

```bash
powershell -NoProfile -File .claude/skills/review-technical/scripts/summarize-dependency-check.ps1
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
4. **Reachable.** Does any path or background path of the inventory use the vulnerable module, class or feature?
   Being unreachable lowers the priority but does not remove the finding.
5. **Fix.** Name the fixed version and the smallest change: bump the direct dependency, override the BOM-managed
   version (check the Spring Boot 4.1 docs for the version property), or wait for a Boot patch release. A version
   change is "Ask first" in `AGENTS.md`: propose it, never apply it.
6. **KEV.** A CVE in CISA's Known Exploited Vulnerabilities list moves up one severity level.

Severity, one finding per real CVE: CVSS 9 or more on the runtime classpath and reachable is Critical; 7 or more on
the runtime classpath is High; anything else on the runtime classpath is Medium; test-only or build-only is at most
Medium; unreachable is one level lower, with the reason stated. A false positive is not a finding; list it in the CVE
table with the reason.

Do not create a suppression file yourself. For confirmed false positives, propose suppression entries and ask with
`AskUserQuestion`, since that is a new file.

### Troubleshooting

- `NoSuchMethodError` during analysis involving Jackson, commons-lang3 or commons-text: a buildscript classpath
  conflict that the plugin README documents. Report it and do not edit `build.gradle` without asking.
- NVD HTTP 403 or 429: the key is missing, wrong or rate-limited. Retry later, or ask the engineer with
  `AskUserQuestion` to set `NVD_API_KEY`.
- OSS Index 401: the token is missing or expired.

### Container images

dependency-check does not scan container images. List the images from `Dockerfile` and `docker-compose.yml` with
their tags, flag unpinned tags (CFG7), and write "image CVEs not scanned" under "Unverified".
