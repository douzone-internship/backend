package com.douzone_internship.backend.domain;

import jakarta.persistence.*;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "result")
public class Result {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "result_id")
    private UUID resultId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "search_id")
    private SearchLog searchLog;

    @Column(name = "hospital_name")
    private String hospitalName;

    @Column(name = "clinic_name")
    private String clinicName;

    @Column(name = "sggu_name")
    private String sgguName;

    @Column(name = "max_price")
    private int maxPrice;

    @Column(name = "min_price")
    private int minPrice;

    @Column(name = "hospital_url")
    private String hospitalUrl;
}
