package com.kkori.api.photo.dto.request;

import jakarta.validation.constraints.NotBlank;

public record UpdateDailyPhotoRequest(
        @NotBlank String photoBase64,
        String caption
) {}
