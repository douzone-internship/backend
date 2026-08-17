// 모든 k6 시나리오가 공유하는 헬퍼.
// - BASE_URL: 테스트 대상 (기본 로컬 docker-compose 스택)
// - discoverCombos(): 시딩된 HIRA 데이터에서 실제로 존재하는
//   {clinicCode, sidoCode, sigguCode, hospitalName} 조합을 동적으로 수집한다.
//   정적 CSV 픽스처를 쓰지 않는 이유: DB 내용이 바뀌어도(재시딩 등) 항상 최신 상태와
//   동기화되기 때문. 단, 검색 키워드 자체는 실제 시딩된 데이터셋에 맞게
//   BASE_URL과 함께 -e 옵션으로 넘기거나 아래 기본값을 조정해야 한다.
//   (예: docker compose 기동 후 `curl "$BASE_URL/api/home/clinics?name=<키워드>"`로
//   먼저 확인해볼 것 — README 참고)
import http from 'k6/http';
import { check } from 'k6';

export const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

const LOCATION_KEYWORDS = (__ENV.LOCATION_KEYWORDS || '서울,경기,부산,인천,대구')
    .split(',')
    .map((s) => s.trim())
    .filter(Boolean);

const CLINIC_KEYWORDS = (__ENV.CLINIC_KEYWORDS || '라식,임플란트,MRI,도수치료,피부')
    .split(',')
    .map((s) => s.trim())
    .filter(Boolean);

function jsonGet(path) {
    const res = http.get(`${BASE_URL}${path}`);
    check(res, { [`GET ${path} 200`]: (r) => r.status === 200 });
    if (res.status !== 200) {
        return null;
    }
    try {
        return res.json();
    } catch (e) {
        return null;
    }
}

function discoverLocations() {
    const locations = [];
    for (const kw of LOCATION_KEYWORDS) {
        const body = jsonGet(`/api/home/locations?name=${encodeURIComponent(kw)}`);
        if (body && Array.isArray(body.locations)) {
            locations.push(...body.locations);
        }
    }
    return locations;
}

function discoverClinics() {
    const clinics = [];
    for (const kw of CLINIC_KEYWORDS) {
        const body = jsonGet(`/api/home/clinics?name=${encodeURIComponent(kw)}`);
        if (body && Array.isArray(body.clinicResponseDTOList)) {
            clinics.push(...body.clinicResponseDTOList);
        }
    }
    return clinics;
}

function discoverHospitalNames(sidoCode, keyword) {
    const body = jsonGet(`/api/home/hospitals?location=${encodeURIComponent(sidoCode)}&name=${encodeURIComponent(keyword)}`);
    if (body && Array.isArray(body.nameList)) {
        return body.nameList;
    }
    return [];
}

/**
 * GET /api/home/hospitals 하나만 반복 타격하는 시나리오(01, 02번)를 위한
 * 가벼운 셋업: 실제로 결과가 나오는 {location, name} 쿼리 파라미터 하나를 찾는다.
 */
export function discoverHospitalQuery() {
    const locations = discoverLocations();
    for (const location of locations) {
        for (const kw of CLINIC_KEYWORDS) {
            const names = discoverHospitalNames(location.sidoCode, kw);
            if (names.length > 0) {
                return { location: location.sidoCode, name: kw };
            }
        }
    }
    // 못 찾으면 그래도 첫 지역+첫 키워드로 폴백 (결과가 비어도 DB 조회 자체는 발생하므로
    // Hikari 풀 부하 시나리오 목적엔 지장 없음)
    return {
        location: locations[0] ? locations[0].sidoCode : '110000',
        name: CLINIC_KEYWORDS[0],
    };
}

/**
 * 실제 DB에 존재하는 {clinicCode, sidoCode, sigguCode, hospitalName} 조합을 수집한다.
 * setup()에서 한 번 호출해서 VU들이 공유하도록 사용할 것.
 */
export function discoverCombos(maxCombos = 20) {
    const locations = discoverLocations();
    const clinics = discoverClinics();
    const combos = [];

    if (locations.length === 0 || clinics.length === 0) {
        return combos;
    }

    // 병원명은 지역+검색어 조합마다 다시 조회해야 하므로, 조합 개수를 제한해서
    // discoverCombos() 자체가 과도한 셋업 요청을 만들지 않도록 한다.
    outer: for (const location of locations) {
        for (const kw of CLINIC_KEYWORDS) {
            const hospitalNames = discoverHospitalNames(location.sidoCode, kw);
            for (const clinic of clinics) {
                if (hospitalNames.length === 0) {
                    combos.push({
                        clinicCode: clinic.clinicCode,
                        sidoCode: location.sidoCode,
                        sigguCode: location.sgguCode || undefined,
                        hospitalName: undefined,
                    });
                } else {
                    for (const hospitalName of hospitalNames) {
                        combos.push({
                            clinicCode: clinic.clinicCode,
                            sidoCode: location.sidoCode,
                            sigguCode: location.sgguCode || undefined,
                            hospitalName,
                        });
                        if (combos.length >= maxCombos) break outer;
                    }
                }
                if (combos.length >= maxCombos) break outer;
            }
        }
    }

    return combos;
}

/**
 * 회원가입 + 로그인을 1회 수행해 accessToken을 반환한다.
 * 매 실행마다 겹치지 않도록 emailPrefix에 타임스탬프 등을 섞어서 호출할 것.
 */
export function signupAndLogin(emailPrefix) {
    const email = `${emailPrefix}-${Date.now()}@loadtest.local`;
    const password = 'loadtest-password-1234';

    const signupRes = http.post(
        `${BASE_URL}/api/auth/signup`,
        JSON.stringify({ email, password, name: 'k6 loadtest' }),
        { headers: { 'Content-Type': 'application/json' } },
    );
    check(signupRes, { 'signup 200': (r) => r.status === 200 });

    const loginRes = http.post(
        `${BASE_URL}/api/auth/login`,
        JSON.stringify({ email, password }),
        { headers: { 'Content-Type': 'application/json' } },
    );
    check(loginRes, { 'login 200': (r) => r.status === 200 });

    if (loginRes.status !== 200) {
        return null;
    }
    return loginRes.json('accessToken');
}

export function authHeaders(token) {
    return token ? { Authorization: `Bearer ${token}` } : {};
}
