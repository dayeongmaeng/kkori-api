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
    PHOTO_004(HttpStatus.INTERNAL_SERVER_ERROR, "사진 업로드에 실패했습니다."),
    PHOTO_005(HttpStatus.BAD_REQUEST, "허용되지 않는 파일 형식입니다. (JPEG, PNG만 허용)"),
    PHOTO_006(HttpStatus.BAD_REQUEST, "파일 크기가 허용 한도를 초과했습니다."),
    LOG_001(HttpStatus.NOT_FOUND, "일일 기록을 찾을 수 없습니다."),
    LOG_002(HttpStatus.CONFLICT, "해당 날짜에 이미 기록이 존재합니다."),
    LOG_PHOTO_001(HttpStatus.NOT_FOUND, "기록 사진을 찾을 수 없습니다."),
    LOG_PHOTO_002(HttpStatus.CONFLICT, "한 기록에는 사진을 최대 3장까지 첨부할 수 있습니다.");

    private final HttpStatus httpStatus;
    private final String message;

    public String getCode() {
        return name();
    }
}
