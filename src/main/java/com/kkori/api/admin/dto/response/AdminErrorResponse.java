package com.kkori.api.admin.dto.response;

/**
 * {@code /internal/admin/**}는 어드민 서버 전용 내부 연동 API라 공개 API의
 * {@link com.kkori.api.common.dto.ApiResponse} 포맷을 따르지 않고 단순한 형태를 쓴다.
 */
public record AdminErrorResponse(String message) {
}
