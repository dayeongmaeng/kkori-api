package com.kkori.api.photo.dto.response;

import com.kkori.api.photo.entity.DailyPhoto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record DailyPhotoResponse(
        String externalId,
        Long petId,
        Long caregiverId,
        LocalDate date,
        String caption,
        String mediumUrl,
        String thumbnailUrl,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static DailyPhotoResponse from(DailyPhoto photo) {
        return new DailyPhotoResponse(
                photo.getExternalId(),
                photo.getPetId(),
                photo.getCaregiverId(),
                photo.getDate(),
                photo.getCaption(),
                photo.getMediumUrl(),
                photo.getThumbnailUrl(),
                photo.getCreatedAt(),
                photo.getUpdatedAt()
        );
    }
}
