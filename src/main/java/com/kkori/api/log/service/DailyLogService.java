package com.kkori.api.log.service;

import com.kkori.api.caregiver.entity.Caregiver;
import com.kkori.api.caregiver.repository.CaregiverRepository;
import com.kkori.api.common.exception.BusinessException;
import com.kkori.api.common.exception.ErrorCode;
import com.kkori.api.device.entity.Device;
import com.kkori.api.device.repository.DeviceRepository;
import com.kkori.api.log.dto.request.CreateDailyLogRequest;
import com.kkori.api.log.dto.request.UpdateDailyLogRequest;
import com.kkori.api.log.dto.response.DailyLogResponse;
import com.kkori.api.log.entity.DailyLog;
import com.kkori.api.log.repository.DailyLogRepository;
import com.kkori.api.pet.entity.Pet;
import com.kkori.api.pet.repository.PetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DailyLogService {

    private final DailyLogRepository dailyLogRepository;
    private final PetRepository petRepository;
    private final CaregiverRepository caregiverRepository;
    private final DeviceRepository deviceRepository;

    @Transactional
    public DailyLogResponse create(String deviceExternalId, CreateDailyLogRequest request) {
        Device device = resolveDevice(deviceExternalId);
        Pet pet = petRepository.findByExternalId(request.petExternalId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PET_001));
        verifyPetOwnership(pet, device.getId());

        Caregiver caregiver = caregiverRepository.findByExternalId(request.caregiverExternalId())
                .orElseThrow(() -> new BusinessException(ErrorCode.CAREGIVER_001));

        if (dailyLogRepository.existsByPetIdAndDate(pet.getId(), request.date())) {
            throw new BusinessException(ErrorCode.LOG_002);
        }

        String externalId = resolveExternalId(request.externalId());
        if (request.externalId() != null && !request.externalId().isBlank()
                && dailyLogRepository.existsByExternalId(externalId)) {
            throw new BusinessException(ErrorCode.LOG_003);
        }

        DailyLog log = DailyLog.builder()
                .externalId(externalId)
                .petId(pet.getId())
                .caregiverId(caregiver.getId())
                .date(request.date())
                .meal(request.meal())
                .water(request.water())
                .walkMinutes(request.walkMinutes())
                .pooCondition(request.pooCondition())
                .urineColor(request.urineColor())
                .condition(request.condition())
                .weightKg(request.weightKg())
                .memo(request.memo())
                .build();

        return DailyLogResponse.from(dailyLogRepository.save(log));
    }

    public List<DailyLogResponse> findByPet(String deviceExternalId, String petExternalId) {
        Device device = resolveDevice(deviceExternalId);
        Pet pet = petRepository.findByExternalId(petExternalId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PET_001));
        verifyPetOwnership(pet, device.getId());

        return dailyLogRepository.findByPetId(pet.getId()).stream()
                .map(DailyLogResponse::from)
                .toList();
    }

    public DailyLogResponse findByExternalId(String deviceExternalId, String externalId) {
        Device device = resolveDevice(deviceExternalId);
        DailyLog log = dailyLogRepository.findByExternalId(externalId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LOG_001));
        verifyPetOwnershipById(log.getPetId(), device.getId());
        return DailyLogResponse.from(log);
    }

    @Transactional
    public DailyLogResponse update(String deviceExternalId, String externalId, UpdateDailyLogRequest request) {
        Device device = resolveDevice(deviceExternalId);
        DailyLog log = dailyLogRepository.findByExternalId(externalId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LOG_001));
        verifyPetOwnershipById(log.getPetId(), device.getId());
        log.update(request.meal(), request.water(), request.walkMinutes(),
                request.pooCondition(), request.urineColor(),
                request.condition(), request.weightKg(), request.memo());
        return DailyLogResponse.from(log);
    }

    @Transactional
    public void delete(String deviceExternalId, String externalId) {
        Device device = resolveDevice(deviceExternalId);
        DailyLog log = dailyLogRepository.findByExternalId(externalId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LOG_001));
        verifyPetOwnershipById(log.getPetId(), device.getId());
        dailyLogRepository.delete(log);
    }

    private Device resolveDevice(String deviceExternalId) {
        return deviceRepository.findByExternalId(deviceExternalId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEVICE_002));
    }

    private void verifyPetOwnership(Pet pet, Long deviceId) {
        if (!pet.getDeviceId().equals(deviceId)) {
            throw new BusinessException(ErrorCode.PET_001);
        }
    }

    private void verifyPetOwnershipById(Long petId, Long deviceId) {
        Pet pet = petRepository.findById(petId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PET_001));
        verifyPetOwnership(pet, deviceId);
    }

    private String resolveExternalId(String externalId) {
        return (externalId == null || externalId.isBlank())
                ? UUID.randomUUID().toString()
                : externalId;
    }
}
