// 시나리오 03: POST /api/result/reports의 캐시-hit(대량, DB 전용) vs
// 캐시-miss(단발, 실제 Gemini 호출) 처리량/지연 차이를 비교한다.
//
// AiResponseCache의 캐시 키는 clinicCode+sidoCode+sigguCode+hospitalName+댓글수의
// SHA-256이라 TTL이 없고, 같은 파라미터로 다시 요청하면 DB 읽기만으로 응답된다.
// 그래서:
//   - setup()에서 warmCombo 하나를 골라 실제 Gemini 호출 1회로 캐시를 미리 채우고
//   - cache_hit_bulk 시나리오는 그 warmCombo만 반복 요청 (대량 동시성 안전 — Gemini 호출 없음)
//   - cache_miss_probe 시나리오는 한 번도 안 쓴 조합 2개만 아주 저동시성으로 찔러봄
//     (Gemini 실호출 비용/서킷브레이커 제약 때문에 절대 대량으로 돌리지 않는다)
//
// 확인 방법: k6 요약에서 cache_hit_bulk의 http_req_duration(수십 ms대 기대)과
// cache_miss_probe의 개별 요청 소요시간(수 초 이상, OpenData API + Gemini 왕복
// 때문)을 비교. cache_hit_bulk 도중 Grafana의 tomcat_threads_busy_threads /
// http_server_requests_active가 VU 수를 잘 따라가다가 ramp-down 직후 바로
// 회복되는지도 확인 (스레드풀 적체가 없는지).
import http from 'k6/http';
import { check, sleep } from 'k6';
import { BASE_URL, discoverCombos } from './lib.js';

export const options = {
    scenarios: {
        cache_hit_bulk: {
            executor: 'ramping-vus',
            exec: 'cacheHitBulk',
            startVUs: 0,
            stages: [
                { duration: '30s', target: 30 },
                { duration: '60s', target: 30 },
                { duration: '15s', target: 0 },
            ],
        },
        cache_miss_probe: {
            executor: 'shared-iterations',
            exec: 'cacheMissProbe',
            vus: 1,
            iterations: 2,
            startTime: '0s',
            maxDuration: '5m', // Gemini 호출은 수십 초까지 걸릴 수 있음
        },
    },
};

function buildBody(combo) {
    const body = { clinicCode: combo.clinicCode, sidoCode: combo.sidoCode };
    if (combo.hospitalName) body.hospitalName = combo.hospitalName;
    if (combo.sigguCode) body.sigguCode = combo.sigguCode;
    return body;
}

function postReport(combo) {
    return http.post(`${BASE_URL}/api/result/reports`, JSON.stringify(buildBody(combo)), {
        headers: { 'Content-Type': 'application/json' },
        timeout: '60s',
    });
}

export function setup() {
    const combos = discoverCombos(10);
    if (combos.length < 3) {
        throw new Error(
            `setup 실패: 유효한 조합을 ${combos.length}개밖에 못 찾음(최소 3개 필요). ` +
                'lib.js의 LOCATION_KEYWORDS/CLINIC_KEYWORDS를 시딩된 데이터에 맞게 조정할 것.',
        );
    }

    const warmCombo = combos[0];
    const missCombos = combos.slice(1, 3);

    console.log(`[setup] warming cache with clinicCode=${warmCombo.clinicCode} sidoCode=${warmCombo.sidoCode}`);
    const warmRes = postReport(warmCombo);
    check(warmRes, { 'warmup 200': (r) => r.status === 200 });

    return { warmCombo, missCombos };
}

export function cacheHitBulk(data) {
    const res = postReport(data.warmCombo);
    check(res, { 'cache hit 200': (r) => r.status === 200 });
    sleep(0.1);
}

export function cacheMissProbe(data) {
    const combo = data.missCombos[__ITER % data.missCombos.length];
    const res = postReport(combo);
    check(res, { 'cache miss 200': (r) => r.status === 200 });
}
