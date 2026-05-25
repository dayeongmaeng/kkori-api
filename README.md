# kkori-api

반려동물 일상 기록 앱 [꼬리](https://kkori.co.kr)의 백엔드 서버.

## 스택

- Java 21 / Spring Boot 3.5.14
- PostgreSQL 16
- JPA + Hibernate + Lombok
- Docker Compose
- AWS S3

## 로컬 실행

### 요구사항

- Java 21
- Docker / Docker Compose

### 환경변수 설정

```bash
cp .env.example .env
# .env 파일에 값 입력
```

필수 환경변수:

| 키 | 설명 |
|---|---|
| `POSTGRES_USER` / `POSTGRES_PASSWORD` / `POSTGRES_DB` | DB 접속 정보 |
| `JWT_SECRET` | 최소 32자 이상 랜덤 문자열 |
| `AWS_ACCESS_KEY_ID` / `AWS_SECRET_ACCESS_KEY` | S3 IAM 키 |
| `AWS_S3_BUCKET` | S3 버킷명 (순수 버킷명만, `s3://` 제외) |
| `AWS_S3_REGION` | S3 리전 (예: `ap-northeast-2`) |
| `GOOGLE_WEB_CLIENT_ID` | Google OAuth Web 클라이언트 ID |
| `GOOGLE_IOS_CLIENT_ID` | Google OAuth iOS 클라이언트 ID |
| `KAKAO_REST_API_KEY` / `KAKAO_NATIVE_APP_KEY` | Kakao OAuth 키 |
| `OAUTH_TOKEN_ENCRYPTION_KEY` | Google OAuth 토큰 AES-256-GCM 암호화 키 |

### Docker Compose로 실행

```bash
docker compose up -d
docker compose logs -f api
```

### Gradle로 직접 실행

```bash
./gradlew bootRun
```

### 테스트

```bash
./gradlew test
```

## 운영 인프라

| 항목 | 값 |
|---|---|
| 운영 API URL | `https://api.kkori.co.kr` |
| 서버 | AWS Lightsail Seoul Zone A (`ap-northeast-2a`) |
| OS | Ubuntu 24.04 LTS |
| 인프라 흐름 | 앱 → Nginx 443 → Spring Boot 8080 → PostgreSQL 16 → S3 |

## 주요 API

| 메서드 | 경로 | 설명 |
|---|---|---|
| `POST` | `/api/v1/auth/oauth/login` | Google/Kakao OAuth 로그인 |
| `POST` | `/api/v1/auth/refresh` | accessToken 재발급 |
| `POST` | `/api/v1/auth/logout` | 로그아웃 |
| `DELETE` | `/api/v1/users/me` | 회원 탈퇴 (JWT 필수) |
| `POST` | `/api/v1/devices/register` | 기기 등록 |
| `GET` | `/api/v1/devices/me` | 내 기기 조회 |
| `GET/POST/PATCH/DELETE` | `/api/v1/pets` | 반려동물 CRUD |
| `GET/POST/PATCH/DELETE` | `/api/v1/photos` | 데일리 포토 |
| `POST` | `/api/v1/photos/{externalId}/upload` | 사진 S3 업로드 |
| `GET` | `/api/v1/photos/{externalId}/share` | 공유 조회 (인증 불필요) |
| `GET/POST/PATCH/DELETE` | `/api/v1/daily-logs` | 일일 건강 기록 |
| `POST` | `/api/v1/daily-logs/{externalId}/photos/upload` | 기록 사진 업로드 |
| `DELETE` | `/api/v1/daily-logs/{externalId}/photos/{photoExternalId}` | 기록 사진 삭제 |

API 문서: `/swagger-ui.html`

## 인증

- OAuth 전용 (Google, Kakao). 이메일/비밀번호 가입 없음.
- 로그인 성공 시 JWT accessToken / refreshToken 발급.
- 요청 헤더: `Authorization: Bearer {accessToken}`
- 기기 식별 헤더: `X-Device-Id`

## 회원 탈퇴

`DELETE /api/v1/users/me` — JWT 인증 필수, 204 반환.

- 소유 반려동물 기록/사진 cascade soft delete
- 개인정보 익명화 (email, provider 등 null 처리)
- 탈퇴 후 동일 Google/Kakao 계정으로 재가입 가능

> **배포 전 필수**: `src/main/resources/db/user-withdrawal-migration.sql`을 운영 DB에 수동 실행해야 합니다. (`ddl-auto=update`는 NOT NULL 제약 자동 제거 불가)

## 관련 저장소

- 클라이언트: [kkori](https://github.com/dayeongmaeng/kkori)
