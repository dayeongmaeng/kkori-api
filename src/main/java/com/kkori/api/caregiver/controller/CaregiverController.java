package com.kkori.api.caregiver.controller;

import com.kkori.api.caregiver.dto.request.CreateCaregiverRequest;
import com.kkori.api.caregiver.dto.request.UpdateCaregiverRequest;
import com.kkori.api.caregiver.dto.response.CaregiverResponse;
import com.kkori.api.caregiver.service.CaregiverService;
import com.kkori.api.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Caregiver", description = "보호자 관리")
@RestController
@RequestMapping("/api/v1/caregivers")
@RequiredArgsConstructor
public class CaregiverController {

    private final CaregiverService caregiverService;

    @Operation(summary = "보호자 등록")
    @PostMapping
    public ResponseEntity<ApiResponse<CaregiverResponse>> create(
            @RequestAttribute("deviceId") String deviceId,
            @Valid @RequestBody CreateCaregiverRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(caregiverService.create(deviceId, request)));
    }

    @Operation(summary = "보호자 목록 조회")
    @GetMapping
    public ResponseEntity<ApiResponse<List<CaregiverResponse>>> findAll(
            @RequestAttribute("deviceId") String deviceId) {
        return ResponseEntity.ok(ApiResponse.ok(caregiverService.findAll(deviceId)));
    }

    @Operation(summary = "보호자 단건 조회")
    @GetMapping("/{externalId}")
    public ResponseEntity<ApiResponse<CaregiverResponse>> findOne(
            @PathVariable String externalId) {
        return ResponseEntity.ok(ApiResponse.ok(caregiverService.findByExternalId(externalId)));
    }

    @Operation(summary = "보호자 수정")
    @PutMapping("/{externalId}")
    public ResponseEntity<ApiResponse<CaregiverResponse>> update(
            @PathVariable String externalId,
            @Valid @RequestBody UpdateCaregiverRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(caregiverService.update(externalId, request)));
    }

    @Operation(summary = "보호자 삭제")
    @DeleteMapping("/{externalId}")
    public ResponseEntity<Void> delete(@PathVariable String externalId) {
        caregiverService.delete(externalId);
        return ResponseEntity.noContent().build();
    }
}
