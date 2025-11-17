package com.douzone_internship.backend.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "result")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
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

    @Column(name = "hospital_addr", nullable = false)
    private String hospitalAddress;

    @Column(name = "max_price", nullable = false)
    private int maxPrice;

    @Column(name = "min_price", nullable = false)
    private int minPrice;

    @Column(name = "hospital_url", nullable = false)
    private String hospitalUrl;
}
