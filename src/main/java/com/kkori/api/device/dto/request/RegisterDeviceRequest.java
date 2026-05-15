package com.kkori.api.device.dto.request;

import com.kkori.api.device.entity.Platform;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RegisterDeviceRequest(
        @NotBlank String externalId,
        @NotNull Platform platform
) {}
