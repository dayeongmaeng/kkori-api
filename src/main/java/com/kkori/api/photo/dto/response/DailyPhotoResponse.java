package com.kkori.api.photo.dto.response;

import com.kkori.api.photo.entity.DailyPhoto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record DailyPhotoResponse(
        String externalId,
        Long petId,
        Long caregiverId,
        LocalDate date,
        String photoBase64,
        String caption,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static DailyPhotoResponse from(DailyPhoto photo) {
        return new DailyPhotoResponse(
                photo.getExternalId(),
                photo.getPetId(),
                photo.getCaregiverId(),
                photo.getDate(),
                photo.getPhotoBase64(),
                photo.getCaption(),
                photo.getCreatedAt(),
                photo.getUpdatedAt()
        );
    }
}
