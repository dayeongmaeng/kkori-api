package com.kkori.api.caregiver.controller;

import com.kkori.api.caregiver.dto.request.CreateCaregiverRequest;
import com.kkori.api.caregiver.dto.request.UpdateCaregiverRequest;
import com.kkori.api.caregiver.dto.response.CaregiverResponse;
import com.kkori.api.caregiver.service.CaregiverService;
import com.kkori.api.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/caregivers")
@RequiredArgsConstructor
public class CaregiverController {

    private final CaregiverService caregiverService;

    @PostMapping
    public ResponseEntity<ApiResponse<CaregiverResponse>> create(
            @RequestAttribute("deviceId") String deviceId,
            @Valid @RequestBody CreateCaregiverRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(caregiverService.create(deviceId, request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CaregiverResponse>>> findAll(
            @RequestAttribute("deviceId") String deviceId) {
        return ResponseEntity.ok(ApiResponse.ok(caregiverService.findAll(deviceId)));
    }

    @GetMapping("/{externalId}")
    public ResponseEntity<ApiResponse<CaregiverResponse>> findOne(
            @PathVariable String externalId) {
        return ResponseEntity.ok(ApiResponse.ok(caregiverService.findByExternalId(externalId)));
    }

    @PutMapping("/{externalId}")
    public ResponseEntity<ApiResponse<CaregiverResponse>> update(
            @PathVariable String externalId,
            @Valid @RequestBody UpdateCaregiverRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(caregiverService.update(externalId, request)));
    }

    @DeleteMapping("/{externalId}")
    public ResponseEntity<Void> delete(@PathVariable String externalId) {
        caregiverService.delete(externalId);
        return ResponseEntity.noContent().build();
    }
}
