package com.douzone_internship.backend.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;


@Entity
@Table(name = "ai_comment")
@Getter
@Setter
@AllArgsConstructor
@RequiredArgsConstructor
@Builder
public class AiComment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "comment_id", nullable = false)
    private UUID commentId;

    @OneToOne
    @JoinColumn(name = "search_id", nullable = false)
    private SearchLog searchLog;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "comment", nullable = false)
    private String comment;
}
