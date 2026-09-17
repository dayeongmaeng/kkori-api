package com.kkori.api.admin.dto.response;

import com.kkori.api.user.entity.User;

import java.time.Instant;
import java.time.ZoneId;

public record AdminMemberDetailResponse(
        String memberId,
        String nickname,
        String email,
        String status,
        Instant joinedAt,
        // 로그인 마지막 활동 시각은 아직 별도로 추적하지 않아 항상 null이다.
        Instant lastActiveAt
) {
    public static AdminMemberDetailResponse from(User user) {
        return new AdminMemberDetailResponse(
                user.getExternalId(),
                user.getNickname(),
                user.getEmail(),
                user.getStatus().name(),
                user.getCreatedAt() == null ? null : user.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant(),
                null
        );
    }
}
