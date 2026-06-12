package com.douzone_internship.backend.service.ai.agent.specs;

import com.douzone_internship.backend.service.ai.agent.ToolSpec;
import com.douzone_internship.backend.service.ai.tools.HospitalsOverviewTool;
import com.google.genai.types.FunctionDeclaration;
import com.google.genai.types.Schema;
import com.google.genai.types.Type;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class HospitalsOverviewToolSpec implements ToolSpec {

    public static final String NAME = "get_hospitals_overview";

    private final HospitalsOverviewTool overviewTool;

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public FunctionDeclaration declaration() {
        return FunctionDeclaration.builder()
                .name(NAME)
                .description(
                        "여러 병원의 종합 리포트(평점·부정신호 등급·부정 키워드·최근 후기 2개)를 한 번에 조회한다. " +
                        "여러 병원을 비교하는 추천 응답을 만들 때 가장 먼저 호출하는 것이 권장된다."
                )
                .parameters(Schema.builder()
                        .type(Type.Known.OBJECT)
                        .properties(Map.of(
                                "hospitalNames", Schema.builder()
                                        .type(Type.Known.ARRAY)
                                        .items(Schema.builder().type(Type.Known.STRING).build())
                                        .description("조회할 병원 이름 목록. 입력 [병원 목록] 블록의 병원들을 그대로 넣는다.")
                                        .build(),
                                "clinicCode", Schema.builder()
                                        .type(Type.Known.STRING)
                                        .description("시술 코드. 입력 데이터에 공통으로 적용된 값을 그대로 사용한다.")
                                        .build()
                        ))
                        .required(List.of("hospitalNames", "clinicCode"))
                        .build())
                .build();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object invoke(Map<String, Object> args) {
        List<String> hospitalNames = (List<String>) args.get("hospitalNames");
        String clinicCode = (String) args.get("clinicCode");
        return overviewTool.getOverview(hospitalNames, clinicCode);
    }
}
