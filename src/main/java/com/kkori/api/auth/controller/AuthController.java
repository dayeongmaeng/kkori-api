package com.kkori.api.auth.controller;

import com.kkori.api.auth.dto.request.LogoutRequest;
import com.kkori.api.auth.dto.request.OAuthLoginRequest;
import com.kkori.api.auth.dto.request.RefreshTokenRequest;
import com.kkori.api.auth.dto.response.LogoutResponse;
import com.kkori.api.auth.dto.response.OAuthLoginResponse;
import com.kkori.api.auth.dto.response.RefreshTokenResponse;
import com.kkori.api.auth.service.AuthService;
import com.kkori.api.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth", description = "OAuth 인증")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "OAuth 로그인")
    @PostMapping("/oauth/login")
    public ResponseEntity<ApiResponse<OAuthLoginResponse>> login(
            @Valid @RequestBody OAuthLoginRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(authService.login(request)));
    }

    @Operation(summary = "JWT accessToken 재발급")
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<RefreshTokenResponse>> refresh(
            @Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(authService.refresh(request)));
    }

    @Operation(summary = "로그아웃")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<LogoutResponse>> logout(
            @RequestBody(required = false) LogoutRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(authService.logout(request)));
    }
}
