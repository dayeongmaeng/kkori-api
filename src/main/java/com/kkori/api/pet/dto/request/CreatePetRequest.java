package com.kkori.api.pet.dto.request;

import com.kkori.api.pet.entity.Species;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreatePetRequest(
        @Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$") String externalId,
        @NotBlank String name,
        @NotNull Species species,
        String breed,
        LocalDate birthDate,
        BigDecimal weightKg,
        boolean neutered,
        String medicalNotes,
        String photoBase64
) {}
