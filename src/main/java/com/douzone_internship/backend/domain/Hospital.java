package com.douzone_internship.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "hospital")
public class Hospital {

    @Id
    @Column(name = "hospital_id")
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID hospitalId;

    @Column(name = "name")
    private String name;

    @Column(name = "hospital_url")
    private String hospitalUrl;
}
