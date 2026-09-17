# kkori-api 작업 가이드

이 문서는 `kkori-api` 저장소에서 작업하는 코딩 에이전트의 단일 기준 문서다. `CLAUDE.md`는 이 파일만 불러온다.

## 1. 우선순위와 작업 범위

- 우선순위는 사용자의 현재 요청 → 이 문서 → 실제 코드·설정·테스트 → `README.md`와 기타 메모 순이다.
- 문서와 구현이 다르면 추측하지 말고 컨트롤러, 서비스, 엔티티/리포지토리, 테스트 순으로 현재 동작을 확인한다.
- `.claude/rules/`는 과거 규칙이 남아 있을 수 있는 참고 자료다. 충돌하면 이 문서와 현재 구현을 따른다.
- 이 저장소는 Spring Boot API 서버다. 클라이언트 저장소 `kkori`는 API 계약 확인이 꼭 필요할 때만 읽고, 별도 요청 없이 수정하지 않는다.
- `.gitignore`에 포함된 `.env`, `logs/`, `build/`, `.gradle/`, IDE 파일과 생성물은 읽거나 커밋하지 않는다. 예외로 추적 중인 로그가 보여도 작업 대상에서 제외한다.
- 실제 비밀값, OAuth 토큰, JWT, 사용자 개인정보를 문서·테스트·로그에 남기지 않는다. 환경변수 이름과 안전한 더미 값만 사용한다.
- 작업 전후 `git status --short`를 확인하고, 기존 변경·삭제를 보존한다. 요청받지 않은 파일을 정리하거나 되돌리지 않는다.

## 2. 프로젝트 개요

- Java 21, Spring Boot 3.5.14, Gradle Wrapper 9.4.1
- Spring Web, Validation, Security, Data JPA, PostgreSQL 16
- AWS SDK v2 S3, springdoc-openapi 2.8.17, Lombok
- 기본 패키지: `com.kkori.api`
- 기본 포트: `8080`
- API prefix: `/api/v1`
- JSON 시간대: `Asia/Seoul`
- 운영 프로필: `prod` (`Swagger`/OpenAPI 비활성화, 애플리케이션 로그 INFO)

`SecurityConfig`는 세션·form login·HTTP Basic을 끄고 모든 요청을 `permitAll`로 둔다. 실제 인증·식별은 `JwtAuthenticationFilter`, `DeviceIdInterceptor`, 서비스의 소유권 검사에서 수행된다. Spring Security의 `authenticated()`가 보호한다고 가정하지 않는다.

## 3. 저장소 구조

```text
src/main/java/com/kkori/api/
├── auth/       OAuth 로그인, JWT, 로그아웃, provider 연결 해제
├── user/       사용자와 회원 탈퇴
├── device/     설치 기기 등록·조회
├── caregiver/  기록 작성자/보호자
├── pet/        반려동물과 삭제 cascade
├── photo/      하루 한 장 사진과 S3 저장소
├── log/        일일 건강 기록과 첨부 사진
├── admin/      kkutudio-admin 전용 내부 API (/internal/admin/**, API 키 인증)
└── common/     설정, 공통 응답, 예외, 필터, 인터셉터

src/main/resources/
├── application.yaml
├── logback-spring.xml
├── db/         수동 검토·적용용 PostgreSQL SQL
└── static/oauth/kakao.html
```

핵심 설정은 `build.gradle`, `application.yaml`, `docker-compose.yml`, `Dockerfile`, `.env.example`에 있다. 배포 상태나 운영 DB 적용 여부는 저장소 문구만으로 단정하지 말고 실제 환경에서 별도 확인한다.

## 4. 실행과 검증

필수 도구는 Java 21과 Docker/Docker Compose다. 시스템 Gradle 대신 Wrapper를 사용한다.

```bash
# macOS/Linux
./gradlew bootRun
./gradlew test
./gradlew clean build
./gradlew test --tests "com.kkori.api.auth.service.AuthServiceTest"

# Windows
gradlew.bat bootRun
gradlew.bat test
gradlew.bat clean build
gradlew.bat test --tests "com.kkori.api.auth.service.AuthServiceTest"

# 전체 로컬 스택
docker compose up -d
docker compose logs -f api
```

- 로컬 애플리케이션 실행과 `@SpringBootTest`는 PostgreSQL 및 필수 설정이 필요할 수 있다.
- 현재 테스트는 JUnit 5, Mockito, AssertJ, Spring Test를 사용한다. Testcontainers 의존성이나 통합 테스트 기반은 아직 없다.
- 변경한 계층의 가장 좁은 테스트부터 실행한 뒤 가능하면 전체 `test`를 실행한다.
- 문서만 바꾼 경우 최소한 `git diff --check`와 최종 diff를 확인한다.
- 실행하지 못한 검증은 성공했다고 쓰지 말고, 실행하지 못한 이유와 예상 영향을 보고한다.

## 5. 설정과 환경변수

`application.yaml`은 `optional:file:.env[.properties]`를 읽는다. `.env`는 로컬 비밀 파일이며 커밋하지 않는다.

| 영역 | 환경변수 |
|---|---|
| DB | `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` |
| Compose DB | `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD` |
| S3 | `AWS_S3_BUCKET`, `AWS_S3_REGION`, `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY` |
| JWT | `JWT_SECRET`(최소 32자), `JWT_ACCESS_TOKEN_TTL_SECONDS`, `JWT_REFRESH_TOKEN_TTL_SECONDS` |
| Google | `GOOGLE_WEB_CLIENT_ID`, `GOOGLE_IOS_CLIENT_ID` |
| Kakao | `KAKAO_REST_API_KEY`, `KAKAO_ADMIN_KEY` |
| OAuth 토큰 암호화 | `OAUTH_TOKEN_ENCRYPTION_KEY`(최소 32바이트) |
| Admin 연동 | `ADMIN_API_KEY` — kkutudio-admin이 `/internal/admin/**` 호출 시 `X-Admin-Api-Key` 헤더로 보내는 값과 비교. 비어 있으면 해당 경로는 전부 401 |

- 기본 JWT TTL은 access 1시간, refresh 30일이다.
- Google audience 검증은 현재 Web/iOS client ID만 허용한다. Android client ID 지원을 가정하지 않는다.
- `OAuthProvider`에는 `APPLE`이 있지만 verifier가 없어 로그인 요청은 `AUTH_001`이 된다.
- `KAKAO_NATIVE_APP_KEY`는 `.env.example`과 Compose에는 있으나 현재 `application.yaml` 바인딩/서버 로직에서 사용되지 않는다.
- OAuth 암호화 키가 없거나 짧으면 Google provider 토큰 저장만 비활성화되고 로그인 자체는 계속된다.
- Compose는 PostgreSQL과 API를 각각 `127.0.0.1:5432`, `127.0.0.1:8080`에만 bind한다. 외부 공개는 별도 reverse proxy 계층의 책임이다.

## 6. 인증, 디바이스, 소유권

### 요청 식별 흐름

1. `Authorization: Bearer <accessToken>`이 있으면 `JwtAuthenticationFilter`가 서명·종류·만료를 검증하고 `AuthContext`에 사용자 ID를 넣는다.
2. 토큰이 없으면 필터는 요청을 거부하지 않고 기존 디바이스 흐름으로 넘긴다.
3. `DeviceIdInterceptor`는 보호 대상 `/api/v1/**`에서 로그인 사용자가 없을 때 `X-Device-Id`를 요구한다.
4. 서비스는 로그인 사용자 소유권을 우선하고, 비로그인 또는 레거시 데이터에만 디바이스 소유권을 fallback으로 사용한다.

잘못된 Bearer 형식/서명은 `AUTH_003`, 만료는 `AUTH_004`로 401을 반환한다. 소유하지 않은 리소스는 존재 여부를 노출하지 않도록 보통 해당 도메인의 404 코드로 처리한다.

### 인증/식별 예외

- 완전 공개: `GET /api/v1/health`, `POST /api/v1/devices/register`, `POST /api/v1/auth/oauth/login`, `POST /api/v1/auth/refresh`, `GET /api/v1/photos/{externalId}/share`
- Swagger/OpenAPI와 `/oauth/kakao`도 JWT 필터 대상이 아니다.
- `POST /api/v1/auth/logout`는 디바이스 인터셉터에서는 제외되지만 서비스가 `AuthContext`를 요구하므로 Bearer access token이 필요하다.
- `DELETE /api/v1/users/me`는 Bearer access token이 필요하다.
- `GET /api/v1/devices/me`와 caregiver 생성/목록은 컨트롤러가 `deviceId` request attribute를 직접 요구하므로, 현재는 로그인 상태여도 `X-Device-Id`를 함께 보내야 안전하다.

OAuth 로그인은 먼저 등록된 `deviceExternalId`를 요구한다. 로그인 성공 시 Device를 User에 연결하고, 그 기기의 `userId == null` 반려동물을 사용자에게 귀속시킨다. 로그아웃은 refresh token 해시를 revoke 목록에 넣을 수 있지만 Device-User 연결은 해제하지 않는다. refresh token rotation은 아직 구현되지 않았다.

## 7. 도메인 불변식

- 외부 API 식별자는 내부 PK가 아니라 `externalId`를 사용한다. 클라이언트가 생성 시 UUID를 보낼 수 있고, 생략하면 서버가 UUID를 만든다.
- 반려동물은 활성 사용자 또는 디바이스당 최대 3마리다 (`PET_003`).
- `Pet.species`: `DOG`, `CAT`; `gender`: `MALE`, `FEMALE`.
- 생일/체중 미상은 각각 `birthDateUnknown`, `weightKgUnknown`으로 표현한다. 미상일 때 체중은 `null`로 저장한다.
- 하루 한 장 사진과 일일 기록은 활성 데이터 기준 반려동물·날짜당 하나다.
- 일일 기록 사진은 기록당 최대 3장이고 `sortOrder`, `id` 순으로 반환한다.
- 건강 enum은 코드 그대로 유지한다.
  - `MealAmount`: `NONE`, `LESS`, `NORMAL`, `MORE`
  - `WaterAmount`: `LESS`, `NORMAL`, `MORE`
  - `StoolCondition`: `NORMAL`, `SOFT`, `HARD`, `DIARRHEA`
  - `UrineColor`: `PALE`, `NORMAL`, `DARK`
  - `UrineAmount`: `NONE`, `LOW`, `NORMAL`, `HIGH`
- Photo의 PATCH/PUT은 caption만 수정한다. URL을 request DTO에 추가해 임의 변경을 허용하지 않는다.

Caregiver는 현재 Device에 속하고, Pet/Photo/Log처럼 User 우선 소유권 모델로 완전히 전환되지 않았다. 관련 작업에서는 caregiver가 요청한 Pet과 같은 소유 범위인지 반드시 검증한다.

## 8. 삭제와 외부 부작용

`SoftDeletableEntity`를 상속하는 엔티티는 User, Caregiver, Pet, DailyPhoto, DailyLog, DailyLogPhoto다. 그러나 현재 삭제 구현은 통일되어 있지 않다.

- Pet 삭제 및 회원 탈퇴 cascade: Pet, 관련 Log/LogPhoto/DailyPhoto를 soft delete한다.
- 개별 Log 삭제: Log와 첨부 사진을 repository `delete`로 hard delete한다.
- 개별 DailyPhoto 삭제: hard delete한다.
- 개별 LogPhoto 삭제: soft delete한다.
- Caregiver 삭제: hard delete하며, 일부 조회도 아직 `deletedAt` 조건 없는 메서드를 사용한다.

기존 의미를 모르고 삭제 방식을 바꾸지 않는다. soft delete를 새로 적용하거나 확장할 때는 모든 조회·중복 검사·count가 `deletedAt IS NULL`을 사용하는지 함께 확인한다. DB의 `(pet_id, date)` unique 제약은 삭제 컬럼을 포함하지 않으므로, soft delete된 동일 날짜 행을 다시 만드는 시나리오도 별도로 검증한다.

DB 트랜잭션과 외부 I/O를 구분한다.

- Pet cascade와 탈퇴의 S3 삭제는 이벤트를 발행하고 `AFTER_COMMIT` + `@Async` listener에서 수행한다.
- 탈퇴 후 OAuth 연결 해제도 `AFTER_COMMIT` + `@Async`이며, 실패는 기록하되 이미 커밋된 탈퇴를 되돌리지 않는다.
- 업로드는 S3 I/O가 DB 커밋보다 먼저 일어날 수 있다. 다중 업로드의 중간 실패나 DB rollback 시 orphan object 가능성을 고려한다.
- 외부 호출을 트랜잭션 안에 추가할 때 실패 원자성, 재시도, 멱등성, 보상 삭제를 명시적으로 설계한다.

## 9. 사진 저장 규칙

- 허용 Content-Type: `image/jpeg`, `image/png`
- medium 최대 1 MiB, thumbnail 최대 200 KiB
- 전체 multipart 설정: 파일당 2 MB, 요청당 5 MB
- S3 key:
  - `photos/{petExternalId}/{photoExternalId}/medium.jpg`
  - `photos/{petExternalId}/{photoExternalId}/thumb.jpg`
- 반환 URL은 `https://{bucket}.s3.{region}.amazonaws.com/{key}` 형식이다.

업로드 파일 확장자는 key에서 항상 `.jpg`지만 PNG Content-Type도 허용한다는 현재 계약을 임의로 바꾸지 않는다. 파일 검증을 강화할 때 클라이언트 호환성, 실제 magic bytes, 기존 객체 URL을 함께 고려한다.

## 10. API 지도

모든 성공 응답은 원칙적으로 `ApiResponse<T>(success, data, error, timestamp)`를 사용한다. DELETE 성공은 현재 body 없는 204다. 검증 실패는 `VALIDATION_001`, 처리되지 않은 예외는 `SERVER_ERROR`로 감싼다.

| 영역 | 메서드와 경로 |
|---|---|
| Health | `GET /api/v1/health` |
| Auth | `POST /api/v1/auth/oauth/login`, `/refresh`, `/logout` |
| User | `DELETE /api/v1/users/me` |
| Device | `POST /api/v1/devices/register`, `GET /api/v1/devices/me` |
| Caregiver | `POST/GET /api/v1/caregivers`, `GET/PUT/DELETE /api/v1/caregivers/{externalId}` |
| Pet | `POST/GET /api/v1/pets`, `GET/PUT/DELETE /api/v1/pets/{externalId}` |
| DailyPhoto | `POST/GET /api/v1/photos`, `GET/PATCH/PUT/DELETE /api/v1/photos/{externalId}` |
| Photo upload/share | `POST /api/v1/photos/{externalId}/upload`, `GET /api/v1/photos/{externalId}/share` |
| DailyLog | `/api/v1/logs`와 `/api/v1/daily-logs` 모두 동일하게 지원 |
| Log 상세 | `POST/GET` base, `GET/PUT/DELETE /{externalId}` |
| Log 사진 | `POST /with-photos`, `POST /{externalId}/photos/upload`, `DELETE /{externalId}/photos/{photoExternalId}` |

`/internal/admin/**`(kkutudio-admin 전용, `X-Admin-Api-Key` 인증)는 이 표에서 제외 — `ApiResponse<T>`를 쓰지 않는 별도 계약이다. 자세한 내용은 "5. 설정과 환경변수"의 `ADMIN_API_KEY`와 `admin/` 패키지 참고.

Photo/Log 목록은 `petExternalId` query parameter를 요구한다. API 계약을 바꾸면 두 alias, Swagger annotation, 클라이언트, DTO, 오류 코드, 테스트를 함께 확인한다.

## 11. OAuth와 회원 탈퇴

- 로그인 구현 제공자는 Google과 Kakao다. 이메일/비밀번호 인증은 없다.
- Google은 ID token을 tokeninfo endpoint로 확인하고 email verified 및 audience를 검증한다.
- Kakao는 access token 직접 검증 또는 authorization code + `redirectUri` 교환을 지원한다.
- Google OAuth access/refresh token이 요청에 포함되고 암호화가 활성화된 경우 AES-256-GCM으로 저장한다.
- Kakao 로그아웃은 요청의 Kakao access token으로 provider logout을 시도한다.
- 회원 탈퇴 시 Pet 관련 데이터를 cascade soft delete하고 개인정보/provider 식별자를 익명화한 뒤 `WITHDRAWN` 처리한다.
- Kakao 탈퇴 연결 해제는 admin key, Google 연결 해제는 저장된 provider token이 있어야 한다.
- access JWT 검증은 DB의 User 상태를 조회하지 않으므로 탈퇴 직전에 발급된 access token은 만료까지 암호학적으로 유효하다. refresh는 User의 deleted/withdrawn 상태를 검사해 차단한다.

로그에 provider token, JWT 원문, 암호화 키를 출력하지 않는다. 키를 마스킹할 때도 운영 비밀값을 재구성할 수 없게 한다.

## 12. DB와 마이그레이션

- 현재 `spring.jpa.hibernate.ddl-auto=update`를 사용하며 Flyway/Liquibase는 없다.
- `src/main/resources/db/*.sql`은 자동 순차 실행되는 migration 체계가 아니다. 대상 DB 스키마를 확인한 뒤 필요한 스크립트를 수동 적용하는 자료다.
- 스크립트가 저장소에 있거나 README에 “적용 완료”라고 적혀 있어도 현재 대상 환경에 적용됐다고 단정하지 않는다.
- 특히 `user-withdrawal-migration.sql`은 provider 컬럼의 NOT NULL 제거를 포함해 `ddl-auto=update`만으로 보장되지 않는다.
- SQL 변경은 가능하면 멱등적으로 작성하고, 기존 데이터 backfill, 제약조건, rollback/복구, 배포 순서를 함께 제시한다.
- Entity 변경만 하고 운영 제약조건을 잊지 않는다. 반대로 SQL만 바꾸고 Entity/Repository/테스트를 누락하지 않는다.

## 13. 구현 규칙

- Controller는 HTTP 매핑·검증·응답 변환만 담당하고 비즈니스 로직은 Service에 둔다.
- 클래스 수준 `@Transactional(readOnly = true)`를 기본으로 하고 쓰기 메서드에 `@Transactional`을 명시한다.
- Request/Response DTO는 record를 우선하고 엔티티를 API에 직접 노출하지 않는다.
- 입력은 Bean Validation으로 검증한다. UUID externalId 정규식과 필드별 범위를 기존 계약에 맞춘다.
- 비즈니스 실패는 `BusinessException(ErrorCode)`으로 표현하고 새 오류는 도메인 prefix를 유지한다.
- Repository 메서드명은 소유권과 삭제 상태를 드러내게 작성한다. soft-deletable 조회에서 무심코 `findById`, `findByExternalId`를 사용하지 않는다.
- 소유권 검사 없이 externalId만으로 update/delete하는 새 코드를 만들지 않는다.
- 외부 리소스가 다른 소유자에게 속한 경우 403보다 도메인 404를 반환하는 현재 정보 은닉 패턴을 유지한다.
- 목록 순서가 API 계약이면 Repository `OrderBy` 또는 명시적 정렬로 보장한다. DB 기본 순서에 의존하지 않는다.
- 로그는 requestId, 비민감 userId, provider, 결과 코드처럼 운영에 필요한 정보만 남긴다.
- 주석은 코드에서 알 수 없는 이유, 제약, 배포 전제만 설명한다. 완료 시점이 지난 계획이나 과거 테스트 개수는 기준 문서에 고정하지 않는다.

## 14. 현재 알려진 주의점

아래 항목은 이미 해결된 것으로 가정하지 않는다. 관련 코드를 수정할 때 범위를 확인하고 회귀 테스트를 추가한다.

- Spring Security가 모든 요청을 permit하므로 누락된 서비스 소유권 검사는 곧 접근 제어 누락이 된다.
- Caregiver 단건 조회/수정/삭제는 현재 device/user 소유권을 직접 확인하지 않는다.
- Caregiver의 soft-delete 상속과 실제 hard-delete/비필터 조회가 일관되지 않는다.
- 개별 삭제와 cascade 삭제 사이에 hard/soft delete 정책이 섞여 있다.
- refresh token은 rotation하지 않으며 로그아웃 시 전달된 token만 revoke한다.
- Apple provider enum은 있지만 로그인 verifier가 없다.
- Google Android OAuth audience 설정이 없다.
- S3 두 파일 업로드 또는 DB 저장의 중간 실패에 대한 자동 보상 삭제가 없다.
- `ddl-auto=update`와 수동 SQL이 함께 있어 환경별 스키마 차이가 생길 수 있다.

이 항목을 요청 범위 밖에서 대규모로 정리하지는 않는다. 다만 변경이 해당 위험을 악화시키지 않는지 확인하고, 직접 닿는 경우 가장 작은 안전한 수정과 테스트를 제안하거나 구현한다.

## 15. 완료 기준

- 요청한 동작과 변경 범위가 명확하다.
- 인증 주체, 소유권, soft delete, 트랜잭션, S3/OAuth 부작용을 검토했다.
- API/DTO/enum/오류 코드 변경 시 클라이언트 호환성을 확인했다.
- 필요한 테스트를 추가·실행했고 결과를 정확히 보고한다.
- DB 변경이면 migration과 배포 순서를 함께 다룬다.
- `git diff --check`가 통과하고, 기존 사용자 변경이 보존되며, 비밀값·생성물·로그가 diff에 없다.
- 최종 보고에는 변경 파일, 핵심 결정, 실행한 검증, 남은 위험만 간결하게 적는다.
