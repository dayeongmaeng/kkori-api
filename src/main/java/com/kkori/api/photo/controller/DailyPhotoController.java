package com.kkori.api.photo.controller;

import com.kkori.api.common.dto.ApiResponse;
import com.kkori.api.photo.dto.request.CreateDailyPhotoRequest;
import com.kkori.api.photo.dto.request.UpdateDailyPhotoRequest;
import com.kkori.api.photo.dto.response.DailyPhotoResponse;
import com.kkori.api.photo.dto.response.DailyPhotoShareResponse;
import com.kkori.api.photo.dto.response.PhotoUploadResponse;
import com.kkori.api.photo.service.DailyPhotoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "DailyPhoto", description = "데일리 포토 관리")
@RestController
@RequestMapping("/api/v1/photos")
@RequiredArgsConstructor
public class DailyPhotoController {

    private final DailyPhotoService dailyPhotoService;

    @Operation(summary = "데일리 포토 등록")
    @PostMapping
    public ResponseEntity<ApiResponse<DailyPhotoResponse>> create(
            @RequestHeader("X-Device-Id") String deviceId,
            @Valid @RequestBody CreateDailyPhotoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(dailyPhotoService.create(deviceId, request)));
    }

    @Operation(summary = "반려동물별 데일리 포토 목록 조회")
    @GetMapping
    public ResponseEntity<ApiResponse<List<DailyPhotoResponse>>> findByPet(
            @RequestHeader("X-Device-Id") String deviceId,
            @RequestParam String petExternalId) {
        return ResponseEntity.ok(ApiResponse.ok(dailyPhotoService.findByPet(deviceId, petExternalId)));
    }

    @Operation(summary = "데일리 포토 단건 조회")
    @GetMapping("/{externalId}")
    public ResponseEntity<ApiResponse<DailyPhotoResponse>> findOne(
            @RequestHeader("X-Device-Id") String deviceId,
            @PathVariable String externalId) {
        return ResponseEntity.ok(ApiResponse.ok(dailyPhotoService.findByExternalId(deviceId, externalId)));
    }

    @Operation(summary = "데일리 포토 공유 데이터 조회")
    @GetMapping("/{externalId}/share")
    public ResponseEntity<ApiResponse<DailyPhotoShareResponse>> findShare(
            @PathVariable String externalId) {
        return ResponseEntity.ok(ApiResponse.ok(dailyPhotoService.findShareByExternalId(externalId)));
    }

    @Operation(summary = "데일리 포토 수정")
    @PatchMapping("/{externalId}")
    public ResponseEntity<ApiResponse<DailyPhotoResponse>> patch(
            @RequestHeader("X-Device-Id") String deviceId,
            @PathVariable String externalId,
            @Valid @RequestBody UpdateDailyPhotoRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(dailyPhotoService.update(deviceId, externalId, request)));
    }

    @Operation(summary = "데일리 포토 수정")
    @PutMapping("/{externalId}")
    public ResponseEntity<ApiResponse<DailyPhotoResponse>> update(
            @RequestHeader("X-Device-Id") String deviceId,
            @PathVariable String externalId,
            @Valid @RequestBody UpdateDailyPhotoRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(dailyPhotoService.update(deviceId, externalId, request)));
    }

    @Operation(summary = "데일리 포토 S3 업로드 (medium + thumbnail)")
    @PostMapping(value = "/{externalId}/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<PhotoUploadResponse>> upload(
            @RequestHeader("X-Device-Id") String deviceId,
            @PathVariable String externalId,
            @RequestPart("medium") MultipartFile medium,
            @RequestPart("thumbnail") MultipartFile thumbnail) {
        return ResponseEntity.ok(ApiResponse.ok(
                dailyPhotoService.uploadPhotos(deviceId, externalId, medium, thumbnail)));
    }

    @Operation(summary = "데일리 포토 삭제")
    @DeleteMapping("/{externalId}")
    public ResponseEntity<Void> delete(
            @RequestHeader("X-Device-Id") String deviceId,
            @PathVariable String externalId) {
        dailyPhotoService.delete(deviceId, externalId);
        return ResponseEntity.noContent().build();
    }
}
