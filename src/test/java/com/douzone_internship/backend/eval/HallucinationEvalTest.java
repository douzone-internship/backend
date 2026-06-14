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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 환각률 측정 — Before(단발 호출, 옛 프롬프트, 도구 없음) vs After(AgentLoop, 도구 호출).
 * <p>
 * 픽스처: 5병원 중 2병원만 후기 보유. 나머지 3병원에 대한 후기/평점 언급은 환각.
 * 실행: ./gradlew test --tests HallucinationEvalTest
 */
@SpringBootTest
@ActiveProfiles("local")
@Tag("eval")
@Transactional
class HallucinationEvalTest {

    private static final String MODEL = "gemini-2.5-flash";
    private static final String CLINIC_CODE = "BO001";

    private static final String OLD_SYSTEM_PROMPT =
            "너는 비급여 진료의 가격을 분석하고 병원을 추천해주는 에이전트야. " +
            "유저 프롬프트의 정보를 살펴보고 최저가, 최고가, 평균가, 추천 병원 및 평가, " +
            "전반적인 시술에 관한 평가 등을 제공해. " +
            "가격도 중요하지만 현재 시점까지 알려진 병원의 후기나 평점을 참고해서 대답해. " +
            "약 3~5개 병원을 추천하되 큰 문제가 있던 병원은 반드시 언급해. 8줄 이하로 작성해.";

    private static final List<String> HALLUCINATION_KEYWORDS = List.of(
            // 후기·평점 인용
            "후기", "평점", "별점", "리뷰",
            // 데이터 없이 만들어낸 평가성 표현
            "만족", "전문성", "전문적", "선호도", "인기", "안정적", "프리미엄", "평판",
            // 의료 사고/부작용 (검증 불가능한 주장)
            "재수술", "부작용", "사고"
    );

    private static final List<String> HOSPITALS_WITHOUT_DATA = List.of(
            "청담리프트의원", "논현스타의원", "역삼우주의원"
    );

    @Autowired private Client geminiClient;
    @Autowired private AgentLoop agentLoop;
    @Autowired private PriceStatsCalculator priceStatsCalculator;
    @Autowired private PromptBuilder promptBuilder;
    @Autowired private OutputValidator outputValidator;
    @Autowired private UsersRepository usersRepository;
    @Autowired private CommentRepository commentRepository;

    @BeforeEach
    void seedFixture() {
        Users testUser = usersRepository.save(Users.builder()
                .email("eval-test-" + System.nanoTime() + "@example.com")
                .name("평가테스트사용자")
                .password("dummy")
                .provider(Provider.GOOGLE)
                .providerId("eval-test-" + System.nanoTime())
                .build());

        for (int i = 0; i < 8; i++) {
            commentRepository.save(Comment.builder()
                    .user(testUser)
                    .hospitalName("강남필의원")
                    .clinicCode(CLINIC_CODE)
                    .comment("친절하고 시술도 만족스러웠어요. 효과도 좋고 통증이 적었습니다.")
                    .score(i < 6 ? 5 : 4)
                    .createdAt(LocalDateTime.now().minusDays(i))
                    .build());
        }

        for (int i = 0; i < 5; i++) {
            commentRepository.save(Comment.builder()
                    .user(testUser)
                    .hospitalName("강남미인의원")
                    .clinicCode(CLINIC_CODE)
                    .comment(i == 0
                            ? "효과가 별로여서 재수술을 고민중입니다."
                            : "보통이에요. 가격 대비 무난합니다.")
                    .score(i == 0 ? 2 : 3)
                    .createdAt(LocalDateTime.now().minusDays(i))
                    .build());
        }
    }

    @Test
    void measureHallucinationRate() {
        List<ResultItemDTO> sample = sampleHospitals();
        PriceStats stats = priceStatsCalculator.compute(sample);
        String formattedUserPrompt = promptBuilder.build("{{resultItems}}", sample, stats, CLINIC_CODE);

        String beforeRaw = callBeforeMode(formattedUserPrompt);
        String beforeFinal = outputValidator.validate(beforeRaw);

        String afterRaw = agentLoop.run(formattedUserPrompt);
        String afterFinal = outputValidator.validate(afterRaw);

        int beforeHallucinations = countHallucinations(beforeFinal);
        int afterHallucinations = countHallucinations(afterFinal);
        int total = HOSPITALS_WITHOUT_DATA.size();

        System.out.println();
        System.out.println("===== HallucinationEval 결과 =====");
        System.out.println("데이터 미보유 병원: " + HOSPITALS_WITHOUT_DATA);
        System.out.println();
        System.out.println("----- Before (단발 호출, 옛 프롬프트, 도구 없음) -----");
        System.out.println(beforeFinal);
        System.out.println();
        System.out.printf("Before 환각: %d / %d (%.0f%%)%n", beforeHallucinations, total,
                beforeHallucinations * 100.0 / total);
        System.out.println();
        System.out.println("----- After (AgentLoop, 도구 호출, 신규 프롬프트) -----");
        System.out.println(afterFinal);
        System.out.println();
        System.out.printf("After 환각: %d / %d (%.0f%%)%n", afterHallucinations, total,
                afterHallucinations * 100.0 / total);
        System.out.println("================================");
    }

    private String callBeforeMode(String userPrompt) {
        GenerateContentConfig config = GenerateContentConfig.builder()
                .systemInstruction(Content.fromParts(Part.fromText(OLD_SYSTEM_PROMPT)))
                .build();
        GenerateContentResponse response = geminiClient.models.generateContent(MODEL, userPrompt, config);
        return response.text() == null ? "" : response.text();
    }

    private int countHallucinations(String response) {
        int count = 0;
        for (String hospital : HOSPITALS_WITHOUT_DATA) {
            int idx = response.indexOf(hospital);
            if (idx < 0) continue;
            int start = Math.max(0, idx - 40);
            int end = Math.min(response.length(), idx + hospital.length() + 60);
            String window = response.substring(start, end);
            for (String kw : HALLUCINATION_KEYWORDS) {
                if (window.contains(kw)) {
                    count++;
                    break;
                }
            }
        }
        return count;
    }

    private List<ResultItemDTO> sampleHospitals() {
        return List.of(
                new ResultItemDTO("강남필의원", "서울 강남구", "보톡스주사", 30000, 50000),
                new ResultItemDTO("강남미인의원", "서울 강남구", "보톡스주사", 35000, 55000),
                new ResultItemDTO("청담리프트의원", "서울 강남구 청담동", "보톡스주사", 45000, 70000),
                new ResultItemDTO("논현스타의원", "서울 강남구 논현동", "보톡스주사", 28000, 48000),
                new ResultItemDTO("역삼우주의원", "서울 강남구 역삼동", "보톡스주사", 40000, 60000)
        );
    }
}
