# API 규칙

## URL 구조
- 베이스: `/api/v1`
- 리소스 복수형: `/pets`, `/photos`, `/logs`
- 중첩 자제, 필요 시 query parameter 사용

## HTTP 메서드
- GET: 조회
- POST: 생성
- PUT: 전체 수정
- PATCH: 부분 수정
- DELETE: 삭제

## 응답 포맷
```java
public record ApiResponse<T>(
    boolean success,
    T data,
    ErrorResponse error
) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null);
    }
    
    public static <T> ApiResponse<T> error(ErrorResponse error) {
        return new ApiResponse<>(false, null, error);
    }
}

public record ErrorResponse(
    String code,
    String message,
    Map<String, String> fields
) {}
```

## 디바이스 식별
- 헤더: `X-Device-Id` 필수
- 컨트롤러: `@RequestHeader("X-Device-Id") String deviceId`
- 없으면 400 응답

## DTO 규칙
### Request DTO
- 명명: `{Action}{Resource}Request` (예: `CreatePetRequest`)
- record 사용 권장
- 검증 어노테이션 필수 (`@NotNull`, `@NotBlank`, `@Min`, `@Size`)

### Response DTO
- 명명: `{Resource}Response` (예: `PetResponse`)
- record 사용 권장
- 정적 팩토리 메서드: `from(Entity)` 또는 `of(...)`
- 엔티티 직접 노출 절대 금지

## 페이지네이션

- Spring Data `Pageable` 사용
- 응답: `PageResponse<T>` 래퍼
- 기본: page=0, size=20

## 에러 코드
- PET_001: 반려동물 없음
- PHOTO_001: 사진 없음
- PHOTO_002: 같은 날짜 사진 중복
- LOG_001: 기록 없음
- DEVICE_001: 디바이스 ID 누락
- VALIDATION_001: 검증 실패

## 검증
- `@Valid` + Bean Validation
- 한국어 메시지: `messages.properties`
- 커스텀 검증: `@Constraint` + Validator

## 예외 처리
`@RestControllerAdvice`로 전역 처리:
- `MethodArgumentNotValidException` → 400
- `BusinessException` → 비즈니스 코드별 매핑
- `Exception` → 500 (로그 + "서버 오류" 메시지)

## 보안 (향후)
- Phase D부터 JWT
- 현재는 디바이스 ID로만 분리
- HTTPS는 배포 단계에서