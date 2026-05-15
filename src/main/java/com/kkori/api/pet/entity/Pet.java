package com.kkori.api.pet.entity;

import com.kkori.api.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "pet")
public class Pet extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_id", nullable = false, unique = true)
    private String externalId;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Species species;

    private String breed;

    private LocalDate birthDate;

    @Column(precision = 5, scale = 2)
    private BigDecimal weightKg;

    @Column(nullable = false)
    private boolean neutered;

    @Column(columnDefinition = "TEXT")
    private String medicalNotes;

    @Column(columnDefinition = "TEXT")
    private String photoBase64;

    @Builder
    public Pet(String externalId, String name, Species species, String breed,
               LocalDate birthDate, BigDecimal weightKg, boolean neutered,
               String medicalNotes, String photoBase64) {
        this.externalId = externalId;
        this.name = name;
        this.species = species;
        this.breed = breed;
        this.birthDate = birthDate;
        this.weightKg = weightKg;
        this.neutered = neutered;
        this.medicalNotes = medicalNotes;
        this.photoBase64 = photoBase64;
    }
}
