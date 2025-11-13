package com.douzone_internship.backend.service;

import com.douzone_internship.backend.dto.response.GenericApiResponseDTO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public abstract class AbstractApiService <T, E>{

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * DTO를 Entity로 변환하는 로직
     */
    protected abstract E convertDtoToEntity(T dto);

    /**
     * 데이터를 저장할 Repository
     */
    protected abstract JpaRepository<E, ?> getRepository();

    public void processApiResponse(String jsonResponse, Class<T> dtoType) {
        try {
            JsonNode rootNode = objectMapper.readTree(jsonResponse);
            JsonNode itemsNode = rootNode.path("response").path("body").path("items").path("item");

            List<E> entities = new ArrayList<>();
            if (itemsNode.isArray()) {
                for (JsonNode item : itemsNode) {
                    T dto = objectMapper.treeToValue(item, dtoType);
                    E entity = convertDtoToEntity(dto);
                    entities.add(entity);
                }
            }

            if (!entities.isEmpty()) {
                getRepository().saveAll(entities);
            }
        } catch (Exception e) {
            log.error("Json 파싱 중 오류:{}", e);
        }
    }
}
