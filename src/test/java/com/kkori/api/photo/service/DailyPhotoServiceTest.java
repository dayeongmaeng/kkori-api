package com.kkori.api.photo.service;

import com.kkori.api.caregiver.repository.CaregiverRepository;
import com.kkori.api.common.exception.BusinessException;
import com.kkori.api.common.exception.ErrorCode;
import com.kkori.api.device.entity.Device;
import com.kkori.api.device.entity.Platform;
import com.kkori.api.device.repository.DeviceRepository;
import com.kkori.api.pet.entity.Pet;
import com.kkori.api.pet.entity.Species;
import com.kkori.api.pet.repository.PetRepository;
import com.kkori.api.photo.dto.request.UpdateDailyPhotoRequest;
import com.kkori.api.photo.dto.response.DailyPhotoResponse;
import com.kkori.api.photo.dto.response.DailyPhotoShareResponse;
import com.kkori.api.photo.entity.DailyPhoto;
import com.kkori.api.photo.repository.DailyPhotoRepository;
import com.kkori.api.photo.storage.S3PhotoStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DailyPhotoServiceTest {

    @Mock
    private DailyPhotoRepository dailyPhotoRepository;

    @Mock
    private PetRepository petRepository;

    @Mock
    private CaregiverRepository caregiverRepository;

    @Mock
    private DeviceRepository deviceRepository;

    @Mock
    private S3PhotoStorage s3PhotoStorage;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private DailyPhotoService dailyPhotoService;

    @BeforeEach
    void setUp() {
        dailyPhotoService = new DailyPhotoService(
                dailyPhotoRepository,
                petRepository,
                caregiverRepository,
                deviceRepository,
                s3PhotoStorage,
                eventPublisher
        );
    }

    @Test
    void updateChangesOnlyCaptionAndKeepsPhotoUrls() {
        Device device = device(1L, "device-1");
        Pet pet = pet(10L, 1L, "pet-1", "꼬리");
        DailyPhoto photo = photo(100L, "photo-1", 10L, "before");
        photo.updateUrls("https://cdn.example.com/medium.jpg", "https://cdn.example.com/thumb.jpg");
        when(deviceRepository.findByExternalId("device-1")).thenReturn(Optional.of(device));
        when(dailyPhotoRepository.findByExternalIdAndDeletedAtIsNull("photo-1")).thenReturn(Optional.of(photo));
        when(petRepository.findById(10L)).thenReturn(Optional.of(pet));

        DailyPhotoResponse response = dailyPhotoService.update(
                "device-1",
                "photo-1",
                new UpdateDailyPhotoRequest("after")
        );

        assertThat(response.caption()).isEqualTo("after");
        assertThat(response.mediumUrl()).isEqualTo("https://cdn.example.com/medium.jpg");
        assertThat(response.thumbnailUrl()).isEqualTo("https://cdn.example.com/thumb.jpg");
    }

    @Test
    void updateByOtherDeviceFailsAsNotFound() {
        Device otherDevice = device(2L, "device-2");
        Pet pet = pet(10L, 1L, "pet-1", "꼬리");
        DailyPhoto photo = photo(100L, "photo-1", 10L, "before");
        when(deviceRepository.findByExternalId("device-2")).thenReturn(Optional.of(otherDevice));
        when(dailyPhotoRepository.findByExternalIdAndDeletedAtIsNull("photo-1")).thenReturn(Optional.of(photo));
        when(petRepository.findById(10L)).thenReturn(Optional.of(pet));

        assertThatThrownBy(() -> dailyPhotoService.update(
                "device-2",
                "photo-1",
                new UpdateDailyPhotoRequest("after")
        ))
                .isInstanceOfSatisfying(BusinessException.class, e ->
                        assertThat(e.getErrorCode().getHttpStatus().value()).isEqualTo(404));
    }

    @Test
    void findShareReturnsWebShareData() {
        Pet pet = pet(10L, 1L, "pet-1", "꼬리");
        DailyPhoto photo = photo(100L, "photo-1", 10L, "caption");
        photo.updateUrls("https://cdn.example.com/medium.jpg", "https://cdn.example.com/thumb.jpg");
        when(dailyPhotoRepository.findByExternalIdAndDeletedAtIsNull("photo-1")).thenReturn(Optional.of(photo));
        when(petRepository.findById(10L)).thenReturn(Optional.of(pet));

        DailyPhotoShareResponse response = dailyPhotoService.findShareByExternalId("photo-1");

        assertThat(response.petName()).isEqualTo("꼬리");
        assertThat(response.date()).isEqualTo(LocalDate.of(2026, 5, 18));
        assertThat(response.caption()).isEqualTo("caption");
        assertThat(response.mediumUrl()).isEqualTo("https://cdn.example.com/medium.jpg");
    }

    private Device device(Long id, String externalId) {
        Device device = Device.builder()
                .externalId(externalId)
                .platform(Platform.IOS)
                .build();
        ReflectionTestUtils.setField(device, "id", id);
        return device;
    }

    private Pet pet(Long id, Long deviceId, String externalId, String name) {
        Pet pet = Pet.builder()
                .externalId(externalId)
                .deviceId(deviceId)
                .name(name)
                .species(Species.DOG)
                .build();
        ReflectionTestUtils.setField(pet, "id", id);
        return pet;
    }

    private DailyPhoto photo(Long id, String externalId, Long petId, String caption) {
        DailyPhoto photo = DailyPhoto.builder()
                .externalId(externalId)
                .petId(petId)
                .caregiverId(1L)
                .date(LocalDate.of(2026, 5, 18))
                .caption(caption)
                .build();
        ReflectionTestUtils.setField(photo, "id", id);
        return photo;
    }
}
