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

### 동작 확인된 것
- Spring Boot 서버 정상 실행
- PostgreSQL 연결 정상 동작
- GitHub push 정상 동작

---

## 진행 중인 작업

### 현재 작업
- 반려동물 기록/관리 기능 구조 설계

### 다음 할 일 (우선순위)
1. 반려동물 등록 API 구현
2. 일일 기록 엔티티 및 DB 설계
3. JWT 인증 구조 정리

---

## 알려진 이슈

### 버그
- 없음

### 기술 부채
- application.yml 환경 분리 필요
- 예외 응답 포맷 통일 필요

### 결정 필요
- 이미지 저장 방식 결정 필요 (로컬 vs S3)
- Redis 사용 여부 검토

---

## 의사결정 기록

| 날짜 | 결정 | 이유 |
|------|------|------|
| 2026-05-15 | PostgreSQL 사용 | JSON 및 확장성 고려 |
| 2026-05-15 | Spring Boot 3 + Java 21 사용 | 최신 LTS 및 유지보수성 |

---

## 환경 정보

### 로컬
- PostgreSQL: localhost:5432 (db: kkori, user: postgres)
- 서버: localhost:8080
- Docker: kkori-pg 컨테이너

### 외부 의존성
- (배포 후 추가)

---

## 자주 쓰는 명령

```bash
# 서버 실행
./gradlew bootRun

# 테스트
./gradlew test

# 빌드
./gradlew build

# PostgreSQL Docker
docker start kkori-pg
docker stop kkori-pg