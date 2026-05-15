package com.kkori.api.caregiver.dto.request;

import com.kkori.api.caregiver.entity.CaregiverRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateCaregiverRequest(
        @NotBlank String externalId,
        @NotBlank String name,
        @NotNull CaregiverRole role,
        @NotBlank String color
) {}
