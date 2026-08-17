package com.douzone_internship.backend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * 배치가 아무것도 저장하지 못했는데도 성공처럼 보이는 실패 모드를 막기 위한 회귀 테스트.
 *
 * 다음 세 가지가 각각 깨지면 배치는 조용히 0건을 만든다.
 *  1) 요청에 `_type=json`이 빠지면 data.go.kr이 XML을 반환한다.
 *  2) JSON이 아닌 응답을 `.json`으로 저장하면 이후 파싱이 0건을 반환하며 실패가 묻힌다.
 *  3) batchAndSave()가 예외를 삼키면 스케줄러가 실패를 성공으로 집계한다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JsonBatchService 단위 테스트")
class JsonBatchServiceTest {

    @InjectMocks
    private JsonBatchService jsonBatchService;

    @Mock
    private RestTemplate restTemplate;

    @TempDir
    Path tempDir;

    private static final String CLINIC_URL =
            "https://apis.data.go.kr/B551182/nonPaymentDamtInfoService/getNonPaymentItemCodeList2";
    private static final String HOSPITAL_URL =
            "https://apis.data.go.kr/B551182/hospInfoServicev2/getHospBasisList";

    private static final byte[] VALID_JSON =
            "{\"response\":{\"body\":{\"items\":{\"item\":[]}}}}".getBytes(StandardCharsets.UTF_8);

    /** `_type=json` 누락 시 오는 XML, 그리고 게이트웨이 이상 시 오는 XML 에러 문서. */
    private static final byte[] XML_ERROR = ("<?xml version=\"1.0\"?><OpenAPI_ServiceResponse>"
            + "<cmmMsgHeader><errMsg>NO_OPENAPI_SERVICE_ERROR</errMsg>"
            + "<returnReasonCode>12</returnReasonCode></cmmMsgHeader></OpenAPI_ServiceResponse>")
            .getBytes(StandardCharsets.UTF_8);

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(jsonBatchService, "apiKey", "test-key");
        ReflectionTestUtils.setField(jsonBatchService, "clinicCodeUrl", CLINIC_URL);
        ReflectionTestUtils.setField(jsonBatchService, "hospitalListUrl", HOSPITAL_URL);
        ReflectionTestUtils.setField(jsonBatchService, "jsonFilePath", tempDir.toString());
        // 테스트에서는 백오프를 없앤다 (운영 기본값은 10s → 20s → 30s)
        ReflectionTestUtils.setField(jsonBatchService, "downloadRetryDelayMs", 0L);
        ReflectionTestUtils.setField(jsonBatchService, "maxDownloadAttempts", 4);
    }

    private ResponseEntity<byte[]> ok(byte[] body) {
        return new ResponseEntity<>(body, HttpStatus.OK);
    }

    @Nested
    @DisplayName("요청 생성")
    class RequestBuilding {

        @Test
        @DisplayName("모든 다운로드 요청에 _type=json을 포함한다")
        void allRequestsIncludeTypeJson() throws IOException {
            given(restTemplate.getForEntity(any(URI.class), eq(byte[].class))).willReturn(ok(VALID_JSON));

            jsonBatchService.batchAndSave();

            ArgumentCaptor<URI> uriCaptor = ArgumentCaptor.forClass(URI.class);
            verify(restTemplate, times(11)).getForEntity(uriCaptor.capture(), eq(byte[].class));

            List<URI> uris = uriCaptor.getAllValues();
            assertThat(uris).allSatisfy(uri ->
                    assertThat(uri.getQuery()).contains("_type=json"));
            // 진료코드 1회 + 병원 목록 10페이지
            assertThat(uris.get(0).toString()).startsWith(CLINIC_URL);
            assertThat(uris.subList(1, 11)).allSatisfy(uri ->
                    assertThat(uri.toString()).startsWith(HOSPITAL_URL));
        }
    }

    @Nested
    @DisplayName("JSON이 아닌 응답 차단")
    class NonJsonRejection {

        @Test
        @DisplayName("XML 에러 문서는 .json으로 저장하지 않고 실패시킨다")
        void doesNotPersistXmlResponse() {
            given(restTemplate.getForEntity(any(URI.class), eq(byte[].class))).willReturn(ok(XML_ERROR));

            assertThatThrownBy(() -> jsonBatchService.batchAndSave())
                    .isInstanceOf(IOException.class)
                    .hasMessageContaining("clinic.json")
                    .hasMessageContaining("4회 모두 실패");

            assertThat(tempDir.resolve("clinic.json")).doesNotExist();
        }

        @Test
        @DisplayName("빈 응답 본문도 저장하지 않는다")
        void doesNotPersistEmptyBody() {
            given(restTemplate.getForEntity(any(URI.class), eq(byte[].class)))
                    .willReturn(ok(new byte[0]));

            assertThatThrownBy(() -> jsonBatchService.batchAndSave())
                    .isInstanceOf(IOException.class);

            assertThat(tempDir.resolve("clinic.json")).doesNotExist();
        }
    }

    @Nested
    @DisplayName("재시도")
    class Retry {

        @Test
        @DisplayName("게이트웨이가 간헐 실패해도 재시도해서 성공하면 저장한다")
        void retriesUntilSuccess() throws IOException {
            // 1,2번째는 죽은 레거시로 302된 XML, 3번째부터 정상
            given(restTemplate.getForEntity(any(URI.class), eq(byte[].class)))
                    .willReturn(ok(XML_ERROR), ok(XML_ERROR), ok(VALID_JSON));

            jsonBatchService.batchAndSave();

            assertThat(tempDir.resolve("clinic.json")).exists();
            assertThat(Files.readAllBytes(tempDir.resolve("clinic.json"))).isEqualTo(VALID_JSON);
        }

        @Test
        @DisplayName("설정된 횟수만큼만 시도하고 포기한다")
        void stopsAfterMaxAttempts() {
            ReflectionTestUtils.setField(jsonBatchService, "maxDownloadAttempts", 2);
            given(restTemplate.getForEntity(any(URI.class), eq(byte[].class))).willReturn(ok(XML_ERROR));

            assertThatThrownBy(() -> jsonBatchService.batchAndSave())
                    .isInstanceOf(IOException.class);

            verify(restTemplate, times(2)).getForEntity(any(URI.class), eq(byte[].class));
        }

        @Test
        @DisplayName("2xx가 아닌 응답도 재시도 대상이다")
        void retriesOnNon2xx() {
            ReflectionTestUtils.setField(jsonBatchService, "maxDownloadAttempts", 3);
            given(restTemplate.getForEntity(any(URI.class), eq(byte[].class)))
                    .willReturn(new ResponseEntity<>(HttpStatus.FOUND));

            assertThatThrownBy(() -> jsonBatchService.batchAndSave())
                    .isInstanceOf(IOException.class);

            verify(restTemplate, times(3)).getForEntity(any(URI.class), eq(byte[].class));
        }
    }

    @Nested
    @DisplayName("실패 전파")
    class FailurePropagation {

        @Test
        @DisplayName("다운로드 실패를 삼키지 않고 호출자에게 전파한다 - 삼키면 스케줄러가 '배치 성공'으로 기록한다")
        void propagatesFailureToCaller() {
            given(restTemplate.getForEntity(any(URI.class), eq(byte[].class))).willReturn(ok(XML_ERROR));

            assertThatThrownBy(() -> jsonBatchService.batchAndSave())
                    .isInstanceOf(IOException.class);
        }

        @Test
        @DisplayName("전부 정상이면 파일 11개가 저장된다")
        void savesAllFilesOnSuccess() throws IOException {
            given(restTemplate.getForEntity(any(URI.class), eq(byte[].class))).willReturn(ok(VALID_JSON));

            jsonBatchService.batchAndSave();

            assertThat(tempDir.resolve("clinic.json")).exists();
            for (int i = 1; i <= 10; i++) {
                assertThat(tempDir.resolve("hospital" + i + ".json")).exists();
            }
        }
    }
}
