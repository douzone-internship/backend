package com.douzone_internship.backend.dto.response;

import java.util.ArrayList;
import lombok.Builder;

@Builder
public record HospitalResponseDTO(
    ArrayList<String> nameList
) {}
