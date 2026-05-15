package com.kkori.api.photo.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;

public record CreateDailyPhotoRequest(
        @Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$") String externalId,
        @NotBlank String petExternalId,
        @NotBlank String caregiverExternalId,
        @NotNull LocalDate date,
        String caption
) {}
