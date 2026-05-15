package com.kkori.api.caregiver.dto.response;

import com.kkori.api.caregiver.entity.Caregiver;
import com.kkori.api.caregiver.entity.CaregiverRole;

import java.time.LocalDateTime;

public record CaregiverResponse(
        String externalId,
        String name,
        CaregiverRole role,
        String color,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static CaregiverResponse from(Caregiver caregiver) {
        return new CaregiverResponse(
                caregiver.getExternalId(),
                caregiver.getName(),
                caregiver.getRole(),
                caregiver.getColor(),
                caregiver.getCreatedAt(),
                caregiver.getUpdatedAt()
        );
    }
}
