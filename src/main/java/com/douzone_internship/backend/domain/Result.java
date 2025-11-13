package com.douzone_internship.backend.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "result")
@Getter
@Setter
public class Result {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "result_id", nullable = false)
    private UUID resultId;

    @ManyToOne
    @JoinColumn(name = "search_id", nullable = false)
    private SearchLog searchLog;

    @Column(name = "hospital_name", nullable = false)
    private String hospitalName;

    @Column(name = "clinic_name", nullable = false)
    private String clinicName;

    @Column(name = "sido_name", nullable = false)
    private String sidoName;

    @Column(name = "sggu_name", nullable = false)
    private String sgguName;

    @Column(name = "max_price", nullable = false)
    private int maxPrice;

    @Column(name = "min_price", nullable = false)
    private int minPrice;

    @Column(name = "hospital_url", nullable = false)
    private String hospitalUrl;
}
