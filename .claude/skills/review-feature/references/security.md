# Safety as the user sees it: probes

These probes settle the Safety checks of this skill (SAF1 to SAF3, SAF5 and SAF8) against the local app. They answer
the user's questions: can someone who is not logged in, someone in another role or someone of another school get at
my work, does a screen show anything it should not, and do I stay logged in for the whole journey. Hostile input,
logs, error internals, rate limits, money flows, internal callers, token handling and library CVEs belong to
`review-technical` (`.claude/skills/review-technical/references/security.md`).

## A. Probes (Run)

All probes live in the `probes` scenario of the k6 script. Each check is named with its cell id, and a failed probe
prints `REVIEW-FAIL <cell id> expected <...> got <...>` without the response body.

### Accounts the probes use

From the seed (`references/performance-k6.md`, section D): one account of every role in school A, students A1 and A2,
and every school role in school B. The system admin account belongs to no school.

### SAF1, SAF2, SAF3: who may call what

`probes()` generates these from `ENTRY_POINTS`, so no entry point a user reaches is skipped:

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

### SAF5: what responses show

The k6 helper `checkExposure()` walks every JSON response of the walk and the probes and fails the cell when it finds
a key named like a secret (`password`, `hash`, `secret`, `otp`, `refreshToken`) or an id that belongs to the other
school. Add the internal-only fields of the feature (for example fields meant for admins only) to its list.

### SAF8: session continuity

See `references/performance-k6.md`, section J. Refresh uses the `refresh_token` cookie and CSRF: the first protected
call without the header answers 403 and sets the `XSRF-TOKEN` cookie, which then goes back as the `X-XSRF-TOKEN`
header (`SecurityConfig`, `CSRF_PROTECTED_API_PATHS`). If refresh keeps answering 403, read
`SecurityConfig#apiCsrfTokenRepository` before calling it a finding.

## B. Reading the code behind a probe (Static)

Read code here only to explain a probe that failed, or to settle a cell whose run was declined:

- Every REST and GraphQL entry point has `@PreAuthorize("hasRole(...)")` with the right roles (`SYSTEM_ADMIN`,
  `SCHOOL_ADMIN`, `TEACHER`, `STUDENT`).
- School scope: follow every id from the request to the query. The use case must compare the resource's school with
  the caller's (`AGENTS.md`, Authorization); the role alone is not enough.
- What a response shows: the response record or GraphQL type, and which fields it maps from the model.
