package com.douzone_internship.backend.eval;

import com.douzone_internship.backend.domain.Comment;
import com.douzone_internship.backend.domain.Provider;
import com.douzone_internship.backend.domain.Users;
import com.douzone_internship.backend.dto.response.ResultItemDTO;
import com.douzone_internship.backend.repository.CommentRepository;
import com.douzone_internship.backend.repository.UsersRepository;
import com.douzone_internship.backend.service.ai.OutputValidator;
import com.douzone_internship.backend.service.ai.PriceStats;
import com.douzone_internship.backend.service.ai.PriceStatsCalculator;
import com.douzone_internship.backend.service.ai.PromptBuilder;
import com.douzone_internship.backend.service.ai.agent.AgentLoop;
import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 사람 라벨링용 환각 평가 벤치.
 * <p>
 * 20개 시나리오 × Before(단발 호출)/After(에이전트) = 40회 LLM 호출.
 * 응답을 build/eval-runs/ 에 저장하고, 사람이 환각을 직접 카운트하도록
 * labeling-sheet.md 를 생성한다. 자동 환각 판정은 하지 않는다.
 * <p>
 * 환각 정의(넓은 정의): 데이터 미보유 병원에 대한
 *   (1) 후기/평점 수치 인용, (2) "전문성·만족도·안정적·인기" 등 평가성 표현.
 */
@SpringBootTest(properties = {
        "agent.model=gemini-2.5-flash-lite",
        "agent.call-delay-ms=5000"
})
@ActiveProfiles("local")
@Tag("eval")
@Transactional
class HallucinationLabelingBench {

    // flash 일일 한도 소진 후 별도 한도가 있는 flash-lite로 시나리오 추가 수집.
    // 시나리오 1~4는 flash, 5~ 는 flash-lite (각 시나리오 내 Before/After 모델 동일).
    private static final String MODEL = "gemini-2.5-flash-lite";
    private static final String CLINIC_CODE = "BO001";
    private static final Path OUT_DIR = Path.of("build", "eval-runs");

    private static final long CALL_DELAY_MS = 5_000;     // before↔after, 시나리오 사이 지연
    private static final long OVERLOAD_BACKOFF_MS = 20_000; // 503 과부하 시 대기
    private static final long DEFAULT_429_WAIT_MS = 60_000; // retryDelay 파싱 실패 시 기본 대기
    private static final long MAX_WAIT_MS = 300_000;     // 이보다 긴 대기 요구면 포기 (일일 한도로 간주)
    private static final int MAX_RETRY = 6;              // 분당/과부하 재시도 횟수

    private static final java.util.regex.Pattern RETRY_DELAY =
            java.util.regex.Pattern.compile("retry in ([0-9.]+)s");

    private static final String OLD_SYSTEM_PROMPT =
            "너는 비급여 진료의 가격을 분석하고 병원을 추천해주는 에이전트야. " +
            "유저 프롬프트의 정보를 살펴보고 최저가, 최고가, 평균가, 추천 병원 및 평가, " +
            "전반적인 시술에 관한 평가 등을 제공해. " +
            "가격도 중요하지만 현재 시점까지 알려진 병원의 후기나 평점을 참고해서 대답해. " +
            "약 3~5개 병원을 추천하되 큰 문제가 있던 병원은 반드시 언급해. 8줄 이하로 작성해.";

    // 병원 프로필: 이름 -> {후기 수, 기본 평점, WARNING 여부}
    private record Profile(String name, int reviewCount, int baseScore, boolean warning) {}

    private static final List<Profile> PROFILES = List.of(
            // 후기 많음 + 평점 높음 (SAFE)
            new Profile("강남필의원", 10, 5, false),
            new Profile("청담S의원", 8, 5, false),
            new Profile("압구정라인의원", 9, 4, false),
            // 후기 적음 (SAFE/CAUTION)
            new Profile("역삼봄의원", 3, 4, false),
            new Profile("선릉뷰의원", 2, 3, false),
            new Profile("삼성웰의원", 4, 4, false),
            // WARNING (재수술·부작용 다수)
            new Profile("강남미인의원", 6, 2, true),
            new Profile("논현케어의원", 5, 2, true),
            // 후기 없음 (UNKNOWN)
            new Profile("청담리프트의원", 0, 0, false),
            new Profile("논현스타의원", 0, 0, false),
            new Profile("역삼우주의원", 0, 0, false),
            new Profile("대치365의원", 0, 0, false)
    );

    private static final Map<String, Integer> PRICE_MIN = new LinkedHashMap<>();
    static {
        int base = 28000;
        for (Profile p : PROFILES) {
            PRICE_MIN.put(p.name(), base);
            base += 2000;
        }
    }

    // 20개 시나리오: 각 시나리오는 5병원 묶음 (인덱스로 표현)
    private static final int[][] SCENARIOS = {
            {0, 3, 8, 6, 9},   {1, 4, 9, 7, 10},  {2, 5, 10, 6, 11}, {0, 1, 8, 7, 9},
            {3, 4, 5, 9, 10},  {0, 6, 8, 10, 11}, {1, 7, 9, 11, 3},  {2, 6, 8, 9, 4},
            {0, 2, 4, 6, 8},   {1, 3, 5, 7, 9},   {8, 9, 10, 11, 0}, {6, 7, 0, 1, 8},
            {3, 8, 9, 10, 11}, {4, 5, 6, 7, 0},   {2, 8, 10, 6, 1},  {0, 3, 6, 9, 11},
            {1, 4, 7, 10, 2},  {5, 8, 11, 6, 0},  {2, 3, 9, 7, 10},  {0, 5, 8, 6, 11}
    };

    @Autowired private Client geminiClient;
    @Autowired private AgentLoop agentLoop;
    @Autowired private PriceStatsCalculator priceStatsCalculator;
    @Autowired private PromptBuilder promptBuilder;
    @Autowired private OutputValidator outputValidator;
    @Autowired private UsersRepository usersRepository;
    @Autowired private CommentRepository commentRepository;

    @Test
    void runScenariosForLabeling() throws IOException {
        seedFixture();
        Files.createDirectories(OUT_DIR);

        // 채점표(labeling-sheet)는 LLM 호출과 무관하게 항상 전체 20행으로 생성
        writeLabelingSheet();

        int completed = 0;
        int skipped = 0;
        boolean stoppedEarly = false;

        for (int s = 0; s < SCENARIOS.length; s++) {
            String num = String.format("%02d", s + 1);
            Path afterFile = OUT_DIR.resolve("scenario-" + num + "-after.txt");

            // 이어하기: 이미 생성된 시나리오는 LLM 호출 없이 건너뜀 (일일 한도 절약)
            if (Files.exists(afterFile)) {
                skipped++;
                continue;
            }

            List<Profile> picked = new ArrayList<>();
            for (int idx : SCENARIOS[s]) picked.add(PROFILES.get(idx));

            List<ResultItemDTO> items = toItems(picked);
            PriceStats stats = priceStatsCalculator.compute(items);
            String userPrompt = promptBuilder.build("{{resultItems}}", items, stats, CLINIC_CODE);
            List<String> noData = picked.stream()
                    .filter(p -> p.reviewCount() == 0).map(Profile::name).toList();

            try {
                String before = callWithAdaptiveRetry(() -> outputValidator.validate(callBefore(userPrompt)));
                sleep(CALL_DELAY_MS);
                String after = callWithAdaptiveRetry(() -> outputValidator.validate(agentLoop.run(userPrompt)));
                sleep(CALL_DELAY_MS);

                writeRun(num, "before", picked, noData, before);
                writeRun(num, "after", picked, noData, after);
                completed++;
                System.out.printf("시나리오 %s 완료 (미보유: %s)%n", num, noData);
            } catch (RuntimeException e) {
                // 재시도 한도 초과 또는 과도한 대기 요구(일일 한도로 간주) 시 중단. 여기까지는 저장됨.
                System.out.printf("시나리오 %s 중단 — 사유: %s%n", num, e.getMessage());
                System.out.println("여기까지 저장하고 종료합니다. 회복 후 다시 실행하면 이어서 진행됩니다.");
                stoppedEarly = true;
                break;
            }
        }

        long totalDone = skipped + completed;
        System.out.println();
        System.out.println("===== 라벨링 진행 현황 =====");
        System.out.printf("전체 %d개 중 누적 완료 %d개 (이번 실행 신규 %d, 기존 %d)%n",
                SCENARIOS.length, totalDone, completed, skipped);
        if (stoppedEarly || totalDone < SCENARIOS.length) {
            System.out.println("미완료분이 있습니다. 한도 회복 후 같은 테스트를 다시 실행하세요 (이어서 진행).");
        } else {
            System.out.println("전체 시나리오 완료. labeling-sheet.md 를 채워 넣으세요.");
        }
        System.out.println("응답 파일: " + OUT_DIR.toAbsolutePath());
    }

    private void writeLabelingSheet() throws IOException {
        StringBuilder sheet = new StringBuilder();
        sheet.append("# 환각 라벨링 시트\n\n");
        sheet.append("기준일: ").append(LocalDateTime.now()).append("\n");
        sheet.append("환각 정의(넓은 정의): 데이터 미보유 병원에 대한 후기/평점 인용 또는 ");
        sheet.append("평가성 표현(전문성·만족도·안정적·인기 등).\n\n");
        sheet.append("각 시나리오의 응답 파일을 읽고 `환각 항목 수`를 채워 넣으세요.\n\n");
        sheet.append("| 시나리오 | 데이터 미보유 병원 | Before 환각수 | After 환각수 | 비고 |\n");
        sheet.append("|---|---|---|---|---|\n");

        for (int s = 0; s < SCENARIOS.length; s++) {
            List<String> noData = new ArrayList<>();
            for (int idx : SCENARIOS[s]) {
                Profile p = PROFILES.get(idx);
                if (p.reviewCount() == 0) noData.add(p.name());
            }
            sheet.append("| ").append(String.format("%02d", s + 1))
                    .append(" | ").append(String.join(", ", noData))
                    .append(" |  |  |  |\n");
        }
        Files.writeString(OUT_DIR.resolve("labeling-sheet.md"), sheet.toString());
    }

    private void writeRun(String num, String mode, List<Profile> picked,
                          List<String> noData, String response) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("시나리오 ").append(num).append(" / ").append(mode.toUpperCase()).append("\n");
        sb.append("=".repeat(50)).append("\n");
        sb.append("병원 목록: ").append(picked.stream().map(Profile::name).toList()).append("\n");
        sb.append("데이터 미보유 병원(이 병원들에 대한 후기/평점/평가 = 환각): ")
                .append(noData).append("\n");
        sb.append("-".repeat(50)).append("\n");
        sb.append(response).append("\n");
        Files.writeString(OUT_DIR.resolve("scenario-" + num + "-" + mode + ".txt"), sb.toString());
    }

    /**
     * 429(분당 한도)는 서버가 알려준 retryDelay만큼 대기 후 재시도.
     * 503(과부하)은 고정 시간 대기 후 재시도.
     * 대기 요구가 MAX_WAIT_MS를 넘으면(일일 한도로 간주) 포기.
     */
    private String callWithAdaptiveRetry(java.util.function.Supplier<String> action) {
        int attempt = 0;
        while (true) {
            try {
                return action.get();
            } catch (RuntimeException e) {
                String msg = String.valueOf(e.getMessage());
                boolean rateLimited = msg.contains("429");
                boolean overloaded = msg.contains("503") || msg.contains("UNAVAILABLE")
                        || msg.contains("high demand");
                if (!rateLimited && !overloaded) {
                    throw e;
                }
                if (++attempt > MAX_RETRY) {
                    throw e;
                }

                long waitMs;
                if (rateLimited) {
                    waitMs = parseRetryDelayMs(msg).orElse(DEFAULT_429_WAIT_MS);
                    if (waitMs > MAX_WAIT_MS) {
                        throw new RuntimeException("대기 요구 " + (waitMs / 1000)
                                + "초 > 한계. 일일 한도로 판단하여 중단.", e);
                    }
                    System.out.printf("  429 분당 한도, 서버 지시 %d초 대기 후 재시도 (%d/%d)%n",
                            waitMs / 1000, attempt, MAX_RETRY);
                } else {
                    waitMs = OVERLOAD_BACKOFF_MS;
                    System.out.printf("  503 과부하, %d초 대기 후 재시도 (%d/%d)%n",
                            waitMs / 1000, attempt, MAX_RETRY);
                }
                sleep(waitMs);
            }
        }
    }

    private java.util.Optional<Long> parseRetryDelayMs(String message) {
        java.util.regex.Matcher m = RETRY_DELAY.matcher(message);
        if (m.find()) {
            double seconds = Double.parseDouble(m.group(1));
            return java.util.Optional.of((long) Math.ceil(seconds + 5) * 1000); // 여유 5초
        }
        return java.util.Optional.empty();
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    private String callBefore(String userPrompt) {
        GenerateContentConfig config = GenerateContentConfig.builder()
                .systemInstruction(Content.fromParts(Part.fromText(OLD_SYSTEM_PROMPT)))
                .build();
        GenerateContentResponse response = geminiClient.models.generateContent(MODEL, userPrompt, config);
        return response.text() == null ? "" : response.text();
    }

    private List<ResultItemDTO> toItems(List<Profile> profiles) {
        List<ResultItemDTO> items = new ArrayList<>();
        for (Profile p : profiles) {
            int min = PRICE_MIN.get(p.name());
            items.add(new ResultItemDTO(p.name(), "서울 강남구", "보톡스주사", min, min + 20000));
        }
        return items;
    }

    private void seedFixture() {
        Users user = usersRepository.save(Users.builder()
                .email("label-eval-" + System.nanoTime() + "@example.com")
                .name("라벨평가")
                .password("dummy")
                .provider(Provider.GOOGLE)
                .providerId("label-eval-" + System.nanoTime())
                .build());

        for (Profile p : PROFILES) {
            for (int i = 0; i < p.reviewCount(); i++) {
                String body;
                int score;
                if (p.warning() && i < 3) {
                    body = "효과가 없어 재수술을 고민중이고 부작용도 있었습니다.";
                    score = 1;
                } else {
                    body = "친절하고 시술 결과 만족스러웠습니다.";
                    score = p.baseScore();
                }
                commentRepository.save(Comment.builder()
                        .user(user)
                        .hospitalName(p.name())
                        .clinicCode(CLINIC_CODE)
                        .comment(body)
                        .score(score)
                        .createdAt(LocalDateTime.now().minusDays(i))
                        .build());
            }
        }
    }
}
