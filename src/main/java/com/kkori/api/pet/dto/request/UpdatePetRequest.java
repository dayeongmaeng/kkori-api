package com.kkori.api.pet.dto.request;

import com.kkori.api.pet.entity.Species;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdatePetRequest(
        @NotBlank String name,
        @NotNull Species species,
        String breed,
        LocalDate birthDate,
        BigDecimal weightKg,
        boolean neutered,
        String medicalNotes,
        String photoBase64
) {}
