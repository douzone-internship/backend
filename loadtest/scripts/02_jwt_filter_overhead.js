// 시나리오 02: JwtFilter가 유효한 Bearer 토큰이 있을 때만 수행하는
// CustomUserDetailsService.loadUserByUsername() DB 조회의 요청당 비용을 격리한다.
//
// 주의: "공개 엔드포인트 vs 인증 필요 엔드포인트"를 비교하면 비즈니스 로직
// 차이까지 섞여버린다. 그래서 *같은* 공개 엔드포인트(GET /api/home/hospitals)를
// Authorization 헤더 유/무로만 다르게 호출해서, 순수하게 JwtFilter의 DB 조회
// 오버헤드만 분리한다.
//
// 확인 방법: k6 요약(summary)에서 scenario별 http_req_duration p50/p95를 비교.
// experimental-prometheus-rw로 내보내면 Grafana에서도 k6_http_req_duration을
// scenario 라벨로 필터링해서 비교 가능.
import http from 'k6/http';
import { check, sleep } from 'k6';
import { BASE_URL, discoverHospitalQuery, signupAndLogin, authHeaders } from './lib.js';

export const options = {
    scenarios: {
        anon_home: {
            executor: 'constant-vus',
            vus: 20,
            duration: '60s',
            exec: 'anonHome',
        },
        auth_home: {
            executor: 'constant-vus',
            vus: 20,
            duration: '60s',
            exec: 'authHome',
            startTime: '65s', // anon_home과 겹치지 않게 순차 실행 (풀 경합 변수 제거)
        },
    },
};

export function setup() {
    const query = discoverHospitalQuery();
    const token = signupAndLogin('jwt-overhead');
    if (!token) {
        throw new Error('setup 실패: 로그인 토큰을 발급받지 못함');
    }
    return { query, token };
}

function callHospitals(query, headers) {
    const res = http.get(
        `${BASE_URL}/api/home/hospitals?location=${encodeURIComponent(query.location)}&name=${encodeURIComponent(query.name)}`,
        { headers },
    );
    check(res, { 'status is 200': (r) => r.status === 200 });
    sleep(0.2);
}

export function anonHome(data) {
    callHospitals(data.query, {});
}

export function authHome(data) {
    callHospitals(data.query, authHeaders(data.token));
}
