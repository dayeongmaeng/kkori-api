package com.kkori.api.log.dto.request;

import com.kkori.api.log.entity.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.math.BigDecimal;

public record UpdateDailyLogRequest(
        MealAmount meal,
        WaterAmount water,
        @Min(0) Integer walkMinutes,
        StoolCondition pooCondition,
        UrineColor urineColor,
        @Min(1) @Max(5) Integer condition,
        BigDecimal weightKg,
        String memo,
        String mealNote,
        String walkNote,
        String pooNote,
        String urineNote,
        String waterNote,
        @Min(0) Integer playMinutes,
        String playNote,
        UrineAmount urineAmount,
        @Min(0) Integer vomitCount,
        String vomitNote
) {}
