// k6 v2 load-test template for the review-feature skill.
// Copy it into the run folder in the session scratchpad and fill in every TODO. Never commit a filled-in copy.
// Run one of: --scenario warmUp,steadyLoad (latency under load) or --scenario racingRequests (race probe).
import http from 'k6/http';
import { check, fail } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://host.docker.internal:8081';
const RUN_ID = __ENV.RUN_ID;
const PERF_PASSWORD = __ENV.PERF_PASSWORD;
const ACCOUNT_ROLE = __ENV.ACCOUNT_ROLE || 'teacher';
const ACCOUNT_COUNT = Number(__ENV.ACCOUNT_COUNT || 1);
const JSON_HEADERS = { 'Content-Type': 'application/json' };

export const options = {
  setupTimeout: '120s',
  teardownTimeout: '120s',
  scenarios: {
    warmUp: {
      executor: 'constant-arrival-rate',
      exec: 'measuredOperation',
      rate: 5,
      timeUnit: '1s',
      duration: '30s',
      preAllocatedVUs: 5,
      maxVUs: 20,
    },
    steadyLoad: {
      executor: 'constant-arrival-rate',
      exec: 'measuredOperation',
      startTime: '30s',
      rate: Number(__ENV.RATE || 20),
      timeUnit: '1s',
      duration: __ENV.DURATION || '2m',
      preAllocatedVUs: 20,
      maxVUs: 100,
    },
    racingRequests: {
      executor: 'per-vu-iterations',
      exec: 'racingOperation',
      vus: Number(__ENV.RACE_VUS || 50),
      iterations: 1,
      maxDuration: '1m',
    },
  },
  thresholds: {
    'http_req_failed{scenario:steadyLoad}': ['rate<0.01'],
    'http_req_duration{scenario:steadyLoad}': ['p(95)<500', 'p(99)<1500'],
    'checks{scenario:steadyLoad}': ['rate>0.99'],
  },
};

export function setup() {
  if (!RUN_ID || !PERF_PASSWORD) {
    fail('RUN_ID and PERF_PASSWORD must be set');
  }
  const tokens = [];
  for (let accountNumber = 1; accountNumber <= ACCOUNT_COUNT; accountNumber++) {
    tokens.push(logIn(`${RUN_ID}-${ACCOUNT_ROLE}-${accountNumber}@perf.invalid`, accountNumber));
  }
  const createdFeatureIds = createFeatureData(tokens[0]);
  return { tokens, createdFeatureIds };
}

export function measuredOperation(data) {
  const token = tokenForCurrentVirtualUser(data.tokens);
  // TODO: call the operation under review, for example:
  const response = http.get(`${BASE_URL}/api/v1/TODO/${data.createdFeatureIds[0]}`, {
    headers: authorizationHeaders(token),
    tags: { name: 'GET /api/v1/TODO/:id' },
  });
  check(response, { 'status is 200': (result) => result.status === 200 });
}

export function racingOperation(data) {
  const token = tokenForCurrentVirtualUser(data.tokens);
  // TODO: every VU fires the same state-changing operation against the same resource at the same moment.
  // Count the expected outcomes here; the invariant itself is checked with SQL after the run.
  const response = http.post(`${BASE_URL}/api/v1/TODO/${data.createdFeatureIds[0]}/TODO`, JSON.stringify({}), {
    headers: authorizationHeaders(token),
    tags: { name: 'POST /api/v1/TODO/:id/TODO' },
  });
  check(response, { 'status is 200 or a business rule refusal': (result) => [200, 409, 422].includes(result.status) });
}

export function teardown(data) {
  // TODO: delete through the API every row createFeatureData created, using data.createdFeatureIds.
  // cleanup.sql removes the accounts and anything this misses; it runs even when setup() fails.
}

function createFeatureData(token) {
  // TODO: create the feature data through the API, with names starting with RUN_ID, and return their ids.
  return [];
}

function logIn(email, accountNumber) {
  const body = {
    login: email,
    password: PERF_PASSWORD,
    device: { deviceId: `${RUN_ID}-device-${accountNumber}`, deviceName: 'k6 review run', platform: 'WEB' },
  };
  const response = http.post(`${BASE_URL}/api/v1/auth/login`, JSON.stringify(body), {
    headers: JSON_HEADERS,
    tags: { name: 'POST /api/v1/auth/login' },
  });
  if (response.status !== 200) {
    fail(`login failed for ${email} with status ${response.status}`);
  }
  return response.json('data.accessToken');
}

// For GraphQL operations; delete it when the feature has none.
function graphQL(token, query, variables, requestName) {
  const response = http.post(`${BASE_URL}/graphql`, JSON.stringify({ query, variables }), {
    headers: authorizationHeaders(token),
    tags: { name: requestName },
  });
  check(response, {
    'graphql status is 200': (result) => result.status === 200,
    'graphql has no errors': (result) => !result.json('errors'),
  });
  return response;
}

function authorizationHeaders(token) {
  return { ...JSON_HEADERS, Authorization: `Bearer ${token}` };
}

function tokenForCurrentVirtualUser(tokens) {
  return tokens[(__VU - 1) % tokens.length];
}
