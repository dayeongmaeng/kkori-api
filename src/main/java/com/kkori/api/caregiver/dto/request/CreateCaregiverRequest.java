package com.kkori.api.caregiver.dto.request;

import com.kkori.api.caregiver.entity.CaregiverRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record CreateCaregiverRequest(
        @Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$") String externalId,
        @NotBlank String name,
        @NotNull CaregiverRole role,
        @NotBlank String color
) {}
