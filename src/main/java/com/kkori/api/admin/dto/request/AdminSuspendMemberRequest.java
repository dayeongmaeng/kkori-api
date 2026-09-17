package com.kkori.api.admin.dto.request;

import jakarta.validation.constraints.NotBlank;

public record AdminSuspendMemberRequest(@NotBlank String reason) {
}
