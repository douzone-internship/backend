package com.douzone_internship.backend.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "hospital")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Hospital {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "hospital_id", nullable = false)
    private UUID hospitalId;

    @ManyToOne
    @JoinColumn(name = "sggu_cd", nullable = false)
    private Sigungu sigungu;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "hospital_url")
    private String hospitalUrl;

    @Column(name = "hospital_addr")
    private String hospitalAddress;

    @Column(name = "ykiho", unique = true)
    private String ykiho;
}
