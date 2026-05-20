package com.kkori.api.pet.service;

import com.kkori.api.auth.context.AuthContext;
import com.kkori.api.auth.context.AuthenticatedUser;
import com.kkori.api.device.repository.DeviceRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
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
}
