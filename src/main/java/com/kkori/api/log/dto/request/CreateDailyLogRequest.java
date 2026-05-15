package com.kkori.api.log.dto.request;

import com.kkori.api.log.entity.*;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CreateDailyLogRequest(
        @NotBlank String externalId,
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
        List<String> photoBase64List
) {}
