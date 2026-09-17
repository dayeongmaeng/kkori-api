package com.kkori.api.admin.controller;

import com.kkori.api.admin.dto.request.AdminSuspendMemberRequest;
import com.kkori.api.admin.dto.response.AdminErrorResponse;
import com.kkori.api.admin.dto.response.AdminHealthResponse;
import com.kkori.api.admin.dto.response.AdminMemberDetailResponse;
import com.kkori.api.admin.dto.response.AdminMemberPageResponse;
import com.kkori.api.admin.exception.AdminMemberNotFoundException;
import com.kkori.api.admin.service.AdminMemberService;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * kkutudio-admin(admin-api)의 AppAdminClient가 서버 대 서버로 호출하는 어드민 전용 API.
 * 최종 사용자에게 노출되지 않으며 {@link com.kkori.api.admin.filter.AdminApiKeyFilter}로 보호된다.
 * 공개 API({@code /api/v1/**})와 달리 {@code ApiResponse<T>} 포맷을 쓰지 않고 응답을 그대로 반환한다 —
 * 이 계약은 admin-api 쪽 {@code AppAdminClient} 인터페이스와 맞춰져 있다.
 */
@Hidden
@RestController
@RequestMapping("/internal/admin")
@RequiredArgsConstructor
public class AdminMemberController {

    private final AdminMemberService adminMemberService;

    @GetMapping("/health")
    public AdminHealthResponse health() {
        return AdminHealthResponse.up();
    }

    @GetMapping("/members")
    public AdminMemberPageResponse getMembers(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return adminMemberService.getMembers(keyword, page, size);
    }

    @GetMapping("/members/{memberId}")
    public AdminMemberDetailResponse getMember(@PathVariable String memberId) {
        return adminMemberService.getMember(memberId);
    }

    @PostMapping("/members/{memberId}/suspend")
    public void suspend(@PathVariable String memberId, @Valid @RequestBody AdminSuspendMemberRequest request) {
        adminMemberService.suspendMember(memberId, request.reason());
    }

    @PostMapping("/members/{memberId}/force-logout")
    public void forceLogout(@PathVariable String memberId) {
        adminMemberService.forceLogoutMember(memberId);
    }

    @ExceptionHandler(AdminMemberNotFoundException.class)
    public ResponseEntity<AdminErrorResponse> handleNotFound(AdminMemberNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new AdminErrorResponse(e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<AdminErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .orElse("invalid request");
        return ResponseEntity.badRequest().body(new AdminErrorResponse(message));
    }
}
