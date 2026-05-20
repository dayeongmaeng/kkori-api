package com.kkori.api.pet.dto.request;

import com.kkori.api.pet.entity.Gender;
import com.kkori.api.pet.entity.Species;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdatePetRequest(
        @NotBlank String name,
        @NotNull Species species,
        @NotNull Gender gender,
        String breed,
        LocalDate birthDate,
        boolean birthDateUnknown,
        LocalDate adoptionDate,
        BigDecimal weightKg,
        boolean neutered,
        String medicalNotes,
        String photoBase64
) {
    @AssertTrue(message = "birthDate is required when birthDateUnknown is false")
    public boolean isBirthDateValid() {
        return birthDateUnknown || birthDate != null;
    }
}
