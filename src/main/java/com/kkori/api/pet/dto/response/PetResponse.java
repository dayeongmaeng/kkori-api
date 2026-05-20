package com.kkori.api.pet.dto.response;

import com.kkori.api.pet.entity.Pet;
import com.kkori.api.pet.entity.Gender;
import com.kkori.api.pet.entity.Species;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record PetResponse(
        String externalId,
        String name,
        Species species,
        Gender gender,
        String breed,
        LocalDate birthDate,
        boolean birthDateUnknown,
        LocalDate adoptionDate,
        BigDecimal weightKg,
        boolean neutered,
        String medicalNotes,
        String photoBase64,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static PetResponse from(Pet pet) {
        return new PetResponse(
                pet.getExternalId(),
                pet.getName(),
                pet.getSpecies(),
                pet.getGender(),
                pet.getBreed(),
                pet.getBirthDate(),
                pet.isBirthDateUnknown(),
                pet.getAdoptionDate(),
                pet.getWeightKg(),
                pet.isNeutered(),
                pet.getMedicalNotes(),
                pet.getPhotoBase64(),
                pet.getCreatedAt(),
                pet.getUpdatedAt()
        );
    }
}
