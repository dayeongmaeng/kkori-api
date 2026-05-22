# 꼬리(kkori) 프로젝트 인수인계

기준 문서: `apiserver.md`
마지막 갱신: 2026-05-22

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
- 🔄 Phase D: OAuth/JWT 인증 1차 구현 완료 + 운영/QA 진행
  - User 소유권 전환
  - Google/Kakao OAuth 검증
  - JWT access/refresh 발급
  - refresh API
  - logout API와 refreshToken 해시 폐기
  - refreshToken rotation은 TODO
- ⬜ Phase F: AI 리포트 미진행

## 서버 데이터 모델

BaseEntity 기반 `createdAt`, `updatedAt` 사용.

- User
  - OAuth 전용 계정 소유자
  - `externalId`, `provider`, `providerUserId`, nullable email/nickname/profileImageUrl
  - `provider + providerUserId` unique
- Device
  - 설치 기기/세션/푸시 보조 정보
  - `externalId`, `platform`, `userId nullable`
  - 기존 `X-Device-Id` 흐름 유지
- Caregiver
  - 보호자
  - `deviceId`, name, role, color
- Pet
  - `deviceId nullable`, `userId nullable`
  - name, species, gender, breed, birthDate, birthDateUnknown, adoptionDate, weightKg, neutered, medicalNotes, photoBase64
  - userId 우선, deviceId fallback
- DailyPhoto
  - `petId`, `caregiverId`, date, caption, mediumUrl, thumbnailUrl
  - unique(`petId`, `date`)
- DailyLog
  - `petId`, `caregiverId`, date, meal, water, walkMinutes, pooCondition, urineColor, condition, weightKg, memo
  - unique(`petId`, `date`)
- DailyLogPhoto
  - DailyLog별 최대 3장
  - `dailyLogId`, `petId`, `caregiverId`, date, mediumUrl, thumbnailUrl, sortOrder
- RevokedRefreshToken
  - 로그아웃된 refreshToken 해시 저장

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

## 테스트 상태

- 일부 자동 테스트 작성됨:
  - AuthService
  - JWT issuer/verifier
  - OAuth verifier
  - JWT filter
  - PetService
  - DailyPhotoService/DTO
- 2026-05-22 기준 `java -jar gradle\wrapper\gradle-wrapper.jar test`
  - 26 tests completed
  - 1 failed
- 실패:
  - `JwtAuthenticationFilterTest.invalidTokenReturns401()`
  - 테스트용 `new ObjectMapper()`가 `ApiResponse.timestamp(LocalDateTime)` 직렬화 모듈을 못 찾아 실패
  - 운영 ObjectMapper 문제라기보다 테스트 구성 문제에 가까움

## 디자인 / 클라이언트 메모

- 톤: 차분/모던, 토스/당근 + 일기장
- 컬러: primary `#191F28`, accent `#E94B5A`, beta 주황 `#E8985C`
- 5탭: 홈/기록/포토/프로필/설정
- 자동저장 800ms debounce + 수동 저장 버튼
- 캘린더: `react-native-calendars`, KST 자정 자동 갱신
- 피드백: 카카오톡 오픈채팅 (BETA 배지 → 연결)

## 클라이언트 / 공유 UI 메모

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

- DB `ddl-auto`: update (Flyway 추후 검토)
- 호스팅: Lightsail
- `kkori.co.kr` / `www.kkori.co.kr`: Vercel 유지
- `api.kkori.co.kr`: Lightsail API 전용
- 인증: OAuth 전용, 이메일/비밀번호 가입 없음
- Device는 소유 주체가 아니라 설치 기기/세션/푸시 보조 정보
- User가 데이터 소유 주체
- 기존 API 호환을 위해 `X-Device-Id` 흐름 유지
- `breed`는 서버 enum/목록으로 관리하지 않고 문자열 저장만 담당
- 알레르기/약/질환 등 상세 건강 필드는 향후 건강관리/AI 리포트 단계에서 추가

## 작업 스타일

- 사용자: 인디 해커, Spring Boot 경험 풍부, React Native는 비교적 초기
- 토큰 절약 선호
- 짧은 프롬프트 + 읽어야 할 파일 + 검증 포인트 분리 선호
- 한국어 `~요` 톤 선호
- 한 번에 한 단계, GitHub Desktop 커밋 자주
- client/api 프롬프트에는 "직접 화면을 열어서 실행/확인"하라는 문구를 넣지 않는다.
- 화면 확인은 사용자가 직접 진행한다.
- 프롬프트 검증은 "검증 포인트/기대 동작" 중심으로 작성한다.

## 다음 작업 후보

1. 실패 테스트 수정: `JwtAuthenticationFilterTest.invalidTokenReturns401()`
2. `AWS_REGION` / `AWS_S3_REGION` 표기 정리
3. multipart 설정 위치 확인 및 필요 시 `spring.servlet.multipart`로 이동
4. 8080 외부 포트 차단 여부 운영 환경에서 확인
5. 실제 Google/Kakao OAuth 실기기 로그인 QA
6. 운영 `JWT_SECRET`, `GOOGLE_CLIENT_ID`, Kakao 키 설정 반영 및 배포 환경 확인
7. 프로필탭 API/클라이언트 QA
8. Vercel에 `kkori.co.kr` / `www.kkori.co.kr` 연결 및 정책/계정삭제 안내 페이지 준비

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
