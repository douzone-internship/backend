package com.douzone_internship.backend.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "sigungu")
@Getter
@Setter
public class Sigungu {

    @Id
    @Column(name = "sigungu_cd", nullable = false)
    private String sgguCd;

    @ManyToOne
    @JoinColumn(name = "sido_cd", nullable = false)
    private Sido sido;

    @Column(name = "name", nullable = false)
    private String name;
}

