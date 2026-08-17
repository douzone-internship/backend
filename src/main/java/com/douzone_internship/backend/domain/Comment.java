package com.douzone_internship.backend.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Nationalized;
import org.hibernate.type.SqlTypes;

@Builder
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Table(name = "comments")
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    @Nationalized
    @Column(name = "hospital_name", nullable = false)
    private String hospitalName;

    @Column(name = "clinic_code", nullable = false)
    private String clinicCode;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "comment", nullable = false)
    private String comment;

    @Column(name = "score", nullable = false)
    private int score;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public void update(String comment, int score) {
        if (comment == null || comment.isBlank()) {
            throw new IllegalArgumentException("댓글 내용은 비어있을 수 없습니다.");
        }
        if (score < 1 || score > 5) {
            throw new IllegalArgumentException("평점은 1~5 사이여야 합니다.");
        }
        this.comment = comment;
        this.score = score;
        this.updatedAt = LocalDateTime.now();
    }
}
