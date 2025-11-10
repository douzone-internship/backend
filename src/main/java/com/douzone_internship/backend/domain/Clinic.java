package com.douzone_internship.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "clinic")
public class Clinic {

    @Id
    @Column(name = "clinic_cd")
    private String clinicCd;

    @Column(name = "name")
    private String name;
}
