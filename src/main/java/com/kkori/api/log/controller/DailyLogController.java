package com.kkori.api.log.controller;

import com.kkori.api.common.dto.ApiResponse;
import com.kkori.api.log.dto.request.CreateDailyLogRequest;
import com.kkori.api.log.dto.request.UpdateDailyLogRequest;
import com.kkori.api.log.dto.response.DailyLogResponse;
import com.kkori.api.log.service.DailyLogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/logs")
@RequiredArgsConstructor
public class DailyLogController {

    private final DailyLogService dailyLogService;

    @PostMapping
    public ResponseEntity<ApiResponse<DailyLogResponse>> create(
            @RequestHeader("X-Device-Id") String deviceId,
            @Valid @RequestBody CreateDailyLogRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(dailyLogService.create(deviceId, request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<DailyLogResponse>>> findByPet(
            @RequestHeader("X-Device-Id") String deviceId,
            @RequestParam String petExternalId) {
        return ResponseEntity.ok(ApiResponse.ok(dailyLogService.findByPet(deviceId, petExternalId)));
    }

    @GetMapping("/{externalId}")
    public ResponseEntity<ApiResponse<DailyLogResponse>> findOne(
            @RequestHeader("X-Device-Id") String deviceId,
            @PathVariable String externalId) {
        return ResponseEntity.ok(ApiResponse.ok(dailyLogService.findByExternalId(deviceId, externalId)));
    }

    @PutMapping("/{externalId}")
    public ResponseEntity<ApiResponse<DailyLogResponse>> update(
            @RequestHeader("X-Device-Id") String deviceId,
            @PathVariable String externalId,
            @Valid @RequestBody UpdateDailyLogRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(dailyLogService.update(deviceId, externalId, request)));
    }

    @DeleteMapping("/{externalId}")
    public ResponseEntity<Void> delete(
            @RequestHeader("X-Device-Id") String deviceId,
            @PathVariable String externalId) {
        dailyLogService.delete(deviceId, externalId);
        return ResponseEntity.noContent().build();
    }
}
