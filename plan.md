# 꼬리 API 개발 로드맵

기준 문서: `apiserver.md`
마지막 업데이트: 2026-05-27

## Phase A: 로컬 백엔드 구축

- [x] A-1: Spring Boot 프로젝트 세팅
- [x] A-2: 엔티티 + DB 설계
  - Device, Caregiver, Pet, DailyPhoto, DailyLog
  - User, RevokedRefreshToken
  - DailyLogPhoto
  - BaseEntity (`createdAt`, `updatedAt`, Auditing)
  - Enum: Species, Gender, CaregiverRole, Platform, MealAmount, WaterAmount, StoolCondition, UrineColor, OAuthProvider
  - unique: `externalId`, `(petId + date)`, `provider + providerUserId`
- [x] A-3: 공통 모듈
  - `ApiResponse<T>`, `ErrorResponse`
  - `ErrorCode`, `BusinessException`, `GlobalExceptionHandler`
  - `DeviceIdInterceptor` (`X-Device-Id` 헤더)
  - `WebMvcConfig`, `HealthController`
- [x] A-4: REST API CRUD
  - Device API (`POST /api/v1/devices/register`, `GET /api/v1/devices/me`)
  - Caregiver API (`/api/v1/caregivers`)
  - Pet API (`/api/v1/pets`)
  - DailyPhoto API (`/api/v1/photos`)
  - DailyLog API (`/api/v1/logs`, `/api/v1/daily-logs`)
  - DailyLogPhoto 업로드/삭제 API
  - externalId 서버 자동 생성, UUID 검증, 중복 체크
  - 빈 응답 204 처리
- [x] A-5: springdoc-openapi
  - `springdoc-openapi-starter-webmvc-ui:2.8.17`
  - `/api-docs`, `/swagger-ui.html`
- [x] A-6: CORS 설정
  - localhost 개발 포트 허용
  - `allowCredentials(true)`
  - OPTIONS preflight 통과
- [x] A-7: 로컬 수동 테스트
- [ ] A-8: 자동 테스트 코드 (일부 작성, 현재 1건 실패)
  - AuthService, JWT issuer/verifier, OAuth verifier, JWT filter, PetService, DailyPhotoService/DTO 테스트 작성
  - 2026-05-22 기준 26 tests completed, 1 failed
  - 실패: `JwtAuthenticationFilterTest.invalidTokenReturns401()`
  - 원인: 테스트용 `new ObjectMapper()`가 `ApiResponse.timestamp(LocalDateTime)` 직렬화 모듈을 못 찾음
- [ ] A-9: 테스트 보강
  - 실패 테스트 수정
  - MockMvc 컨트롤러 테스트 검토
  - Testcontainers 통합 테스트 검토

## Phase B: 클라이언트 연동

- [x] B-1: 클라이언트 API 호출 모듈 (`lib/api.ts`)
- [x] B-2: 디바이스 ID 생성/저장 (`expo-application`)
- [x] B-3: AsyncStorage 기반 데이터의 서버 연동
  - API + AsyncStorage 캐시 패턴
  - 사진 메타데이터 서버 저장
  - 프로필/포토/기록/캘린더 연동
- [x] B-4: 운영 API URL 전환
  - `EXPO_PUBLIC_API_URL=https://api.kkori.co.kr`
- [x] B-5: 오프라인 UX 정리

## Phase C: 배포 / 도메인 / HTTPS

- [x] C-1: 호스팅 선택 및 서버 이전
  - AWS Lightsail 사용
  - 현재 운영 Public IP: `13.124.220.29`
  - 이전 서버 IP `3.38.97.234`는 더 이상 운영 기준 IP 아님
- [x] C-2: 운영 DB 구성
  - PostgreSQL 16 컨테이너 정상 구동 확인
- [x] C-3: 운영 API 컨테이너 구성
  - Spring Boot API 컨테이너 정상 구동 확인
  - 현재 운영 흐름: 앱 -> `https://api.kkori.co.kr` -> Nginx 443 -> Spring Boot 8080 -> PostgreSQL 16 -> S3
- [x] C-4: 도메인 / DNS / HTTPS
  - 도메인: `kkori.co.kr`
  - API 도메인: `api.kkori.co.kr`
  - DNS A 레코드: `api.kkori.co.kr -> 13.124.220.29`
  - 운영 API URL: `https://api.kkori.co.kr`
  - Nginx + Let's Encrypt + Certbot 적용 완료
  - 인증서 도메인: `api.kkori.co.kr`
  - `certbot renew --dry-run` 성공
- [x] C-5: 웹/Vercel 역할 정리
  - `kkori.co.kr` / `www.kkori.co.kr`는 Vercel 유지
  - 용도: 웹 랜딩, 개인정보처리방침, 계정삭제 안내, 가족 공유/메모리얼 페이지
  - `api.kkori.co.kr`만 Lightsail API 서버로 연결
- [x] C-6: 8080 외부 포트 닫기 확인
  - 22, 80, 443은 열어 둔다.
  - 8080은 Nginx 내부 프록시로만 사용한다.
  - 코드의 `docker-compose.yml`은 현재 `8080:8080`으로 바인딩한다.
  - 실제 외부 차단 여부는 Lightsail 방화벽/Nginx 기준으로 확인 필요.
- [x] C-7: 모니터링/로그 체계 정리
  - requestId 기반 MDC 추적 (`RequestLoggingFilter`, `X-Request-Id` 헤더)
  - 파일 로그: `logs/app.log`, `logs/error.log` — 일자별 rolling, 30일 보관 (`logback-spring.xml`)
  - 레벨 정책: JWT/OAuth 인증 실패 → WARN, 5xx/미처리 예외 → ERROR, 주요 성공 흐름 → INFO
  - `prod` 프로파일: DEBUG 비활성화 (`spring.profiles.active=prod` 설정 필요)
- [ ] C-8: 배포 방식 개선
  - 서버 직접 빌드 대신 로컬/GitHub Actions 빌드 후 배포 검토
- [ ] C-9: 환경변수 표기 정리
  - `application.yaml`은 `AWS_S3_REGION`을 읽음
  - `docker-compose.yml`은 `AWS_REGION`을 전달함
  - `AWS_REGION` / `AWS_S3_REGION` 중 하나로 일관화 필요
- [ ] C-10: multipart 설정 위치 확인
  - 현재 `application.yaml`은 `server.servlet.multipart` 아래에 있음
  - Spring Boot 표준 위치인 `spring.servlet.multipart` 적용 여부 확인 필요

## Phase D: 인증

- [x] D-1: OAuth 전용 회원가입/로그인 설계
  - 이메일/비밀번호 가입 없음
  - provider enum: `GOOGLE`, `KAKAO`, `APPLE`
  - 우선 구현: `GOOGLE`, `KAKAO`
- [x] D-2: User 엔티티 추가
  - `externalId` UUID
  - `provider`, `providerUserId`
  - `email`, `nickname`, `profileImageUrl` nullable
  - `provider + providerUserId` unique
  - BaseEntity 상속
- [x] D-3: OAuth 로그인 API 추가
  - `POST /api/v1/auth/oauth/login`
  - 요청: provider, idToken/accessToken/code/redirectUri, deviceExternalId
  - 응답: JWT accessToken/refreshToken, user
- [x] D-4: JWT access/refresh 발급 구조 추가
  - 민감정보/OAuth 토큰 로그 금지
  - provider별 verifier 구조 추가
  - Google/Kakao verifier 실제 검증 구현 완료
- [x] D-5: Device 역할 전환
  - Device는 설치 기기/세션/푸시 보조 정보로 사용
  - `Device.userId`로 User 연결
  - 기존 `X-Device-Id` 흐름 유지
- [x] D-6: 기존 Device 기반 데이터 단계적 마이그레이션
  - `Pet.userId` nullable 추가
  - OAuth 로그인 성공 시 현재 deviceId의 기존 Pet들을 userId에 연결
  - DailyPhoto/DailyLog는 기존 petId 기반 유지
  - 조회/수정 권한 검증은 userId 우선, 없으면 기존 deviceId fallback
- [x] D-7: 실제 OAuth 토큰 검증 연동
  - Google idToken 검증
  - `GOOGLE_CLIENT_ID` audience 검증
  - Google providerUserId/email/profile 추출
  - Kakao accessToken 검증
  - Kakao authorization code 교환 흐름 구현
  - Kakao 사용자 정보 API 호출
  - Kakao providerUserId/email/profile 추출
  - OAuth 실패 시 401 BusinessException
  - Apple 로그인은 enum만 준비, 추후 구현
- [x] D-8: JWT 인증 골격 완성
  - 환경변수 `JWT_SECRET` 기반 설정
  - `JWT_SECRET` 미설정 또는 32자 미만이면 서버 시작 실패
  - `Authorization: Bearer {accessToken}` 검증 필터 추가
  - 유효하면 요청 컨텍스트에 `userId`, `userExternalId` 세팅
  - 만료/위조 토큰은 401
  - 토큰이 없으면 기존 `X-Device-Id` 흐름 fallback 유지
  - 공개 API(auth, health, swagger, 공유 조회)는 필터 제외
- [x] D-9: refresh 재발급 API 추가
  - `POST /api/v1/auth/refresh`
  - refreshToken 검증 후 새 accessToken 발급
  - 폐기된 refreshToken 해시는 차단
- [x] D-10: 로그아웃 API 추가
  - `POST /api/v1/auth/logout`
  - refreshToken 해시 폐기 저장
  - Kakao provider logout 일부 지원
- [ ] D-11: refreshToken rotation 구현
  - 현재 TODO
  - 로그아웃 기반 revocation/blacklist는 구현됨
- [ ] D-12: 운영 JWT/OAuth 설정 적용 및 QA
  - local/dev/prod 모두 `JWT_SECRET` 설정 필요
  - 운영 `GOOGLE_CLIENT_ID`, Kakao 키 확인 필요
  - 실제 Google/Kakao 실기기 로그인 QA 필요
- [x] D-13: 회원 탈퇴 API 서버 구현
  - `DELETE /api/v1/users/me` — JWT 인증 필수, 204 응답
  - User 개인정보 익명화 + status=WITHDRAWN + softDelete()
  - 소유 Pet cascade soft delete (PetService.deleteAllForUser() 재사용)
  - S3 이미지 비동기 삭제 (기존 PetImageCleanupEvent/Listener 재사용)
  - OAuth 연결 해제 인터페이스 + 스텁 구현 (TODO: 실제 HTTP 호출)
  - 탈퇴 후 refresh 차단 (AuthService.refresh()에 isWithdrawn 가드 추가)
  - 탈퇴 후 logout NPE 수정 (provider null 가드 추가)
  - 재가입: provider+providerUserId null 처리로 동일 계정 재가입 허용
- [ ] D-14: 회원 탈퇴 DB 마이그레이션 (운영 배포 전 필수)
  - `ALTER TABLE users ALTER COLUMN provider DROP NOT NULL`
  - `ALTER TABLE users ALTER COLUMN provider_user_id DROP NOT NULL`
  - `UPDATE users SET status = 'ACTIVE' WHERE status IS NULL`
  - `ALTER TABLE users ALTER COLUMN status SET NOT NULL`
  - 스크립트: `src/main/resources/db/user-withdrawal-migration.sql`
- [x] D-15: 회원 탈퇴 클라이언트 UI
  - 설정 탭 탈퇴 버튼
  - 안내 화면: 복구 불가, 소셜 계정 자체는 유지됨 안내
  - Web/native 확인 모달 대응
  - `DELETE /api/v1/users/me` 호출
  - 성공(204): 로컬 인증 정보 삭제 후 로그인 화면 이동
- [x] D-16: OAuth 연결 해제 실제 구현
  - Kakao unlink: Admin Key 기반 구현 완료 (`KakaoOAuthDisconnectService`)
  - Google revoke: `UserOAuthToken` 기반 구조 구현 완료 (`GoogleOAuthDisconnectService`)
- [x] D-17: Google OAuth token 암호화 저장 구조
  - `UserOAuthToken` 엔티티 (`user_oauth_token` 테이블)
  - AES-256-GCM 암호화
  - revoke 성공 시 `revokedAt` 기록
  - 클라이언트에서 Google access token 전달 구조 추가
- [ ] D-18: 운영 DB 마이그레이션 (배포 전 필수)
  - `user-withdrawal-migration.sql` 운영 적용
  - `user_oauth_token` 테이블 DDL 운영 적용 여부 확인
- [ ] D-19: Google revoke 실기기 QA
  - UserOAuthToken 저장 → 탈퇴 → revoke 호출 확인
- [x] D-20: Google 로그인 Web/iOS 분리 지원
  - Web: `response_type=id_token token`, `EXPO_PUBLIC_GOOGLE_WEB_CLIENT_ID`, access token 서버 전달
  - iOS: native OAuth 흐름, `EXPO_PUBLIC_GOOGLE_IOS_CLIENT_ID`, idToken 기반
  - 서버 audience 검증: `GOOGLE_WEB_CLIENT_ID` + `GOOGLE_IOS_CLIENT_ID` 모두 허용
  - `OAuthTokenEncryptor` + `UserOAuthToken` 저장 구조 (AES-256-GCM), `OAUTH_TOKEN_ENCRYPTION_KEY` 신규 환경변수

## Phase E: 사진 클라우드 저장

- [x] E-1: S3 사용 기준 확정
- [x] E-2: 서버 사진 업로드 API 구현
- [x] E-3: S3 사진 업로드 서버/클라이언트 연동
- [x] E-4: 실기기 검증 완료
  - 펫 조회
  - 일일 기록 저장/조회
  - 사진 메타 생성
  - 사진 업로드
  - 앱 재실행 후 서버/캐시 데이터 확인
- [x] E-5: Docker 환경변수 누락 문제 해결
  - 원인: 컨테이너에 AWS/S3 환경변수가 전달되지 않음
  - `AWS_S3_BUCKET`은 순수 버킷명만 사용
- [x] E-6: 기록 사진 기능 추가
  - DailyLog별 최대 3장
  - S3 업로드
  - `thumbnailUrl`, `mediumUrl` 저장
  - 조회 응답에 사진 목록 포함
  - 삭제 API 추가
  - 디바이스 격리/소유권 검증 유지
- [x] E-7: 공유 조회 API 추가
  - `GET /api/v1/photos/{externalId}/share`
- [x] E-8: 업로드 실패 처리 UX
- [x] E-9: 로딩/재시도 UI
- [x] E-10: thumbnail/medium 표시 품질 확인
- [x] E-11: 기록 사진 큰 이미지 보기
- [x] E-12: 기록 사진 삭제/재시도 UX
- [x] E-13: 기록 사진 UI 수정
  - X 버튼 잘림 해결

## Phase F: AI 리포트

- [ ] F-1: 병원 방문용 리포트
- [ ] F-2: 보호자 확인용 주간 리포트
- [ ] F-3: PDF 생성
- [ ] F-4: 주간 리포트 알림

## 프로필탭 고도화

- [x] P-1: Pet 프로필 필드 확장
  - `gender`: `MALE` / `FEMALE`
  - `adoptionDate`: 함께한 날, nullable
  - `birthDateUnknown`: 생일 모름 여부, boolean
  - `birthDateUnknown=true`이면 `birthDate` nullable 허용
  - `birthDateUnknown=false`이면 `birthDate` 필수 검증
  - `weightKg`, `neutered`, `medicalNotes`, `photoBase64`
- [x] P-2: 서버 정책 정리
  - `breed`는 서버 enum/목록으로 관리하지 않고 string 유지
  - 서버는 `breed` 문자열 저장만 담당
- [x] P-3: 클라이언트 입력 UX 정리
  - 품종 추천은 클라이언트 상수 기반 자동완성으로 처리
  - 품종 자유입력 허용
  - MVP에서는 입력 부담 최소화 우선
  - 알레르기/약/질환 등 건강 세부 필드는 추후 추가
- [ ] P-4: 정책/코드 불일치 확인
  - 문서 정책은 현재 강아지만 유지
  - 코드상 `Species`는 `DOG`, `CAT` 둘 다 열려 있음
  - 정책에 맞춰 서버 검증을 추가할지, 문서를 `DOG/CAT enum 준비`로 바꿀지 결정 필요
- [ ] P-5: 프로필 API/클라이언트 QA
  - 요청/응답, 저장/수정, 기존 데이터 호환성 확인

## 설정/프로필 UX 정리

- [x] S-1: 캐시 비우기 안정화
  - `AsyncStorage.clear()` 제거
  - 앱 전체 저장소 초기화 대신 캐시 키만 선별 삭제
  - 캐시 삭제 로직 `try/catch` 처리 완료
- [x] S-2: 정책/약관/업데이트 소식 연결
  - 정책/약관/업데이트 소식은 Notion 공개 링크로 연결 완료
- [x] S-3: 알림/권한 섹션 정리
  - 알림 섹션은 전체 주석 처리
  - 알림 권한은 권한 섹션으로 이동
- [x] S-4: 이스터에그/후원 UI 정리
  - `꼬리 흔들게 하기`는 이스터에그로 유지
  - `꼬리 응원하기`는 토스 후원 링크 기반 후원 UI 추가 완료

## 운영 검증 체크리스트

- [x] `api.kkori.co.kr -> 13.124.220.29`
- [x] `https://api.kkori.co.kr`
- [x] Lightsail 1GB 이전 완료
- [x] HTTPS/Nginx/Certbot 완료
- [x] S3 업로드 정상 동작
- [x] S3 문제 원인 기록: Docker env 전달 누락
- [x] 클라이언트 운영 API URL: `EXPO_PUBLIC_API_URL=https://api.kkori.co.kr`
- [x] Phase E 및 후속 UX 안정화 완료
- [x] 기록 사진 S3 업로드/조회/삭제 UX 완료
- [x] Vercel은 웹/정책/공유 페이지용 유지
- [x] OAuth 전용 User 소유권 전환 1차 구현 완료
- [x] JWT 인증 골격 및 refresh 재발급 API 구현 완료
- [x] 로그아웃 API와 refreshToken 해시 폐기 구현
- [x] 회원 탈퇴 API 서버 구현 완료 (D-13)
- [ ] **회원 탈퇴 DB 마이그레이션 (D-18)** — 배포 전 필수
- [ ] **운영 서버 `spring.profiles.active=prod` 설정** — 배포 전 필수 (DEBUG 비활성화, 파일 로그)
- [x] 회원 탈퇴 클라이언트 UI (D-15)
- [x] Kakao unlink 실제 구현 (D-16)
- [x] Google revoke 구조 구현 (D-16)
- [x] UserOAuthToken AES-256-GCM 암호화 저장 (D-17)
- [x] Google 로그인 Web/iOS 분리 지원 (D-20)
- [ ] Google revoke 실기기 QA (D-19)
- [ ] 8080 외부 포트 닫기 확인
- [ ] 운영 OAuth/JWT 환경변수 반영 확인 (`GOOGLE_WEB_CLIENT_ID`, `GOOGLE_IOS_CLIENT_ID`, `OAUTH_TOKEN_ENCRYPTION_KEY` 포함)
- [ ] Google/Kakao 실기기 로그인 QA
- [ ] 실패 테스트 수정

## 다음 작업 후보

1. **[배포 전 필수]** 운영 DB 마이그레이션 (D-18) — `user-withdrawal-migration.sql` + `user_oauth_token` DDL 수동 실행
2. **[배포 전 필수]** 운영 서버에 `spring.profiles.active=prod` 설정 — 파일 로그 활성화, DEBUG 비활성화
3. 반려동물 삭제 API 클라이언트 연동 — `DELETE /api/v1/pets/{externalId}` 호출 + 로컬 캐시 정리 + AppHeader 목록 갱신
4. 실패 테스트 수정: `JwtAuthenticationFilterTest.invalidTokenReturns401()`
5. `AWS_REGION` / `AWS_S3_REGION` 표기 정리
6. multipart 설정 위치 확인 및 필요 시 `spring.servlet.multipart`로 이동
7. 8080 외부 포트 차단 여부 운영 환경에서 확인
8. 실제 Google/Kakao OAuth 실기기 로그인 QA + Google revoke 실기기 QA (D-19)
9. 운영 `JWT_SECRET`, `GOOGLE_CLIENT_ID`, Kakao 키 설정 반영 및 배포 환경 확인
10. Vercel에 `kkori.co.kr` / `www.kkori.co.kr` 연결 및 정책/계정삭제 안내 페이지 배포
11. Phase F AI 리포트 설계
