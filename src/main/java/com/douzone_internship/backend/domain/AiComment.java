package com.douzone_internship.backend.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.UUID;
import org.hibernate.annotations.Nationalized;

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

    @Nationalized
    @Column(name = "comment", nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String comment;
}
