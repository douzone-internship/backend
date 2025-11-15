package com.douzone_internship.backend.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class RawClinicPaymentListResponseDTO {
    List<RawClinicPaymentResponseDTO> rawClinicPaymentResponseDTOList;
}
