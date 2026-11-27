// k6 v2 template for the review-feature skill, written against grafana/k6:2.4.0.
// Copy it into the review folder as review.js and fill in every TODO. Never commit a filled-in copy.
// Delete the helpers the feature does not need. Run one scenario group at a time with --scenario
// (references/performance-k6.md, section E):
//   journeyWalk        J1, J5, SPD1, SPD3, TRU4, NSP3, NSP7: one actor walks each path; step and wait times
//   statementCount     SPD5: the same walk with markers around every action, for the SQL statement count
//   probes             SAF1-SAF5, SAF7, TRU1-TRU3, NSP1, J4: one check per matrix cell, named with the cell id
//   warmUp,steadyLoad  SPD2, SPD4: one action (LOAD_STEP) at a steady rate
//   burst              SPD2, SPD4: the crowd (CROWD) doing one action at the same moment
//   racingRequests     NSP2, TRU2: the same write on the same resource at once; the invariant is checked in SQL
//   sessionContinuity  SAF8: a path with an expired token, a refresh and a logout
import http from 'k6/http';
import exec from 'k6/execution';
import { check, fail, sleep } from 'k6';
import { Trend } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://host.docker.internal:8081';
const RUN_ID = __ENV.RUN_ID;
const PERF_PASSWORD = __ENV.PERF_PASSWORD;
const ACTOR_ROLE = (__ENV.ACTOR_ROLE || 'school_admin').toLowerCase();
const ACTOR_COUNT = Number(__ENV.ACTOR_COUNT || 1);
const VOLUME = Number(__ENV.VOLUME || 20);
const WALK_ITERATIONS = Number(__ENV.WALK_ITERATIONS || 20);
const STATEMENT_ITERATIONS = Number(__ENV.STATEMENT_ITERATIONS || 10);
const LOAD_STEP = __ENV.LOAD_STEP || 'P0.S1';
const LOAD_BUDGET_MS = Number(__ENV.LOAD_BUDGET_MS || 1000);
const RATE = Number(__ENV.RATE || 1);
const CONTINUITY_PATH = __ENV.CONTINUITY_PATH || 'P0';
const REFRESH_BEFORE = __ENV.REFRESH_BEFORE || '';
const TOKEN_LIFETIME_SECONDS = Number(__ENV.TOKEN_LIFETIME_SECONDS || 60);
const POLL_INTERVAL_SECONDS = 1;
const IDLE_WINDOW_SECONDS = 10;
const MARK_GAP_SECONDS = 0.6;
const JSON_HEADERS = { 'Content-Type': 'application/json' };
const ROLES = ['system_admin', 'school_admin', 'teacher', 'student'];
const SECRET_FIELD_PATTERN = /password|hash|secret|otp|refreshtoken/i;
const INTERNAL_DETAIL_PATTERN = /com\.sep\.vox|org\.hibernate|org\.springframework|org\.postgresql|java\.[a-z]+\.|[A-Za-z]Exception\b|\bat [\w.$]+\(|SQLState|\bSQL \[/;
const GRAPHQL_STATUS_BY_CLASSIFICATION = {
  UNAUTHORIZED: 401,
  FORBIDDEN: 403,
  NOT_FOUND: 404,
  BAD_REQUEST: 400,
  INTERNAL_ERROR: 500,
};

// Files must be opened here, in the init context, for example:
// const CLEAN_FILE = open('/scripts/fixtures/clean.xlsx', 'b');
// const XLSX_TYPE = 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet';

const stepDuration = new Trend('review_step_duration', true);

// TODO: the confirmed journey (references/journey.md, section 7), one entry per step, in order.
// budgetMs is the step's budget (section 5). An action's run(context) sends its requests with requestParams(),
// stores what later steps need on context, and returns false when the next step is not possible. A wait's
// isDone(context) polls the status once and returns true when the next step is possible; giveUpMs (3x budgetMs by
// default) ends that iteration's walk. Wrap requests in withRefresh() so sessionContinuity can refresh mid-path.
// Put the TRU4, NSP3 and NSP7 checks inside run() and isDone(), each named with its cell id.
const PATHS = {
  P0: [
    // {
    //   id: 'P0.S1', kind: 'action', budgetMs: 10000,
    //   run: (context) => {
    //     const response = withRefresh(context, () => http.post(
    //       `${BASE_URL}/api/v1/schools/${context.data.featureData.schoolAId}/users/import/preview`,
    //       { file: http.file(CLEAN_FILE, 'clean.xlsx', XLSX_TYPE) },
    //       requestParams(context, 'P0.S1', 'POST /api/v1/schools/:schoolId/users/import/preview', false)));
    //     context.sessionId = response.json('data.sessionId');
    //     return response.status === 200;
    //   },
    // },
    // { id: 'P0.W1', kind: 'wait', budgetMs: 60000, isDone: (context) => { /* poll the status once */ return false; } },
  ],
};

// TODO: the observable end of every path, as a function of the walk's context (J1): the success signal for paths that
// end at the target, the final state for the others (for example the session is REJECTED).
const PATH_ENDS = {
  // P0: (context) => ...,
};

// TODO: for paths that J5 applies to, how a fresh login of the same actor finds the in-progress item and its current
// status, as a function of (token, context). The walk calls it after every step of its first iteration.
const PATH_RESUMES = {
  // P0: (token, context) => http.get(`${BASE_URL}/...`, tokenParams(token, 'GET ...')).status === 200,
};

// TODO: every entry point of the inventory (E rows), on a path or not. allowedRoles comes from @PreAuthorize.
// schoolScoped is true when it takes an id or returns school-scoped data. send(token, data) sends a VALID request on
// school A's data, so that only the token decides the outcome.
const ENTRY_POINTS = [
  // {
  //   id: 'E1', allowedRoles: ['school_admin'], schoolScoped: true,
  //   send: (token, data) => http.post(`${BASE_URL}/api/v1/imports/${data.featureData.sessionId}/reject`, '{}',
  //     tokenParams(token, 'POST /api/v1/imports/:sessionId/reject')),
  // },
];

const SELECTED_PATHS = (__ENV.PATHS || Object.keys(PATHS).join(',')).split(',');

export const options = {
  setupTimeout: '300s',
  teardownTimeout: '300s',
  scenarios: {
    journeyWalk: {
      executor: 'shared-iterations', exec: 'journeyWalk', vus: 1, iterations: WALK_ITERATIONS, maxDuration: '60m',
    },
    statementCount: {
      executor: 'shared-iterations', exec: 'statementCount', vus: 1, iterations: STATEMENT_ITERATIONS, maxDuration: '60m',
    },
    probes: {
      executor: 'shared-iterations', exec: 'probes', vus: 1, iterations: 1, maxDuration: '30m',
    },
    warmUp: {
      executor: 'constant-arrival-rate', exec: 'loadStep', rate: Math.max(1, Math.ceil(RATE / 4)), timeUnit: '1s',
      duration: '30s', preAllocatedVUs: 5, maxVUs: 50,
    },
    steadyLoad: {
      executor: 'constant-arrival-rate', exec: 'loadStep', startTime: '30s', rate: RATE, timeUnit: '1s',
      duration: __ENV.DURATION || '2m', preAllocatedVUs: 20, maxVUs: 500,
    },
    burst: {
      executor: 'per-vu-iterations', exec: 'loadStep', vus: Number(__ENV.CROWD || 45), iterations: 1, maxDuration: '5m',
    },
    racingRequests: {
      executor: 'per-vu-iterations', exec: 'racingOperation', vus: Number(__ENV.RACE_VUS || 50), iterations: 1,
      maxDuration: '1m',
    },
    sessionContinuity: {
      executor: 'shared-iterations', exec: 'sessionContinuity', vus: 1, iterations: 1, maxDuration: '60m',
    },
  },
  thresholds: { ...stepThresholds(), ...loadThresholds() },
};

export function setup() {
  if (!RUN_ID || !PERF_PASSWORD) {
    fail('RUN_ID and PERF_PASSWORD must be set');
  }
  const tokens = {};
  for (const account of probeAccounts()) {
    tokens[account.key] = logIn(account.email, account.key).accessToken;
  }
  const actorTokens = [];
  for (let accountNumber = 1; accountNumber <= ACTOR_COUNT; accountNumber++) {
    actorTokens.push(logIn(accountEmail(ACTOR_ROLE, 'a', accountNumber), `actor-${accountNumber}`).accessToken);
  }
  return { tokens, actorTokens, featureData: createFeatureData(tokens, VOLUME) };
}

export function journeyWalk(data) {
  for (const pathId of SELECTED_PATHS) {
    walkPath(pathId, data, false);
  }
}

export function statementCount(data) {
  if (exec.scenario.iterationInTest === 0) {
    mark('BEGIN', 'IDLE');
    sleep(IDLE_WINDOW_SECONDS);
    mark('END', 'IDLE');
  }
  for (const pathId of SELECTED_PATHS) {
    walkPath(pathId, data, true);
  }
}

export function probes(data) {
  for (const entryPoint of ENTRY_POINTS) {
    expectOutcome(`${entryPoint.id}:SAF1`, entryPoint.send(null, data), [401]);
    for (const role of ROLES.filter((candidate) => !entryPoint.allowedRoles.includes(candidate))) {
      const response = entryPoint.send(tokenFor(data, role, 'a'), data);
      expectOutcome(`${entryPoint.id}:SAF2:${role.toUpperCase()}`, response, [403]);
    }
    if (entryPoint.schoolScoped) {
      for (const role of entryPoint.allowedRoles.filter((candidate) => candidate !== 'system_admin')) {
        const response = entryPoint.send(tokenFor(data, role, 'b'), data);
        expectOutcome(`${entryPoint.id}:SAF3:SCHOOL_B_${role.toUpperCase()}`, response, [403, 404]);
      }
    }
  }
  // TODO: one expectOutcome per remaining probe cell, named with its cell id: TRU1 (fail at the last write, then count
  // rows in SQL afterwards), TRU2 (the same request twice), TRU3 (each bad value), NSP1 (each wrong state), SAF4 (each
  // hostile input), SAF3 for student A2 on student A1's data, and J4 (the step at which each refusal fires).
  // Call checkExposure(cellId, response, data) on every response that returns data (SAF5), and
  // checkErrorDetails(cellId, response) on every error response (SAF7).
}

export function loadStep(data) {
  const step = stepById(LOAD_STEP);
  const context = {
    data,
    token: tokenForCurrentVirtualUser(data),
    pathId: LOAD_STEP.split('.')[0],
    iteration: exec.scenario.iterationInTest,
  };
  // TODO: when the step needs earlier state (accepting needs a previewed session), take one item that setup() prepared
  // for this iteration, so the measured request is the only one sent, for example:
  // context.sessionId = data.featureData.preparedSessionIds[exec.scenario.iterationInTest];
  step.run(context);
}

export function racingOperation(data) {
  const context = { data, token: tokenForCurrentVirtualUser(data) };
  // TODO: every VU sends the same write on the same resource at the same moment. Count the outcomes here; the
  // invariant itself is checked with SQL after the run (references/performance-k6.md, section I).
  const response = http.post(`${BASE_URL}/api/v1/TODO/${data.featureData.raceTargetId}/TODO`, JSON.stringify({}),
    requestParams(context, 'P0.TODO', 'POST /api/v1/TODO/:id/TODO'));
  check(response, {
    'P0.TODO:NSP2 answered 200 or a refusal': (result) => [200, 409, 422].includes(outcomeOf(result)),
  });
}

export function sessionContinuity(data) {
  // Logs in here, because the refresh_token cookie lives in this VU's cookie jar and setup() cannot hand it over.
  const session = logIn(accountEmail(ACTOR_ROLE, 'a', 1), 'continuity');
  const context = { data, token: session.accessToken, deviceId: session.deviceId, pathId: CONTINUITY_PATH, iteration: 0 };
  const cellPrefix = `${CONTINUITY_PATH}:SAF8`;
  for (const step of PATHS[CONTINUITY_PATH]) {
    if (step.id === REFRESH_BEFORE) {
      sleep(TOKEN_LIFETIME_SECONDS + 5);
      expectOutcome(`${cellPrefix}:expired token refused`, cheapAuthenticatedRead(context.token), [401]);
    }
    const finished = step.kind === 'action' ? runAction(step, context, false) : waitUntilDone(step, context);
    check(null, { [`${cellPrefix}:${step.id} goes on`]: () => finished });
    if (!finished) {
      return;
    }
  }
  check(context, {
    [`${cellPrefix}:path reaches its end`]: PATH_ENDS[CONTINUITY_PATH] || (() => false),
    [`${cellPrefix}:token was refreshed`]: () => (context.refreshCount || 0) > 0,
  });
  logOut(context.deviceId);
  expectOutcome(`${cellPrefix}:refresh refused after logout`, refreshAccessToken(context.deviceId), [401, 403]);
}

export function teardown(data) {
  // TODO: delete through the API every row createFeatureData created, using data.featureData.
  // cleanup.sql removes the accounts and anything this misses; it runs even when setup() fails.
}

function createFeatureData(tokens, volume) {
  // TODO: create the feature data through the API, as the seeded accounts, with names that start with RUN_ID, at the
  // given volume of child rows. Return the ids later steps and probes need, including school B's ids (schoolBIds) so
  // checkExposure() can spot them.
  return { schoolBIds: [] };
}

function cheapAuthenticatedRead(token) {
  // TODO: a read the actor may always do (their own profile, for example).
  return http.get(`${BASE_URL}/api/v1/TODO`, tokenParams(token, 'GET /api/v1/TODO'));
}

function walkPath(pathId, data, markActions) {
  const session = logIn(accountEmail(ACTOR_ROLE, 'a', 1), `walk-${pathId}`);
  const context = { data, token: session.accessToken, pathId, iteration: exec.scenario.iterationInTest };
  for (const step of PATHS[pathId]) {
    const finished = step.kind === 'action' ? runAction(step, context, markActions) : waitUntilDone(step, context);
    if (!finished) {
      check(null, { [`${pathId}:J1 path reaches its end`]: () => false });
      return;
    }
    checkResume(pathId, step.id, context, markActions);
  }
  check(context, { [`${pathId}:J1 path reaches its end`]: PATH_ENDS[pathId] || (() => false) });
}

function checkResume(pathId, stepId, context, markActions) {
  const findInProgressItem = PATH_RESUMES[pathId];
  if (!findInProgressItem || markActions || context.iteration !== 0) {
    return;
  }
  const freshSession = logIn(accountEmail(ACTOR_ROLE, 'a', 1), `resume-${pathId}`);
  check(context, {
    [`${pathId}:J5 found again after ${stepId}`]: () => findInProgressItem(freshSession.accessToken, context),
  });
}

function runAction(step, context, markActions) {
  if (markActions) {
    sleep(MARK_GAP_SECONDS);
    mark('BEGIN', step.id);
  }
  const startedAt = Date.now();
  let nextStepPossible = false;
  try {
    nextStepPossible = step.run(context) !== false;
  } finally {
    stepDuration.add(Date.now() - startedAt, { step: step.id, kind: 'action' });
    if (markActions) {
      mark('END', step.id);
      sleep(MARK_GAP_SECONDS);
    }
  }
  return nextStepPossible;
}

function waitUntilDone(step, context) {
  const startedAt = Date.now();
  const giveUpAt = startedAt + (step.giveUpMs || 3 * step.budgetMs);
  while (Date.now() < giveUpAt) {
    if (step.isDone(context)) {
      stepDuration.add(Date.now() - startedAt, { step: step.id, kind: 'wait' });
      return true;
    }
    sleep(POLL_INTERVAL_SECONDS);
  }
  stepDuration.add(Date.now() - startedAt, { step: step.id, kind: 'wait' });
  console.warn(`REVIEW-FAIL ${step.id}:SPD3 expected the wait to end within ${giveUpAt - startedAt} ms`);
  return false;
}

function mark(kind, stepId) {
  console.log(`REVIEW-MARK ${kind} ${stepId} ${new Date().toISOString()}`);
}

function expectOutcome(cellId, response, expectedStatuses) {
  const outcome = outcomeOf(response);
  const passed = expectedStatuses.includes(outcome);
  check(response, { [`${cellId} expects ${expectedStatuses.join(' or ')}`]: () => passed });
  if (!passed) {
    console.warn(`REVIEW-FAIL ${cellId} expected ${expectedStatuses.join(' or ')} got ${outcome}`);
  }
  return passed;
}

function outcomeOf(response) {
  if (response.url.indexOf('/graphql') === -1) {
    return response.status;
  }
  const body = parsedJson(response);
  if (response.status !== 200 || body === null) {
    return response.status;
  }
  if (!body.errors || body.errors.length === 0) {
    return 200;
  }
  const extensions = body.errors[0].extensions || {};
  return GRAPHQL_STATUS_BY_CLASSIFICATION[extensions.classification] || 500;
}

function checkExposure(cellId, response, data) {
  const body = parsedJson(response);
  const exposedPaths = [];
  if (body !== null) {
    collectExposedPaths(body, data.featureData.schoolBIds || [], '$', exposedPaths);
  }
  check(response, { [`${cellId} shows nothing the actor may not see`]: () => exposedPaths.length === 0 });
  if (exposedPaths.length > 0) {
    // Prints the JSON paths only, never the values.
    console.warn(`REVIEW-FAIL ${cellId} exposed ${exposedPaths.join(', ')}`);
  }
}

function collectExposedPaths(value, otherSchoolIds, path, exposedPaths) {
  if (Array.isArray(value)) {
    value.forEach((item, index) => collectExposedPaths(item, otherSchoolIds, `${path}[${index}]`, exposedPaths));
  } else if (value !== null && typeof value === 'object') {
    for (const key of Object.keys(value)) {
      if (SECRET_FIELD_PATTERN.test(key)) {
        exposedPaths.push(`${path}.${key}`);
      }
      collectExposedPaths(value[key], otherSchoolIds, `${path}.${key}`, exposedPaths);
    }
  } else if (otherSchoolIds.includes(value)) {
    exposedPaths.push(`${path} (a school B id)`);
  }
}

function checkErrorDetails(cellId, response) {
  const revealsInternals = INTERNAL_DETAIL_PATTERN.test(String(response.body || ''));
  check(response, { [`${cellId} error reveals no internals`]: () => !revealsInternals });
  if (revealsInternals) {
    console.warn(`REVIEW-FAIL ${cellId} error body reveals internals`);
  }
}

function withRefresh(context, sendRequest) {
  const response = sendRequest();
  if (!context.deviceId || outcomeOf(response) !== 401) {
    return response;
  }
  const refreshed = refreshAccessToken(context.deviceId);
  if (refreshed.status !== 200) {
    return response;
  }
  context.token = refreshed.json('data.accessToken');
  context.refreshCount = (context.refreshCount || 0) + 1;
  return sendRequest();
}

function logIn(email, accountKey) {
  const deviceId = `${RUN_ID}-device-${accountKey}`;
  const body = {
    login: email,
    password: PERF_PASSWORD,
    device: { deviceId, deviceName: 'k6 review run', platform: 'WEB' },
  };
  const response = http.post(`${BASE_URL}/api/v1/auth/login`, JSON.stringify(body), {
    headers: JSON_HEADERS,
    tags: { name: 'POST /api/v1/auth/login', step: 'login' },
  });
  if (response.status !== 200) {
    fail(`login failed for account ${accountKey} with status ${response.status}`);
  }
  return { accessToken: response.json('data.accessToken'), deviceId };
}

function refreshAccessToken(deviceId) {
  return postWithCsrf(`${BASE_URL}/api/v1/auth/refresh`, JSON.stringify({ deviceId }), 'POST /api/v1/auth/refresh');
}

function logOut(deviceId) {
  return postWithCsrf(`${BASE_URL}/api/v1/auth/logout`, JSON.stringify({ deviceId }), 'POST /api/v1/auth/logout');
}

function postWithCsrf(url, body, name) {
  if (!xsrfCookie()) {
    // The first protected call without the header answers 403 and sets the XSRF-TOKEN cookie
    // (SecurityConfig, CSRF_PROTECTED_API_PATHS).
    http.post(url, body, { headers: JSON_HEADERS, tags: { name: `${name} (csrf cookie)`, step: 'session' },
      responseCallback: http.expectedStatuses(403) });
  }
  return http.post(url, body, {
    headers: { ...JSON_HEADERS, 'X-XSRF-TOKEN': xsrfCookie() || '' },
    tags: { name, step: 'session' },
  });
}

function xsrfCookie() {
  const cookies = http.cookieJar().cookiesForURL(BASE_URL);
  return cookies['XSRF-TOKEN'] ? cookies['XSRF-TOKEN'][0] : null;
}

// For GraphQL operations; delete it when the feature has none.
function graphQL(context, stepId, query, variables, requestName) {
  return withRefresh(context, () => http.post(`${BASE_URL}/graphql`, JSON.stringify({ query, variables }),
    requestParams(context, stepId, requestName)));
}

function requestParams(context, stepId, requestName, sendsJson = true) {
  const headers = sendsJson ? { ...JSON_HEADERS } : {};
  if (context.token) {
    headers.Authorization = `Bearer ${context.token}`;
  }
  return { headers, tags: { name: requestName, step: stepId } };
}

function tokenParams(token, requestName) {
  return requestParams({ token }, 'probe', requestName);
}

function parsedJson(response) {
  try {
    return response.json();
  } catch (error) {
    return null;
  }
}

function stepById(stepId) {
  for (const steps of Object.values(PATHS)) {
    const step = steps.find((candidate) => candidate.id === stepId);
    if (step) {
      return step;
    }
  }
  fail(`no step ${stepId} in PATHS`);
}

function stepThresholds() {
  const thresholds = {};
  for (const steps of Object.values(PATHS)) {
    for (const step of steps) {
      const statistic = step.kind === 'action' ? 'p(95)' : 'max';
      thresholds[`review_step_duration{scenario:journeyWalk,step:${step.id}}`] = [`${statistic}<=${step.budgetMs}`];
    }
  }
  return thresholds;
}

function loadThresholds() {
  const thresholds = {};
  for (const scenarioName of ['steadyLoad', 'burst']) {
    thresholds[`http_req_duration{scenario:${scenarioName},step:${LOAD_STEP}}`] = [`p(95)<=${LOAD_BUDGET_MS}`];
    thresholds[`http_req_failed{scenario:${scenarioName}}`] = ['rate<0.01'];
  }
  thresholds['dropped_iterations{scenario:steadyLoad}'] = ['count==0'];
  return thresholds;
}

function probeAccounts() {
  const accounts = [{ key: 'x-system_admin-1', email: accountEmail('system_admin', 'x', 1) }];
  for (const school of ['a', 'b']) {
    for (const role of ROLES.filter((candidate) => candidate !== 'system_admin')) {
      accounts.push({ key: `${school}-${role}-1`, email: accountEmail(role, school, 1) });
    }
  }
  accounts.push({ key: 'a-student-2', email: accountEmail('student', 'a', 2) });
  return accounts;
}

function accountEmail(role, school, accountNumber) {
  return `${RUN_ID}-${schoolKeyOf(role, school)}-${role}-${accountNumber}@perf.invalid`;
}

function tokenFor(data, role, school) {
  return data.tokens[`${schoolKeyOf(role, school)}-${role}-1`];
}

function schoolKeyOf(role, school) {
  // The seeded system admin belongs to no school (seed.sql uses 'X').
  return role === 'system_admin' ? 'x' : school;
}

function tokenForCurrentVirtualUser(data) {
  return data.actorTokens[(exec.vu.idInTest - 1) % data.actorTokens.length];
}
