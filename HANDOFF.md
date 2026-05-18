# 인수인계 노트

세션 종료 또는 컨텍스트 전환 시 작성. 다음 세션에서 빠르게 복귀하기 위함.

---

## 현재 상태

마지막 업데이트: 2026-05-18

### 완료된 작업

#### Phase A: 로컬 백엔드 구축 (완료)
- **A-1**: Spring Boot 3.5.14 + Java 21 프로젝트 세팅, GitHub 연결, PostgreSQL Docker 구성
- **A-2**: JPA 엔티티 설계
  - `BaseEntity` (createdAt, updatedAt — LocalDateTime, KST 직렬화)
  - `Device`, `Caregiver`, `Pet` (deviceId 포함), `DailyPhoto`, `DailyLog`
  - Enum: `MealAmount(NONE/LESS/NORMAL/MORE)`, `WaterAmount(LESS/NORMAL/MORE)`, `StoolCondition(NORMAL/SOFT/HARD/DIARRHEA)`, `UrineColor(PALE/NORMAL/DARK)`
- **A-3**: 공통 모듈
  - `ApiResponse<T>`, `ErrorResponse`, `ErrorCode`, `BusinessException`, `GlobalExceptionHandler`
  - `DeviceIdInterceptor` (X-Device-Id 헤더 검증, `/register` · `/health` 제외)
- **A-4**: REST API CRUD 완료 + 검증 완료
  - Device: `POST /api/v1/devices/register`, `GET /api/v1/devices/me`
  - Caregiver: CRUD `/api/v1/caregivers` — deviceId 격리
  - Pet: CRUD `/api/v1/pets` — deviceId 격리 (`Pet.deviceId` 컬럼)
  - DailyPhoto: CRUD `/api/v1/photos` — Pet 소유 디바이스 검증
  - DailyLog: CRUD `/api/v1/logs` — Pet 소유 디바이스 검증
  - externalId: 클라이언트 미전송 시 서버 UUID 자동 생성, UUID 형식 검증(@Pattern), 중복 409
  - 사진 필드 없음 (DailyPhoto.photoBase64, DailyLog.photoBase64List 제거 — Phase E에서 재구현)
  - 빈 응답(204 No Content) 처리 클라이언트 측 대응
  - 디바이스 격리: 다른 X-Device-Id로 접근 시 404
  - 중복 거부: 같은 (petId, date) 사진/로그
- **A-5**: springdoc-openapi 도입
  - `springdoc-openapi-starter-webmvc-ui:2.8.17` (Spring Boot 3.5.x 호환, 2.6.0은 NoSuchMethodError)
  - GET `/api-docs`, GET `/swagger-ui.html`
  - 전 컨트롤러 `@Tag` + `@Operation` 추가
- **A-6**: CORS 설정
  - `WebMvcConfig.addCorsMappings` — localhost:8081/19006/3000, `allowCredentials(true)`
  - `DeviceIdInterceptor`에 OPTIONS 통과 추가 (preflight가 `X-Device-Id` 없이 오는 것 허용)
- **A-7**: 로컬 테스트 (Postman) — 전 도메인 CRUD 검증 완료

#### Phase B: 클라이언트 연동 (완료)
- **B-1**: 클라이언트 API 호출 모듈 (`lib/api.ts`) 구현 (별도 저장소: kkori)
- **B-2**: 디바이스 ID 생성/저장 (expo-application)
- **B-3**: AsyncStorage → 서버 마이그레이션
  - API + AsyncStorage 캐시 패턴 (로컬 캐시 유지 + 서버 동기화)
  - 사진: 메타데이터 서버, base64 로컬 저장
  - 프로필/포토/기록/홈 탭 모두 연동 완료

#### Phase C: 배포 / 도메인 / HTTPS (운영 기준 확정)
- AWS Lightsail 새 1GB 서버로 이전 완료
  - 새 서버 Public IP: `13.124.220.29`
  - 이전 서버 IP `3.38.97.234`는 더 이상 운영 기준 IP가 아님
  - 512MB 서버에서 Docker/Gradle 빌드가 매우 느렸고, Spring Boot + PostgreSQL + Docker 운영에는 최소 1GB 이상 필요하다고 판단
- 도메인 결정
  - 루트 도메인: `kkori.co.kr`
  - API 서브도메인: `api.kkori.co.kr`
  - DNS A 레코드: `api.kkori.co.kr -> 13.124.220.29`
  - 운영 API URL: `https://api.kkori.co.kr`
  - 클라이언트 환경변수: `EXPO_PUBLIC_API_URL=https://api.kkori.co.kr`
- HTTPS 적용 완료
  - Nginx + Let's Encrypt + Certbot 사용
  - 인증서 도메인: `api.kkori.co.kr`
  - `certbot renew --dry-run` 성공
- Vercel 유지 결정
  - `kkori.co.kr` / `www.kkori.co.kr`: Vercel 웹/정책/공유 페이지 용도
  - `api.kkori.co.kr`: Lightsail Spring Boot API 용도
  - 향후 가족 공유 링크/메모리얼 웹 페이지 가능성이 있어 `kkori.co.kr` 전용 도메인 사용
- 현재 인프라 흐름
  - 앱 -> `https://api.kkori.co.kr` -> Nginx 443 -> Spring Boot 8080 -> PostgreSQL -> S3

---

## 다음 할 일 (우선순위)

1. **Phase A-8: 자동 테스트 코드** (미진행)
   - JUnit 5 + Mockito 단위 테스트 (서비스 레이어 80% 목표)
   - MockMvc 컨트롤러 테스트 (성공/실패 시나리오)
   - Testcontainers 통합 테스트 (선택)

2. **운영 안정화**
   - 8080 외부 공개 차단: HTTPS API 확인 후 Nginx 내부 프록시 포트로만 사용
   - 모니터링/로그 확인 체계 정리
   - 서버에서 직접 빌드하지 않고 로컬 또는 GitHub Actions에서 빌드 후 배포하는 방식 검토

3. **Phase E: 사진 업로드 API**
   - 중간 해상도(1080px) + 썸네일(300px) 업로드 API
   - S3 presigned URL 발급/업로드 흐름 정리

---

## 알려진 이슈

### 버그
- S3 업로드 관련 `NoResourceFoundException: No static resource api/v1/photos/.../upload`는 access key 문제가 아니라 서버 Controller 매핑 또는 배포 코드 불일치 가능성이 높음

### 기술 부채
- `application.yaml` 환경 분리 필요 (dev / prod 프로파일)
- B-4 오프라인 대응 미구현 (클라이언트 측)
- 운영 배포 자동화 미구현 (서버 직접 Gradle/Docker 빌드 대신 로컬/GitHub Actions 빌드 후 배포 검토)

### 결정 필요
- 8080 외부 공개 차단 시점
- 사진 업로드 API 세부 계약(Presigned URL, 원본/중간/썸네일 보관 정책)

---

## 의사결정 기록

| 날짜 | 결정 | 이유 |
|------|------|------|
| 2026-05-15 | PostgreSQL 사용 | JSON 및 확장성 고려 |
| 2026-05-15 | Spring Boot 3.5.14 + Java 21 | 안정적인 LTS |
| 2026-05-15 | 연관관계 ID(FK)만 사용, 객체 참조 X | 단순성 유지 |
| 2026-05-15 | externalId (UUID String) 별도 관리 | 내부 PK 노출 방지 |
| 2026-05-15 | photoBase64 필드 제거 | Phase A 단순화, Phase E에서 전용 업로드 API |
| 2026-05-15 | externalId 서버 자동 생성 | 클라이언트 편의, 서버가 UUID 생성 후 응답에 포함 (Device 제외 전 도메인) |
| 2026-05-16 | Pet에 deviceId 컬럼 추가 (denormalize) | Photo/Log 접근 시 매번 Caregiver 조인 없이 Pet에서 바로 소유 디바이스 확인 |
| 2026-05-16 | Enum 단순화 (4종 모두 재정의) | 클라이언트-서버 계약 명확화, 불필요한 값 제거 |
| 2026-05-16 | 시간 필드 LocalDateTime + KST 직렬화 | 클라이언트가 타임존 변환 없이 바로 표시 가능 (Jackson timezone: Asia/Seoul) |
| 2026-05-17 | springdoc-openapi 2.8.17 선택 | 2.6.0은 Spring Boot 3.5.x(Spring Framework 6.2.x)와 NoSuchMethodError; 2.8.9+에서 수정 |
| 2026-05-17 | CORS allowCredentials(true) + 명시적 Origin | allowedOriginPatterns("*")는 쿠키 불가, 추후 JWT 도입 고려해 credentials 유지 |
| 2026-05-17 | DeviceIdInterceptor OPTIONS 통과 | preflight는 헤더 없이 오므로 인터셉터에서 막으면 CORS 응답 자체가 실패 |
| 2026-05-17 | 사진 원본 폰 저장, 서버는 메타데이터만 | Phase A/B 단순화; 향후 Phase E에서 중간(1080)+썸네일(300) 업로드 API 별도 설계 |
| 2026-05-17 | Phase B 캐시 전략: API + AsyncStorage 병행 | 오프라인 UX 유지하면서 서버 동기화; 사진 base64는 로컬, 나머지 메타는 서버 |
| 2026-05-18 | 도메인 `kkori.co.kr`, API `api.kkori.co.kr` 확정 | 가족 공유 링크/메모리얼 웹 페이지 가능성을 고려해 전용 도메인 사용 |
| 2026-05-18 | Lightsail 1GB 서버로 이전 | 512MB 서버에서 Docker/Gradle 빌드가 매우 느렸고, Spring Boot + PostgreSQL + Docker 운영에는 최소 1GB 이상 필요 |
| 2026-05-18 | HTTPS는 Nginx + Let's Encrypt + Certbot 사용 | 무료 인증서로 충분하며 `certbot renew --dry-run` 성공 |
| 2026-05-18 | Vercel 유지 | `kkori.co.kr` / `www.kkori.co.kr`은 웹/정책/공유 페이지, `api.kkori.co.kr`은 Lightsail API로 분리 |

---

## 환경 정보

### 로컬
- PostgreSQL: localhost:5432 (db: kkori, user: postgres, pw: kkori)
- 서버: localhost:8080
- Docker: kkori-pg 컨테이너
- Java: 21
- Spring Boot: 3.5.14

### 운영
- 서버: AWS Lightsail `kkori-api`
- 리전: Seoul, Zone A (ap-northeast-2a)
- OS: Ubuntu 24.04 LTS
- Public IP: `13.124.220.29`
- 이전 서버 IP: `3.38.97.234` (과거 IP, 현재 운영 기준 아님)
- 운영 API URL: `https://api.kkori.co.kr`
- 클라이언트 환경변수: `EXPO_PUBLIC_API_URL=https://api.kkori.co.kr`
- HTTPS: Nginx + Let's Encrypt + Certbot
- 열려 있어야 하는 포트: 22(SSH), 80(HTTP/Certbot 갱신/HTTPS redirect), 443(HTTPS API)
- 향후 닫을 포트: 8080(외부 공개 차단, Nginx 내부 프록시로만 사용)
- S3 환경변수: Lightsail에서 S3 접근 시 IAM Role을 따로 붙인 것이 아니라면 `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_REGION`, `AWS_S3_BUCKET` 필요

---

## 자주 쓰는 명령

```bash
# 서버 실행
./gradlew bootRun

# 컴파일만
./gradlew compileJava

# 테스트
./gradlew test

# 빌드
./gradlew build

# PostgreSQL Docker
docker start kkori-pg
docker stop kkori-pg

# DB 초기화 (스키마 변경 후 필요 시)
docker exec -it kkori-pg psql -U postgres -d kkori -c "DROP SCHEMA public CASCADE; CREATE SCHEMA public;"
```
