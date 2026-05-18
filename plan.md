# 꼬리 API 개발 로드맵

마지막 업데이트: 2026-05-18

## Phase A: 로컬 백엔드 구축

- [x] A-1: Spring Boot 프로젝트 세팅
- [x] A-2: 엔티티 + DB 설계
  - Device, Caregiver, Pet, DailyPhoto, DailyLog
  - BaseEntity (`createdAt`, `updatedAt`, Auditing)
  - Enum: Species, CaregiverRole, Platform, MealAmount, WaterAmount, StoolCondition, UrineColor
  - 인덱스: `deviceId`, `externalId(unique)`, `(petId + date)` unique constraint
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
  - DailyLog API (`/api/v1/logs`)
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
- [ ] A-8: 자동 테스트 코드
  - JUnit 5 + Mockito 단위 테스트
  - MockMvc 컨트롤러 테스트
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
- [ ] B-5: 오프라인 UX 정리

## Phase C: 배포 / 도메인 / HTTPS

- [x] C-1: 호스팅 선택 및 서버 이전
  - AWS Lightsail 사용
  - 기존 512MB 서버에서 새 1GB 서버로 이전 완료
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
- [ ] C-6: 8080 외부 포트 닫기 확인
  - 22, 80, 443은 열어 둔다.
  - 8080은 Nginx 내부 프록시로만 사용한다.
  - 현재 상태는 닫기 예정/확인 필요.
- [ ] C-7: 모니터링/로그 체계 정리
- [ ] C-8: 배포 방식 개선
  - 서버 직접 빌드 대신 로컬/GitHub Actions 빌드 후 배포 검토

## Phase D: 인증

- [ ] D-1: 회원가입 설계 (이메일/비밀번호 또는 소셜)
- [ ] D-2: JWT 발급/검증
- [ ] D-3: 디바이스 ID -> User 연결 마이그레이션
- [ ] D-4: 카카오/애플 로그인 검토

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
  - 해결: `docker-compose.yml`의 `api.environment`에 AWS/S3 환경변수 추가
  - `AWS_S3_BUCKET`은 순수 버킷명만 사용
- [ ] E-6: 업로드 실패 처리 UX
- [ ] E-7: 로딩/재시도 UI
- [ ] E-8: thumbnail/medium 표시 품질 확인

## Phase F: AI 리포트

- [ ] F-1: 병원 방문용 리포트
- [ ] F-2: 보호자 확인용 주간 리포트
- [ ] F-3: PDF 생성
- [ ] F-4: 주간 리포트 알림

## 다음 작업 후보

1. 8080 외부 포트 닫기 확인
2. 업로드 실패/재시도 UX 정리
3. Vercel에 `kkori.co.kr` / `www.kkori.co.kr` 연결
4. 개인정보처리방침/계정삭제 안내 페이지 준비
5. Phase D 로그인/회원가입 설계

## 운영 검증 체크리스트

- [x] `api.kkori.co.kr -> 13.124.220.29`
- [x] `https://api.kkori.co.kr`
- [x] Lightsail 1GB 이전 완료
- [x] HTTPS/Nginx/Certbot 완료
- [x] S3 업로드 정상 동작
- [x] S3 문제 원인 기록: Docker env 전달 누락
- [x] 클라이언트 검증 완료
- [x] Vercel은 웹/정책/공유 페이지용 유지
- [x] `EXPO_PUBLIC_API_URL=https://api.kkori.co.kr`
- [ ] 8080 외부 포트 닫기 확인 필요
