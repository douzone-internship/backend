package com.douzone_internship.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.Nationalized;

@Entity
@Table(name = "clinic")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Clinic {

    @Id
    @Column(name = "clinic_cd", nullable = false)
    private String clinicCd;

    @Nationalized
    @Column(name = "name", nullable = false)
    private String name;
}
