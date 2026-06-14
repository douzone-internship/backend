package com.douzone_internship.backend.eval;

import com.douzone_internship.backend.domain.Comment;
import com.douzone_internship.backend.domain.Provider;
import com.douzone_internship.backend.domain.Users;
import com.douzone_internship.backend.repository.CommentRepository;
import com.douzone_internship.backend.repository.UsersRepository;
import com.douzone_internship.backend.service.ai.tools.HospitalsOverviewTool;
import com.douzone_internship.backend.service.ai.tools.NegativeFlagTool;
import com.douzone_internship.backend.service.ai.tools.RatingTool;
import com.douzone_internship.backend.service.ai.tools.ReviewTool;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
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
 * DB 쿼리 횟수 측정 — 개별 도구 × N병원 vs 배치 도구 1회.
 * N+1 회피 효과 정량화. LLM 호출 무관, 결정론적.
 */
@SpringBootTest
@ActiveProfiles("local")
@Tag("eval")
@Transactional
class QueryCountBench {

    private static final String CLINIC_CODE = "BO001";

    private static final List<String> HOSPITALS = List.of(
            "강남필의원", "강남미인의원", "청담리프트의원", "논현스타의원", "역삼우주의원"
    );

    @Autowired private EntityManagerFactory emf;
    @Autowired private ReviewTool reviewTool;
    @Autowired private RatingTool ratingTool;
    @Autowired private NegativeFlagTool negativeFlagTool;
    @Autowired private HospitalsOverviewTool hospitalsOverviewTool;
    @Autowired private UsersRepository usersRepository;
    @Autowired private CommentRepository commentRepository;

    @BeforeEach
    void seedFixture() {
        Users testUser = usersRepository.save(Users.builder()
                .email("query-eval-" + System.nanoTime() + "@example.com")
                .name("쿼리테스트")
                .password("dummy")
                .provider(Provider.GOOGLE)
                .providerId("query-eval-" + System.nanoTime())
                .build());

        for (String hospital : List.of("강남필의원", "강남미인의원")) {
            for (int i = 0; i < 3; i++) {
                commentRepository.save(Comment.builder()
                        .user(testUser)
                        .hospitalName(hospital)
                        .clinicCode(CLINIC_CODE)
                        .comment("샘플 후기 " + i)
                        .score(4)
                        .createdAt(LocalDateTime.now().minusDays(i))
                        .build());
            }
        }
    }

    @Test
    void compareQueryCount() {
        Statistics stats = emf.unwrap(SessionFactory.class).getStatistics();
        stats.setStatisticsEnabled(true);

        // Scenario A: 개별 도구 호출 — 각 병원마다 Review/Rating/NegativeFlag 모두 조회
        stats.clear();
        for (String hospital : HOSPITALS) {
            reviewTool.getReviews(hospital, CLINIC_CODE, null);
            ratingTool.getRating(hospital, CLINIC_CODE);
            negativeFlagTool.getNegativeFlags(hospital, CLINIC_CODE);
        }
        long individualQueries = stats.getPrepareStatementCount();

        // Scenario B: 배치 도구 1회 호출 — IN 절로 모든 병원 동시 조회
        stats.clear();
        hospitalsOverviewTool.getOverview(HOSPITALS, CLINIC_CODE);
        long batchQueries = stats.getPrepareStatementCount();

        double reduction = individualQueries == 0 ? 0
                : (1.0 - (double) batchQueries / individualQueries) * 100;

        System.out.println();
        System.out.println("===== QueryCountBench 결과 =====");
        System.out.println("병원 수: " + HOSPITALS.size());
        System.out.println("Before (개별 도구 × 5병원): SQL " + individualQueries + " 건");
        System.out.println("After  (배치 도구 1회):     SQL " + batchQueries + " 건");
        System.out.printf("절감률: %.0f%% (%d → %d)%n", reduction, individualQueries, batchQueries);
        System.out.println("================================");
    }
}
