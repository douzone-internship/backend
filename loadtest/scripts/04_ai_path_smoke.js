// 시나리오 04: 실제 Gemini 호출 경로를 저동시성으로 검증한다.
// 비용/자체 서킷브레이커(geminiApi: 슬라이딩 윈도우 5, 실패율 50%, open 60s)
// 제약 때문에 절대 대량 동시성으로 돌리지 않는다 — 이 스크립트는 그 제약을
// 지키기 위해 총 9회(3 VU x 3 iterations)로 고정되어 있다.
//
// 확인 방법:
//   - 각 요청의 http_req_duration (AgentLoop는 MAX_STEPS=5, 보통 성공 시
//     Gemini 2회 왕복 — 수 초~수십 초 걸리는 게 정상)
//   - Grafana에서 resilience4j_circuitbreaker_calls_seconds_count{name="geminiApi"}
//     (kind="successful"|"failed") 와 resilience4j_circuitbreaker_state{name="geminiApi"}
//     — 착수 전에 `curl $BASE_URL/actuator/prometheus | grep resilience4j_circuitbreaker`로
//     이 resilience4j 버전의 정확한 메트릭/라벨명을 먼저 확인할 것 (버전별로 조금씩 다름)
//
// 서킷브레이커 open → aiApiFallback 동작 자체는 이 스크립트로 강제할 수 없다
// (실제 Gemini를 고의로 실패시킬 수 없으므로). loadtest/README.md의
// "서킷브레이커 수동 검증" 절차를 참고할 것.
import http from 'k6/http';
import { check } from 'k6';
import { BASE_URL, discoverCombos } from './lib.js';

export const options = {
    scenarios: {
        ai_path_smoke: {
            executor: 'per-vu-iterations',
            vus: 3,
            iterations: 3,
            maxDuration: '10m',
        },
    },
};

export function setup() {
    // 매 iteration마다 서로 다른(캐시-미스가 보장되는) 조합이 필요하므로 9개 이상 확보
    const combos = discoverCombos(15);
    if (combos.length < 9) {
        throw new Error(
            `setup 실패: 유효한 조합을 ${combos.length}개밖에 못 찾음(9개 필요). ` +
                'lib.js의 LOCATION_KEYWORDS/CLINIC_KEYWORDS를 시딩된 데이터에 맞게 조정할 것.',
        );
    }
    return { combos };
}

export default function (data) {
    // VU(1~3) x iteration(1~3) 조합마다 겹치지 않는 인덱스를 사용
    const index = (__VU - 1) * 3 + (__ITER % 3);
    const combo = data.combos[index % data.combos.length];

    const body = { clinicCode: combo.clinicCode, sidoCode: combo.sidoCode };
    if (combo.hospitalName) body.hospitalName = combo.hospitalName;
    if (combo.sigguCode) body.sigguCode = combo.sigguCode;

    const res = http.post(`${BASE_URL}/api/result/reports`, JSON.stringify(body), {
        headers: { 'Content-Type': 'application/json' },
        timeout: '60s',
    });
    check(res, { 'status is 200': (r) => r.status === 200 });
}
