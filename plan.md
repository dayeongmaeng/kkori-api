# 꼬리 API 개발 로드맵

## Phase A: 로컬 백엔드 구축
- [x] A-1: Spring Boot 프로젝트 세팅
- [ ] A-2: 엔티티 + DB 설계
    - Device, Caregiver, Pet, DailyPhoto, DailyLog
    - JPA Auditing 설정
    - 인덱스 설계 (deviceId, petId+date 등)
- [ ] A-3: 공통 모듈
    - ApiResponse, ErrorResponse
    - GlobalExceptionHandler
    - 디바이스 ID 인터셉터
- [ ] A-4: REST API CRUD
    - Pet API
    - Caregiver API
    - DailyPhoto API
    - DailyLog API
- [ ] A-5: 로컬 테스트 (Postman / IntelliJ HTTP Client)

## Phase B: 클라이언트 연동
- [ ] B-1: 클라이언트 API 호출 모듈 (lib/api.ts)
- [ ] B-2: 디바이스 ID 생성/저장 (expo-application)
- [ ] B-3: 기존 AsyncStorage → 서버 마이그레이션
    - 로컬 우선 + 서버 동기화 전략
    - 또는 서버 전용으로 전환
- [ ] B-4: 오프라인 대응

## Phase C: 배포
- [ ] C-1: 호스팅 선택 (Railway / Render / Fly.io)
- [ ] C-2: PostgreSQL 클라우드 (Supabase DB / Railway PG / Neon)
- [ ] C-3: 환경 변수 (application-prod.yml)
- [ ] C-4: HTTPS + 도메인
- [ ] C-5: 모니터링 (로그, 메트릭)

## Phase D: 인증
- [ ] D-1: 회원가입 (이메일 + 비밀번호 또는 소셜)
- [ ] D-2: JWT 발급/검증
- [ ] D-3: 디바이스 ID → User 연결 마이그레이션
- [ ] D-4: 카카오 / 애플 로그인

## Phase E: 사진 클라우드 저장
- [ ] E-1: S3 또는 Cloudflare R2 선택
- [ ] E-2: Presigned URL 발급
- [ ] E-3: 사진 압축 + CDN
- [ ] E-4: 기존 base64 데이터 마이그레이션

## Phase F: AI 리포트
- [ ] F-1: 병원 방문용 리포트 (Claude/OpenAI API)
- [ ] F-2: 보호자 확인용 주간 리포트
- [ ] F-3: PDF 생성
- [ ] F-4: 알림 (주간 리포트 알림)

## 미정 / 검토 필요
- 가족 공유 (다중 디바이스 → 같은 펫 공유)
- 심박수 / 호흡수 측정 기능
- 약 관리 + 알림
- 메모리얼 모드
- 어필리에이트 (사료/영양제)