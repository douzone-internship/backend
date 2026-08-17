package com.douzone_internship.backend.service;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
@Slf4j
public class JsonBatchService {

    private final RestTemplate restTemplate;

    @Value("${env.open-data-key}")
    private String apiKey;

    @Value("${env.clinic-code-url}")
    private String clinicCodeUrl;

    @Value("${env.hospital-list-url}")
    private String hospitalListUrl;

    private static final String MAX_CLINIC_CODE_COUNT = "1000";

    private static final String MAX_HOSPITAL_COUNT = "8000";
    private static final int MAX_HOSPITAL_ROW = 10;

    @Value("${hira.batch.data-dir:data/}")
    private String jsonFilePath = "data/";

    @Value("${hira.batch.max-download-attempts:4}")
    private int maxDownloadAttempts = 4;

    // 게이트웨이 이상은 분 단위로 지속될 수 있어 초 단위 백오프로는 부족하다.
    // 10s → 20s → 30s (최대 약 1분 대기)로 짧은 장애 구간을 넘긴다.
    @Value("${hira.batch.retry-delay-ms:10000}")
    private long downloadRetryDelayMs = 10_000L;
    
    // 모든 배치 작업 실행
    // 다운로드 실패를 여기서 삼키면 JsonScheduler가 "배치 성공"으로 기록하고
    // BatchFreshnessHealthIndicator까지 UP으로 보고한다(관측 지표가 거짓말을 함).
    // 반드시 호출자에게 전파해서 실패로 집계되게 한다.
    public void batchAndSave() throws IOException {
        getClinicCodeJson();
        getHospitalJson();
    }

    private void getClinicCodeJson() throws IOException {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("ServiceKey", apiKey);
        params.add("pageNo", "1");
        params.add("numOfRows", MAX_CLINIC_CODE_COUNT);
        params.add("_type", "json");

        downloadJson(clinicCodeUrl, params, "clinic.json");
    }

    private void getHospitalJson() throws IOException {
        for(int i = 1; i <= MAX_HOSPITAL_ROW; i++) {
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("ServiceKey", apiKey);
            params.add("pageNo", String.valueOf(i));
            params.add("numOfRows", MAX_HOSPITAL_COUNT);
            params.add("_type", "json");

            downloadJson(hospitalListUrl, params, String.format("hospital%d.json", i));
        }
    }
    
    // API 요청후 Json 파일 생성
    private Path downloadJson(String apiUrl, MultiValueMap<String, String> params, String fileName) throws IOException {
        URI uri = UriComponentsBuilder.fromHttpUrl(apiUrl)
                .queryParams(params)
                .build(true)
                .toUri();

        byte[] body = fetchWithRetry(uri, fileName);

        Path dir = Paths.get(jsonFilePath);
        Files.createDirectories(dir);
        Path target = dir.resolve(fileName);
        Files.write(target, body, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        log.info("Json 저장 완료: {}", target.toAbsolutePath());
        return target;
    }

    /**
     * data.go.kr 게이트웨이는 동일한 요청에도 간헐적으로 폐기된 레거시 경로로 302
     * 리다이렉트를 보내며, 그쪽은 NO_OPENAPI_SERVICE_ERROR(코드 12)를 반환한다.
     * 요청 간격이나 커넥션 재사용 여부와 무관하게 발생하고 각 시도가 독립적으로
     * 성공/실패하므로, 백오프를 둔 재시도로 상당 부분 흡수할 수 있다.
     */
    private byte[] fetchWithRetry(URI uri, String fileName) throws IOException {
        IOException lastFailure = null;

        for (int attempt = 1; attempt <= maxDownloadAttempts; attempt++) {
            try {
                ResponseEntity<byte[]> resp = restTemplate.getForEntity(uri, byte[].class);
                if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
                    throw new IOException("Json 다운로드 실패: status=" + resp.getStatusCode());
                }
                if (!looksLikeJson(resp.getBody())) {
                    // XML 에러 문서(NO_OPENAPI_SERVICE_ERROR 등)를 .json으로 저장하면
                    // 이후 파싱이 0건을 반환하며 조용히 실패한다. 여기서 걸러낸다.
                    throw new IOException("Json이 아닌 응답 수신 (앞부분: " + preview(resp.getBody()) + ")");
                }
                if (attempt > 1) {
                    log.info("{} 다운로드 성공 (시도 {}회차)", fileName, attempt);
                }
                return resp.getBody();
            } catch (IOException | org.springframework.web.client.RestClientException e) {
                lastFailure = (e instanceof IOException io) ? io : new IOException(e.getMessage(), e);
                log.warn("{} 다운로드 실패 (시도 {}/{}): {}",
                        fileName, attempt, maxDownloadAttempts, e.getMessage());
                if (attempt < maxDownloadAttempts) {
                    sleepQuietly(downloadRetryDelayMs * attempt);
                }
            }
        }
        throw new IOException(fileName + " 다운로드가 " + maxDownloadAttempts + "회 모두 실패", lastFailure);
    }

    private boolean looksLikeJson(byte[] body) {
        for (byte b : body) {
            if (b == ' ' || b == '\n' || b == '\r' || b == '\t') {
                continue;
            }
            return b == '{' || b == '[';
        }
        return false;
    }

    private String preview(byte[] body) {
        return new String(body, 0, Math.min(120, body.length), java.nio.charset.StandardCharsets.UTF_8)
                .replaceAll("\\s+", " ");
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}
