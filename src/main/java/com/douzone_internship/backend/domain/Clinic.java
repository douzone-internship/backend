package com.douzone_internship.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "clinic")
@Getter
@Setter
public class Clinic {

    @Id
    @Column(name = "clinic_cd", nullable = false)
    private String clinicCd;

    @Column(name = "name", nullable = false)
    private String name;
}
