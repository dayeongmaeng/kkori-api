# 인수인계 노트

세션 종료 또는 컨텍스트 전환 시 다음 세션에서 바로 이어받기 위한 최신 상태 기록.

기준 문서: `apiserver.md`
마지막 업데이트: 2026-06-02

## 현재 운영 상태

- 루트 도메인: `kkori.co.kr`
- 운영 API 도메인: `api.kkori.co.kr`
- DNS A 레코드: `api.kkori.co.kr -> 13.124.220.29`
- 운영 API URL: `https://api.kkori.co.kr`
- 클라이언트 환경변수: `EXPO_PUBLIC_API_URL=https://api.kkori.co.kr`
- 서버 JWT 환경변수: `JWT_SECRET` 필수, 최소 32자 이상
- 서버: AWS Lightsail `kkori-api`, Seoul Zone A (`ap-northeast-2a`), Ubuntu 24.04 LTS
- 서버 사양: 1GB RAM, 2 vCPUs, 40GB SSD
- 현재 운영 Public IP: `13.124.220.29`
- 이전 서버 IP: `3.38.97.234` (더 이상 운영 기준 IP 아님)
- HTTPS: Nginx + Let's Encrypt + Certbot 적용 완료
- 인증서 도메인: `api.kkori.co.kr`
- `certbot renew --dry-run` 성공

## 현재 인프라 흐름

앱 -> `https://api.kkori.co.kr` -> Nginx 443 -> Spring Boot 8080 -> PostgreSQL 16 -> S3

## 포트 정책

- 열려 있어야 함: 22(SSH), 80(HTTP/Certbot 갱신/HTTPS redirect), 443(HTTPS API)
- Spring Boot 8080은 외부 공개 대상이 아니다. Nginx 내부 프록시 포트로만 사용한다.
- `docker-compose.yml`은 `127.0.0.1:8080:8080`으로 바인딩 (로컬호스트 전용, 외부 차단) — 2026-05-31 QA에서 적용.

## 현재 코드 기준 진행상황

- Phase A: 로컬 API 구축 완료
- Phase B: React Native 클라이언트 연동 완료
- Phase C: Lightsail 배포 + 도메인 + HTTPS 적용 완료
- Phase D: OAuth/JWT 인증 1차 구현 완료 + 운영/QA 진행
- Phase D+: 회원 탈퇴 API 서버 구현 완료 + 클라이언트 UI 연동 완료 (DB 마이그레이션 운영 적용 대기)
- Phase E: S3 사진 업로드 및 후속 UX 안정화 완료
- Phase C+: requestId 기반 로그 정책 적용 완료 (2026-05-27)
- Phase C+: 8080 외부 노출 차단 (127.0.0.1 바인딩), prod 프로파일 docker-compose 설정 완료 (2026-05-31)
- Phase D+ (고양이): DailyLog 확장 필드 추가 + UrineAmount enum + migration SQL (2026-05-31)
- Phase F: AI 리포트 미진행

## 로그 정책 (2026-05-27 적용)

- `logback-spring.xml`: 파일 로그, 일자별 rolling, 30일 보관
  - `logs/app.log` / `logs/error.log` (ERROR only)
- `RequestLoggingFilter` (`common/filter/`): requestId UUID → MDC + `X-Request-Id` 응답 헤더 + HTTP 접속 로그
- `prod` 프로파일: `com.kkori.api` → INFO (DEBUG 비활성화). 운영 서버에 `spring.profiles.active=prod` 환경변수 필요.
- WARN: JWT 인증/인가 실패, OAuth 검증 실패 reason, AUTH_* BusinessException
- ERROR: 5xx BusinessException, 미처리 예외
- INFO: 로그인·토큰 재발급·로그아웃 성공

## 구현 완료된 주요 서버 기능

- Device/Caregiver/Pet/DailyPhoto/DailyLog CRUD
- DailyPhoto S3 업로드
- DailyPhoto 공유 조회 API
- DailyLogPhoto 업로드/조회/삭제
- User 엔티티와 OAuth 전용 계정 구조
- Device.userId 연결
- Pet.userId 기반 User 소유권 전환
- 기존 `X-Device-Id` fallback 유지
- Google/Kakao OAuth 검증
- Kakao authorization code 교환 흐름
- JWT access/refresh 발급
- JWT 인증 필터
- refresh API
- logout API
- 로그아웃된 refreshToken 해시 저장 및 refresh 차단
- Kakao provider logout 일부 지원
- SoftDeletableEntity 도입 및 User/Pet/Caregiver/DailyLog/DailyPhoto/DailyLogPhoto soft delete 적용
- Pet 삭제 시 cascade soft delete (Pet → DailyLog → DailyLogPhoto / DailyPhoto)
- S3 이미지 삭제를 AFTER_COMMIT 비동기 처리로 분리 (PetImageCleanupEvent, Listener, AsyncConfig)
- Spring Security 도입 + CorsConfigurationSource 기반 CORS 일원화
- OPTIONS preflight 허용, 401 응답에도 CORS 헤더 유지
- **회원 탈퇴 API** (`DELETE /api/v1/users/me`): 개인정보 익명화 + WITHDRAWN 처리 + 소유 Pet cascade soft delete + S3 이미지 비동기 삭제 + OAuth 연결 해제 실제 구현
- **Kakao unlink**: Admin Key 기반 HTTP 호출 구현 완료
- **Google revoke**: `UserOAuthToken` 기반 revoke 구조 구현 완료
- **UserOAuthToken**: Google OAuth token AES-256-GCM 암호화 저장 (`user_oauth_token` 테이블). UNIQUE(user_id, provider). 필드: `encrypted_access_token`, `encrypted_refresh_token`, `access_token_expires_at`, `scope`, `revoked_at`. revoke 성공 시 `revokedAt` 기록
- **Google OAuth access token 전달 구조**: Web 로그인 시 access token을 서버로 전달해 `UserOAuthToken`에 저장. `OAUTH_TOKEN_ENCRYPTION_KEY` 환경변수 필수.
- **반려동물 멀티 선택/추가**: `AppHeader` 드롭다운으로 반려동물 전환 및 추가. 전환 시 홈/기록/포토/프로필 전체 갱신. 추가는 `POST /api/v1/pets` 호출 후 신규 pet을 currentPet으로 설정. 선택 ID는 `pet-care:api:current-pet-id`에 캐시 (2026-05-24)

## 인증 구현 상세

- 이메일/비밀번호 가입은 사용하지 않는다.
- provider enum: `GOOGLE`, `KAKAO`, `APPLE`
- 우선 구현 provider: `GOOGLE`, `KAKAO`
- Apple은 enum만 준비된 상태.
- `POST /api/v1/auth/oauth/login`
  - 요청: provider, idToken/accessToken/code/redirectUri, deviceExternalId
  - 응답: accessToken, refreshToken, user
  - Google audience 검증: `GOOGLE_WEB_CLIENT_ID`와 `GOOGLE_IOS_CLIENT_ID` 모두 허용
    - Web: access token도 함께 수신해 `UserOAuthToken`에 저장
    - iOS: idToken 기반 로그인
- `POST /api/v1/auth/refresh`
  - refreshToken 검증 후 새 accessToken 발급
  - 폐기된 refreshToken 해시는 차단
  - 탈퇴 사용자(isDeleted || isWithdrawn)는 AUTH_003으로 차단
  - refreshToken rotation은 아직 TODO
- `POST /api/v1/auth/logout`
  - 현재 사용자 인증 필요
  - refreshToken이 있으면 해시로 저장하여 폐기 처리
  - Kakao access token이 있으면 provider logout 시도
  - 탈퇴 사용자가 호출해도 NPE 없이 안전하게 처리됨 (provider null 가드 적용)
- `DELETE /api/v1/users/me`
  - JWT 인증 필수
  - 소유 Pet 전체 cascade soft delete (기존 PetService 로직 재사용)
  - 개인정보 익명화: email/providerUserId/profileImageUrl null, nickname="탈퇴한 사용자", provider null
  - status=WITHDRAWN, deletedAt=now
  - OAuth 연결 해제는 AFTER_COMMIT 비동기 처리 (실패해도 탈퇴 결과에 영향 없음)
  - 응답: 204 No Content
- `JWT_SECRET` 미설정 또는 32자 미만이면 서버 시작 시 실패
- OAuth 토큰, JWT, provider 민감정보는 로그에 남기지 않는다.

## 회원 탈퇴 정책

### 인증 토큰

| 항목 | 정책 |
|---|---|
| access token | 탈퇴 후 최대 1시간 유효 (JWT 필터는 DB 조회 안 함) |
| refresh token | 탈퇴 즉시 차단 (`isDeleted \|\| isWithdrawn` 검사) |
| 재발급 | refresh 차단으로 인해 1시간 후 자연 만료 |

### 재가입

- 탈퇴 시 `provider=null`, `providerUserId=null` 처리
- PostgreSQL unique constraint는 (NULL, NULL) 행을 서로 독립으로 취급 → 재가입 가능
- 재가입 시 신규 User row 생성. 이전 데이터 복구 불가.

### OAuth 연결 해제

- `OAuthDisconnectService` 인터페이스 + `KakaoOAuthDisconnectService`, `GoogleOAuthDisconnectService` 구현 완료
- Kakao unlink: Admin Key 기반 HTTP 호출 실제 구현 완료
- Google revoke: `UserOAuthToken`에서 AES-256-GCM 복호화한 access token으로 revoke 호출 구조 구현 완료
  - Google OAuth access token은 로그인 시 `user_oauth_token` 테이블에 암호화 저장
  - revoke 성공 시 `revokedAt` 기록
- AFTER_COMMIT 비동기. 실패해도 탈퇴 결과에 영향 없음
- 실패 로그: `OAuthDisconnectListener`에서 기록

### 배포 전 필수 DB 마이그레이션

`ddl-auto: update` 는 기존 컬럼 NOT NULL 제약을 자동 제거하지 않는다.
탈퇴 API 활성화 전 운영 DB에 수동 실행 필요.

스크립트 위치: `src/main/resources/db/user-withdrawal-migration.sql`

```sql
-- provider/provider_user_id NOT NULL 제거 (탈퇴 시 null 처리를 위해)
ALTER TABLE users ALTER COLUMN provider DROP NOT NULL;
ALTER TABLE users ALTER COLUMN provider_user_id DROP NOT NULL;

-- status 컬럼 기존 데이터 초기화 및 NOT NULL 적용
UPDATE users SET status = 'ACTIVE' WHERE status IS NULL;
ALTER TABLE users ALTER COLUMN status SET NOT NULL;
```

이 마이그레이션 없이 탈퇴 API를 호출하면 DB constraint violation(500)이 발생한다.

### 클라이언트 회원 탈퇴 UI 구현 가이드

- 탈퇴 진행 전 안내 표시:
  - "반려동물 기록과 사진이 모두 삭제되며 복구할 수 없습니다."
  - "소셜 계정(Google/Kakao) 자체는 삭제되지 않습니다."
- 확인 후 `DELETE /api/v1/users/me` 호출 (`Authorization: Bearer {accessToken}` 헤더 필수)
- 성공(204): 로컬 인증 정보(accessToken, refreshToken, 캐시) 삭제 → 로그인 화면 이동
- 실패: "오류가 발생했습니다. 다시 시도해 주세요."

## 데이터 소유권

- User가 데이터 소유 주체다.
- Device는 설치 기기/세션/푸시 보조 정보다.
- 기존 API 호환을 위해 `X-Device-Id` 흐름은 유지한다.
- OAuth 로그인 성공 시 현재 deviceId의 기존 Pet 중 `userId`가 없는 데이터를 User에 연결한다.
- Pet/Photo/Log 권한 검증은 userId 우선, 없으면 deviceId fallback을 사용한다.
- DailyPhoto/DailyLog는 기존 `petId` 기반을 유지한다.

## 반려동물 삭제 정책

클라이언트는 `DELETE /api/v1/pets/{externalId}` 하나만 호출한다. API 내부에서:

1. Pet soft delete (`deletedAt` 설정)
2. 해당 Pet의 DailyLog soft delete
3. DailyLog의 DailyLogPhoto soft delete
4. 해당 Pet의 DailyPhoto soft delete
5. 트랜잭션 커밋 후 `PetImageCleanupEvent` 발행 → `PetImageCleanupListener`에서 비동기로 S3 이미지 실제 삭제

응답: 204 No Content.

회원 탈퇴 시에도 동일한 cascade 로직을 내부에서 호출하므로 클라이언트가 반려동물 삭제 API를 별도 호출할 필요 없다 (`PetService.deleteAllForUser()`).

## DailyLog 확장 필드 (2026-05-31 추가)

고양이 지원 추가와 함께 DailyLog에 상세 건강 메모 필드가 추가됨.

- 신규 enum: `UrineAmount` (LITTLE, NORMAL, MUCH)
- 신규 필드: `urineAmount`, `mealNote`, `walkNote`, `pooNote`, `urineNote`, `waterNote`, `playMinutes`, `playNote`, `vomitCount`, `vomitNote`
- **운영 DB 적용 필수**: `src/main/resources/db/daily-log-note-fields-migration.sql`

## Pet 프로필 상태

- 서버 코드 기준 프로필 고도화 구현 완료
- 필드:
  - `gender`
  - `breed`
  - `birthDate`
  - `birthDateUnknown`
  - `adoptionDate`
  - `weightKg`
  - `neutered`
  - `medicalNotes`
  - `photoBase64`
- `birthDateUnknown=false`이면 `birthDate` 필수 검증 있음
- `breed`는 서버 enum/목록으로 관리하지 않고 string 저장만 담당
- `Species`: `DOG`, `CAT` 모두 공식 지원 (2026-05-31 고양이 추가)

## 사진 기능 상태

- DailyPhoto
  - `petId + date` unique
  - caption 수정
  - medium/thumbnail URL 저장
  - 공유 조회 API 제공
- DailyLogPhoto
  - DailyLog별 최대 3장
  - S3 medium/thumbnail 업로드
  - sortOrder 저장
  - 조회 응답에 photos 포함
  - 삭제 시 S3 객체 삭제
- S3 key:
  - `photos/{petExternalId}/{photoExternalId}/medium.jpg`
  - `photos/{petExternalId}/{photoExternalId}/thumb.jpg`
- 파일 제한:
  - JPEG/PNG만 허용
  - medium 1MB
  - thumbnail 200KB

## API 주요 경로

- `POST /api/v1/devices/register`
- `GET /api/v1/devices/me`
- `/api/v1/caregivers`
- `/api/v1/pets`
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
- `DELETE /api/v1/users/me` ← 회원 탈퇴 (신규)

## S3 업로드 문제 해결 기록

사진 업로드 중 `S3Exception: The specified bucket is not valid`가 발생했던 원인은 IAM access key 자체 문제가 아니라 Docker 컨테이너 안에 AWS/S3 환경변수가 전달되지 않던 문제였다.

운영 `.env`에는 아래 값들이 필요하다. 민감정보는 문서에 기록하지 않는다.

```dotenv
AWS_ACCESS_KEY_ID=...
AWS_SECRET_ACCESS_KEY=...
AWS_REGION=ap-northeast-2
AWS_S3_BUCKET=버킷명만
```

`AWS_S3_BUCKET`에는 `s3://`, URL, 경로를 넣지 않고 순수 버킷명만 넣어야 한다.

## 확인 필요한 설정 불일치

- `application.yaml`은 S3 region으로 `AWS_S3_REGION`을 읽는다.
- `docker-compose.yml`은 현재 `AWS_REGION`을 전달한다.
- `AWS_REGION` / `AWS_S3_REGION` 표기를 정리해야 한다.
- ~~multipart 설정~~: `spring.servlet.multipart`로 이동 완료 (2026-05-31 QA).
- `GOOGLE_CLIENT_ID` 단일 키 대신 `GOOGLE_WEB_CLIENT_ID` + `GOOGLE_IOS_CLIENT_ID`로 분리됨. 운영 환경변수 갱신 필요.
- `OAUTH_TOKEN_ENCRYPTION_KEY` 신규 환경변수 추가 필요. AES-256-GCM 암호화 키.

## 테스트 상태

- 일부 자동 테스트 작성됨:
  - AuthService
  - JWT issuer/verifier
  - OAuth verifier
  - JWT filter
  - PetService
  - DailyPhotoService/DTO
- 2026-05-22 기준 `./gradlew test`
  - 26 tests completed
  - 1 failed
- 실패:
  - `JwtAuthenticationFilterTest.invalidTokenReturns401()`
  - 테스트용 `new ObjectMapper()`가 `ApiResponse.timestamp(LocalDateTime)` 직렬화 모듈을 못 찾아 실패
  - 운영 ObjectMapper 문제라기보다 테스트 구성 문제에 가까움
- 회원 탈퇴 관련 자동 테스트 미작성 (추후 추가 필요)

## 클라이언트/공유 UI 메모

- 포토탭 상단 오늘/선택 날짜 표시
- 캡션 수정 기능
- 사진 자체 수정 불가
- 수정 시 `수정됨` 표시
- 공유하기 버튼 및 공유 링크 생성 UI
- 삭제 완료 시 기록탭과 동일하게 `삭제되었습니다` 문구 표시
- 대상 파일:
  - `components/CaptionModal.tsx`
  - `app/photos/[externalId].tsx`
  - `api/share-photo.js`
- 공유 미리보기 모달과 실제 공유화면을 최대한 일치시킨다.
- 날짜, 이미지, 캡션, 홍보문구, 버튼 여백을 일관되게 맞춘다.
- 미리보기/공유화면 모두 가능하면 스크롤 없이 한 화면에 들어오도록 압축한다.
- 로고는 `32px x 32px`, `object-fit: contain`, `object-position: right center`.
- `app/photos/[externalId].tsx`와 `api/share-photo.js`의 로고 위치를 일관되게 유지한다.
- `수정됨` 표시는 작고 약하게 유지한다.
- `꼬리에서 공유됨` 뱃지는 더 우측에 붙는 방향을 유지한다.

## 클라이언트 오류 메모

`Error while reading cache, falling back to a full crawl: Unable to deserialize cloned data`는 Expo/Metro 캐시 손상 가능성이 높다.

우선 아래 명령으로 캐시를 비우고 재시작한다.

```bash
npx expo start -c
```

필요 시 `.expo`, `node_modules/.cache`, temp metro/haste-map 캐시를 삭제한다.

## 웹/Vercel 결정

- `kkori.co.kr` / `www.kkori.co.kr`는 Vercel 유지
- 용도: 웹 랜딩, 개인정보처리방침, 계정삭제 안내, 가족 공유/메모리얼 페이지
- `api.kkori.co.kr`만 Lightsail API 서버로 연결

## 다음 작업 후보

1. **[배포 전 필수]** 운영 DB 마이그레이션 — 아래 3개 스크립트 모두 운영 DB 수동 실행
   - `user-withdrawal-migration.sql`
   - `user_oauth_token` 테이블 DDL 수동 실행 여부 확인
   - `daily-log-note-fields-migration.sql` (DailyLog 확장 필드)
2. 반려동물 삭제 API 클라이언트 연동 — `DELETE /api/v1/pets/{externalId}` 호출 + 로컬 캐시 정리 + AppHeader 목록 갱신
3. 실패 테스트 수정: `JwtAuthenticationFilterTest.invalidTokenReturns401()`
4. `AWS_REGION` / `AWS_S3_REGION` 표기 정리
5. 실제 Google/Kakao OAuth 실기기 로그인 QA (Kakao unlink, Google revoke 포함)
6. Google revoke 실기기 QA (UserOAuthToken 저장 → 탈퇴 → revoke 확인)
7. Vercel에 `kkori.co.kr` / `www.kkori.co.kr` 연결 및 정책/계정삭제 안내 페이지 배포
8. Phase F AI 리포트 설계

## 운영 주의사항

- 현재 운영 IP는 반드시 `13.124.220.29` 기준으로 본다.
- `3.38.97.234`는 이전 서버 IP이며 운영 기준으로 사용하지 않는다.
- 현재 운영 API URL은 `https://api.kkori.co.kr`이다.
- AWS secret key 등 민감정보는 문서와 Git에 기록하지 않는다.
- `JWT_SECRET`도 민감정보이므로 문서와 Git에 실제 값을 기록하지 않는다.
- local/dev/prod 모두 `JWT_SECRET=<32자 이상 랜덤 문자열>`이 필요하다.
- Spring Boot 8080은 외부 공개 대상이 아니다. Nginx 내부 프록시 포트로만 사용한다.
- **회원 탈퇴 API 활성화 전 DB 마이그레이션 선행 필수** — 없으면 탈퇴 시 500 오류 발생.
- `docker-compose.yml`에 `SPRING_PROFILES_ACTIVE: prod` 설정 완료 (2026-05-31). Swagger UI는 prod에서 비활성화됨.
- `daily-log-note-fields-migration.sql` 운영 DB 수동 실행 필요 (고양이 추가 확장 필드).

## 작업 스타일 / 프롬프트 작성 규칙

- client/api 프롬프트에는 "직접 화면을 열어서 실행/확인"하라는 문구를 넣지 않는다.
- 화면 확인은 사용자가 직접 진행한다.
- 프롬프트 검증은 "검증 포인트/기대 동작" 중심으로 작성한다.
- 불필요한 리팩터링은 금지한다.
- React Native + Expo + TypeScript strict를 유지한다.
- 공유 API/조회 로직은 UI 수정 중 변경하지 않는다.
- 인증은 OAuth 전용으로 간다. 이메일/비밀번호 가입은 추가하지 않는다.
- Device는 소유 주체가 아니다. User가 데이터 소유 주체이며, Device는 설치 기기/세션/푸시 보조 정보로만 본다.
- 기존 API 호환을 위해 `X-Device-Id` 흐름은 유지한다.
- OAuth 토큰, JWT, provider 민감정보는 로그에 남기지 않는다.
- 서버는 `breed` 문자열 저장만 담당한다.
- 캐시 비우기 기능은 `AsyncStorage.clear()`로 전체 저장소를 지우지 않는다.

## 자주 쓰는 명령

```bash
# 서버 실행
./gradlew bootRun

# 컴파일
./gradlew compileJava

# 테스트
./gradlew test

# 빌드
./gradlew build

# Docker Compose
docker compose up -d
docker compose logs -f api
docker compose logs -f db
```
