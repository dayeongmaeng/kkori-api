package com.kkori.api.photo.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CreateDailyPhotoRequest(
        @NotBlank String externalId,
        @NotBlank String petExternalId,
        @NotBlank String caregiverExternalId,
        @NotNull LocalDate date,
        @NotBlank String photoBase64,
        String caption
) {}
