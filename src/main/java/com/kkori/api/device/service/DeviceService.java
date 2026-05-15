package com.kkori.api.device.service;

import com.kkori.api.common.exception.BusinessException;
import com.kkori.api.common.exception.ErrorCode;
import com.kkori.api.device.dto.request.RegisterDeviceRequest;
import com.kkori.api.device.dto.response.DeviceResponse;
import com.kkori.api.device.entity.Device;
import com.kkori.api.device.repository.DeviceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DeviceService {

    private final DeviceRepository deviceRepository;

    @Transactional
    public DeviceResponse register(RegisterDeviceRequest request) {
        Device device = deviceRepository.findByExternalId(request.externalId())
                .orElseGet(() -> Device.builder()
                        .externalId(request.externalId())
                        .platform(request.platform())
                        .build());
        device.update(request.platform());
        return DeviceResponse.from(deviceRepository.save(device));
    }

    public DeviceResponse findMe(String externalId) {
        return deviceRepository.findByExternalId(externalId)
                .map(DeviceResponse::from)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEVICE_002));
    }
}
