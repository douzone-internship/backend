package com.douzone_internship.backend.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class RawHospitalResponseDTO {
    private String sgguCd;
    private String yadmNm;
    private String hospUrl;
    private String addr;
}
