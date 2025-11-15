package com.douzone_internship.backend.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class RawClinicPaymentResponseDTO {
    private String maxPrc;
    private String minPrc;
    private String npayKorNm;
    private String ykiho;
    private String yadmNm;
}
