package com.douzone_internship.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "sido")
@Getter
@Setter
public class Sido {

    @Id
    @Column(name = "sido_cd", nullable = false)
    private String sidoCd;

    @Column(name = "name", nullable = false)
    private String name;
}

