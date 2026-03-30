package com.douzone_internship.backend.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.UUID;
import org.hibernate.annotations.Nationalized;

@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Users {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;


    private String email;

    @Nationalized
    private String name;

    private String password;

    @Enumerated(EnumType.STRING)
    private Provider provider;

    @Nationalized
    private String providerId;

    public Users update(String name) {
        this.name = name;
        return this;
    }
}
