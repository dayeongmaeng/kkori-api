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
- 사양: 1GB  RAM, 2 vCPUs, 40GB SSD

## 도메인 모델
- **Device**: 디바이스 ID 기반 소유자 식별 (회원가입 없이, 추후 User 계정과 연결)
- **Caregiver**: 보호자 (가족 공유 대비, 한 Device에 여러 Caregiver 가능)
- **Pet**: 반려동물 (이름, 견종, 생일, 체중, 중성화, 메모, 사진)
- **DailyPhoto**: 하루 한 장 데일리 포토 (petId + date unique)
- **DailyLog**: 일일 건강 기록 (식사/산책/배변/소변/컨디션/체중/메모/사진)

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

## 향후 계획
- Phase A: 로컬 API 구축 (진행 중)
- Phase B: React Native 클라이언트 연동
- Phase C: 배포 (Railway/Render 검토)
- Phase D: JWT 인증, 회원가입
- Phase E: 사진 클라우드 저장 (S3/R2)
- Phase F: AI 리포트 (Claude/OpenAI API)