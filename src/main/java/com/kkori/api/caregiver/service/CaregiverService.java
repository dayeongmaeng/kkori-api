package com.kkori.api.caregiver.service;

import com.kkori.api.caregiver.dto.request.CreateCaregiverRequest;
import com.kkori.api.caregiver.dto.request.UpdateCaregiverRequest;
import com.kkori.api.caregiver.dto.response.CaregiverResponse;
import com.kkori.api.caregiver.entity.Caregiver;
import com.kkori.api.caregiver.repository.CaregiverRepository;
import com.kkori.api.common.exception.BusinessException;
import com.kkori.api.common.exception.ErrorCode;
import com.kkori.api.device.entity.Device;
import com.kkori.api.device.repository.DeviceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CaregiverService {

    private final CaregiverRepository caregiverRepository;
    private final DeviceRepository deviceRepository;

    @Transactional
    public CaregiverResponse create(String deviceExternalId, CreateCaregiverRequest request) {
        Device device = deviceRepository.findByExternalId(deviceExternalId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEVICE_002));

        String externalId = resolveExternalId(request.externalId());
        if (request.externalId() != null && !request.externalId().isBlank()
                && caregiverRepository.existsByExternalIdAndDeviceId(externalId, device.getId())) {
            throw new BusinessException(ErrorCode.CAREGIVER_002);
        }

        Caregiver caregiver = Caregiver.builder()
                .externalId(externalId)
                .deviceId(device.getId())
                .name(request.name())
                .role(request.role())
                .color(request.color())
                .build();

        return CaregiverResponse.from(caregiverRepository.save(caregiver));
    }

    public List<CaregiverResponse> findAll(String deviceExternalId) {
        Device device = deviceRepository.findByExternalId(deviceExternalId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEVICE_002));

        return caregiverRepository.findByDeviceId(device.getId()).stream()
                .map(CaregiverResponse::from)
                .toList();
    }

    public CaregiverResponse findByExternalId(String externalId) {
        return caregiverRepository.findByExternalId(externalId)
                .map(CaregiverResponse::from)
                .orElseThrow(() -> new BusinessException(ErrorCode.CAREGIVER_001));
    }

    @Transactional
    public CaregiverResponse update(String externalId, UpdateCaregiverRequest request) {
        Caregiver caregiver = caregiverRepository.findByExternalId(externalId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CAREGIVER_001));
        caregiver.update(request.name(), request.role(), request.color());
        return CaregiverResponse.from(caregiver);
    }

    @Transactional
    public void delete(String externalId) {
        Caregiver caregiver = caregiverRepository.findByExternalId(externalId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CAREGIVER_001));
        caregiverRepository.delete(caregiver);
    }

    private String resolveExternalId(String externalId) {
        return (externalId == null || externalId.isBlank())
                ? UUID.randomUUID().toString()
                : externalId;
    }
}
