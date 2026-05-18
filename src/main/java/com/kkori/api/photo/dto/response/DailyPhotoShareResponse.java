package com.kkori.api.photo.dto.response;

import com.kkori.api.pet.entity.Pet;
import com.kkori.api.photo.entity.DailyPhoto;

import java.time.LocalDate;

public record DailyPhotoShareResponse(
        String petName,
        LocalDate date,
        String caption,
        String mediumUrl
) {
    public static DailyPhotoShareResponse of(DailyPhoto photo, Pet pet) {
        return new DailyPhotoShareResponse(
                pet.getName(),
                photo.getDate(),
                photo.getCaption(),
                photo.getMediumUrl()
        );
    }
}
