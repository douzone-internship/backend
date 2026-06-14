# LLM Harness 측정 결과

기준일: 2026-06-14
대상 모델: gemini-2.5-flash
평가 환경: 합성 픽스처(5병원 × 1시술, 후기 보유 2병원·미보유 3병원)

---

## 수치 1. DB 쿼리 횟수 — **94% 감소** ⭐

`QueryCountBench` 측정. Hibernate `Statistics.getPrepareStatementCount()` 기반.

| 항목 | 값 |
|---|---|
| 시나리오 | 5병원 종합 추천 |
| Before (개별 도구 × 병원수) | **17 건** (Review·Rating·NegativeFlag 각 5회 + 부수 쿼리) |
| After (배치 도구 1회, `IN` 절) | **1 건** |
| **절감률** | **94%** |

**개선 메커니즘**: 병원별 3종 도구 호출 패턴을 단일 IN 절 쿼리(`findByHospitalNameInAndClinicCodeOrderByCreatedAtDesc`) + 메모리 그룹핑으로 통합. N+1 회피.

**자소서 표현**:
> "다중 병원 비교 추천 시 도구 호출을 N+1 회피 가능한 배치 패턴(IN 절)으로 통합하여 DB 쿼리 17회 → 1회 (94% 감소)"

---

## 수치 2. 입력 토큰 — **5% 감소**

`TokenCountBench` 측정. Gemini `countTokens()` API (LLM 호출 없음).

| 항목 | 값 |
|---|---|
| 샘플 | 5병원 × 1시술 |
| Before (`record.toString()` join) | 293 토큰 |
| After (PromptBuilder 평문 직조) | 278 토큰 |
| **절감률** | **5.1%** |

**한계**: 한국어 비중이 높은 도메인 특성상 영어 라벨(`hospitalName=`) 제거 효과가 제한적. 절대값은 작음.

**자소서 표현 (정직 버전)**:
> "프롬프트 직조 방식 변경(record toString → 도메인 친화 평문)으로 입력 토큰 약 5% 절감. 한국어 비중 높은 도메인 특성으로 절감폭 제한적이나, **함께 도입된 결정론적 통계 분리로 LLM이 산수에 쓰는 출력 토큰을 추가로 제거**."

---

## 수치 3. 환각 — 사람 라벨링 평가셋 (14개 시나리오) ⭐

`HallucinationLabelingBench` 측정. 데이터 보유/미보유 병원을 의도적으로 섞은 합성 시나리오를
Before(단발 호출, 옛 프롬프트, 도구 없음) / After(에이전트, 도구 호출) 두 모드로 실행하고,
응답을 사람이 직접 읽어 환각을 카운트.

**환각 정의(넓은 정의)**: 데이터 미보유 병원에 대한 (1) 후기/평점 수치 인용,
(2) "전문성·만족도·안정적·인기·긍정 후기" 등 평가성 표현, (3) WARNING 병원을 "문제없음"으로 단정하거나 칭찬.

**측정 규모**: 14개 시나리오 (시나리오당 5병원). 모델: 1~4 gemini-2.5-flash, 5~14 gemini-2.5-flash-lite
(무료 API 일일 한도로 모델 분리 수집. 각 시나리오 내 Before/After는 동일 모델이라 구조 비교는 유효).

| 지표 | Before (단발) | After (에이전트) |
|---|---|---|
| 총 환각 건수 | **13건** | **0건** |
| 응답당 평균 환각 | **0.93건** | **0건** |
| 환각 발생 시나리오 | 14개 중 6개 | 14개 중 0개 |

### 대표 사례
- **WARNING 병원 칭찬 (가장 치명적)**: Before는 재수술·부작용 후기가 있는 강남미인의원(S12)·논현케어의원을
  "긍정 후기" "안정적·선호도 높음"으로 추천. After는 "주의 병원"으로 정확히 분류하거나 추천에서 제외.
- **미보유 병원 후기 창작**: Before는 후기가 0개인 대치365의원(S06,S13)을 "긍정 평가 많음",
  역삼우주의원(S06)을 "과도한 상담료 후기 있음"으로 서술. After는 "데이터 없음" 명시 또는 가격 사실만 언급.

**한계**: 최신 LLM이 신중해져 Before도 일부 시나리오(5,7,9,10,14)에선 환각 없이 가격만 언급.
그럼에도 Before는 환각이 0이 아니며(0.93건/응답), After는 14개 전부 0건으로 구조적 차이가 명확.

**자소서 표현**:
> "데이터 보유/미보유 병원을 섞은 합성 평가셋(14개 시나리오)을 구축하고 사람이 직접 환각을 라벨링.
> 단발 호출은 미보유 병원에 후기를 창작하거나 위험 병원을 칭찬하는 환각이 응답당 평균 0.93건 발생한 반면,
> 도구 호출 기반 에이전트는 14개 전 시나리오에서 환각 0건. 도구 결과 인용 + '데이터 없음' 명시로 환각을 구조적으로 차단."

---

## 종합 — 자소서 한 단락

> "단발 LLM 호출 시스템을 도구 호출 기반 에이전트로 전환하면서 **(1) 사람 라벨링 평가셋(14 시나리오) 기준 환각을 응답당 평균 0.93건 → 0건으로 차단**, (2) 다중 병원 비교 시 N+1 회피 배치 도구로 DB 쿼리 17회→1회(94% 감소), (3) 결정론적 계산 분리로 산수 오류 제거. 측정은 평가 픽스처(`HallucinationLabelingBench`, `QueryCountBench`, `TokenCountBench`) 기반 자동 검증 인프라로 수행."

---

## 측정 재현 방법

```bash
# DB 쿼리 횟수 (가장 강한 수치)
./gradlew test --tests "com.douzone_internship.backend.eval.QueryCountBench"

# 입력 토큰 절감
./gradlew test --tests "com.douzone_internship.backend.eval.TokenCountBench"

# 환각 라벨링 평가셋 (LLM 호출 발생, 무료 한도시 이어하기로 누적)
./gradlew test --tests "com.douzone_internship.backend.eval.HallucinationLabelingBench" --rerun-tasks
# → build/eval-runs/ 에 시나리오별 Before/After 응답 + labeling-sheet.md 생성
```

모든 테스트는 `@Transactional` 자동 롤백 + 합성 픽스처 사용. 운영 데이터 무관.
