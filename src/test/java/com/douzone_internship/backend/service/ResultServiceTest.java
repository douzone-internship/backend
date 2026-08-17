package com.douzone_internship.backend.service;

import com.douzone_internship.backend.domain.Hospital;
import com.douzone_internship.backend.dto.request.ResultRequest;
import com.douzone_internship.backend.dto.response.ResultItemDTO;
import com.douzone_internship.backend.dto.response.ResultListResponseDTO;
import com.douzone_internship.backend.repository.HospitalRepository;
import com.douzone_internship.backend.service.ai.cache.AiResponseCache;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * "모든 검색이 조용히 결과 0건"이 되는 실패 모드를 막기 위한 회귀 테스트.
 *
 * 다음 세 가지가 각각 깨지면 리포트가 에러 없이 빈 결과만 반환하게 된다.
 *  1) 요청에 `_type=json`이 빠지면 data.go.kr이 XML을 반환한다.
 *  2) isEmptyResponse()가 파싱 실패를 "결과 없음"으로 처리하면 그 사실이 감춰진다.
 *  3) 파싱 경로가 응답의 `response` 래퍼와 어긋나면 항목이 하나도 잡히지 않는다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ResultService 단위 테스트")
class ResultServiceTest {

    @InjectMocks
    private ResultService resultService;

    @Mock
    private RestTemplate restTemplate;
    @Mock
    private HospitalRepository hospitalRepository;
    @Mock
    private ResultSaveService resultSaveService;
    @Mock
    private AiService aiService;
    @Mock
    private AiResponseCache aiResponseCache;

    private static final String PAYMENT_URL =
            "http://apis.data.go.kr/B551182/nonPaymentDamtInfoService/getNonPaymentItemHospList2";

    /** 실제 API 응답 구조 그대로 — `response` 래퍼가 있다. */
    private static final String VALID_JSON = """
            {"response":{"header":{"resultCode":"00","resultMsg":"NORMAL SERVICE."},
             "body":{"items":{"item":[
               {"yadmNm":"강남세브란스병원","npayKorNm":"라식","minPrc":2380000,"maxPrc":2380000,
                "sgguCd":"110001","sidoCd":"110000"},
               {"yadmNm":"서울성모병원","npayKorNm":"라식","minPrc":2286000,"maxPrc":2743200,
                "sgguCd":"110002","sidoCd":"110000"}
             ]},"numOfRows":1000,"pageNo":1,"totalCount":2}}}
            """;

    /** `_type=json`을 빼먹었을 때 실제로 돌아오던 응답. */
    private static final String XML_RESPONSE =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?><response><header>"
                    + "<resultCode>00</resultCode></header><body><items><item>"
                    + "<yadmNm>강남세브란스병원</yadmNm></item></items></body></response>";

    private final ResultRequest request = new ResultRequest("2Z9610001", null, "110000", null);

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(resultService, "clinicPaymentUrl", PAYMENT_URL);
        ReflectionTestUtils.setField(resultService, "apiKey", "test-key");
        ReflectionTestUtils.setField(resultService, "objectMapper", new ObjectMapper());
    }

    private void stubCacheMiss() {
        given(aiResponseCache.buildKey(any())).willReturn("cache-key");
        given(aiResponseCache.load("cache-key")).willReturn(Optional.empty());
    }

    @Nested
    @DisplayName("외부 API 요청 생성")
    class RequestBuilding {

        @Test
        @DisplayName("_type=json 파라미터를 반드시 포함한다 - 빠지면 XML이 와서 전체 검색이 0건이 된다")
        void includesTypeJsonParam() {
            stubCacheMiss();
            given(restTemplate.getForObject(any(URI.class), eq(String.class))).willReturn(VALID_JSON);
            given(aiService.callAiApi(any(), any())).willReturn("AI 코멘트");
            given(aiService.isFallback(any())).willReturn(false);
            given(hospitalRepository.findFirstByName(any())).willReturn(Optional.empty());

            resultService.generateResult(request);

            ArgumentCaptor<URI> uriCaptor = ArgumentCaptor.forClass(URI.class);
            verify(restTemplate).getForObject(uriCaptor.capture(), eq(String.class));
            assertThat(uriCaptor.getValue().getQuery()).contains("_type=json");
        }
    }

    @Nested
    @DisplayName("응답 파싱")
    class ResponseParsing {

        @Test
        @DisplayName("response 래퍼가 있는 정상 JSON을 파싱해 항목을 채운다")
        void parsesItemsFromWrappedResponse() {
            stubCacheMiss();
            given(restTemplate.getForObject(any(URI.class), eq(String.class))).willReturn(VALID_JSON);
            given(aiService.callAiApi(any(), any())).willReturn("AI 코멘트");
            given(aiService.isFallback(any())).willReturn(false);

            Hospital hospital = new Hospital();
            hospital.setHospitalAddress("서울특별시 강남구 언주로 211");
            given(hospitalRepository.findFirstByName("강남세브란스병원")).willReturn(Optional.of(hospital));
            given(hospitalRepository.findFirstByName("서울성모병원")).willReturn(Optional.empty());

            ResultListResponseDTO response = resultService.generateResult(request);

            assertThat(response.resultCount()).isEqualTo(2);
            assertThat(response.aiComment()).isEqualTo("AI 코멘트");
            assertThat(response.list())
                    .extracting(ResultItemDTO::hospitalName)
                    .containsExactly("강남세브란스병원", "서울성모병원");
            assertThat(response.list().get(0).minPrice()).isEqualTo(2380000);
            assertThat(response.list().get(0).location()).isEqualTo("서울특별시 강남구 언주로 211");
            // 병원 매칭이 안 되면 주소는 폴백 문구
            assertThat(response.list().get(1).location()).isEqualTo("주소 정보 없음");
        }

        @Test
        @DisplayName("XML 응답이 오면 결과 0건으로 처리하고 AI를 호출하지 않는다")
        void treatsXmlAsEmptyWithoutCallingAi() {
            stubCacheMiss();
            given(restTemplate.getForObject(any(URI.class), eq(String.class))).willReturn(XML_RESPONSE);

            ResultListResponseDTO response = resultService.generateResult(request);

            assertThat(response.resultCount()).isZero();
            assertThat(response.aiComment()).isEqualTo("no result");
            verify(aiService, never()).callAiApi(any(), any());
            verify(resultSaveService, never()).saveResultAsync(any(), any(), any());
        }

        @Test
        @DisplayName("totalCount가 0이면 결과 0건으로 처리한다")
        void treatsZeroTotalCountAsEmpty() {
            stubCacheMiss();
            given(restTemplate.getForObject(any(URI.class), eq(String.class)))
                    .willReturn("{\"response\":{\"body\":{\"items\":\"\",\"totalCount\":0}}}");

            ResultListResponseDTO response = resultService.generateResult(request);

            assertThat(response.resultCount()).isZero();
            assertThat(response.aiComment()).isEqualTo("no result");
        }
    }

    @Nested
    @DisplayName("캐시")
    class Caching {

        @Test
        @DisplayName("캐시 hit이면 외부 API와 AI를 전혀 호출하지 않는다")
        void cacheHitSkipsExternalCalls() {
            ResultListResponseDTO cached = ResultListResponseDTO.builder()
                    .resultCount(1)
                    .list(java.util.List.of(new ResultItemDTO("병원", "주소", "라식", 100, 200)))
                    .aiComment("캐시된 코멘트")
                    .build();
            given(aiResponseCache.buildKey(any())).willReturn("cache-key");
            given(aiResponseCache.load("cache-key")).willReturn(Optional.of(cached));

            ResultListResponseDTO response = resultService.generateResult(request);

            assertThat(response.aiComment()).isEqualTo("캐시된 코멘트");
            verify(restTemplate, never()).getForObject(any(URI.class), eq(String.class));
            verify(aiService, never()).callAiApi(any(), any());
        }

        @Test
        @DisplayName("AI 폴백 응답은 캐시에 저장하지 않는다")
        void doesNotCacheFallbackComment() {
            stubCacheMiss();
            given(restTemplate.getForObject(any(URI.class), eq(String.class))).willReturn(VALID_JSON);
            given(hospitalRepository.findFirstByName(any())).willReturn(Optional.empty());
            given(aiService.callAiApi(any(), any())).willReturn("AI 분석을 일시적으로 제공할 수 없습니다.");
            given(aiService.isFallback("AI 분석을 일시적으로 제공할 수 없습니다.")).willReturn(true);

            resultService.generateResult(request);

            verify(resultSaveService, never()).saveResultAsync(any(), any(), any());
        }

        @Test
        @DisplayName("정상 AI 응답은 캐시에 저장한다")
        void cachesSuccessfulComment() {
            stubCacheMiss();
            given(restTemplate.getForObject(any(URI.class), eq(String.class))).willReturn(VALID_JSON);
            given(hospitalRepository.findFirstByName(any())).willReturn(Optional.empty());
            given(aiService.callAiApi(any(), any())).willReturn("정상 코멘트");
            given(aiService.isFallback("정상 코멘트")).willReturn(false);

            resultService.generateResult(request);

            verify(resultSaveService).saveResultAsync(eq(request), any(), eq("정상 코멘트"));
        }
    }
}
