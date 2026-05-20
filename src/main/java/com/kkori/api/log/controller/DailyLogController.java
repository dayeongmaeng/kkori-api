package com.kkori.api.log.controller;

import com.kkori.api.common.dto.ApiResponse;
import com.kkori.api.log.dto.request.CreateDailyLogRequest;
import com.kkori.api.log.dto.request.UpdateDailyLogRequest;
import com.kkori.api.log.dto.response.DailyLogPhotoResponse;
import com.kkori.api.log.dto.response.DailyLogResponse;
import com.kkori.api.log.service.DailyLogService;
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

@Tag(name = "DailyLog", description = "일일 건강 기록 관리")
@RestController
@RequestMapping({"/api/v1/logs", "/api/v1/daily-logs"})
@RequiredArgsConstructor
public class DailyLogController {

    private final DailyLogService dailyLogService;

    @Operation(summary = "일일 기록 등록")
    @PostMapping
    public ResponseEntity<ApiResponse<DailyLogResponse>> create(
            @RequestHeader(value = "X-Device-Id", required = false) String deviceId,
            @Valid @RequestBody CreateDailyLogRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(dailyLogService.create(deviceId, request)));
    }

    @Operation(summary = "반려동물별 일일 기록 목록 조회")
    @GetMapping
    public ResponseEntity<ApiResponse<List<DailyLogResponse>>> findByPet(
            @RequestHeader(value = "X-Device-Id", required = false) String deviceId,
            @RequestParam String petExternalId) {
        return ResponseEntity.ok(ApiResponse.ok(dailyLogService.findByPet(deviceId, petExternalId)));
    }

    @Operation(summary = "일일 기록 단건 조회")
    @GetMapping("/{externalId}")
    public ResponseEntity<ApiResponse<DailyLogResponse>> findOne(
            @RequestHeader(value = "X-Device-Id", required = false) String deviceId,
            @PathVariable String externalId) {
        return ResponseEntity.ok(ApiResponse.ok(dailyLogService.findByExternalId(deviceId, externalId)));
    }

    @Operation(summary = "일일 기록 수정")
    @PutMapping("/{externalId}")
    public ResponseEntity<ApiResponse<DailyLogResponse>> update(
            @RequestHeader(value = "X-Device-Id", required = false) String deviceId,
            @PathVariable String externalId,
            @Valid @RequestBody UpdateDailyLogRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(dailyLogService.update(deviceId, externalId, request)));
    }

    @Operation(summary = "일일 기록 사진 S3 업로드 (medium + thumbnail)")
    @PostMapping(value = "/{externalId}/photos/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<DailyLogPhotoResponse>> uploadPhoto(
            @RequestHeader(value = "X-Device-Id", required = false) String deviceId,
            @PathVariable String externalId,
            @RequestPart("medium") MultipartFile medium,
            @RequestPart("thumbnail") MultipartFile thumbnail) {
        return ResponseEntity.ok(ApiResponse.ok(
                dailyLogService.uploadPhoto(deviceId, externalId, medium, thumbnail)));
    }

    @Operation(summary = "일일 기록 사진 삭제")
    @DeleteMapping("/{externalId}/photos/{photoExternalId}")
    public ResponseEntity<Void> deletePhoto(
            @RequestHeader(value = "X-Device-Id", required = false) String deviceId,
            @PathVariable String externalId,
            @PathVariable String photoExternalId) {
        dailyLogService.deletePhoto(deviceId, externalId, photoExternalId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "일일 기록 삭제")
    @DeleteMapping("/{externalId}")
    public ResponseEntity<Void> delete(
            @RequestHeader(value = "X-Device-Id", required = false) String deviceId,
            @PathVariable String externalId) {
        dailyLogService.delete(deviceId, externalId);
        return ResponseEntity.noContent().build();
    }
}
