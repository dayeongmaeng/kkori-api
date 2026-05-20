package com.kkori.api.pet.service;

import com.kkori.api.common.exception.BusinessException;
import com.kkori.api.common.exception.ErrorCode;
import com.kkori.api.device.entity.Device;
import com.kkori.api.device.repository.DeviceRepository;
import com.kkori.api.pet.dto.request.CreatePetRequest;
import com.kkori.api.pet.dto.request.UpdatePetRequest;
import com.kkori.api.pet.dto.response.PetResponse;
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
public class PetService {

    private final PetRepository petRepository;
    private final DeviceRepository deviceRepository;

    @Transactional
    public PetResponse create(String deviceExternalId, CreatePetRequest request) {
        Device device = deviceRepository.findByExternalId(deviceExternalId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEVICE_002));

        String externalId = resolveExternalId(request.externalId());
        if (request.externalId() != null && !request.externalId().isBlank()
                && petRepository.existsByExternalId(externalId)) {
            throw new BusinessException(ErrorCode.PET_002);
        }

        Pet pet = Pet.builder()
                .externalId(externalId)
                .deviceId(device.getId())
                .name(request.name())
                .species(request.species())
                .gender(request.gender())
                .breed(request.breed())
                .birthDate(request.birthDate())
                .birthDateUnknown(request.birthDateUnknown())
                .adoptionDate(request.adoptionDate())
                .weightKg(request.weightKg())
                .neutered(request.neutered())
                .medicalNotes(request.medicalNotes())
                .photoBase64(request.photoBase64())
                .build();
        return PetResponse.from(petRepository.save(pet));
    }

    public List<PetResponse> findAll(String deviceExternalId) {
        Device device = deviceRepository.findByExternalId(deviceExternalId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEVICE_002));
        return petRepository.findByDeviceId(device.getId()).stream()
                .map(PetResponse::from)
                .toList();
    }

    public PetResponse findByExternalId(String deviceExternalId, String externalId) {
        Device device = deviceRepository.findByExternalId(deviceExternalId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEVICE_002));
        return petRepository.findByExternalIdAndDeviceId(externalId, device.getId())
                .map(PetResponse::from)
                .orElseThrow(() -> new BusinessException(ErrorCode.PET_001));
    }

    @Transactional
    public PetResponse update(String deviceExternalId, String externalId, UpdatePetRequest request) {
        Device device = deviceRepository.findByExternalId(deviceExternalId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEVICE_002));
        Pet pet = petRepository.findByExternalIdAndDeviceId(externalId, device.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PET_001));
        pet.update(request.name(), request.species(), request.gender(), request.breed(), request.birthDate(),
                request.birthDateUnknown(), request.adoptionDate(),
                request.weightKg(), request.neutered(), request.medicalNotes(), request.photoBase64());
        return PetResponse.from(pet);
    }

    @Transactional
    public void delete(String deviceExternalId, String externalId) {
        Device device = deviceRepository.findByExternalId(deviceExternalId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEVICE_002));
        Pet pet = petRepository.findByExternalIdAndDeviceId(externalId, device.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PET_001));
        petRepository.delete(pet);
    }

    private String resolveExternalId(String externalId) {
        return (externalId == null || externalId.isBlank())
                ? UUID.randomUUID().toString()
                : externalId;
    }
}
