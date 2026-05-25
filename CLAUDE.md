# 꼬리 API

반려동물 일상 기록 앱 "꼬리"의 백엔드 서버.

기준 문서: `apiserver.md`
마지막 갱신: 2026-05-25

## 프로젝트 컨텍스트

- 클라이언트: React Native + Expo (별도 저장소: `kkori`)
- 백엔드: 이 저장소 (`kkori-api`)
- 개발 형태: 1인 개발 인디 프로젝트

## 스택

- Spring Boot 3.5.14
- Java 21
- PostgreSQL 16
- JPA + Hibernate
- Gradle 9.4.1 wrapper
- Lombok
- springdoc-openapi
- Docker Compose
- AWS SDK S3

## 서버 정보

- 인스턴스: `kkori-api`
- 리전: Seoul, Zone A (`ap-northeast-2a`)
- OS: Ubuntu 24.04 LTS
- Public IP: `13.124.220.29`
- 사양: 1GB RAM, 2 vCPUs, 40GB SSD
- 이전 서버 IP: `3.38.97.234` (더 이상 운영 기준 IP 아님)

## 도메인 / 운영 인프라

- 루트 도메인: `kkori.co.kr`
- API 도메인: `api.kkori.co.kr`
- DNS A 레코드: `api.kkori.co.kr -> 13.124.220.29`
- 운영 API URL: `https://api.kkori.co.kr`
- 클라이언트 환경변수: `EXPO_PUBLIC_API_URL=https://api.kkori.co.kr`
- 클라이언트 개발 환경은 로컬 API를 사용하고, 운영 환경은 `https://api.kkori.co.kr`를 사용한다.
- Expo/React Native에서는 `__DEV__`로 개발/배포 구분 가능
- 실기기 개발 시 `localhost`는 폰 자신을 의미하므로 PC LAN IP 또는 `EXPO_PUBLIC_DEV_API_URL` 사용 필요
- HTTPS: Nginx + Let's Encrypt + Certbot 적용 완료
- 인증서 도메인: `api.kkori.co.kr`
- `certbot renew --dry-run` 성공
- Vercel 유지: `kkori.co.kr` / `www.kkori.co.kr`는 웹, 정책, 공유 페이지 용도
- Lightsail Spring Boot API: `api.kkori.co.kr` 전용
- 현재 흐름: 앱 -> `https://api.kkori.co.kr` -> Nginx 443 -> Spring Boot 8080 -> PostgreSQL 16 -> S3

## 운영 포트

- 열려 있어야 함: 22(SSH), 80(HTTP/Certbot 갱신/HTTPS redirect), 443(HTTPS API)
- Spring Boot 8080은 Nginx 프록시 뒤 내부 포트로만 사용한다.
- 코드의 `docker-compose.yml`은 현재 `8080:8080`으로 바인딩한다.
- 실제 8080 외부 공개 차단 여부는 Lightsail 방화벽/Nginx 기준으로 확인 필요하다.

## 진행 상태

- Phase A: 로컬 API 구축 완료
- Phase B: React Native 클라이언트 연동 완료
- Phase C: Lightsail 배포 + 도메인 + HTTPS 적용 완료
- Phase D: OAuth/JWT 인증 1차 구현 완료 + 운영/QA 진행
- Phase D+: 회원 탈퇴 API 서버 구현 완료 + 클라이언트 UI 연동 완료
- Phase E: S3 사진 업로드 및 후속 UX 안정화 완료
- Phase F: AI 리포트 미진행

## 인증 상태

- 이메일/비밀번호 가입은 사용하지 않는다. 인증은 OAuth 전용이다.
- provider enum: `GOOGLE`, `KAKAO`, `APPLE`
- 실제 구현/검증 대상: `GOOGLE`, `KAKAO`
- OAuth 로그인 API: `POST /api/v1/auth/oauth/login`
- 요청 필드: provider, idToken/accessToken/code/redirectUri, deviceExternalId
- 응답: JWT accessToken, refreshToken, user
- Google은 idToken을 `https://oauth2.googleapis.com/tokeninfo`로 검증하고 `GOOGLE_CLIENT_ID` audience를 대조한다.
- Kakao는 accessToken으로 `https://kapi.kakao.com/v2/user/me`를 호출한다.
- Kakao authorization code 교환 흐름도 구현되어 있다.
- JWT access/refresh 발급, accessToken 필터, refresh API가 구현되어 있다.
- 로그아웃 API: `POST /api/v1/auth/logout`
- 로그아웃 시 refreshToken 해시를 `revoked_refresh_token`에 저장하고, Kakao provider logout을 일부 지원한다.
- refreshToken rotation은 아직 TODO다.
- 로그아웃으로 폐기된 refreshToken은 refresh API에서 차단한다.
- OAuth 토큰, JWT, provider 민감정보는 로그에 남기지 않는다.

## JWT 설정

- `JWT_SECRET`은 환경변수로만 설정한다.
- 로컬/개발/운영 모두 최소 32자 이상 값이 필요하다.
- `JWT_SECRET` 미설정 또는 32자 미만이면 서버 시작 시 명확한 에러로 실패한다.
- 선택 값:
  - `JWT_ACCESS_TOKEN_TTL_SECONDS=3600`
  - `JWT_REFRESH_TOKEN_TTL_SECONDS=2592000`

## 도메인 모델

- **SoftDeletableEntity**: `deletedAt` 필드를 가진 공통 베이스. soft delete 대상 엔티티는 이를 상속한다.
- **User**: OAuth 전용 계정 소유자 (`GOOGLE`, `KAKAO`, `APPLE`), `provider + providerUserId` unique. soft delete 적용.
  - `status` 필드: `UserStatus.ACTIVE` / `UserStatus.WITHDRAWN`
  - `withdraw()` 메서드: status=WITHDRAWN, deletedAt=now, email/nickname 익명화("탈퇴한 사용자"), provider/providerUserId/profileImageUrl null 처리
  - 탈퇴 후 같은 OAuth 계정으로 재가입 가능 (provider+providerUserId 가 null 처리되므로 unique constraint 충돌 없음)
- **Device**: 설치 기기/세션/푸시 보조 정보, `Device.userId`로 User 연결, 기존 `X-Device-Id` 흐름 유지
- **Caregiver**: 보호자 (가족 공유 대비, 한 Device에 여러 Caregiver 가능). soft delete 적용.
- **Pet**: 반려동물. `userId` nullable + 기존 `deviceId` fallback. soft delete 적용.
- **DailyPhoto**: 하루 한 장 데일리 포토 (`petId + date` unique), caption, mediumUrl, thumbnailUrl. soft delete 적용.
- **DailyLog**: 일일 건강 기록 (식사/물/산책/배변/소변/컨디션/체중/메모). soft delete 적용.
- **DailyLogPhoto**: 기록탭 사진. DailyLog별 최대 3장, S3 medium/thumbnail URL 저장. soft delete 적용.
- **UserOAuthToken**: Google OAuth access token 암호화 저장 (`user_oauth_token` 테이블). AES-256-GCM 암호화. revoke 성공 시 `revokedAt` 기록. Google revoke 시 사용.
- **RevokedRefreshToken**: 로그아웃된 refreshToken 해시 저장

## Pet 프로필 상태

- 서버 코드 기준 프로필 고도화 구현 완료
- 필드: `gender`, `breed`, `birthDate`, `birthDateUnknown`, `adoptionDate`, `weightKg`, `neutered`, `medicalNotes`, `photoBase64`
- `birthDateUnknown=false`이면 `birthDate` 필수 검증 있음
- `breed`는 서버 enum/목록으로 관리하지 않고 string 저장만 담당
- 품종 추천/자동완성은 클라이언트 상수 기반으로 처리한다.
- 코드상 `Species`는 `DOG`, `CAT` 둘 다 열려 있다. "현재 강아지만" 정책과 맞출지 확인 필요.

## 사진 기능 상태

- DailyPhoto
  - `petId + date` unique
  - caption 수정
  - medium/thumbnail URL 저장
  - 공유 조회 API 제공: `GET /api/v1/photos/{externalId}/share`
- DailyLogPhoto
  - DailyLog별 최대 3장
  - S3 medium/thumbnail 업로드
  - sortOrder 저장
  - DailyLog 조회 응답에 `photos` 포함
  - 삭제 시 S3 객체 삭제
- S3 key
  - `photos/{petExternalId}/{photoExternalId}/medium.jpg`
  - `photos/{petExternalId}/{photoExternalId}/thumb.jpg`
- 파일 제한
  - JPEG/PNG만 허용
  - medium 1MB
  - thumbnail 200KB

## 주요 API

- `POST /api/v1/devices/register`
- `GET /api/v1/devices/me`
- `/api/v1/caregivers`
- `GET/POST/PUT /api/v1/pets`
- `DELETE /api/v1/pets/{externalId}` — soft delete + cascade (DailyLog, DailyLogPhoto, DailyPhoto) + S3 이미지 AFTER_COMMIT 비동기 삭제. 응답: 204.
- `/api/v1/photos`
- `POST /api/v1/photos/{externalId}/upload`
- `GET /api/v1/photos/{externalId}/share`
- `/api/v1/logs`
- `/api/v1/daily-logs`
- `POST /api/v1/daily-logs/{externalId}/photos/upload`
- `DELETE /api/v1/daily-logs/{externalId}/photos/{photoExternalId}`
- `POST /api/v1/auth/oauth/login`
- `POST /api/v1/auth/refresh`
- `POST /api/v1/auth/logout`
- `DELETE /api/v1/users/me` — 회원 탈퇴. JWT 인증 필수. 소유 Pet/기록/사진 cascade soft delete + S3 이미지 비동기 삭제 + 개인정보 익명화 + WITHDRAWN 처리. 응답: 204.

## 패키지 구조

```text
com.kkori.api
├── common         # 공통 응답, 예외, 설정 (AsyncConfig 포함)
├── auth           # OAuth 로그인, JWT, 로그아웃
│   └── oauth
│       └── disconnect  # OAuthDisconnectService 인터페이스
│                       # KakaoOAuthDisconnectService (Admin Key 기반 unlink 구현 완료)
│                       # GoogleOAuthDisconnectService (UserOAuthToken 기반 revoke 구조 구현 완료)
│                       # OAuthDisconnectListener (AFTER_COMMIT @Async)
├── user           # OAuth 사용자, UserStatus, UserWithdrawalService, UserController
│   ├── event      # UserWithdrawalEvent
│   └── oauth      # UserOAuthToken (Google OAuth token AES-256-GCM 암호화 저장)
├── device         # 디바이스 관리
├── caregiver      # 보호자
├── pet            # 반려동물 (PetImageCleanupEvent, PetImageCleanupListener 포함)
├── photo          # 데일리 포토
└── log            # 일일 기록
```

각 도메인 하위:

```text
controller
service
repository
entity
dto
├── request
└── response
```

## 환경변수 / 설정 메모

- `.env.example`에 DB, S3, Google, Kakao, JWT 환경변수 예시가 있다.
- `application.yaml`은 S3 region으로 `AWS_S3_REGION`을 읽는다.
- `docker-compose.yml`은 현재 `AWS_REGION`을 전달한다.
- `AWS_REGION`과 `AWS_S3_REGION` 표기가 섞여 있으므로 정리 필요.
- `AWS_S3_BUCKET`은 `s3://`, URL, 경로 없이 순수 버킷명만 사용한다.
- `application.yaml`의 multipart 설정은 현재 `server.servlet.multipart` 아래에 있다. Spring Boot 표준 위치인 `spring.servlet.multipart`로 맞출지 확인 필요.

## 테스트 상태

- 일부 자동 테스트가 작성되어 있다.
  - AuthService
  - JWT issuer/verifier
  - OAuth verifier
  - JWT filter
  - PetService
  - DailyPhotoService/DTO
- 2026-05-22 기준 `java -jar gradle\wrapper\gradle-wrapper.jar test` 결과:
  - 26 tests completed
  - 1 failed
- 실패 테스트:
  - `JwtAuthenticationFilterTest.invalidTokenReturns401()`
  - 테스트용 `new ObjectMapper()`가 `ApiResponse.timestamp(LocalDateTime)` 직렬화 모듈을 못 찾아 실패
  - 운영 ObjectMapper 문제라기보다 테스트 구성 문제에 가까움

## 코딩 규칙

### 일반

- Java 21 기능 적극 활용 (record, switch expression, pattern matching)
- Lombok 사용 (`@Getter`, `@Builder`, `@RequiredArgsConstructor`)
- DTO 사용, 엔티티 직접 노출 금지
- 기존 패키지와 코드 스타일을 우선한다.
- 불필요한 리팩터링은 금지한다.
- React Native + Expo + TypeScript strict를 유지한다.
- 공유 API/조회 로직은 UI 수정 중 변경하지 않는다.
- client/api 프롬프트에는 "직접 화면을 열어서 실행/확인"하라는 문구를 넣지 않는다.
- 화면 확인은 사용자가 직접 진행한다.
- 프롬프트의 검증은 "검증 포인트/기대 동작" 중심으로 작성한다.

### 컨트롤러

- `@RestController` + `ResponseEntity<ApiResponse<T>>`
- URL: `/api/v1/{resource}`
- 디바이스 식별: `X-Device-Id` 헤더 (`@RequestHeader`)
- accessToken이 있으면 JWT 필터가 user 컨텍스트를 세팅한다.
- 토큰이 없으면 기존 `X-Device-Id` 흐름을 사용한다.
- 검증: `@Valid` + `@NotNull` / `@NotBlank` / `@Min` / `@Max`

### 서비스

- `@Service` + `@RequiredArgsConstructor`
- 트랜잭션: `@Transactional(readOnly = true)` 기본, 쓰기는 명시
- 예외: 비즈니스 예외는 커스텀 `BusinessException` 사용
- 권한 검증은 userId 우선, 없으면 deviceId fallback을 사용한다.
- soft delete: 삭제 메서드는 `deletedAt`을 현재 시각으로 설정한다. 물리 삭제를 하지 않는다.
- Pet 삭제 시 DailyLog → DailyLogPhoto, DailyPhoto 순으로 cascade soft delete를 수행한다.
- S3 이미지 실제 삭제는 `PetImageCleanupEvent`를 발행하고 `@TransactionalEventListener(AFTER_COMMIT)` 리스너에서 비동기 처리한다.

### 리포지토리

- `JpaRepository<Entity, Long>` 상속
- 복잡 쿼리: QueryDSL 또는 `@Query`
- soft delete 대상 엔티티는 `deletedAt IS NULL` 조건을 포함한 조회 메서드를 사용한다. 기본 `findById` 대신 `findByExternalIdAndDeletedAtIsNull` 등을 사용한다.

### 엔티티

- `@Entity` + `@Table(name = "...")`
- ID: `Long` + `@GeneratedValue(strategy = IDENTITY)`
- 클라이언트 ID: `externalId` (String, UUID, unique)
- 시간: `@CreatedDate`, `@LastModifiedDate` (`@EnableJpaAuditing`)
- 연관관계: 단방향 우선, 필요 시 양방향
- enum: `@Enumerated(EnumType.STRING)`

## 보안 / CORS

- Spring Security 도입 완료.
- CORS는 `CorsConfigurationSource` 빈 하나로 일원화한다. `@CrossOrigin`이나 별도 Filter를 추가하지 않는다.
- OPTIONS preflight 요청은 Spring Security 필터 체인에서 허용한다.
- 401 응답에도 CORS 헤더가 포함된다 (Spring Security 설정에서 처리).
- 개발: `http://localhost:8081`(웹) ↔ `http://localhost:8080`(API) CORS 허용.

## 예외 처리

- 전역 핸들러: `@RestControllerAdvice`
- 응답 포맷 통일: `ApiResponse<T> { success, data, error, timestamp }`
- 검증 실패: 400
- 인증 실패: 401
- 비즈니스 예외: 400/404/409
- 서버 에러: 500

## 클라이언트 / 공유 UI 메모

- 포토탭 고도화 방향
  - 상단 오늘/선택 날짜 표시
  - 캡션 수정 기능
  - 사진 자체 수정 불가
  - 수정 시 `수정됨` 표시
  - 공유하기 버튼 및 공유 링크 생성 UI
  - 삭제 완료 시 기록탭과 동일하게 `삭제되었습니다` 문구 표시
- 공유 UI 대상 파일
  - `components/CaptionModal.tsx`
  - `app/photos/[externalId].tsx`
  - `api/share-photo.js`
- 공유 미리보기 모달과 실제 공유화면은 최대한 일치시킨다.
- 날짜, 이미지, 캡션, 홍보문구, 버튼 여백을 일관되게 맞춘다.
- 미리보기/공유화면 모두 가능하면 스크롤 없이 한 화면에 들어오도록 압축한다.
- 로고는 `32px x 32px`, `object-fit: contain`, `object-position: right center` 기준으로 맞춘다.
- `app/photos/[externalId].tsx`와 `api/share-photo.js`의 로고 위치를 일관되게 유지한다.
- `수정됨` 표시는 너무 눈에 띄지 않도록 작고 약하게 유지한다.
- `꼬리에서 공유됨` 뱃지는 더 우측에 붙는 방향을 유지한다.

## 클라이언트 오류 메모

`Error while reading cache, falling back to a full crawl: Unable to deserialize cloned data`는 Expo/Metro 캐시 손상 가능성이 높다.

```bash
npx expo start -c
```

필요 시 `.expo`, `node_modules/.cache`, temp metro/haste-map 캐시를 삭제한다.

## 회원 탈퇴 운영 정책

### 배포 전 필수 DB 마이그레이션

`ddl-auto: update`는 기존 컬럼의 NOT NULL 제약을 자동 제거하지 않는다.
탈퇴 API 활성화 전 반드시 아래 SQL을 운영 DB에 수동 실행해야 한다.
스크립트: `src/main/resources/db/user-withdrawal-migration.sql`

```sql
ALTER TABLE users ALTER COLUMN provider DROP NOT NULL;
ALTER TABLE users ALTER COLUMN provider_user_id DROP NOT NULL;
UPDATE users SET status = 'ACTIVE' WHERE status IS NULL;
ALTER TABLE users ALTER COLUMN status SET NOT NULL;
```

이 마이그레이션 없이 탈퇴 API를 호출하면 `provider=null` 저장 시 DB constraint violation이 발생한다.

### 인증 토큰 처리 정책

- **access token**: 탈퇴 후에도 만료(최대 1시간)까지 유효할 수 있다. JWT 필터는 DB 상태를 조회하지 않는다. 허용 범위로 간주하며, 탈퇴 후 1시간 내 데이터 접근 가능성은 인지하고 운영한다.
- **refresh token**: 탈퇴 사용자에게 즉시 차단된다. `AuthService.refresh()`에서 `user.isDeleted() || user.isWithdrawn()` 검사로 AUTH_003(401) 반환.
- **재발급 불가**: refresh가 차단되므로 1시간 후 access token도 자연 만료된다.

### 재가입 정책

- 탈퇴 시 `provider`와 `providerUserId`를 null 처리하므로, 동일한 Google/Kakao 계정으로 재가입 가능하다.
- PostgreSQL의 unique constraint는 `(NULL, NULL)` 행을 서로 독립으로 취급하므로 탈퇴 row가 재가입을 막지 않는다.
- 재가입 시 신규 User row가 생성된다. 이전 탈퇴 이력(pet/기록/사진)은 복구되지 않는다.

### OAuth 연결 해제

- `OAuthDisconnectService` 인터페이스 + `KakaoOAuthDisconnectService`, `GoogleOAuthDisconnectService` 구현 완료.
- Kakao unlink: Admin Key 기반 HTTP 호출 실제 구현 완료. 탈퇴 API 성패에 영향 없음.
- Google revoke: `UserOAuthToken`에서 AES-256-GCM 복호화한 access token으로 revoke 호출 구조 구현 완료.
  - Google OAuth access token은 로그인 시 `user_oauth_token` 테이블에 AES-256-GCM 암호화로 저장된다.
  - revoke 성공 시 `revokedAt` 기록.
- OAuth 해제는 AFTER_COMMIT 비동기 처리. 실패해도 탈퇴 결과에 영향 없음.
- 해제 시도 결과는 `OAuthDisconnectListener` 로그로만 기록된다.

### 클라이언트 회원 탈퇴 UI 구현 가이드

- 탈퇴 버튼 전 안내 화면 표시:
  - "탈퇴 시 반려동물 기록과 사진이 모두 삭제됩니다. 이 작업은 되돌릴 수 없습니다."
  - "소셜 계정(Google/Kakao) 자체는 삭제되지 않습니다."
- 사용자 확인 후 `DELETE /api/v1/users/me` 호출 (Authorization 헤더 필수)
- 성공(204) 시: 로컬 저장 인증 정보(accessToken, refreshToken, deviceId 관련 캐시) 삭제 후 로그인 화면으로 이동
- 실패 시: "오류가 발생했습니다. 다시 시도해 주세요." 메시지 표시

## 다음 작업 후보

1. **[배포 전 필수]** 회원 탈퇴 DB 마이그레이션 — `user-withdrawal-migration.sql` 운영 DB 수동 실행
2. **[배포 전 필수]** `user_oauth_token` 테이블 운영 DB 마이그레이션 — `ddl-auto: update`로 자동 생성되지 않을 경우 수동 DDL 실행 필요
3. 반려동물 삭제 버튼 API 연동 (프로필 탭 → `DELETE /api/v1/pets/{externalId}` + 로컬 캐시 정리 + AppHeader 목록 갱신)
4. 실패 테스트 수정: `JwtAuthenticationFilterTest.invalidTokenReturns401()`
5. `AWS_REGION` / `AWS_S3_REGION` 표기 정리
6. multipart 설정 위치 확인 및 필요 시 `spring.servlet.multipart`로 이동
7. 8080 외부 포트 차단 여부 운영 환경에서 확인
8. 실제 Google/Kakao OAuth 실기기 로그인 QA (Kakao unlink, Google revoke 포함)
9. 운영 `JWT_SECRET`, `GOOGLE_CLIENT_ID`, Kakao 키 설정 반영 및 배포 환경 확인
10. Vercel에 `kkori.co.kr` / `www.kkori.co.kr` 연결 및 정책/계정삭제 안내 페이지 배포
11. Google revoke 실기기 QA (UserOAuthToken 저장 → 탈퇴 → revoke 호출 확인)
12. Phase F AI 리포트 설계
