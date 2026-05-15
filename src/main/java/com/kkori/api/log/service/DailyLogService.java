package com.kkori.api.log.service;

import com.kkori.api.caregiver.entity.Caregiver;
import com.kkori.api.caregiver.repository.CaregiverRepository;
import com.kkori.api.common.exception.BusinessException;
import com.kkori.api.common.exception.ErrorCode;
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

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DailyLogService {

    private final DailyLogRepository dailyLogRepository;
    private final PetRepository petRepository;
    private final CaregiverRepository caregiverRepository;

    @Transactional
    public DailyLogResponse create(CreateDailyLogRequest request) {
        Pet pet = petRepository.findByExternalId(request.petExternalId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PET_001));
        Caregiver caregiver = caregiverRepository.findByExternalId(request.caregiverExternalId())
                .orElseThrow(() -> new BusinessException(ErrorCode.CAREGIVER_001));

        if (dailyLogRepository.existsByPetIdAndDate(pet.getId(), request.date())) {
            throw new BusinessException(ErrorCode.LOG_002);
        }

        DailyLog log = DailyLog.builder()
                .externalId(request.externalId())
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
                .photoBase64List(request.photoBase64List())
                .build();

        return DailyLogResponse.from(dailyLogRepository.save(log));
    }

    public List<DailyLogResponse> findByPet(String petExternalId) {
        Pet pet = petRepository.findByExternalId(petExternalId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PET_001));
        return dailyLogRepository.findByPetId(pet.getId()).stream()
                .map(DailyLogResponse::from)
                .toList();
    }

    public DailyLogResponse findByExternalId(String externalId) {
        return dailyLogRepository.findByExternalId(externalId)
                .map(DailyLogResponse::from)
                .orElseThrow(() -> new BusinessException(ErrorCode.LOG_001));
    }

    @Transactional
    public DailyLogResponse update(String externalId, UpdateDailyLogRequest request) {
        DailyLog log = dailyLogRepository.findByExternalId(externalId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LOG_001));
        log.update(request.meal(), request.water(), request.walkMinutes(),
                request.pooCondition(), request.urineColor(),
                request.condition(), request.weightKg(), request.memo(),
                request.photoBase64List());
        return DailyLogResponse.from(log);
    }

    @Transactional
    public void delete(String externalId) {
        DailyLog log = dailyLogRepository.findByExternalId(externalId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LOG_001));
        dailyLogRepository.delete(log);
    }
}
