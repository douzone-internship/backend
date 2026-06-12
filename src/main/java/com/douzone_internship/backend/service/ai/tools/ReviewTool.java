package com.douzone_internship.backend.service.ai.tools;

import com.douzone_internship.backend.domain.Comment;
import com.douzone_internship.backend.repository.CommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ReviewTool {

    private static final int DEFAULT_LIMIT = 5;
    private static final int CONTENT_MAX_CHARS = 200;

    private final CommentRepository commentRepository;

    public HospitalReviews getReviews(String hospitalName, String clinicCode, Integer limit) {
        if (isBlank(hospitalName) || isBlank(clinicCode)) {
            return HospitalReviews.empty(hospitalName, clinicCode);
        }

        List<Comment> all = commentRepository
                .findByHospitalNameAndClinicCodeOrderByCreatedAtDesc(hospitalName, clinicCode);

        if (all.isEmpty()) {
            return HospitalReviews.empty(hospitalName, clinicCode);
        }

        int cap = (limit == null || limit <= 0) ? DEFAULT_LIMIT : limit;

        List<ReviewSnippet> samples = all.stream()
                .limit(cap)
                .map(this::toSnippet)
                .toList();

        return new HospitalReviews(hospitalName, clinicCode, all.size(), samples);
    }

    private ReviewSnippet toSnippet(Comment c) {
        String body = c.getComment() == null ? "" : c.getComment();
        String truncated = body.length() <= CONTENT_MAX_CHARS
                ? body
                : body.substring(0, CONTENT_MAX_CHARS) + "...";
        LocalDateTime created = c.getCreatedAt();
        return new ReviewSnippet(truncated, c.getScore(), created == null ? "" : created.toString());
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
