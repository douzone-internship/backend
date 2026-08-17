// 시나리오 01: Hikari 커넥션 풀(maximum-pool-size=5)이 동시성 병목의
// 지배적 요인인지 확인한다. 대상은 인증이 필요 없는 순수 DB 조회 엔드포인트
// (GET /api/home/hospitals)라서, 여기서 관찰되는 열화는 비즈니스 로직이 아니라
// 온전히 "DB 커넥션을 기다리는 시간"에서 온다.
//
// 확인 방법 (Grafana):
//   - hikaricp_connections_active 가 5에서 평평해지는지
//   - hikaricp_connections_pending 이 VU 수가 풀 크기를 넘어서는 시점부터
//     0에서 벗어나 치솟는지
//   - 같은 시점에 k6의 http_req_duration p95/p99가 같이 꺾이는지
//   - jvm_memory_used_bytes / process_cpu_usage 는 평평하게 유지되는지
//     (CPU 바운드가 아니라 풀 대기임을 배제하기 위함)
import http from 'k6/http';
import { check, sleep } from 'k6';
import { BASE_URL, discoverHospitalQuery } from './lib.js';

export const options = {
    scenarios: {
        hikari_saturation: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '30s', target: 20 },
                { duration: '60s', target: 20 },
                { duration: '30s', target: 50 },
                { duration: '60s', target: 50 },
                { duration: '15s', target: 0 },
            ],
        },
    },
    thresholds: {
        // 탐색적 테스트라 pass/fail보다는 수치 확인이 목적이지만,
        // 완전히 죽지는 않는지 정도의 안전핀은 걸어둔다.
        http_req_failed: ['rate<0.5'],
    },
};

export function setup() {
    const query = discoverHospitalQuery();
    console.log(`[setup] using location=${query.location} name=${query.name}`);
    return query;
}

export default function (query) {
    const res = http.get(
        `${BASE_URL}/api/home/hospitals?location=${encodeURIComponent(query.location)}&name=${encodeURIComponent(query.name)}`,
    );
    check(res, { 'status is 200': (r) => r.status === 200 });
    sleep(0.2);
}
