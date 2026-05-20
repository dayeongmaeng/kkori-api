# 꼬리 API
반려동물 일상 기록 앱 "꼬리"의 백엔드 서버.

## 프로젝트 컨텍스트
- 클라이언트: React Native + Expo (별도 저장소: kkori)
- 백엔드: 이 저장소 (kkori-api)
- 1인 개발 (인디 해커)

## 스택
- Spring Boot 3.5.14
- Java 21
- PostgreSQL 16
- JPA + Hibernate
- Gradle
- Lombok

## 서버 정보
- 인스턴스: kkori-api
- 리전: Seoul, Zone A (ap-northeast-2a)
- OS: Ubuntu 24.04 LTS
- Public IP: 13.124.220.29
- 사양: 1GB RAM, 2 vCPUs, 40GB SSD
- 이전 서버 IP: 3.38.97.234 (더 이상 운영 기준 IP 아님)

## 도메인 / 운영 인프라
- 루트 도메인: `kkori.co.kr`
- API 도메인: `api.kkori.co.kr`
- DNS A 레코드: `api.kkori.co.kr -> 13.124.220.29`
- 운영 API URL: `https://api.kkori.co.kr`
- 클라이언트 환경변수: `EXPO_PUBLIC_API_URL=https://api.kkori.co.kr`
- 클라이언트 개발 환경은 로컬 API, 운영 환경은 `https://api.kkori.co.kr` 사용
- Expo/React Native에서는 `__DEV__`로 개발/배포 구분 가능
- 실기기 개발 시 `localhost`는 폰 자신을 의미하므로 PC LAN IP 또는 `EXPO_PUBLIC_DEV_API_URL` 사용 필요
- HTTPS: Nginx + Let's Encrypt + Certbot 적용 완료
- 인증서 도메인: `api.kkori.co.kr`
- `certbot renew --dry-run` 성공
- Vercel 유지: `kkori.co.kr` / `www.kkori.co.kr`는 웹, 정책, 공유 페이지 용도
- Lightsail Spring Boot API: `api.kkori.co.kr` 전용
- 현재 흐름: 앱 -> `https://api.kkori.co.kr` -> Nginx 443 -> Spring Boot 8080 -> PostgreSQL -> S3

## 운영 포트
- 열려 있어야 함: 22(SSH), 80(HTTP/Certbot 갱신/HTTPS redirect), 443(HTTPS API)
- 8080은 Nginx 프록시 뒤 내부 포트로만 사용하고, 최종적으로 외부 공개를 닫는 방향

## 도메인 모델
- **Device**: 디바이스 ID 기반 소유자 식별 (회원가입 없이, 추후 User 계정과 연결)
- **Caregiver**: 보호자 (가족 공유 대비, 한 Device에 여러 Caregiver 가능)
- **Pet**: 반려동물 (이름, 성별, 견종, 생일/생일 모름, 함께한 날, 체중, 중성화, 메모, 사진)
- **DailyPhoto**: 하루 한 장 데일리 포토 (petId + date unique)
- **DailyLog**: 일일 건강 기록 (식사/산책/배변/소변/컨디션/체중/메모)
- **DailyLogPhoto**: 기록탭 사진 (DailyLog별 최대 3장, S3 medium/thumbnail URL 저장)

## 패키지 구조
com.kkori.api  
├── common         # 공통 응답, 예외, 설정  
├── device         # 디바이스 관리  
├── caregiver      # 보호자  
├── pet            # 반려동물  
├── photo          # 데일리 포토  
└── log            # 일일 기록  
각 도메인 하위:  
├── controller  
├── service  
├── repository  
├── entity  
└── dto  
├── request  
└── response  

## 코딩 규칙

### 일반
- Java 21 기능 적극 활용 (record, switch expression, pattern matching)
- Lombok 사용 (`@Getter`, `@Builder`, `@RequiredArgsConstructor`)
- 불변 객체 선호 (`@Builder` + 필드 final)
- DTO 사용, 엔티티 직접 노출 금지
- 불필요한 리팩터링 금지
- React Native + Expo + TypeScript strict 유지
- 공유 API/조회 로직은 UI 수정 중 변경하지 않기
- client/api 프롬프트에는 “직접 화면을 열어서 실행/확인”하라는 문구를 넣지 않기
- 화면 확인은 사용자가 직접 진행
- 프롬프트 검증은 “검증 포인트/기대 동작” 중심으로 작성

### 로컬 실행 / DB
- `Connection to localhost:5432 refused`는 로컬 PostgreSQL 미실행 또는 datasource URL 문제 우선 확인
- Spring 직접 실행 시 DB URL은 `localhost:5432`
- Docker Compose 내부 API 컨테이너에서는 `postgres:5432`
- Windows Docker 오류는 Docker Desktop 미실행으로 발생할 수 있으며, Docker Desktop 실행 후 재시도

### 컨트롤러
- `@RestController` + `ResponseEntity<ApiResponse<T>>`
- URL: `/api/v1/{resource}`
- 디바이스 식별: `X-Device-Id` 헤더 (`@RequestHeader`)
- 검증: `@Valid` + `@NotNull` / `@NotBlank` / `@Min` / `@Max`

### 서비스
- `@Service` + `@RequiredArgsConstructor`
- 트랜잭션: `@Transactional(readOnly = true)` 기본, 쓰기는 명시
- 예외: 비즈니스 예외는 커스텀 (BusinessException 상속)

### 리포지토리
- `JpaRepository<Entity, Long>` 상속
- 복잡 쿼리: QueryDSL 또는 `@Query`

### 엔티티
- `@Entity` + `@Table(name = "...")`
- ID: `Long` + `@GeneratedValue(strategy = IDENTITY)`
- 클라이언트 ID: `externalId` (String, UUID, unique)
- 시간: `@CreatedDate`, `@LastModifiedDate` (`@EnableJpaAuditing`)
- 연관관계: 단방향 우선, 필요 시 양방향
- enum: `@Enumerated(EnumType.STRING)`

## 예외 처리
- 전역 핸들러: `@RestControllerAdvice`
- 응답 포맷 통일: ApiResponse<T> { success, data, error }
- 검증 실패: 400, 비즈니스 예외: 400/404/409, 서버 에러: 500

## 진행 상태 / 향후 계획
- Phase A: 로컬 API 구축 완료
- Phase B: React Native 클라이언트 연동 완료
- Phase C: Lightsail 배포 + 도메인 + HTTPS 적용 완료
- Phase E: 사진 클라우드 저장 및 UX 안정화 완료
  - 하루 한 장 사진 S3 업로드
  - 기록 사진 기능 추가: DailyLog별 최대 3장, S3 업로드, thumbnail/medium 저장
  - 클라이언트 큰 이미지 보기, 삭제, 업로드 실패/재시도 UX 정리
  - 기록 사진 UI의 X 버튼 잘림 수정
- Phase E 후속 클라이언트/공유 UI 정리
  - 포토탭 상단 오늘/선택 날짜 표시, 캡션 수정, `수정됨` 표시, 공유하기/공유 링크 생성 UI 프롬프트 작성
  - 삭제 완료 시 기록탭과 동일하게 `삭제되었습니다` 문구 표시
  - 공유 UI 대상 파일: `components/CaptionModal.tsx`, `app/photos/[externalId].tsx`, `api/share-photo.js`
  - 공유 미리보기 모달 기준으로 실제 공유화면을 최대한 일치
  - 날짜, 이미지, 캡션, 홍보문구, 버튼 여백 일관성 유지
  - 가능하면 미리보기/공유화면 모두 스크롤 없이 한 화면에 들어오도록 압축
  - 로고는 `32px x 32px`, `object-fit: contain`, `object-position: right center`
  - `app/photos/[externalId].tsx`와 `api/share-photo.js`의 로고 위치 일관성 유지
  - `수정됨` 표시는 작고 약하게, `꼬리에서 공유됨` 뱃지는 더 우측으로
- 프로필탭 고도화 진행
  - 목적: 반려동물 기본 정보 관리 + 향후 건강관리/AI 리포트 확장 기반
  - 현재 타겟은 강아지만 유지
  - 추가 필드: `gender` (`MALE`/`FEMALE`), `adoptionDate`(함께한 날, nullable), `birthDateUnknown`(생일 모름, boolean)
  - `birthDateUnknown=true`이면 `birthDate` nullable 허용
  - `breed`는 서버 enum/목록으로 관리하지 않고 string 유지
  - 품종 추천은 클라이언트 상수 기반 자동완성으로 처리하고 자유입력 허용
  - 서버는 `breed` 문자열 저장만 담당
  - MVP 입력 부담 최소화를 우선하며 알레르기/약/질환 등은 추후 추가
- 설정/프로필탭 UX 정리 완료
  - 캐시 비우기는 `AsyncStorage.clear()` 제거, 캐시 키만 선별 삭제 + `try/catch` 처리 완료
  - 정책/약관/업데이트 소식은 Notion 공개 링크로 연결 완료
  - 알림 섹션은 전체 주석 처리, 알림 권한은 권한 섹션으로 이동
  - `꼬리 흔들게 하기`는 이스터에그로 유지
  - `꼬리 응원하기`는 토스 후원 링크 기반 후원 UI 추가 완료
- 클라이언트 오류 메모
  - `Error while reading cache, falling back to a full crawl: Unable to deserialize cloned data`는 Expo/Metro 캐시 손상 가능성이 높음
  - 우선 `npx expo start -c`
  - 필요 시 `.expo`, `node_modules/.cache`, temp metro/haste-map 캐시 삭제
- 다음 작업 후보
  - Vercel 도메인 연결 및 정책 페이지 준비 (`kkori.co.kr`, `www.kkori.co.kr`)
  - Phase D 회원가입/JWT 인증 설계
  - 프로필탭 고도화 완료 및 API/클라이언트 QA
  - 설정/정책/권한/후원 UI 최종 QA
  - 포토탭 고도화와 공유 UI 최종 QA
  - 개발/운영 API 환경 분리 값을 클라이언트 문서 또는 `.env` 예시에 정리
- Phase F: AI 리포트 (Codex/OpenAI API)
