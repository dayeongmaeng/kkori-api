package com.kkori.api.pet.controller;

import com.kkori.api.common.dto.ApiResponse;
import com.kkori.api.pet.dto.request.CreatePetRequest;
import com.kkori.api.pet.dto.request.UpdatePetRequest;
import com.kkori.api.pet.dto.response.PetResponse;
import com.kkori.api.pet.service.PetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/pets")
@RequiredArgsConstructor
public class PetController {

    private final PetService petService;

    @PostMapping
    public ResponseEntity<ApiResponse<PetResponse>> create(
            @Valid @RequestBody CreatePetRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(petService.create(request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PetResponse>>> findAll() {
        return ResponseEntity.ok(ApiResponse.ok(petService.findAll()));
    }

    @GetMapping("/{externalId}")
    public ResponseEntity<ApiResponse<PetResponse>> findOne(
            @PathVariable String externalId) {
        return ResponseEntity.ok(ApiResponse.ok(petService.findByExternalId(externalId)));
    }

    @PutMapping("/{externalId}")
    public ResponseEntity<ApiResponse<PetResponse>> update(
            @PathVariable String externalId,
            @Valid @RequestBody UpdatePetRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(petService.update(externalId, request)));
    }

    @DeleteMapping("/{externalId}")
    public ResponseEntity<Void> delete(@PathVariable String externalId) {
        petService.delete(externalId);
        return ResponseEntity.noContent().build();
    }
}
