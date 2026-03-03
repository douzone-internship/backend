package com.douzone_internship.backend.service;

import com.douzone_internship.backend.domain.*;
import com.douzone_internship.backend.dto.request.ResultRequest;
import com.douzone_internship.backend.dto.response.RawClinicPaymentResponseDTO;
import com.douzone_internship.backend.dto.response.ResultItemDTO;
import com.douzone_internship.backend.dto.response.ResultListResponseDTO;
import com.douzone_internship.backend.exceptions.ResourceNotFoundException;
import com.douzone_internship.backend.repository.AiCommentRepository;
import com.douzone_internship.backend.repository.HospitalRepository;
import com.douzone_internship.backend.repository.ResultRepository;
import com.douzone_internship.backend.repository.SearchLogRepository;
import com.douzone_internship.backend.util.SHA256;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.security.NoSuchAlgorithmException;
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
    private final SearchLogRepository searchLogRepository;
    private final ResultRepository resultRepository;
    private final AiCommentRepository aiCommentRepository;
    private final AiService aiService;
    private final SHA256 sha256;

    @Transactional(readOnly = true)
    public ResultListResponseDTO generateResult(ResultRequest resultRequest)
            throws NoSuchAlgorithmException {

        String keyword = extractKeyword(resultRequest);
        String hashedKeyword = sha256.encrypt(keyword);

        // DB에 캐싱 여부 확인
        if (checkSearchLog(resultRequest, hashedKeyword)) {
            return getCachedResult(hashedKeyword);
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

        String jsonResponse = restTemplate.getForObject(uri, String.class);

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

        String aiComment = aiService.callAiApi(resultItems);

        ResultListResponseDTO response = ResultListResponseDTO.builder()
                .resultCount(resultItems.size())
                .list(resultItems)
                .aiComment(aiComment)
                .build();

        resultSaveService.saveResultAsync(resultRequest, resultItems, aiComment);

        return response;
    }

    private String extractKeyword(ResultRequest resultRequest) {
        StringBuilder keyword = new StringBuilder();
        keyword.append(resultRequest.clinicCode());
        keyword.append(resultRequest.sidoCode());
        keyword.append(resultRequest.sigguCode() == null ? "null" : resultRequest.sigguCode());
        keyword.append(resultRequest.hospitalName() == null ? "null" : resultRequest.hospitalName());
        return keyword.toString();
    }

    private ResultListResponseDTO getCachedResult(String keyword) {
        SearchLog searchLog = searchLogRepository.findBySearchKeyword(keyword)
                .orElseThrow(() -> new ResourceNotFoundException("캐싱된 검색 결과를 찾을 수 없습니다."));

        List<ResultItemDTO> cachedResults = resultRepository.findBySearchLogWithFetch(searchLog)
                .stream()
                .map(result -> new ResultItemDTO(
                        result.getHospitalName(),
                        result.getHospitalAddress(),
                        result.getClinicName(),
                        result.getMinPrice(),
                        result.getMaxPrice()))
                .toList();

        String cachedAiComment = aiCommentRepository.findBySearchLog(searchLog)
                .map(AiComment::getComment)
                .orElse("");

        return ResultListResponseDTO.builder()
                .resultCount(cachedResults.size())
                .list(cachedResults)
                .aiComment(cachedAiComment)
                .build();
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
