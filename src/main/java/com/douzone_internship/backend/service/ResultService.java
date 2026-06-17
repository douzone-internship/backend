package com.douzone_internship.backend.service;

import com.douzone_internship.backend.domain.*;
import com.douzone_internship.backend.dto.request.ResultRequest;
import com.douzone_internship.backend.dto.response.RawClinicPaymentResponseDTO;
import com.douzone_internship.backend.dto.response.ResultItemDTO;
import com.douzone_internship.backend.dto.response.ResultListResponseDTO;
import com.douzone_internship.backend.repository.HospitalRepository;
import com.douzone_internship.backend.service.ai.cache.AiResponseCache;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

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
    private final AiService aiService;
    private final AiResponseCache aiResponseCache;

    // @Transactional을 메서드 전체에 걸지 않는다. 중간에 외부 OpenData API 호출과
    // Gemini 에이전트 루프(수십 초 소요)가 있어, 트랜잭션으로 묶으면 그 시간 동안
    // DB 커넥션을 점유해 커넥션 풀 leak이 발생한다. 각 DB 작업은 자체 트랜잭션으로 처리된다.
    public ResultListResponseDTO generateResult(ResultRequest resultRequest) {

        String cacheKey = aiResponseCache.buildKey(resultRequest);
        Optional<ResultListResponseDTO> cached = aiResponseCache.load(cacheKey);
        if (cached.isPresent()) {
            return cached.get();
        }

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("ServiceKey", apiKey);
        params.add("pageNo", "1");
        params.add("numOfRows", "1000");
        params.add("sidoCd", resultRequest.sidoCode());
        if (resultRequest.sigguCode() != null) {
            params.add("sgguCd", resultRequest.sigguCode());
        }
        if (resultRequest.hospitalName() != null) {
            params.add("yadmNm", resultRequest.hospitalName());
        }
        params.add("itemCd", resultRequest.clinicCode());

        URI uri = UriComponentsBuilder.fromHttpUrl(clinicPaymentUrl)
                .queryParams(params)
                .encode()
                .build()
                .toUri();

        String jsonResponse = callOpenDataApi(uri);

        if (Objects.requireNonNull(jsonResponse).isEmpty() || isEmptyResponse(jsonResponse)) {
            return ResultListResponseDTO.builder()
                    .resultCount(0)
                    .list(List.of())
                    .aiComment("no result")
                    .build();
        }

        List<RawClinicPaymentResponseDTO> rawItems = parseApiResponse(jsonResponse, RawClinicPaymentResponseDTO.class);

        List<ResultItemDTO> resultItems = rawItems.stream()
                .map(rawItem -> {
                    Optional<Hospital> hospitalOpt;

                    if (resultRequest.sigguCode() != null) {
                        hospitalOpt = hospitalRepository.findFirstByNameAndSigungu_SgguCd(
                                rawItem.getYadmNm(),
                                resultRequest.sigguCode());
                    } else {
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
                            rawItem.getMaxPrc());
                })
                .toList();

        String aiComment = aiService.callAiApi(resultItems, resultRequest.clinicCode());

        ResultListResponseDTO response = ResultListResponseDTO.builder()
                .resultCount(resultItems.size())
                .list(resultItems)
                .aiComment(aiComment)
                .build();

        // AI 호출이 실패(폴백)했거나 빈 응답인 경우 캐싱하지 않는다.
        // 실패/빈 응답을 저장하면 다음 동일 검색에서 캐시 hit으로 잘못된 결과가 영구 반환된다.
        boolean savable = !aiService.isFallback(aiComment)
                && aiComment != null && !aiComment.isBlank();
        if (savable) {
            resultSaveService.saveResultAsync(resultRequest, resultItems, aiComment);
        }

        return response;
    }

    @Retry(name = "openDataApi")
    @CircuitBreaker(name = "openDataApi")
    public String callOpenDataApi(URI uri) {
        return restTemplate.getForObject(uri, String.class);
    }

    private boolean isEmptyResponse(String json) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(json);
            JsonNode body = root.path("response").path("body");
            int totalCount = body.path("totalCount").asInt(-1);
            JsonNode itemsNode = body.path("items");
            boolean itemsEmpty = itemsNode.isMissingNode()
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
