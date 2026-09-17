package com.kkori.api.admin.dto.response;

import com.kkori.api.user.entity.User;

import java.time.Instant;
import java.time.ZoneId;

public record AdminMemberSummaryResponse(
        String memberId,
        String nickname,
        String email,
        String status,
        Instant joinedAt
) {
    public static AdminMemberSummaryResponse from(User user) {
        return new AdminMemberSummaryResponse(
                user.getExternalId(),
                user.getNickname(),
                user.getEmail(),
                user.getStatus().name(),
                user.getCreatedAt() == null ? null : user.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant()
        );
    }
}
