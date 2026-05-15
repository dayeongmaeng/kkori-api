package com.kkori.api.device.dto.response;

import com.kkori.api.device.entity.Device;
import com.kkori.api.device.entity.Platform;

import java.time.LocalDateTime;

public record DeviceResponse(
        String externalId,
        Platform platform,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static DeviceResponse from(Device device) {
        return new DeviceResponse(
                device.getExternalId(),
                device.getPlatform(),
                device.getCreatedAt(),
                device.getUpdatedAt()
        );
    }
}
