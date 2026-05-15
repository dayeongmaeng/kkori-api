package com.kkori.api.photo.service;

import com.kkori.api.caregiver.entity.Caregiver;
import com.kkori.api.caregiver.repository.CaregiverRepository;
import com.kkori.api.common.exception.BusinessException;
import com.kkori.api.common.exception.ErrorCode;
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

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DailyPhotoService {

    private final DailyPhotoRepository dailyPhotoRepository;
    private final PetRepository petRepository;
    private final CaregiverRepository caregiverRepository;

    @Transactional
    public DailyPhotoResponse create(CreateDailyPhotoRequest request) {
        Pet pet = petRepository.findByExternalId(request.petExternalId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PET_001));
        Caregiver caregiver = caregiverRepository.findByExternalId(request.caregiverExternalId())
                .orElseThrow(() -> new BusinessException(ErrorCode.CAREGIVER_001));

        if (dailyPhotoRepository.existsByPetIdAndDate(pet.getId(), request.date())) {
            throw new BusinessException(ErrorCode.PHOTO_002);
        }

        DailyPhoto photo = DailyPhoto.builder()
                .externalId(request.externalId())
                .petId(pet.getId())
                .caregiverId(caregiver.getId())
                .date(request.date())
                .photoBase64(request.photoBase64())
                .caption(request.caption())
                .build();

        return DailyPhotoResponse.from(dailyPhotoRepository.save(photo));
    }

    public List<DailyPhotoResponse> findByPet(String petExternalId) {
        Pet pet = petRepository.findByExternalId(petExternalId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PET_001));
        return dailyPhotoRepository.findByPetId(pet.getId()).stream()
                .map(DailyPhotoResponse::from)
                .toList();
    }

    public DailyPhotoResponse findByExternalId(String externalId) {
        return dailyPhotoRepository.findByExternalId(externalId)
                .map(DailyPhotoResponse::from)
                .orElseThrow(() -> new BusinessException(ErrorCode.PHOTO_001));
    }

    @Transactional
    public DailyPhotoResponse update(String externalId, UpdateDailyPhotoRequest request) {
        DailyPhoto photo = dailyPhotoRepository.findByExternalId(externalId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PHOTO_001));
        photo.update(request.photoBase64(), request.caption());
        return DailyPhotoResponse.from(photo);
    }

    @Transactional
    public void delete(String externalId) {
        DailyPhoto photo = dailyPhotoRepository.findByExternalId(externalId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PHOTO_001));
        dailyPhotoRepository.delete(photo);
    }
}
