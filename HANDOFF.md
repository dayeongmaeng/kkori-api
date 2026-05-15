# 인수인계 노트

세션 종료 또는 컨텍스트 전환 시 작성. 다음 세션에서 빠르게 복귀하기 위함.

---

## 현재 상태

마지막 업데이트: 2026-05-16

### 완료된 작업 (Phase A-1 ~ A-4)

- **A-1**: Spring Boot 프로젝트 세팅, GitHub 연결, PostgreSQL Docker 구성
- **A-2**: JPA 엔티티 설계
  - `BaseEntity` (createdAt, updatedAt — LocalDateTime, KST 직렬화)
  - `Device`, `Caregiver`, `Pet` (deviceId 포함), `DailyPhoto`, `DailyLog`
  - Enum: `MealAmount(NONE/LESS/NORMAL/MORE)`, `WaterAmount(LESS/NORMAL/MORE)`, `StoolCondition(NORMAL/SOFT/HARD/DIARRHEA)`, `UrineColor(PALE/NORMAL/DARK)`
- **A-3**: 공통 모듈
  - `ApiResponse<T>`, `ErrorResponse`, `ErrorCode`, `BusinessException`, `GlobalExceptionHandler`
  - `DeviceIdInterceptor` (X-Device-Id 헤더 검증, `/register` · `/health` 제외)
- **A-4**: REST API CRUD 완료
  - Device: `POST /api/v1/devices/register`, `GET /api/v1/devices/me`
  - Caregiver: CRUD `/api/v1/caregivers` — deviceId 격리
  - Pet: CRUD `/api/v1/pets` — deviceId 격리 (`Pet.deviceId` 컬럼)
  - DailyPhoto: CRUD `/api/v1/photos` — Pet 소유 디바이스 검증
  - DailyLog: CRUD `/api/v1/logs` — Pet 소유 디바이스 검증
  - externalId: 클라이언트 미전송 시 서버 UUID 자동 생성, UUID 형식 검증(@Pattern), 중복 409
  - 사진 필드 없음 (DailyPhoto.photoBase64, DailyLog.photoBase64List 제거 — Phase E에서 재구현)

### 동작 확인된 것
- Spring Boot 서버 정상 실행
- PostgreSQL 연결 정상 동작

---

## 진행 중인 작업

### 다음 할 일 (우선순위)
1. **A-5**: Postman / IntelliJ HTTP Client로 전 API 로컬 테스트
2. **Phase B**: 클라이언트(React Native) 연동 시작

---

## 알려진 이슈

### 버그
- 없음

### 기술 부채
- `application.yaml` 환경 분리 필요 (dev / prod 프로파일)
- Pet DB에 `device_id` 컬럼 추가됨 — 기존 데이터 있으면 `NOT NULL` 제약으로 재기동 실패 가능 → 스키마 초기화 필요

### 결정 필요
- 사진 업로드 API 설계 (Phase E, S3/R2 선택 후)

---

## 의사결정 기록

| 날짜 | 결정 | 이유 |
|------|------|------|
| 2026-05-15 | PostgreSQL 사용 | JSON 및 확장성 고려 |
| 2026-05-15 | Spring Boot 3.5.14 + Java 21 | 안정적인 LTS |
| 2026-05-15 | 연관관계 ID(FK)만 사용, 객체 참조 X | 단순성 유지 |
| 2026-05-15 | externalId (UUID String) 별도 관리 | 내부 PK 노출 방지 |
| 2026-05-15 | photoBase64 필드 제거 | Phase A 단순화, Phase E에서 전용 업로드 API |
| 2026-05-15 | externalId 서버 자동 생성 | 클라이언트 편의, 서버가 UUID 생성 후 응답에 포함 |
| 2026-05-16 | Pet에 deviceId 컬럼 추가 (denormalize) | Photo/Log 접근 시 매번 Caregiver 조인 없이 Pet에서 바로 소유 디바이스 확인 |
| 2026-05-16 | Enum 단순화 (4종 모두 재정의) | 클라이언트-서버 계약 명확화, 불필요한 값 제거 |
| 2026-05-16 | 시간 필드 LocalDateTime + KST 직렬화 | 클라이언트가 타임존 변환 없이 바로 표시 가능 |

---

## 환경 정보

### 로컬
- PostgreSQL: localhost:5432 (db: kkori, user: postgres, pw: kkori)
- 서버: localhost:8080
- Docker: kkori-pg 컨테이너
- Java: 21
- Spring Boot: 3.5.14

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

# DB 초기화 (스키마 변경 후 필요 시)
docker exec -it kkori-pg psql -U postgres -d kkori -c "DROP SCHEMA public CASCADE; CREATE SCHEMA public;"
```
