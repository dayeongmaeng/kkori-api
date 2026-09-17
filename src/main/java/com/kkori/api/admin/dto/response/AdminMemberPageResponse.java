package com.kkori.api.admin.dto.response;

import org.springframework.data.domain.Page;

import java.util.List;

public record AdminMemberPageResponse(
        List<AdminMemberSummaryResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static AdminMemberPageResponse from(Page<AdminMemberSummaryResponse> page) {
        return new AdminMemberPageResponse(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
