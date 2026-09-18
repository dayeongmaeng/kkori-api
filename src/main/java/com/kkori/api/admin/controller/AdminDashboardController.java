package com.kkori.api.admin.controller;

import com.kkori.api.admin.dto.request.AdminDashboardPeriodRequest;
import com.kkori.api.admin.dto.response.AdminErrorResponse;
import com.kkori.api.admin.dto.response.dashboard.AdminDashboardConversionResponse;
import com.kkori.api.admin.dto.response.dashboard.AdminDashboardMembersResponse;
import com.kkori.api.admin.dto.response.dashboard.AdminDashboardOverviewResponse;
import com.kkori.api.admin.dto.response.dashboard.AdminDashboardPetsResponse;
import com.kkori.api.admin.dto.response.dashboard.AdminDashboardRecordsResponse;
import com.kkori.api.admin.exception.AdminDashboardInvalidPeriodException;
import com.kkori.api.admin.service.AdminDashboardService;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * kkutudio-admin(admin-api)의 AppAdminClient가 서버 대 서버로 호출하는 어드민 전용 대시보드(통계) API.
 * {@link AdminMemberController}와 동일하게 {@code ApiResponse<T>} 포맷을 쓰지 않고 응답을 그대로 반환한다.
 * Admin DB에는 아무것도 저장하지 않고 매 요청마다 실시간 집계한다.
 */
@Hidden
@RestController
@RequestMapping("/internal/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    @GetMapping("/overview")
    public AdminDashboardOverviewResponse overview(@RequestParam String from, @RequestParam String to,
            @RequestParam(defaultValue = "DAY") String unit) {
        return adminDashboardService.getOverview(AdminDashboardPeriodRequest.of(from, to, unit));
    }

    @GetMapping("/members")
    public AdminDashboardMembersResponse members(@RequestParam String from, @RequestParam String to,
            @RequestParam(defaultValue = "DAY") String unit) {
        return adminDashboardService.getMembers(AdminDashboardPeriodRequest.of(from, to, unit));
    }

    @GetMapping("/pets")
    public AdminDashboardPetsResponse pets(@RequestParam String from, @RequestParam String to,
            @RequestParam(defaultValue = "DAY") String unit) {
        return adminDashboardService.getPets(AdminDashboardPeriodRequest.of(from, to, unit));
    }

    @GetMapping("/records")
    public AdminDashboardRecordsResponse records(@RequestParam String from, @RequestParam String to,
            @RequestParam(defaultValue = "DAY") String unit) {
        return adminDashboardService.getRecords(AdminDashboardPeriodRequest.of(from, to, unit));
    }

    @GetMapping("/conversion")
    public AdminDashboardConversionResponse conversion(@RequestParam String from, @RequestParam String to,
            @RequestParam(defaultValue = "DAY") String unit) {
        return adminDashboardService.getConversion(AdminDashboardPeriodRequest.of(from, to, unit));
    }

    @ExceptionHandler(AdminDashboardInvalidPeriodException.class)
    public ResponseEntity<AdminErrorResponse> handleInvalidPeriod(AdminDashboardInvalidPeriodException e) {
        return ResponseEntity.badRequest().body(new AdminErrorResponse(e.getMessage()));
    }
}
