package com.kkori.api.admin.exception;

public class AdminMemberNotFoundException extends RuntimeException {

    public AdminMemberNotFoundException(String memberId) {
        super("member not found: " + memberId);
    }
}
