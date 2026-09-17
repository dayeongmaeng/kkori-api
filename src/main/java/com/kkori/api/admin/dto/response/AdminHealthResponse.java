package com.kkori.api.admin.dto.response;

public record AdminHealthResponse(String status) {
    public static AdminHealthResponse up() {
        return new AdminHealthResponse("UP");
    }
}
