package com.kkori.api.pet.entity;

import com.kkori.api.common.entity.SoftDeletableEntity;
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
public class Pet extends SoftDeletableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_id", nullable = false, unique = true)
    private String externalId;

    @Column(name = "device_id")
    private Long deviceId;

    @Column(name = "user_id")
    private Long userId;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Species species;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    private String breed;

    private LocalDate birthDate;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean birthDateUnknown = false;

    private LocalDate adoptionDate;

    @Column(precision = 5, scale = 2)
    private BigDecimal weightKg;

    @Column(nullable = false)
    private boolean neutered;

    @Column(columnDefinition = "TEXT")
    private String medicalNotes;

    @Column(columnDefinition = "TEXT")
    private String photoBase64;

    @Builder
    public Pet(String externalId, Long deviceId, Long userId, String name, Species species, String breed,
               Gender gender, LocalDate birthDate, boolean birthDateUnknown, LocalDate adoptionDate,
               BigDecimal weightKg, boolean neutered,
               String medicalNotes, String photoBase64) {
        this.externalId = externalId;
        this.deviceId = deviceId;
        this.userId = userId;
        this.name = name;
        this.species = species;
        this.gender = gender;
        this.breed = breed;
        this.birthDate = birthDate;
        this.birthDateUnknown = birthDateUnknown;
        this.adoptionDate = adoptionDate;
        this.weightKg = weightKg;
        this.neutered = neutered;
        this.medicalNotes = medicalNotes;
        this.photoBase64 = photoBase64;
    }

    public void connectUser(Long userId) {
        this.userId = userId;
    }

    public void update(String name, Species species, Gender gender, String breed, LocalDate birthDate,
                       boolean birthDateUnknown, LocalDate adoptionDate,
                       BigDecimal weightKg, boolean neutered, String medicalNotes, String photoBase64) {
        this.name = name;
        this.species = species;
        this.gender = gender;
        this.breed = breed;
        this.birthDate = birthDate;
        this.birthDateUnknown = birthDateUnknown;
        this.adoptionDate = adoptionDate;
        this.weightKg = weightKg;
        this.neutered = neutered;
        this.medicalNotes = medicalNotes;
        this.photoBase64 = photoBase64;
    }
}
