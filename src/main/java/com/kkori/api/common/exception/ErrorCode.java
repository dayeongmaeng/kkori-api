package com.kkori.api.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    VALIDATION_001(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다."),
    DEVICE_001(HttpStatus.BAD_REQUEST, "X-Device-Id 헤더가 누락되었습니다."),
    DEVICE_002(HttpStatus.NOT_FOUND, "디바이스를 찾을 수 없습니다."),
    PET_001(HttpStatus.NOT_FOUND, "반려동물을 찾을 수 없습니다."),
    CAREGIVER_001(HttpStatus.NOT_FOUND, "보호자를 찾을 수 없습니다."),
    CAREGIVER_002(HttpStatus.CONFLICT, "이미 존재하는 보호자 ID입니다."),
    PET_002(HttpStatus.CONFLICT, "이미 존재하는 반려동물 ID입니다."),
    PHOTO_003(HttpStatus.CONFLICT, "이미 존재하는 사진 ID입니다."),
    LOG_003(HttpStatus.CONFLICT, "이미 존재하는 기록 ID입니다."),
    PHOTO_001(HttpStatus.NOT_FOUND, "사진을 찾을 수 없습니다."),
    PHOTO_002(HttpStatus.CONFLICT, "해당 날짜에 이미 사진이 존재합니다."),
    LOG_001(HttpStatus.NOT_FOUND, "일일 기록을 찾을 수 없습니다."),
    LOG_002(HttpStatus.CONFLICT, "해당 날짜에 이미 기록이 존재합니다.");

    private final HttpStatus httpStatus;
    private final String message;

    public String getCode() {
        return name();
    }
}
