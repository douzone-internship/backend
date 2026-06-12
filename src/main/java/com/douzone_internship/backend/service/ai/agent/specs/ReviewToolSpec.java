package com.douzone_internship.backend.service.ai.agent.specs;

import com.douzone_internship.backend.service.ai.agent.ToolSpec;
import com.douzone_internship.backend.service.ai.tools.ReviewTool;
import com.google.genai.types.FunctionDeclaration;
import com.google.genai.types.Schema;
import com.google.genai.types.Type;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ReviewToolSpec implements ToolSpec {

    public static final String NAME = "get_hospital_reviews";

    private final ReviewTool reviewTool;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public FunctionDeclaration declaration() {
        return FunctionDeclaration.builder()
                .name(NAME)
                .description("특정 병원과 시술의 최신 후기를 조회한다. 본문은 200자로 컷된다.")
                .parameters(Schema.builder()
                        .type(Type.Known.OBJECT)
                        .properties(Map.of(
                                "hospitalName", Schema.builder()
                                        .type(Type.Known.STRING)
                                        .description("병원 이름 (예: 강남필의원)")
                                        .build(),
                                "clinicCode", Schema.builder()
                                        .type(Type.Known.STRING)
                                        .description("시술 코드 (예: BO001)")
                                        .build(),
                                "limit", Schema.builder()
                                        .type(Type.Known.INTEGER)
                                        .description("가져올 후기 개수. 미지정 시 5.")
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
        Integer limit = args.get("limit") instanceof Number n ? n.intValue() : null;
        return reviewTool.getReviews(hospitalName, clinicCode, limit);
    }
}
