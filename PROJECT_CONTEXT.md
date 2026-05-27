# 꼬리(kkori) 프로젝트 인수인계

기준 문서: `apiserver.md`
마지막 갱신: 2026-05-27

## 프로젝트 한 줄

반려동물 일상 기록 + 건강 관리 + 메모리얼 앱. 17년 키운 말티즈를 떠나보낸 경험에서 출발.

## 저장소

- 클라이언트: https://github.com/dayeongmaeng/kkori (`C:\dev\projects\kkori`)
- 서버: https://github.com/dayeongmaeng/kkori-api (`C:\dev\projects\kkori-api`)

## 스택

- **클라이언트**: React Native + Expo, TypeScript strict, Expo Router, AsyncStorage 캐시
- **서버**: Spring Boot 3.5.14, Java 21, PostgreSQL 16, JPA/Hibernate, Gradle, Lombok, springdoc-openapi
- **인프라**: AWS Lightsail Seoul Zone A, Ubuntu 24.04 LTS, Docker Compose, PostgreSQL 16, Nginx, Let's Encrypt, S3
- **도구**: ClaudeCode/Codex, GitHub Desktop

## 현재 운영 상태

- 루트 도메인: `kkori.co.kr`
- 운영 API 도메인: `api.kkori.co.kr`
- DNS A 레코드: `api.kkori.co.kr -> 13.124.220.29`
- 운영 API URL: `https://api.kkori.co.kr`
- 클라이언트 환경변수: `EXPO_PUBLIC_API_URL=https://api.kkori.co.kr`
- 운영 서버: AWS Lightsail `kkori-api`, Seoul Zone A (`ap-northeast-2a`)
- OS/사양: Ubuntu 24.04 LTS, 1GB RAM, 2 vCPUs, 40GB SSD
- 현재 운영 Public IP: `13.124.220.29`
- 이전 서버 IP: `3.38.97.234` (더 이상 운영 기준 IP 아님)
- HTTPS: Nginx + Let's Encrypt + Certbot 적용 완료
- 인증서 도메인: `api.kkori.co.kr`
- `certbot renew --dry-run` 성공

## 현재 인프라 흐름

앱 -> `https://api.kkori.co.kr` -> Nginx 443 -> Spring Boot 8080 -> PostgreSQL 16 -> S3

## 포트 정책

- 열려 있어야 함: 22(SSH), 80(HTTP/Certbot 갱신/HTTPS redirect), 443(HTTPS API)
- Spring Boot 8080은 외부 공개 대상이 아니라 Nginx 내부 프록시 포트로만 사용한다.
- 코드의 `docker-compose.yml`은 현재 API를 `8080:8080`으로 바인딩한다.
- 운영에서 8080 외부 공개가 실제 차단되어 있는지는 Lightsail 방화벽/Nginx 기준으로 확인 필요하다.

## 진행 상태

- ✅ Phase A: 서버 API CRUD 완료
  - Device/Caregiver/Pet/DailyPhoto/DailyLog CRUD
  - `X-Device-Id` 기반 기존 흐름
  - ApiResponse/BusinessException/GlobalExceptionHandler
- ✅ Phase B: React Native 클라이언트 연동 완료
  - 서버 우선 + AsyncStorage 캐시 패턴
  - 운영 API URL 전환
- ✅ Phase C: Lightsail 배포 + 도메인 + HTTPS 완료
  - `api.kkori.co.kr`
  - Nginx + Let's Encrypt
  - PostgreSQL 16 + S3 흐름
- ✅ Phase E: 사진 클라우드 저장 및 UX 안정화 완료
  - 하루 한 장 사진 S3 업로드
  - 기록 사진 S3 업로드/조회/삭제
  - thumbnail/medium URL 저장
  - 공유 조회 API
- ✅ Phase E+: soft delete + 반려동물 삭제 정책 완료
  - SoftDeletableEntity 도입
  - User/Pet/Caregiver/DailyLog/DailyPhoto/DailyLogPhoto soft delete 적용
  - Pet 삭제 cascade soft delete
  - S3 이미지 AFTER_COMMIT 비동기 삭제 (PetImageCleanupEvent/Listener/AsyncConfig)
  - Spring Security 도입 + CorsConfigurationSource 기반 CORS 일원화
  - localhost web ↔ api CORS 해결, 401 응답에도 CORS 헤더 유지
- ✅ Phase C+: requestId 기반 로그 정책 적용 완료 (2026-05-27)
- 🔄 Phase D: OAuth/JWT 인증 1차 구현 완료 + 운영/QA 진행
  - User 소유권 전환
  - Google/Kakao OAuth 검증
  - JWT access/refresh 발급
  - refresh API
  - logout API와 refreshToken 해시 폐기
  - refreshToken rotation은 TODO
- ✅ Phase D+: 회원 탈퇴 API 서버 구현 완료 + 클라이언트 UI 연동 완료 (운영 DB 마이그레이션 적용 대기)
- ⬜ Phase F: AI 리포트 미진행

## 서버 데이터 모델

BaseEntity 기반 `createdAt`, `updatedAt` 사용.
SoftDeletableEntity: `deletedAt` 필드를 가진 공통 베이스 (BaseEntity 확장). soft delete 대상 엔티티는 이를 상속한다.

- User — soft delete 적용
  - OAuth 전용 계정 소유자
  - `externalId`, `provider` (nullable), `providerUserId` (nullable), nullable email/nickname/profileImageUrl
  - `provider + providerUserId` unique (PostgreSQL에서 NULL은 unique 제약의 독립 값으로 취급)
  - `status`: `ACTIVE` | `WITHDRAWN` (탈퇴 시 WITHDRAWN, deletedAt 설정, 개인정보 익명화)
  - 탈퇴 시: provider/providerUserId/email/profileImageUrl null, nickname="탈퇴한 사용자", status=WITHDRAWN
- Device
  - 설치 기기/세션/푸시 보조 정보
  - `externalId`, `platform`, `userId nullable`
  - 기존 `X-Device-Id` 흐름 유지
- Caregiver — soft delete 적용
  - 보호자
  - `deviceId`, name, role, color
- Pet — soft delete 적용
  - `deviceId nullable`, `userId nullable`
  - name, species, gender, breed, birthDate, birthDateUnknown, adoptionDate, weightKg, neutered, medicalNotes, photoBase64
  - userId 우선, deviceId fallback
  - 삭제 시 DailyLog → DailyLogPhoto, DailyPhoto cascade soft delete
- DailyPhoto — soft delete 적용
  - `petId`, `caregiverId`, date, caption, mediumUrl, thumbnailUrl
  - unique(`petId`, `date`)
- DailyLog — soft delete 적용
  - `petId`, `caregiverId`, date, meal, water, walkMinutes, pooCondition, urineColor, condition, weightKg, memo
  - unique(`petId`, `date`)
- DailyLogPhoto — soft delete 적용
  - DailyLog별 최대 3장
  - `dailyLogId`, `petId`, `caregiverId`, date, mediumUrl, thumbnailUrl, sortOrder
- UserOAuthToken — soft delete 미적용
  - Google OAuth access token AES-256-GCM 암호화 저장
  - `user_oauth_token` 테이블, UNIQUE(user_id, provider)
  - 필드: `encrypted_access_token`, `encrypted_refresh_token`, `access_token_expires_at`, `scope`, `revoked_at`
  - revoke 성공 시 `revokedAt` 기록
- RevokedRefreshToken
  - 로그아웃된 refreshToken 해시 저장

## API 주요 경로

- `POST /api/v1/devices/register`
- `GET /api/v1/devices/me`
- `/api/v1/caregivers`
- `GET/POST/PUT /api/v1/pets`
- `DELETE /api/v1/pets/{externalId}` — 204. cascade soft delete + S3 이미지 AFTER_COMMIT 비동기 삭제.
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
- `DELETE /api/v1/users/me` — 204. 회원 탈퇴. JWT 인증 필수.

## 인증 / 소유권 전환

- 이메일/비밀번호 가입은 사용하지 않는다.
- OAuth provider enum은 `GOOGLE`, `KAKAO`, `APPLE`.
- 실제 구현은 Google/Kakao 중심이며 Apple은 enum만 준비된 상태.
- OAuth 로그인 성공 시 현재 `deviceExternalId`의 기존 Pet 중 `userId`가 없는 데이터를 User에 연결한다.
- DailyPhoto/DailyLog는 기존 `petId` 기반을 유지한다.
- Pet/Photo/Log 권한 검증은 userId 우선, 없으면 기존 deviceId fallback을 사용한다.
- accessToken이 있으면 JWT 필터가 `AuthContext`와 request attribute에 `userId`, `userExternalId`를 세팅한다.
- 토큰이 없으면 기존 `X-Device-Id` 흐름을 유지한다.
- 만료/위조 토큰은 401 + `ApiResponse<T>` 에러 포맷으로 응답한다.
- 공개 API: auth login/refresh, health, swagger, 공유 조회.

## 회원 탈퇴 정책

### 인증 토큰

| 항목 | 정책 |
|---|---|
| access token | 탈퇴 후 최대 1시간 유효 (JWT 필터는 DB 상태 미조회, 설계상 허용 범위) |
| refresh token | 탈퇴 즉시 차단 (`isDeleted \|\| isWithdrawn` 검사 → AUTH_003) |
| 재발급 | refresh 차단으로 1시간 후 자연 만료 |

### 재가입

- 탈퇴 시 provider+providerUserId null 처리 → 동일 OAuth 계정으로 재가입 가능
- 재가입 시 신규 User row 생성. 이전 데이터 복구 불가.

### 배포 전 필수 DB 마이그레이션

스크립트: `src/main/resources/db/user-withdrawal-migration.sql`

```sql
ALTER TABLE users ALTER COLUMN provider DROP NOT NULL;
ALTER TABLE users ALTER COLUMN provider_user_id DROP NOT NULL;
UPDATE users SET status = 'ACTIVE' WHERE status IS NULL;
ALTER TABLE users ALTER COLUMN status SET NOT NULL;
```

이 마이그레이션 없이 탈퇴 API를 호출하면 500 오류가 발생한다.

### OAuth 연결 해제

- `OAuthDisconnectService` 인터페이스 + `KakaoOAuthDisconnectService`, `GoogleOAuthDisconnectService` 구현 완료
- Kakao unlink: Admin Key 기반 HTTP 호출 실제 구현 완료
- Google revoke: `UserOAuthToken`에서 복호화한 access token으로 revoke 호출 구조 구현 완료
- AFTER_COMMIT 비동기. 실패해도 탈퇴 결과에 영향 없음

### 클라이언트 회원 탈퇴 UI

- 탈퇴 전 안내: 복구 불가, 소셜 계정 자체는 유지됨
- `DELETE /api/v1/users/me` 호출 (JWT 필수)
- Web/native 확인 모달 대응
- 성공(204): 로컬 인증 정보 삭제 → 로그인 화면 이동

## 사진 전략

- 원본: 클라이언트 폰 보관
- 서버: medium + thumbnail만 S3 저장
- Pet 프로필 사진: 현재 `photoBase64` 유지
- 리사이즈: 클라이언트에서 수행, 서버는 받은 파일 저장
- S3 key:
  - `photos/{petExternalId}/{photoExternalId}/medium.jpg`
  - `photos/{petExternalId}/{photoExternalId}/thumb.jpg`
- 서버 파일 제한:
  - JPEG/PNG만 허용
  - medium 1MB
  - thumbnail 200KB

## S3 / 환경변수 메모

사진 업로드 중 `S3Exception: The specified bucket is not valid`가 발생했던 원인은 IAM access key 자체 문제가 아니라 Docker 컨테이너에 AWS/S3 환경변수가 전달되지 않던 문제였다.

`AWS_S3_BUCKET`에는 `s3://`, URL, 경로를 넣지 않고 순수 버킷명만 넣어야 한다.

주의할 불일치:

- `application.yaml`은 S3 region으로 `AWS_S3_REGION`을 읽는다.
- `docker-compose.yml`은 현재 `AWS_REGION`을 전달한다.
- `AWS_REGION` / `AWS_S3_REGION` 표기를 정리해야 한다.
- `application.yaml`의 multipart 설정은 현재 `server.servlet.multipart` 아래에 있다. Spring Boot 표준 위치인 `spring.servlet.multipart`로 맞출지 확인 필요.
- `GOOGLE_CLIENT_ID` 단일 키 대신 `GOOGLE_WEB_CLIENT_ID`와 `GOOGLE_IOS_CLIENT_ID`로 분리됨.
- `OAUTH_TOKEN_ENCRYPTION_KEY`: AES-256-GCM 기반 OAuth 토큰 암호화 키. 운영 환경변수 필수.

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
- 회원 탈퇴 관련 자동 테스트 미작성

## 디자인 / 클라이언트 메모

- 톤: 차분/모던, 토스/당근 + 일기장
- 컬러: primary `#191F28`, accent `#E94B5A`, beta 주황 `#E8985C`
- 5탭: 홈/기록/포토/프로필/설정
- 자동저장 800ms debounce + 수동 저장 버튼
- 캘린더: `react-native-calendars`, KST 자정 자동 갱신
- 피드백: 카카오톡 오픈채팅 (BETA 배지 → 연결)

## 클라이언트 / 공유 UI 메모

- **반려동물 멀티 선택/추가 (2026-05-24)**: `AppHeader` 좌측 반려동물 이름 탭 → 드롭다운으로 전환/추가. 전환 시 홈/기록/포토/프로필 데이터 currentPet 기준 자동 갱신. 추가는 기존 프로필 입력 UI를 생성 모드로 재사용(`POST /api/v1/pets`). 선택한 ID는 `pet-care:api:current-pet-id`에 캐시.
- 포토탭 상단 오늘/선택 날짜 표시
- 캡션 수정 기능
- 사진 자체 수정 불가
- 수정 시 `수정됨` 표시
- 공유하기 버튼 및 공유 링크 생성 UI
- 삭제 완료 시 기록탭과 동일하게 `삭제되었습니다` 문구 표시
- 공유 UI 대상 파일:
  - `components/CaptionModal.tsx`
  - `app/photos/[externalId].tsx`
  - `api/share-photo.js`
- 공유 미리보기 모달과 실제 공유화면을 최대한 일치시킨다.
- 가능하면 미리보기/공유화면 모두 스크롤 없이 한 화면에 들어오도록 압축한다.
- 로고는 `32px x 32px`, `object-fit: contain`, `object-position: right center`.
- `app/photos/[externalId].tsx`와 `api/share-photo.js`의 로고 위치를 일관되게 유지한다.

## 클라이언트 오류 메모

`Error while reading cache, falling back to a full crawl: Unable to deserialize cloned data`는 Expo/Metro 캐시 손상 가능성이 높다.

우선:

```bash
npx expo start -c
```

필요 시 `.expo`, `node_modules/.cache`, temp metro/haste-map 캐시를 삭제한다.

## 의사결정 기록

- DB `ddl-auto`: update (Flyway 추후 검토). ddl-auto=update는 NOT NULL 제약 제거를 자동 처리하지 않으므로 스키마 변경 시 수동 마이그레이션 필요.
- 호스팅: Lightsail
- `kkori.co.kr` / `www.kkori.co.kr`: Vercel 유지
- `api.kkori.co.kr`: Lightsail API 전용
- 인증: OAuth 전용, 이메일/비밀번호 가입 없음
- Device는 소유 주체가 아니라 설치 기기/세션/푸시 보조 정보
- User가 데이터 소유 주체
- 기존 API 호환을 위해 `X-Device-Id` 흐름 유지
- `breed`는 서버 enum/목록으로 관리하지 않고 문자열 저장만 담당
- 알레르기/약/질환 등 상세 건강 필드는 향후 건강관리/AI 리포트 단계에서 추가
- 회원 탈퇴 access token 창(최대 1시간): JWT 필터의 stateless 특성 유지, DB 조회 없음. 소규모 앱에서 허용 범위로 결정.
- OAuth revoke/unlink는 인터페이스+스텁으로 설계. 탈퇴 API 성패에 영향 없음. 추후 실제 구현.

## 작업 스타일

- 사용자: 인디 해커, Spring Boot 경험 풍부, React Native는 비교적 초기
- 토큰 절약 선호
- 짧은 프롬프트 + 읽어야 할 파일 + 검증 포인트 분리 선호
- 한국어 `~요` 톤 선호
- 한 번에 한 단계, GitHub Desktop 커밋 자주
- client/api 프롬프트에는 "직접 화면을 열어서 실행/확인"하라는 문구를 넣지 않는다.
- 화면 확인은 사용자가 직접 진행한다.
- 프롬프트 검증은 "검증 포인트/기대 동작" 중심으로 작성한다.

## 로그 정책 (2026-05-27 적용)

- `logback-spring.xml`: 파일 로그 설정, 일자별 rolling, 30일 보관
  - `logs/app.log` — 전체 로그
  - `logs/error.log` — ERROR 레벨만
- `common/filter/RequestLoggingFilter`: 모든 HTTP 요청에 requestId 생성 → MDC + `X-Request-Id` 응답 헤더 + 접속 로그
  - 로그 형식: `HTTP {method} {path} {status} {elapsedMs}ms requestId={id} [userId={id}]`
- `prod` 프로파일: `com.kkori.api` → INFO (DEBUG 비활성화), Hibernate SQL 억제
- 로그 레벨 정책:
  - JWT 인증/인가 실패 → WARN (`JwtAuthenticationFilter`)
  - OAuth 검증 단계별 실패 reason → WARN (`GoogleOAuthVerifier`, `KakaoOAuthVerifier`)
  - AUTH_* BusinessException → WARN (`GlobalExceptionHandler`)
  - 5xx BusinessException, 예상 밖 예외 → ERROR
  - 로그인 성공, 토큰 재발급 성공 → INFO (`AuthService`)
- 운영 배포 시 `spring.profiles.active=prod` 설정 필요

## 다음 작업 후보

1. **[배포 전 필수]** 운영 DB 마이그레이션 — `user-withdrawal-migration.sql` + `user_oauth_token` DDL 수동 실행
2. 반려동물 삭제 API 연동 (프로필 탭 → `DELETE /api/v1/pets/{externalId}` + 로컬 캐시 정리 + AppHeader 목록 갱신)
3. 실패 테스트 수정: `JwtAuthenticationFilterTest.invalidTokenReturns401()`
4. `AWS_REGION` / `AWS_S3_REGION` 표기 정리
5. multipart 설정 위치 확인 및 필요 시 `spring.servlet.multipart`로 이동
6. 8080 외부 포트 차단 여부 운영 환경에서 확인
7. 실제 Google/Kakao OAuth 실기기 로그인 QA + Google revoke 실기기 QA
8. 운영 `JWT_SECRET`, `GOOGLE_CLIENT_ID`, Kakao 키 설정 반영 및 배포 환경 확인
9. Vercel에 `kkori.co.kr` / `www.kkori.co.kr` 연결 및 정책/계정삭제 안내 페이지 배포

## 실행 명령

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
