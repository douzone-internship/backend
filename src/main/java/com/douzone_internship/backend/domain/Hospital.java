package com.douzone_internship.backend.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "hospital")
@Getter
@Setter
public class Hospital {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "hospital_id", nullable = false)
    private UUID hospitalId;

    @ManyToOne
    @JoinColumn(name = "sigungu_cd", nullable = false)
    private Sigungu sigungu;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "hospital_url")
    private String hospitalUrl;
}
