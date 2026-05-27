package com.kkori.api.common.exception;

import com.kkori.api.common.dto.ApiResponse;
import com.kkori.api.common.dto.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException e) {
        ErrorCode code = e.getErrorCode();
        if (code.getHttpStatus().is5xxServerError()) {
            log.error("Business exception: code={}", code.getCode(), e);
        } else if (isAuthCode(code)) {
            log.warn("Auth exception: code={}", code.getCode());
        }
        ErrorResponse error = new ErrorResponse(code.getCode(), code.getMessage(), null);
        return ResponseEntity.status(code.getHttpStatus()).body(ApiResponse.error(error));
    }

    private boolean isAuthCode(ErrorCode code) {
        return code.getCode().startsWith("AUTH_");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException e) {
        Map<String, String> fields = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors()
                .forEach(fe -> fields.putIfAbsent(fe.getField(), fe.getDefaultMessage()));

        ErrorCode code = ErrorCode.VALIDATION_001;
        ErrorResponse error = new ErrorResponse(code.getCode(), code.getMessage(), fields);
        return ResponseEntity.badRequest().body(ApiResponse.error(error));
    }

    @ExceptionHandler(ServletRequestBindingException.class)
    public ResponseEntity<ApiResponse<Void>> handleBinding(ServletRequestBindingException e) {
        ErrorCode code = ErrorCode.VALIDATION_001;
        ErrorResponse error = new ErrorResponse(code.getCode(), e.getMessage(), null);
        return ResponseEntity.badRequest().body(ApiResponse.error(error));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidJson(HttpMessageNotReadableException e) {
        ErrorCode code = ErrorCode.VALIDATION_001;
        ErrorResponse error = new ErrorResponse(code.getCode(), code.getMessage(), null);
        return ResponseEntity.badRequest().body(ApiResponse.error(error));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("Unhandled exception", e);
        ErrorResponse error = new ErrorResponse("SERVER_ERROR", "서버 오류가 발생했습니다.", null);
        return ResponseEntity.internalServerError().body(ApiResponse.error(error));
    }
}
