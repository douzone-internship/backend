package com.douzone_internship.backend.controller;

import com.douzone_internship.backend.dto.request.ResultRequest;
import com.douzone_internship.backend.dto.response.ResultListResponseDTO;
import com.douzone_internship.backend.service.ResultService;
import jakarta.servlet.http.HttpSession;
import java.security.NoSuchAlgorithmException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequiredArgsConstructor
public class ResultController {

    private final ResultService resultService;

    @PostMapping("/result/reports")
    public ResponseEntity<ResultListResponseDTO> generateReport(@Validated @RequestBody ResultRequest resultRequest, HttpSession httpSession)
            throws NoSuchAlgorithmException {
        httpSession.setAttribute("keyword", resultService.extractKeyword(resultRequest));
        return resultService.generateResult(resultRequest);
    }


    @GetMapping("/result/reports")
    public ResponseEntity<ResultListResponseDTO> getReport(HttpSession httpSession) throws NoSuchAlgorithmException {
        return resultService.getResult((String) httpSession.getAttribute("keyword"));
    }
}
