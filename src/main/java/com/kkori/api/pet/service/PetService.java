package com.kkori.api.pet.service;

import com.kkori.api.common.exception.BusinessException;
import com.kkori.api.common.exception.ErrorCode;
import com.kkori.api.pet.dto.request.CreatePetRequest;
import com.kkori.api.pet.dto.request.UpdatePetRequest;
import com.kkori.api.pet.dto.response.PetResponse;
import com.kkori.api.pet.entity.Pet;
import com.kkori.api.pet.repository.PetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PetService {

    private final PetRepository petRepository;

    @Transactional
    public PetResponse create(CreatePetRequest request) {
        Pet pet = Pet.builder()
                .externalId(request.externalId())
                .name(request.name())
                .species(request.species())
                .breed(request.breed())
                .birthDate(request.birthDate())
                .weightKg(request.weightKg())
                .neutered(request.neutered())
                .medicalNotes(request.medicalNotes())
                .photoBase64(request.photoBase64())
                .build();
        return PetResponse.from(petRepository.save(pet));
    }

    public List<PetResponse> findAll() {
        return petRepository.findAll().stream()
                .map(PetResponse::from)
                .toList();
    }

    public PetResponse findByExternalId(String externalId) {
        return petRepository.findByExternalId(externalId)
                .map(PetResponse::from)
                .orElseThrow(() -> new BusinessException(ErrorCode.PET_001));
    }

    @Transactional
    public PetResponse update(String externalId, UpdatePetRequest request) {
        Pet pet = petRepository.findByExternalId(externalId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PET_001));
        pet.update(request.name(), request.species(), request.breed(), request.birthDate(),
                request.weightKg(), request.neutered(), request.medicalNotes(), request.photoBase64());
        return PetResponse.from(pet);
    }

    @Transactional
    public void delete(String externalId) {
        Pet pet = petRepository.findByExternalId(externalId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PET_001));
        petRepository.delete(pet);
    }
}
