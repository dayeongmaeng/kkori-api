package com.kkori.api.pet.service;

import com.kkori.api.auth.context.AuthContext;
import com.kkori.api.auth.context.AuthenticatedUser;
import com.kkori.api.device.entity.Device;
import com.kkori.api.device.entity.Platform;
import com.kkori.api.device.repository.DeviceRepository;
import com.kkori.api.common.exception.BusinessException;
import com.kkori.api.pet.dto.response.PetResponse;
import com.kkori.api.pet.entity.Pet;
import com.kkori.api.pet.entity.Species;
import com.kkori.api.pet.repository.PetRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PetServiceTest {

    @Mock
    private PetRepository petRepository;

    @Mock
    private DeviceRepository deviceRepository;

    private PetService petService;

    @BeforeEach
    void setUp() {
        petService = new PetService(petRepository, deviceRepository);
    }

    @AfterEach
    void tearDown() {
        AuthContext.clear();
    }

    @Test
    void findAllUsesAuthenticatedUserBeforeDeviceFallback() {
        AuthContext.set(new AuthenticatedUser(1L, "user-1"));
        Pet pet = pet(10L, 1L, "pet-1");
        when(petRepository.findByUserId(1L)).thenReturn(List.of(pet));

        List<PetResponse> response = petService.findAll(null);

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().externalId()).isEqualTo("pet-1");
        verifyNoInteractions(deviceRepository);
    }

    @Test
    void authenticatedUserCannotAccessOtherUsersPetThroughSameDeviceFallback() {
        AuthContext.set(new AuthenticatedUser(2L, "user-2"));
        Device device = device(1L, "device-1", 2L);
        when(deviceRepository.findByExternalId("device-1")).thenReturn(Optional.of(device));
        when(petRepository.findByExternalIdAndUserId("pet-1", 2L)).thenReturn(Optional.empty());
        when(petRepository.findByExternalIdAndDeviceIdAndUserIdIsNull("pet-1", 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> petService.findByExternalId("device-1", "pet-1"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void authenticatedUserCanAccessLegacyDevicePetOnlyWhenPetHasNoUserId() {
        AuthContext.set(new AuthenticatedUser(1L, "user-1"));
        Device device = device(1L, "device-1", 1L);
        Pet pet = pet(10L, null, "pet-1");
        when(deviceRepository.findByExternalId("device-1")).thenReturn(Optional.of(device));
        when(petRepository.findByExternalIdAndUserId("pet-1", 1L)).thenReturn(Optional.empty());
        when(petRepository.findByExternalIdAndDeviceIdAndUserIdIsNull("pet-1", 1L)).thenReturn(Optional.of(pet));

        PetResponse response = petService.findByExternalId("device-1", "pet-1");

        assertThat(response.externalId()).isEqualTo("pet-1");
    }

    private Pet pet(Long id, Long userId, String externalId) {
        Pet pet = Pet.builder()
                .externalId(externalId)
                .userId(userId)
                .name("꼬리")
                .species(Species.DOG)
                .build();
        ReflectionTestUtils.setField(pet, "id", id);
        return pet;
    }

    private Device device(Long id, String externalId, Long userId) {
        Device device = Device.builder()
                .externalId(externalId)
                .platform(Platform.IOS)
                .userId(userId)
                .build();
        ReflectionTestUtils.setField(device, "id", id);
        return device;
    }
}
