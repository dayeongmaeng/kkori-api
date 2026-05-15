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
        List<String> photoBase64List,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static DailyLogResponse from(DailyLog log) {
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
                log.getPhotoBase64List(),
                log.getCreatedAt(),
                log.getUpdatedAt()
        );
    }
}
