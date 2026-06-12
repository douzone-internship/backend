package com.douzone_internship.backend.service.ai.tools;

import com.douzone_internship.backend.domain.Comment;
import com.douzone_internship.backend.repository.CommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class RatingTool {

    private final CommentRepository commentRepository;

    public HospitalRating getRating(String hospitalName, String clinicCode) {
        if (isBlank(hospitalName) || isBlank(clinicCode)) {
            return HospitalRating.empty(hospitalName, clinicCode);
        }

        List<Comment> all = commentRepository
                .findByHospitalNameAndClinicCodeOrderByCreatedAtDesc(hospitalName, clinicCode);

        if (all.isEmpty()) {
            return HospitalRating.empty(hospitalName, clinicCode);
        }

        double rawAvg = all.stream().mapToInt(Comment::getScore).average().orElse(0.0);
        double rounded = Math.round(rawAvg * 10.0) / 10.0;

        return new HospitalRating(hospitalName, clinicCode, rounded, all.size());
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
