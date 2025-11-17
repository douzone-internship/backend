// ResultService.java
package com.douzone_internship.backend.service;

import com.douzone_internship.backend.domain.Hospital;
import com.douzone_internship.backend.dto.request.ResultRequest;
import com.douzone_internship.backend.dto.response.RawClinicPaymentResponseDTO;
import com.douzone_internship.backend.dto.response.ResultItemDTO;
import com.douzone_internship.backend.dto.response.ResultListResponseDTO;
import com.douzone_internship.backend.repository.HospitalRepository;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ResultService extends AbstractApiService<RawClinicPaymentResponseDTO, Void> {

    @Value("${env.clinic-payment-url}")
    private String clinicPaymentUrl;

    @Value("${env.open-data-key}")
    private String apiKey;

    private final RestTemplate restTemplate;
    private final HospitalRepository hospitalRepository;
    private final AiService aiService;

    public ResponseEntity<ResultListResponseDTO> generateResult(ResultRequest resultRequest) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("ServiceKey", apiKey);
        params.add("pageNo", "1");
        params.add("numOfRows", "1000");
        params.add("sidoCd", resultRequest.sidoCode());
        if(resultRequest.sigguCode() != null) {
            params.add("sgguCd", resultRequest.sigguCode());
        }
        if(resultRequest.hospitalName() != null) {
            params.add("yadmNm", resultRequest.hospitalName());
        }
        params.add("itemCd", resultRequest.clinicCode());

        URI uri = UriComponentsBuilder.fromHttpUrl(clinicPaymentUrl)
                .queryParams(params)
                .build(true)
                .toUri();

        // API 호출
        String jsonResponse = restTemplate.getForObject(uri, String.class);

        // 검색 결과가 없는경우
        if(Objects.requireNonNull(jsonResponse).isEmpty()) {
            ResultListResponseDTO emptyResponse = ResultListResponseDTO.builder()
                    .resultCount(0)
                    .list(List.of())
                    .aiComment("no result")
                    .build();
            return ResponseEntity.ok(emptyResponse);
        }

        // 상속받은 parseApiResponse 메서드로 DTO 리스트 파싱
        List<RawClinicPaymentResponseDTO> rawItems = parseApiResponse(jsonResponse, RawClinicPaymentResponseDTO.class);

        // DTO 리스트를 ResultItemDTO 리스트로 변환
        List<ResultItemDTO> resultItems = rawItems.stream()
                .map(rawItem -> {
                    Optional<Hospital> hospitalOpt;

                    // sigguCode가 있으면 시군구 + 이름으로 조회
                    if (resultRequest.sigguCode() != null) {
                        hospitalOpt = hospitalRepository.findFirstByNameAndSigungu_SgguCd(
                                rawItem.getYadmNm(),
                                resultRequest.sigguCode()
                        );
                    } else {
                        // sigguCode가 없으면 이름으로 조회
                        hospitalOpt = hospitalRepository.findFirstByName(rawItem.getYadmNm());
                    }

                    String location = hospitalOpt
                            .map(Hospital::getHospitalAddress)
                            .orElse("주소 정보 없음");

                    return new ResultItemDTO(
                            rawItem.getYadmNm(),
                            location,
                            rawItem.getNpayKorNm(),
                            Integer.parseInt(rawItem.getMinPrc()),
                            Integer.parseInt(rawItem.getMaxPrc())
                    );
                })
                .collect(Collectors.toList());

        String aiComment = aiService.callAiApi(resultItems);

        ResultListResponseDTO response = ResultListResponseDTO.builder()
                .resultCount(resultItems.size())
                .list(resultItems)
                .aiComment(aiComment)
                .build();

        return ResponseEntity.ok(response);
    }

    @Override
    protected Void convertDtoToEntity(RawClinicPaymentResponseDTO dto) {
        return null;
    }

    @Override
    protected JpaRepository<Void, ?> getRepository() {
        return null;
    }
}
