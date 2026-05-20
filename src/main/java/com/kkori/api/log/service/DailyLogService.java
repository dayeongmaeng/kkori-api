package com.kkori.api.log.service;

import com.kkori.api.auth.context.AuthContext;
import com.kkori.api.auth.context.AuthenticatedUser;
import com.kkori.api.caregiver.entity.Caregiver;
import com.kkori.api.caregiver.repository.CaregiverRepository;
import com.kkori.api.common.exception.BusinessException;
import com.kkori.api.common.exception.ErrorCode;
import com.kkori.api.device.entity.Device;
import com.kkori.api.device.repository.DeviceRepository;
import com.kkori.api.log.dto.request.CreateDailyLogRequest;
import com.kkori.api.log.dto.request.UpdateDailyLogRequest;
import com.kkori.api.log.dto.response.DailyLogPhotoResponse;
import com.kkori.api.log.dto.response.DailyLogResponse;
import com.kkori.api.log.entity.DailyLog;
import com.kkori.api.log.entity.DailyLogPhoto;
import com.kkori.api.log.repository.DailyLogPhotoRepository;
import com.kkori.api.log.repository.DailyLogRepository;
import com.kkori.api.pet.entity.Pet;
import com.kkori.api.pet.repository.PetRepository;
import com.kkori.api.photo.storage.S3PhotoStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DailyLogService {

    private static final int MAX_PHOTOS_PER_LOG = 3;

    private final DailyLogRepository dailyLogRepository;
    private final DailyLogPhotoRepository dailyLogPhotoRepository;
    private final PetRepository petRepository;
    private final CaregiverRepository caregiverRepository;
    private final DeviceRepository deviceRepository;
    private final S3PhotoStorage s3PhotoStorage;

    @Transactional
    public DailyLogResponse create(String deviceExternalId, CreateDailyLogRequest request) {
        Device device = resolveDeviceForRequest(deviceExternalId);
        Pet pet = petRepository.findByExternalId(request.petExternalId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PET_001));
        verifyPetOwnership(pet, device);

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

        return toResponse(dailyLogRepository.save(log));
    }

    public List<DailyLogResponse> findByPet(String deviceExternalId, String petExternalId) {
        Device device = resolveDeviceForRequest(deviceExternalId);
        Pet pet = petRepository.findByExternalId(petExternalId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PET_001));
        verifyPetOwnership(pet, device);

        return dailyLogRepository.findByPetId(pet.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    public DailyLogResponse findByExternalId(String deviceExternalId, String externalId) {
        Device device = resolveDeviceForRequest(deviceExternalId);
        DailyLog log = dailyLogRepository.findByExternalId(externalId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LOG_001));
        verifyPetOwnershipById(log.getPetId(), device);
        return toResponse(log);
    }

    @Transactional
    public DailyLogResponse update(String deviceExternalId, String externalId, UpdateDailyLogRequest request) {
        Device device = resolveDeviceForRequest(deviceExternalId);
        DailyLog log = dailyLogRepository.findByExternalId(externalId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LOG_001));
        verifyPetOwnershipById(log.getPetId(), device);
        log.update(request.meal(), request.water(), request.walkMinutes(),
                request.pooCondition(), request.urineColor(),
                request.condition(), request.weightKg(), request.memo());
        return toResponse(log);
    }

    @Transactional
    public DailyLogPhotoResponse uploadPhoto(String deviceExternalId, String externalId,
                                             MultipartFile medium, MultipartFile thumbnail) {
        Device device = resolveDeviceForRequest(deviceExternalId);
        DailyLog log = dailyLogRepository.findByExternalId(externalId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LOG_001));
        verifyPetOwnershipById(log.getPetId(), device);

        List<DailyLogPhoto> currentPhotos = dailyLogPhotoRepository.findByDailyLogIdOrderBySortOrderAscIdAsc(log.getId());
        if (currentPhotos.size() >= MAX_PHOTOS_PER_LOG) {
            throw new BusinessException(ErrorCode.LOG_PHOTO_002);
        }

        Pet pet = petRepository.findById(log.getPetId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PET_001));

        String photoExternalId = UUID.randomUUID().toString();
        String mediumUrl = s3PhotoStorage.uploadMedium(pet.getExternalId(), photoExternalId, medium);
        String thumbnailUrl = s3PhotoStorage.uploadThumbnail(pet.getExternalId(), photoExternalId, thumbnail);

        DailyLogPhoto photo = DailyLogPhoto.builder()
                .externalId(photoExternalId)
                .dailyLogId(log.getId())
                .petId(log.getPetId())
                .caregiverId(log.getCaregiverId())
                .date(log.getDate())
                .mediumUrl(mediumUrl)
                .thumbnailUrl(thumbnailUrl)
                .sortOrder(resolveNextSortOrder(currentPhotos))
                .build();
        return DailyLogPhotoResponse.from(dailyLogPhotoRepository.save(photo));
    }

    @Transactional
    public void deletePhoto(String deviceExternalId, String externalId, String photoExternalId) {
        Device device = resolveDeviceForRequest(deviceExternalId);
        DailyLog log = dailyLogRepository.findByExternalId(externalId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LOG_001));
        verifyPetOwnershipById(log.getPetId(), device);

        DailyLogPhoto photo = dailyLogPhotoRepository.findByExternalId(photoExternalId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LOG_PHOTO_001));
        if (!photo.getDailyLogId().equals(log.getId())) {
            throw new BusinessException(ErrorCode.LOG_PHOTO_001);
        }

        Pet pet = petRepository.findById(photo.getPetId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PET_001));
        s3PhotoStorage.delete(pet.getExternalId(), photo.getExternalId());
        dailyLogPhotoRepository.delete(photo);
    }

    @Transactional
    public void delete(String deviceExternalId, String externalId) {
        Device device = resolveDeviceForRequest(deviceExternalId);
        DailyLog log = dailyLogRepository.findByExternalId(externalId)
                .orElseThrow(() -> new BusinessException(ErrorCode.LOG_001));
        verifyPetOwnershipById(log.getPetId(), device);

        Pet pet = petRepository.findById(log.getPetId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PET_001));
        dailyLogPhotoRepository.findByDailyLogIdOrderBySortOrderAscIdAsc(log.getId())
                .forEach(photo -> s3PhotoStorage.delete(pet.getExternalId(), photo.getExternalId()));
        dailyLogPhotoRepository.deleteByDailyLogId(log.getId());
        dailyLogRepository.delete(log);
    }

    private DailyLogResponse toResponse(DailyLog log) {
        List<DailyLogPhotoResponse> photos = dailyLogPhotoRepository
                .findByDailyLogIdOrderBySortOrderAscIdAsc(log.getId())
                .stream()
                .map(DailyLogPhotoResponse::from)
                .toList();
        return DailyLogResponse.from(log, photos);
    }

    private int resolveNextSortOrder(List<DailyLogPhoto> currentPhotos) {
        return currentPhotos.stream()
                .mapToInt(DailyLogPhoto::getSortOrder)
                .max()
                .orElse(-1) + 1;
    }

    private Device resolveDeviceForRequest(String deviceExternalId) {
        if (AuthContext.currentUser().isPresent()) {
            return resolveDevice(deviceExternalId).orElse(null);
        }
        return resolveDevice(deviceExternalId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DEVICE_002));
    }

    private Optional<Device> resolveDevice(String deviceExternalId) {
        if (deviceExternalId == null || deviceExternalId.isBlank()) {
            return Optional.empty();
        }
        return deviceRepository.findByExternalId(deviceExternalId);
    }

    private void verifyPetOwnership(Pet pet, Device device) {
        Optional<AuthenticatedUser> currentUser = AuthContext.currentUser();
        if (currentUser.isPresent() && pet.getUserId() != null && pet.getUserId().equals(currentUser.get().userId())) {
            return;
        }
        if (device != null && device.getUserId() != null && pet.getUserId() != null && pet.getUserId().equals(device.getUserId())) {
            return;
        }
        if (device == null || pet.getDeviceId() == null || !pet.getDeviceId().equals(device.getId())) {
            throw new BusinessException(ErrorCode.PET_001);
        }
    }

    private void verifyPetOwnershipById(Long petId, Device device) {
        Pet pet = petRepository.findById(petId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PET_001));
        verifyPetOwnership(pet, device);
    }

    private String resolveExternalId(String externalId) {
        return (externalId == null || externalId.isBlank())
                ? UUID.randomUUID().toString()
                : externalId;
    }
}
