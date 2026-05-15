package com.kkori.api.photo.controller;

import com.kkori.api.common.dto.ApiResponse;
import com.kkori.api.photo.dto.request.CreateDailyPhotoRequest;
import com.kkori.api.photo.dto.request.UpdateDailyPhotoRequest;
import com.kkori.api.photo.dto.response.DailyPhotoResponse;
import com.kkori.api.photo.service.DailyPhotoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/photos")
@RequiredArgsConstructor
public class DailyPhotoController {

    private final DailyPhotoService dailyPhotoService;

    @PostMapping
    public ResponseEntity<ApiResponse<DailyPhotoResponse>> create(
            @RequestHeader("X-Device-Id") String deviceId,
            @Valid @RequestBody CreateDailyPhotoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(dailyPhotoService.create(deviceId, request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<DailyPhotoResponse>>> findByPet(
            @RequestHeader("X-Device-Id") String deviceId,
            @RequestParam String petExternalId) {
        return ResponseEntity.ok(ApiResponse.ok(dailyPhotoService.findByPet(deviceId, petExternalId)));
    }

    @GetMapping("/{externalId}")
    public ResponseEntity<ApiResponse<DailyPhotoResponse>> findOne(
            @RequestHeader("X-Device-Id") String deviceId,
            @PathVariable String externalId) {
        return ResponseEntity.ok(ApiResponse.ok(dailyPhotoService.findByExternalId(deviceId, externalId)));
    }

    @PutMapping("/{externalId}")
    public ResponseEntity<ApiResponse<DailyPhotoResponse>> update(
            @RequestHeader("X-Device-Id") String deviceId,
            @PathVariable String externalId,
            @Valid @RequestBody UpdateDailyPhotoRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(dailyPhotoService.update(deviceId, externalId, request)));
    }

    @DeleteMapping("/{externalId}")
    public ResponseEntity<Void> delete(
            @RequestHeader("X-Device-Id") String deviceId,
            @PathVariable String externalId) {
        dailyPhotoService.delete(deviceId, externalId);
        return ResponseEntity.noContent().build();
    }
}
