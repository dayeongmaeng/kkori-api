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
            @Valid @RequestBody CreateDailyLogRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(dailyLogService.create(request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<DailyLogResponse>>> findByPet(
            @RequestParam String petExternalId) {
        return ResponseEntity.ok(ApiResponse.ok(dailyLogService.findByPet(petExternalId)));
    }

    @GetMapping("/{externalId}")
    public ResponseEntity<ApiResponse<DailyLogResponse>> findOne(
            @PathVariable String externalId) {
        return ResponseEntity.ok(ApiResponse.ok(dailyLogService.findByExternalId(externalId)));
    }

    @PutMapping("/{externalId}")
    public ResponseEntity<ApiResponse<DailyLogResponse>> update(
            @PathVariable String externalId,
            @Valid @RequestBody UpdateDailyLogRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(dailyLogService.update(externalId, request)));
    }

    @DeleteMapping("/{externalId}")
    public ResponseEntity<Void> delete(@PathVariable String externalId) {
        dailyLogService.delete(externalId);
        return ResponseEntity.noContent().build();
    }
}
