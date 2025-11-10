package com.douzone_internship.backend.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "search_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SearchLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "search_id")
    private UUID searchId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "search_keyword")
    private String searchKeyword;

    @OneToMany(mappedBy = "searchLog", cascade = CascadeType.ALL)
    private List<Result> results = new ArrayList<>();

    @OneToMany(mappedBy = "searchLog", cascade = CascadeType.ALL)
    private List<AiComment> aiComments = new ArrayList<>();
}
