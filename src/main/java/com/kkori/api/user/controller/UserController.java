package com.kkori.api.user.controller;

import com.kkori.api.auth.context.AuthContext;
import com.kkori.api.auth.context.AuthenticatedUser;
import com.kkori.api.common.exception.BusinessException;
import com.kkori.api.common.exception.ErrorCode;
import com.kkori.api.user.service.UserWithdrawalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Users", description = "사용자")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserWithdrawalService userWithdrawalService;

    @Operation(summary = "회원 탈퇴", description = "인증된 사용자의 계정을 탈퇴 처리합니다. 소유 반려동물 및 모든 기록/사진이 삭제됩니다.")
    @DeleteMapping("/me")
    public ResponseEntity<Void> withdraw() {
        AuthenticatedUser authenticatedUser = AuthContext.currentUser()
                .orElseThrow(() -> new BusinessException(ErrorCode.AUTH_003));
        userWithdrawalService.withdraw(authenticatedUser);
        return ResponseEntity.noContent().build();
    }
}
