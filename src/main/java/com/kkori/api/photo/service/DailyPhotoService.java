package com.kkori.api.photo.service;

import com.kkori.api.caregiver.entity.Caregiver;
import com.kkori.api.caregiver.repository.CaregiverRepository;
import com.kkori.api.common.exception.BusinessException;
import com.kkori.api.common.exception.ErrorCode;
import com.kkori.api.device.entity.Device;
import com.kkori.api.device.repository.DeviceRepository;
import com.kkori.api.pet.entity.Pet;
import com.kkori.api.pet.repository.PetRepository;
import com.kkori.api.photo.dto.request.CreateDailyPhotoRequest;
import com.kkori.api.photo.dto.request.UpdateDailyPhotoRequest;
import com.kkori.api.photo.dto.response.DailyPhotoResponse;
import com.kkori.api.photo.entity.DailyPhoto;
import com.kkori.api.photo.repository.DailyPhotoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DailyPhotoService {

    private final DailyPhotoRepository dailyPhotoRepository;
    private final PetRepository petRepository;
    private final CaregiverRepository caregiverRepository;
    private final DeviceRepository deviceRepository;

    @Transactional
    public DailyPhotoResponse create(String deviceExternalId, CreateDailyPhotoRequest request) {
        Device device = resolveDevice(deviceExternalId);
        Pet pet = petRepository.findByExternalId(request.petExternalId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PET_001));
        verifyPetOwnership(pet, device.getId());

        Caregiver caregiver = caregiverRepository.findByExternalId(request.caregiverExternalId())
                .orElseThrow(() -> new BusinessException(ErrorCode.CAREGIVER_001));

        if (dailyPhotoRepository.existsByPetIdAndDate(pet.getId(), request.date())) {
            throw new BusinessException(ErrorCode.PHOTO_002);
        }

        String externalId = resolveExternalId(request.externalId());
        if (request.externalId() != null && !request.externalId().isBlank()
                && dailyPhotoRepository.existsByExternalId(externalId)) {
            throw new BusinessException(ErrorCode.PHOTO_003);
        }

        DailyPhoto photo = DailyPhoto.builder()
                .externalId(externalId)
                .petId(pet.getId())
                .caregiverId(caregiver.getId())
                .date(request.date())
                .caption(request.caption())
                .build();

        return DailyPhotoResponse.from(dailyPhotoRepository.save(photo));
    }

    public List<DailyPhotoResponse> findByPet(String deviceExternalId, String petExternalId) {
        Device device = resolveDevice(deviceExternalId);
        Pet pet = petRepository.findByExternalId(petExternalId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PET_001));
        verifyPetOwnership(pet, device.getId());

        return dailyPhotoRepository.findByPetId(pet.getId()).stream()
                .map(DailyPhotoResponse::from)
                .toList();
    }

    public DailyPhotoResponse findByExternalId(String deviceExternalId, String externalId) {
        Device device = resolveDevice(deviceExternalId);
        DailyPhoto photo = dailyPhotoRepository.findByExternalId(externalId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PHOTO_001));
        verifyPetOwnershipById(photo.getPetId(), device.getId());
        return DailyPhotoResponse.from(photo);
    }

    @Transactional
    public DailyPhotoResponse update(String deviceExternalId, String externalId, UpdateDailyPhotoRequest request) {
        Device device = resolveDevice(deviceExternalId);
        DailyPhoto photo = dailyPhotoRepository.findByExternalId(externalId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PHOTO_001));
        verifyPetOwnershipById(photo.getPetId(), device.getId());
        photo.update(request.caption());
        return DailyPhotoResponse.from(photo);
    }

    @Transactional
    public void delete(String deviceExternalId, String externalId) {
        Device device = resolveDevice(deviceExternalId);
        DailyPhoto photo = dailyPhotoRepository.findByExternalId(externalId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PHOTO_001));
        verifyPetOwnershipById(photo.getPetId(), device.getId());
        dailyPhotoRepository.delete(photo);
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
