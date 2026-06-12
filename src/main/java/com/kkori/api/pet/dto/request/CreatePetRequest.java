package com.kkori.api.pet.dto.request;

import com.kkori.api.pet.entity.Gender;
import com.kkori.api.pet.entity.Species;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreatePetRequest(
        @Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$") String externalId,
        @NotBlank String name,
        @NotNull Species species,
        @NotNull Gender gender,
        String breed,
        LocalDate birthDate,
        boolean birthDateUnknown,
        LocalDate adoptionDate,
        BigDecimal weightKg,
        boolean weightKgUnknown,
        boolean neutered,
        String medicalNotes,
        String photoBase64
) {
    @AssertTrue(message = "birthDate is required when birthDateUnknown is false")
    public boolean isBirthDateValid() {
        return birthDateUnknown || birthDate != null;
    }

    @AssertTrue(message = "weightKg is required when weightKgUnknown is false")
    public boolean isWeightKgValid() {
        return weightKgUnknown || weightKg != null;
    }
}
