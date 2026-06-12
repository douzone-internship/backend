package com.douzone_internship.backend.service.ai.agent.specs;

import com.douzone_internship.backend.service.ai.agent.ToolSpec;
import com.douzone_internship.backend.service.ai.tools.RatingTool;
import com.google.genai.types.FunctionDeclaration;
import com.google.genai.types.Schema;
import com.google.genai.types.Type;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class RatingToolSpec implements ToolSpec {

    public static final String NAME = "get_hospital_rating";

    private final RatingTool ratingTool;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public FunctionDeclaration declaration() {
        return FunctionDeclaration.builder()
                .name(NAME)
                .description("특정 병원과 시술의 평균 별점(1~5, 소수 1자리)과 후기 수를 조회한다.")
                .parameters(Schema.builder()
                        .type(Type.Known.OBJECT)
                        .properties(Map.of(
                                "hospitalName", Schema.builder()
                                        .type(Type.Known.STRING)
                                        .description("병원 이름")
                                        .build(),
                                "clinicCode", Schema.builder()
                                        .type(Type.Known.STRING)
                                        .description("시술 코드")
                                        .build()
                        ))
                        .required(List.of("hospitalName", "clinicCode"))
                        .build())
                .build();
    }

    @Override
    public Object invoke(Map<String, Object> args) {
        String hospitalName = (String) args.get("hospitalName");
        String clinicCode = (String) args.get("clinicCode");
        return ratingTool.getRating(hospitalName, clinicCode);
    }
}
