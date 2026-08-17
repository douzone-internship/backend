// 시나리오 05 (스트레치, 선택): CommentRepository/FavoriteRepository/
// ResultRepository가 페이지네이션 없이 List<>를 통째로 반환하는데, 데이터량이
// 늘어나면 GET /api/comments 응답 시간/페이로드 크기가 어떻게 열화되는지 관찰한다.
// 01~04번이 안정적으로 끝난 뒤, 시간이 남을 때만 시도할 것.
//
// setup()에서 SEED_COUNT(기본 2000)개의 댓글을 하나의 hospitalName+clinicCode에
// 몰아서 시딩한 뒤, 그 조합을 저동시성으로 반복 조회한다.
// 시딩 자체가 오래 걸릴 수 있어 setupTimeout을 넉넉히 잡았다.
//
// 확인 방법: 요청 수를 늘려가며(예: SEED_COUNT=500/2000/5000로 반복 실행)
// http_req_duration과 응답 바디 크기(k6 data_received)가 선형에 가깝게
// 증가하는지, jvm_memory_used_bytes가 조회 시점마다 튀는지 Grafana에서 관찰.
import http from 'k6/http';
import { check, sleep } from 'k6';
import { BASE_URL, discoverCombos, signupAndLogin, authHeaders } from './lib.js';

const SEED_COUNT = parseInt(__ENV.SEED_COUNT || '2000', 10);

export const options = {
    setupTimeout: '5m',
    scenarios: {
        unbounded_list_soak: {
            executor: 'constant-vus',
            vus: 5,
            duration: '60s',
        },
    },
};

export function setup() {
    const combos = discoverCombos(200).filter((c) => c.hospitalName);
    if (combos.length === 0) {
        throw new Error('setup 실패: hospitalName이 포함된 조합을 찾지 못함');
    }
    const target = combos[0];

    const token = signupAndLogin('unbounded-soak');
    if (!token) {
        throw new Error('setup 실패: 로그인 토큰을 발급받지 못함');
    }
    const headers = { 'Content-Type': 'application/json', ...authHeaders(token) };

    console.log(`[setup] seeding ${SEED_COUNT} comments for hospitalName=${target.hospitalName} clinicCode=${target.clinicCode}`);
    for (let i = 0; i < SEED_COUNT; i++) {
        const res = http.post(
            `${BASE_URL}/api/comments`,
            JSON.stringify({
                hospitalName: target.hospitalName,
                clinicCode: target.clinicCode,
                comment: `load test seed comment #${i}`,
                score: (i % 5) + 1,
            }),
            { headers },
        );
        if (res.status !== 200 && i % 100 === 0) {
            console.warn(`[setup] seed #${i} failed with status ${res.status}`);
        }
    }

    return { target };
}

export default function (data) {
    const res = http.get(
        `${BASE_URL}/api/comments?hospitalName=${encodeURIComponent(data.target.hospitalName)}&clinicCode=${encodeURIComponent(data.target.clinicCode)}`,
    );
    check(res, { 'status is 200': (r) => r.status === 200 });
    sleep(0.5);
}
