package com.douzone_internship.backend.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class RawClinicPaymentResponseDTO {

    @JsonProperty("yadmNm")
    private String yadmNm;

    @JsonProperty("npayKorNm")
    private String npayKorNm;

    @JsonProperty("minPrc")
    private int minPrc;

    @JsonProperty("maxPrc")
    private int maxPrc;

    @JsonProperty("sgguCd")
    private String sgguCd;

    @JsonProperty("sidoCd")
    private String sidoCd;

    @JsonProperty("ykiho")
    private String ykiho;

    @JsonProperty("urlAddr")
    private String urlAddr;
}
