package com.douzone_internship.backend.domain;

import jakarta.persistence.*;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "ai_comments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiComment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "comment_id")
    private UUID commentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "search_id")
    private SearchLog searchLog;

    @Column(name = "comment")
    private String comment;
}
