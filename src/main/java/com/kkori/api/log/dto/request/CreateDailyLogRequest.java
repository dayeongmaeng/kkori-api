package com.kkori.api.log.dto.request;

import com.kkori.api.log.entity.*;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateDailyLogRequest(
        @Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$") String externalId,
        @NotBlank String petExternalId,
        @NotBlank String caregiverExternalId,
        @NotNull LocalDate date,
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
