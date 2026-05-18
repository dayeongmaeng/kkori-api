package com.kkori.api.log.dto.response;

import com.kkori.api.log.entity.DailyLogPhoto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record DailyLogPhotoResponse(
        String externalId,
        Long dailyLogId,
        Long petId,
        Long caregiverId,
        LocalDate date,
        String mediumUrl,
        String thumbnailUrl,
        Integer sortOrder,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static DailyLogPhotoResponse from(DailyLogPhoto photo) {
        return new DailyLogPhotoResponse(
                photo.getExternalId(),
                photo.getDailyLogId(),
                photo.getPetId(),
                photo.getCaregiverId(),
                photo.getDate(),
                photo.getMediumUrl(),
                photo.getThumbnailUrl(),
                photo.getSortOrder(),
                photo.getCreatedAt(),
                photo.getUpdatedAt()
        );
    }
}
