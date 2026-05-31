package com.kkori.api.log.dto.response;

import com.kkori.api.log.entity.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record DailyLogResponse(
        String externalId,
        Long petId,
        Long caregiverId,
        LocalDate date,
        MealAmount meal,
        WaterAmount water,
        Integer walkMinutes,
        StoolCondition pooCondition,
        UrineColor urineColor,
        Integer condition,
        BigDecimal weightKg,
        String memo,
        String mealNote,
        String walkNote,
        String pooNote,
        String urineNote,
        String waterNote,
        Integer playMinutes,
        String playNote,
        UrineAmount urineAmount,
        Integer vomitCount,
        String vomitNote,
        List<DailyLogPhotoResponse> photos,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static DailyLogResponse from(DailyLog log) {
        return from(log, List.of());
    }

    public static DailyLogResponse from(DailyLog log, List<DailyLogPhotoResponse> photos) {
        return new DailyLogResponse(
                log.getExternalId(),
                log.getPetId(),
                log.getCaregiverId(),
                log.getDate(),
                log.getMeal(),
                log.getWater(),
                log.getWalkMinutes(),
                log.getPooCondition(),
                log.getUrineColor(),
                log.getCondition(),
                log.getWeightKg(),
                log.getMemo(),
                log.getMealNote(),
                log.getWalkNote(),
                log.getPooNote(),
                log.getUrineNote(),
                log.getWaterNote(),
                log.getPlayMinutes(),
                log.getPlayNote(),
                log.getUrineAmount(),
                log.getVomitCount(),
                log.getVomitNote(),
                photos,
                log.getCreatedAt(),
                log.getUpdatedAt()
        );
    }
}
