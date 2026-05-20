package com.kkori.api.auth.jwt;

public enum JwtTokenType {
    ACCESS("access"),
    REFRESH("refresh");

    private final String value;

    JwtTokenType(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
