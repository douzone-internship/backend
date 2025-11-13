package com.douzone_internship.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
@Slf4j
@RequiredArgsConstructor
public class DataInsertService {

    private final ClinicDataService clinicDataService;
    private final HospitalDataService hospitalDataService;

    private static final int MAX_HOSPITAL_ROW = 10;

    public void insertOpenDataToDB() {
        insertClinicData();
        insertHospitalData();
    }

    public void insertClinicData() {
        try {
            Path clinicJsonPath = Paths.get("data", "clinic.json");

            if (!Files.exists(clinicJsonPath)) {
                log.warn("clinic.json 파일이 존재하지 않습니다: {}", clinicJsonPath.toAbsolutePath());
                return;
            }
            log.info("clinic.json 데이터 처리 시작");
            String jsonContent = Files.readString(clinicJsonPath);
            clinicDataService.updateClinics(jsonContent);
            log.info("clinic.json 데이터 처리 완료");
        } catch (IOException e) {
            log.error("clinic.json 파일 읽기 실패", e);
            throw new RuntimeException("clinic.json 처리 중 오류 발생", e);
        }
    }


    public void insertHospitalData() {
        try {
            log.info("hospital.json 데이터 처리 시작");
            for (int i = 1; i <= MAX_HOSPITAL_ROW; i++) {
                Path hospitalJsonPath = Paths.get("data", String.format("hospital%d.json", i));

                if (!Files.exists(hospitalJsonPath)) {
                    log.warn("hospital.json 파일이 존재하지 않습니다: {}", hospitalJsonPath.toAbsolutePath());
                    return;
                }
                String jsonContent = Files.readString(hospitalJsonPath);
                hospitalDataService.updateHospitals(jsonContent);
                log.info("work: {}", i);
            }
            log.info("hospital.json 데이터 처리 완료");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
