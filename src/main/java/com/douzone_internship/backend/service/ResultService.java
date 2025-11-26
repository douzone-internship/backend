package com.douzone_internship.backend.service;

import com.douzone_internship.backend.domain.*;
import com.douzone_internship.backend.dto.request.ResultRequest;
import com.douzone_internship.backend.dto.response.RawClinicPaymentResponseDTO;
import com.douzone_internship.backend.dto.response.ResultItemDTO;
import com.douzone_internship.backend.dto.response.ResultListResponseDTO;
import com.douzone_internship.backend.repository.AiCommentRepository;
import com.douzone_internship.backend.repository.HospitalRepository;
import java.util.Objects;

import com.douzone_internship.backend.repository.ResultRepository;
import com.douzone_internship.backend.repository.SearchLogRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
    private final ResultSaveService resultSaveService;
    private final SearchLogRepository searchLogRepository;
    private final ResultRepository resultRepository;
    private final AiCommentRepository aiCommentRepository;
    private final AiService aiService;

    @Transactional(readOnly = true)
    public ResponseEntity<ResultListResponseDTO> generateResult(ResultRequest resultRequest) {

        String keyword = extractKeyword(resultRequest);

        // Db에 캐싱 여부 확인
        if(checkSearchLog(resultRequest, keyword)) {
            return getCachedResult(keyword);
        }

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
                .encode()
                .build()
                .toUri();

        // API 호출
        String jsonResponse = restTemplate.getForObject(uri, String.class);

        // 검색 결과가 없는경우
        if(Objects.requireNonNull(jsonResponse).isEmpty() || isEmptyResponse(jsonResponse)) {
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
                            rawItem.getMinPrc(),
                            rawItem.getMaxPrc()
                    );
                })
                .collect(Collectors.toList());

        String aiComment = aiService.callAiApi(resultItems);

        ResultListResponseDTO response = ResultListResponseDTO.builder()
                .resultCount(resultItems.size())
                .list(resultItems)
                .aiComment(aiComment)
                .build();

        // 비동기로 DB에 저장
        resultSaveService.saveResultAsync(resultRequest, resultItems, aiComment);

        return ResponseEntity.ok(response);
    }

    public ResponseEntity<ResultListResponseDTO> getResult(String keyword) {
        return getCachedResult(keyword);
    }

    public String extractKeyword(ResultRequest resultRequest) {
        StringBuilder keyword = new StringBuilder();
        keyword.append(resultRequest.clinicCode());
        keyword.append(resultRequest.sidoCode());
        keyword.append(resultRequest.sigguCode() == null ? "null" : resultRequest.sigguCode());
        keyword.append(resultRequest.hospitalName() == null ? "null" : resultRequest.hospitalName());
        return keyword.toString();
    }

    private ResponseEntity<ResultListResponseDTO> getCachedResult(String keyword) {
        Optional<SearchLog> cachedLog = searchLogRepository.findBySearchKeyword(keyword);

        SearchLog searchLog = cachedLog.get();

        // Result 리스트 조회
        List<ResultItemDTO> cachedResults = resultRepository.findBySearchLogWithFetch(searchLog)
                .stream()
                .map(result -> new ResultItemDTO(
                        result.getHospitalName(),
                        result.getHospitalAddress(),
                        result.getClinicName(),
                        result.getMinPrice(),
                        result.getMaxPrice()
                ))
                .collect(Collectors.toList());

        // AI 코멘트 조회
        String cachedAiComment = aiCommentRepository.findBySearchLog(searchLog)
                .map(AiComment::getComment)
                .orElse("");

        // 캐싱된 응답 생성
        ResultListResponseDTO cachedResponse = ResultListResponseDTO.builder()
                .resultCount(cachedResults.size())
                .list(cachedResults)
                .aiComment(cachedAiComment)
                .build();

        return ResponseEntity.ok(cachedResponse);
    }

    private boolean checkSearchLog(ResultRequest resultRequest, String keyWord) {

        return searchLogRepository.existsSearchLogBySearchKeyword(keyWord);
    }

    private boolean isEmptyResponse(String json) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(json);
            JsonNode body = root.path("response").path("body");
            int totalCount = body.path("totalCount").asInt(-1);
            JsonNode itemsNode = body.path("items");
            boolean itemsEmpty =
                    itemsNode.isMissingNode()
                            || (itemsNode.isTextual() && itemsNode.asText().isBlank())
                            || (itemsNode.has("item") && itemsNode.path("item").isMissingNode());
            return totalCount == 0 || itemsEmpty;
        } catch (Exception e) {
            return true;
        }
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
