package com.kkori.api.pet.controller;

import com.kkori.api.common.dto.ApiResponse;
import com.kkori.api.pet.dto.request.CreatePetRequest;
import com.kkori.api.pet.dto.request.UpdatePetRequest;
import com.kkori.api.pet.dto.response.PetResponse;
import com.kkori.api.pet.service.PetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Pet", description = "반려동물 관리")
@RestController
@RequestMapping("/api/v1/pets")
@RequiredArgsConstructor
public class PetController {

    private final PetService petService;

    @Operation(summary = "반려동물 등록")
    @PostMapping
    public ResponseEntity<ApiResponse<PetResponse>> create(
            @RequestHeader(value = "X-Device-Id", required = false) String deviceId,
            @Valid @RequestBody CreatePetRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(petService.create(deviceId, request)));
    }

    @Operation(summary = "반려동물 목록 조회")
    @GetMapping
    public ResponseEntity<ApiResponse<List<PetResponse>>> findAll(
            @RequestHeader(value = "X-Device-Id", required = false) String deviceId) {
        return ResponseEntity.ok(ApiResponse.ok(petService.findAll(deviceId)));
    }

    @Operation(summary = "반려동물 단건 조회")
    @GetMapping("/{externalId}")
    public ResponseEntity<ApiResponse<PetResponse>> findOne(
            @RequestHeader(value = "X-Device-Id", required = false) String deviceId,
            @PathVariable String externalId) {
        return ResponseEntity.ok(ApiResponse.ok(petService.findByExternalId(deviceId, externalId)));
    }

    @Operation(summary = "반려동물 수정")
    @PutMapping("/{externalId}")
    public ResponseEntity<ApiResponse<PetResponse>> update(
            @RequestHeader(value = "X-Device-Id", required = false) String deviceId,
            @PathVariable String externalId,
            @Valid @RequestBody UpdatePetRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(petService.update(deviceId, externalId, request)));
    }

    @Operation(summary = "반려동물 삭제")
    @DeleteMapping("/{externalId}")
    public ResponseEntity<Void> delete(
            @RequestHeader(value = "X-Device-Id", required = false) String deviceId,
            @PathVariable String externalId) {
        petService.delete(deviceId, externalId);
        return ResponseEntity.noContent().build();
    }
}
