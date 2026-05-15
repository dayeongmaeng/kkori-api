# 인수인계 노트

세션 종료 또는 컨텍스트 전환 시 작성. 다음 세션에서 빠르게 복귀하기 위함.

---

## 현재 상태

마지막 업데이트: 2026-05-15

### 완료된 작업
- 프로젝트 초기 세팅 완료
- GitHub 원격 저장소 연결
- PostgreSQL Docker 컨테이너 구성
- Spring Boot 프로젝트 실행 확인
- **JPA 엔티티 5개 + BaseEntity + JpaAuditingConfig 생성**
  - `common/entity/BaseEntity` (createdAt, updatedAt, @MappedSuperclass)
  - `common/config/JpaAuditingConfig` (@EnableJpaAuditing)
  - `device/entity/Device`, `Platform`
  - `caregiver/entity/Caregiver`, `CaregiverRole`
  - `pet/entity/Pet`, `Species`
  - `photo/entity/DailyPhoto`
  - `log/entity/DailyLog`, `MealAmount`, `WaterAmount`, `StoolCondition`, `UrineColor`
- **공통 모듈 생성**
  - `common/dto/ApiResponse<T>` (record, @JsonInclude NON_NULL, timestamp Instant)
  - `common/dto/ErrorResponse` (record)
  - `common/exception/ErrorCode` (enum, HttpStatus 포함)
  - `common/exception/BusinessException`
  - `common/exception/GlobalExceptionHandler` (@RestControllerAdvice)
  - `common/interceptor/DeviceIdInterceptor` (X-Device-Id 헤더 검증)
  - `common/config/WebMvcConfig` (/api/v1/** 인터셉터 등록)
  - `common/controller/HealthController` (GET /api/v1/health, 인터셉터 제외)

### 동작 확인된 것
- Spring Boot 서버 정상 실행
- PostgreSQL 연결 정상 동작
- GitHub push 정상 동작

---

## 진행 중인 작업

### 다음 할 일 (우선순위)
1. A-4: Pet, Caregiver, Device REST API CRUD 구현
2. A-4: DailyPhoto, DailyLog API 구현
3. A-5: Postman / HTTP Client로 로컬 테스트

---

## 알려진 이슈

### 버그
- 없음

### 기술 부채
- application.yaml 환경 분리 필요 (dev / prod 프로파일)

### 결정 필요
- 이미지 저장 방식 (현재: base64 TEXT 컬럼, 향후 Phase E에서 S3/R2로 전환)
- Redis 사용 여부 검토

---

## 의사결정 기록

| 날짜 | 결정 | 이유 |
|------|------|------|
| 2026-05-15 | PostgreSQL 사용 | JSON 및 확장성 고려 |
| 2026-05-15 | Spring Boot **3.5.14** + Java 21 사용 | 안정적인 LTS, Jackson 2.x 내장으로 설정 단순 |
| 2026-05-15 | 연관관계 ID(FK)만 사용, 객체 참조 X | 단순성 유지, 필요 시 추후 변경 |
| 2026-05-15 | externalId (UUID String) 별도 관리 | 클라이언트에 내부 PK 노출 방지 |
| 2026-05-15 | photoBase64 TEXT 컬럼 저장 | Phase A 단순화, Phase E에서 S3 전환 예정 |
| 2026-05-15 | DailyLog.photoBase64List → @ElementCollection | 다중 사진 지원, 별도 테이블(daily_log_photo) |

---

## 환경 정보

### 로컬
- PostgreSQL: localhost:5432 (db: kkori, user: postgres, pw: kkori)
- 서버: localhost:8080
- Docker: kkori-pg 컨테이너
- Java: 21
- Spring Boot: 3.5.14
- Jackson: 2.x (com.fasterxml.jackson, Spring Boot 내장)

### 외부 의존성
- (배포 후 추가)

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
```
