package com.kkori.api.pet.repository;

import com.kkori.api.pet.entity.Pet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PetRepository extends JpaRepository<Pet, Long> {
    Optional<Pet> findByExternalId(String externalId);
}
