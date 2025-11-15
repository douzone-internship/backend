// AbstractApiService.java
package com.douzone_internship.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public abstract class AbstractApiService<T, E> {

    @Autowired
    private ObjectMapper objectMapper;

    protected abstract E convertDtoToEntity(T dto);
    protected abstract JpaRepository<E, ?> getRepository();

    /**
     * JSON 응답을 파싱하여 DTO 리스트로 변환하는 공통 메서드
     */
    protected List<T> parseApiResponse(String jsonResponse, Class<T> dtoType) {
        try {
            JsonNode rootNode = objectMapper.readTree(jsonResponse);
            JsonNode itemsNode = rootNode.path("response").path("body").path("items").path("item");

            List<T> dtoList = new ArrayList<>();
            if (itemsNode.isArray()) {
                for (JsonNode item : itemsNode) {
                    T dto = objectMapper.treeToValue(item, dtoType);
                    dtoList.add(dto);
                }
            }
            return dtoList;
        } catch (Exception e) {
            log.error("JSON 파싱 중 오류 발생", e);
            return Collections.emptyList();
        }
    }

    /**
     * API 응답을 DB에 저장하는 메서드
     */
    public void processApiResponse(String jsonResponse, Class<T> dtoType) {
        List<T> dtoList = parseApiResponse(jsonResponse, dtoType);
        if (dtoList.isEmpty()) {
            return;
        }

        List<E> entities = new ArrayList<>();
        for (T dto : dtoList) {
            entities.add(convertDtoToEntity(dto));
        }

        if (!entities.isEmpty()) {
            getRepository().saveAll(entities);
            log.info("총 {}개의 데이터 저장 완료", entities.size());
        }
    }
}
