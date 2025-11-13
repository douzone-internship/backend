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

    private static final String JSON_FILE_PATH = "data/";
    
    // 모든 배치 작업 실행
    public void batchAndSave() {
        try {
            getClinicCodeJson();
            getHospitalJson();
        } catch (IOException e) {
            log.error("Json 다운로드 실패", e);
        }
    }

    private void getClinicCodeJson() throws IOException {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("ServiceKey", apiKey);
        params.add("pageNo", "1");
        params.add("numOfRows", MAX_CLINIC_CODE_COUNT);

        downloadJson(clinicCodeUrl, params, "clinic.json");
    }

    private void getHospitalJson() throws IOException {
        for(int i = 1; i <= MAX_HOSPITAL_ROW; i++) {
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("ServiceKey", apiKey);
            params.add("pageNo", String.valueOf(i));
            params.add("numOfRows", MAX_HOSPITAL_COUNT);

            downloadJson(hospitalListUrl, params, String.format("hospital%d.json", i));
        }
    }
    
    // API 요청후 Json 파일 생성
    private Path downloadJson(String apiUrl, MultiValueMap<String, String> params, String fileName) throws IOException {
        URI uri = UriComponentsBuilder.fromHttpUrl(apiUrl)
                .queryParams(params)
                .build(true)
                .toUri();

        ResponseEntity<byte[]> resp = restTemplate.getForEntity(uri, byte[].class);
        if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
            throw new IOException("Json 다운로드 실패: status=" + resp.getStatusCode());
        }

        Path dir = Paths.get(JSON_FILE_PATH);
        Files.createDirectories(dir);
        Path target = dir.resolve(fileName);
        Files.write(target, resp.getBody(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        log.info("Json 저장 완료: {}", target.toAbsolutePath());
        return target;
    }
}
