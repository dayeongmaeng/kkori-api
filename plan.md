# 꼬리 API 개발 로드맵

## Phase A: 로컬 백엔드 구축
- [x] A-1: Spring Boot 프로젝트 세팅
- [x] A-2: 엔티티 + DB 설계
    - Device, Caregiver, Pet, DailyPhoto, DailyLog
    - BaseEntity (createdAt, updatedAt, @MappedSuperclass + Auditing)
    - JpaAuditingConfig
    - Enum: Species, CaregiverRole, Platform, MealAmount, WaterAmount, StoolCondition, UrineColor
    - 인덱스: deviceId, externalId(unique), (petId+date) unique constraint
- [x] A-3: 공통 모듈
    - ApiResponse<T>, ErrorResponse (record)
    - ErrorCode (enum, HttpStatus 포함)
    - BusinessException, GlobalExceptionHandler
    - DeviceIdInterceptor (X-Device-Id 헤더)
    - WebMvcConfig (인터셉터 등록)
    - HealthController (GET /api/v1/health)
- [x] A-4: REST API CRUD
    - Device API (POST /api/v1/devices/register, GET /me)
    - Caregiver API (CRUD /api/v1/caregivers) — deviceId 격리
    - Pet API (CRUD /api/v1/pets) — deviceId 격리
    - DailyPhoto API (CRUD /api/v1/photos) — Pet 소유 디바이스 검증
    - DailyLog API (CRUD /api/v1/logs) — Pet 소유 디바이스 검증
    - externalId 서버 자동 생성 (UUID), UUID 형식 검증, 중복 체크
    - Enum 정리: MealAmount, WaterAmount, StoolCondition, UrineColor
    - 빈 응답(204) 처리 클라이언트 측 대응
- [x] A-5: springdoc-openapi (Swagger UI)
    - springdoc-openapi-starter-webmvc-ui:2.8.17 (Spring Boot 3.5.x 호환)
    - /api-docs, /swagger-ui.html
    - 전 컨트롤러 @Tag + @Operation 추가
- [x] A-6: CORS 설정
    - WebMvcConfig.addCorsMappings — localhost:8081/19006/3000, allowCredentials(true)
    - DeviceIdInterceptor OPTIONS 통과 (preflight 수정)
- [x] A-7: 로컬 테스트 (Postman / IntelliJ HTTP Client)
- [ ] A-8: 자동 테스트 코드
    - 단위 테스트: JUnit 5 + Mockito (서비스 레이어 80% 목표)
    - 컨트롤러 테스트: MockMvc + @WebMvcTest (성공/실패 시나리오)
    - 통합 테스트: Testcontainers (PostgreSQL) — 선택

## Phase B: 클라이언트 연동
- [x] B-1: 클라이언트 API 호출 모듈 (lib/api.ts)
- [x] B-2: 디바이스 ID 생성/저장 (expo-application)
- [x] B-3: 기존 AsyncStorage → 서버 마이그레이션
    - API + AsyncStorage 캐시 패턴 (로컬 캐시 + 서버 동기화)
    - 사진: 메타데이터 서버, base64 로컬
    - 프로필/포토/기록/홈 탭 모두 연동
- [ ] B-4: 오프라인 대응

## Phase C: 배포
- [x] C-1: 호스팅 선택
    - AWS Lightsail 사용
    - 기존 512MB 최저 사양 서버에서 새 1GB 서버로 이전 완료
    - 새 서버 Public IP: 13.124.220.29
    - 이전 서버 IP 3.38.97.234는 더 이상 운영 기준 IP가 아님
- [x] C-2: 운영 DB 구성
    - 현재 운영 흐름: Nginx 443 → Spring Boot 8080 → PostgreSQL → S3
- [x] C-3: 운영 환경 변수 기준 정리
    - 클라이언트: EXPO_PUBLIC_API_URL=https://api.kkori.co.kr
    - Lightsail에서 S3 접근 시 IAM Role을 따로 붙인 것이 아니라면 AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY, AWS_REGION, AWS_S3_BUCKET 필요
- [x] C-4: HTTPS + 도메인
    - 도메인: kkori.co.kr
    - API 서브도메인: api.kkori.co.kr
    - DNS A 레코드: api.kkori.co.kr → 13.124.220.29
    - 운영 API URL: https://api.kkori.co.kr
    - Nginx + Let's Encrypt + Certbot 적용 완료
    - 인증서 도메인: api.kkori.co.kr
    - certbot renew --dry-run 성공
    - kkori.co.kr / www.kkori.co.kr은 Vercel 웹/정책/공유 페이지 용도로 유지
- [ ] C-5: 모니터링 (로그, 메트릭)
- [ ] C-6: 8080 외부 공개 차단
    - HTTPS API 확인 후 8080은 외부 공개를 닫고 Nginx 내부 프록시로만 접근
- [ ] C-7: 배포 방식 개선
    - 512MB 서버에서 Docker/Gradle 빌드가 매우 느렸음
    - Spring Boot + PostgreSQL + Docker 운영에는 최소 1GB 이상 필요하다고 판단
    - 추후 서버 직접 빌드 대신 로컬/GitHub Actions 빌드 후 배포 검토

## Phase D: 인증
- [ ] D-1: 회원가입 (이메일 + 비밀번호 또는 소셜)
- [ ] D-2: JWT 발급/검증
- [ ] D-3: 디바이스 ID → User 연결 마이그레이션
- [ ] D-4: 카카오 / 애플 로그인

## Phase E: 사진 클라우드 저장
- [x] E-1: S3 사용 기준
    - 운영 흐름에 S3 포함
    - NoResourceFoundException: No static resource api/v1/photos/.../upload 발생 시 access key 문제보다 서버 Controller 매핑 또는 배포 코드 불일치 가능성이 높음
- [ ] E-2: Presigned URL 발급
- [ ] E-3: 사진 압축 (중간 1080px + 썸네일 300px) + CDN
- [ ] E-4: 기존 base64 데이터 마이그레이션

## Phase F: AI 리포트
- [ ] F-1: 병원 방문용 리포트 (Claude/OpenAI API)
- [ ] F-2: 보호자 확인용 주간 리포트
- [ ] F-3: PDF 생성
- [ ] F-4: 알림 (주간 리포트 알림)

## 미정 / 검토 필요
- 가족 공유 (다중 디바이스 → 같은 펫 공유)
    - 향후 가족 공유 링크/메모리얼 웹 페이지 가능성이 있어 `kkori.co.kr` 전용 도메인 사용
- 심박수 / 호흡수 측정 기능
- 약 관리 + 알림
- 메모리얼 모드
- 어필리에이트 (사료/영양제)
