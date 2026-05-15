package com.kkori.api.device.controller;

import com.kkori.api.common.dto.ApiResponse;
import com.kkori.api.device.dto.request.RegisterDeviceRequest;
import com.kkori.api.device.dto.response.DeviceResponse;
import com.kkori.api.device.service.DeviceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/devices")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceService deviceService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<DeviceResponse>> register(
            @Valid @RequestBody RegisterDeviceRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(deviceService.register(request)));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<DeviceResponse>> getMe(
            @RequestAttribute("deviceId") String deviceId) {
        return ResponseEntity.ok(ApiResponse.ok(deviceService.findMe(deviceId)));
    }
}
