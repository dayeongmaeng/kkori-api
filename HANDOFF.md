# 인수인계 노트

세션 종료 또는 컨텍스트 전환 시 다음 세션에서 바로 이어받기 위한 최신 상태 기록.

마지막 업데이트: 2026-05-20

## 현재 운영 상태

- 루트 도메인: `kkori.co.kr`
- 운영 API 도메인: `api.kkori.co.kr`
- DNS A 레코드: `api.kkori.co.kr -> 13.124.220.29`
- 운영 API URL: `https://api.kkori.co.kr`
- 클라이언트 환경변수: `EXPO_PUBLIC_API_URL=https://api.kkori.co.kr`
- 서버 JWT 환경변수: `JWT_SECRET` 필수, 최소 32자 이상
- 서버: AWS Lightsail `kkori-api`, Seoul Zone A (`ap-northeast-2a`), Ubuntu 24.04 LTS
- 서버 사양: 1GB RAM, 2 vCPUs, 40GB SSD
- 현재 운영 Public IP: `13.124.220.29`
- 이전 서버 IP: `3.38.97.234` (더 이상 운영 기준 IP 아님)
- HTTPS: Nginx + Let's Encrypt + Certbot 적용 완료
- 인증서 도메인: `api.kkori.co.kr`
- `certbot renew --dry-run` 성공

## 현재 인프라 흐름

앱 -> `https://api.kkori.co.kr` -> Nginx 443 -> Spring Boot 8080 -> PostgreSQL 16 -> S3

## 포트 정책

- 열려 있어야 함: 22(SSH), 80(HTTP/Certbot 갱신/HTTPS redirect), 443(HTTPS API)
- 8080: Nginx 프록시 뒤 내부 포트로만 사용해야 함
- 8080 외부 공개 차단 상태: 확인 필요. 이미 닫았는지 확실하지 않으므로 "닫기 예정/확인 필요"로 관리

## 이번 세션 완료 작업

- JWT 인증 골격 완성
  - `JWT_SECRET`은 환경변수 기반으로 읽음
  - `JWT_SECRET` 미설정 또는 32자 미만이면 서버 시작 시 명확한 에러로 실패
  - 민감정보/JWT/OAuth 토큰 로그 출력 금지 기준 유지
  - `Authorization: Bearer {accessToken}` 검증 필터 추가
  - 유효한 accessToken이면 요청 컨텍스트에 `userId`, `userExternalId` 세팅
  - 만료/위조 토큰은 401 + 기존 `ApiResponse<T>` 에러 포맷으로 응답
  - 토큰이 없으면 기존 `X-Device-Id` 흐름 fallback 유지
  - 공개 API(auth, health, swagger, 공유 조회)는 JWT 필터 제외
  - `POST /api/v1/auth/refresh` 추가
  - refreshToken 검증 후 새 accessToken 발급
  - refreshToken rotation/revocation은 TODO
  - Pet/Photo/Log 권한 검증은 userId 우선, deviceId fallback 유지
- OAuth 전용 회원가입/로그인 1차 구현
  - 이메일/비밀번호 가입 없음
  - provider enum은 `GOOGLE`, `KAKAO`, `APPLE` 준비
  - 우선 구현 provider는 `GOOGLE`, `KAKAO`
  - User 엔티티 추가: `externalId` UUID, provider, providerUserId, nullable email/nickname/profileImageUrl, `provider + providerUserId` unique, BaseEntity 상속
  - Device는 소유자가 아니라 설치 기기/세션/푸시 보조 정보로 역할 전환
  - `Device.userId`로 User 연결
  - 기존 `X-Device-Id` 흐름은 유지
  - `Pet.userId` nullable 추가
  - OAuth 로그인 성공 시 현재 `deviceExternalId`의 기존 Pet 중 `userId`가 없는 데이터를 User에 연결
  - DailyPhoto/DailyLog는 기존 petId 기반 유지
  - 조회/수정 권한 검증은 userId 우선, 없으면 기존 deviceId fallback
  - `POST /api/v1/auth/oauth/login` 추가
  - 요청: provider, idToken/accessToken, deviceExternalId
  - 응답: JWT accessToken/refreshToken, user
  - OAuth verifier는 provider별로 분리
  - Google은 idToken을 `https://oauth2.googleapis.com/tokeninfo`로 검증하고 `GOOGLE_CLIENT_ID` audience 대조
  - Kakao는 accessToken으로 `https://kapi.kakao.com/v2/user/me` 호출 후 사용자 정보 추출
  - Google/Kakao providerUserId/email/profile 추출 구현 완료
  - OAuth 실패는 401 BusinessException으로 처리
  - 민감정보와 OAuth 토큰은 로그에 남기지 않는 기준 유지
- 설정/프로필탭 UX 정리 완료
  - 캐시 비우기는 `AsyncStorage.clear()`를 제거하고 캐시 키만 선별 삭제하도록 변경
  - 캐시 삭제 로직은 `try/catch` 처리 완료
  - 정책/약관/업데이트 소식은 Notion 공개 링크로 연결 완료
  - 알림 섹션은 전체 주석 처리
  - 알림 권한은 권한 섹션으로 이동
  - `꼬리 흔들게 하기`는 이스터에그로 유지
  - `꼬리 응원하기`는 토스 후원 링크 기반 후원 UI 추가 완료
- 프로필탭 고도화 진행 기준 정리
  - 목적: 반려동물 기본 정보 관리 + 향후 건강관리/AI 리포트 확장 기반
  - 현재 타겟은 강아지만 유지
  - 추가 필드: `gender` (`MALE`/`FEMALE`), `adoptionDate`(함께한 날, nullable), `birthDateUnknown`(생일 모름, boolean)
  - `birthDateUnknown=true`이면 `birthDate` nullable 허용
  - `breed`는 서버 enum/목록으로 관리하지 않고 string 유지
  - 품종 추천은 클라이언트 상수 기반 자동완성으로 처리하고 자유입력 허용
  - 서버는 `breed` 문자열 저장만 담당
  - MVP 입력 부담 최소화를 우선하며 알레르기/약/질환 등은 추후 추가
- 클라이언트 개발/운영 API 환경 분리 방향 정리
  - 개발 환경은 로컬 API 사용
  - 운영 환경은 `https://api.kkori.co.kr` 사용
  - Expo/React Native에서는 `__DEV__`로 개발/배포 구분 가능
  - 실기기 개발 시 `localhost`는 폰 자신을 의미하므로 PC LAN IP 또는 `EXPO_PUBLIC_DEV_API_URL` 사용 필요
- 로컬 서버/DB 실행 이슈 확인
  - `Connection to localhost:5432 refused`는 로컬 PostgreSQL 미실행 또는 datasource URL 문제
  - Spring 직접 실행 시 DB URL은 `localhost:5432`
  - Docker Compose 내부 API 컨테이너에서는 `postgres:5432`
  - Windows Docker 오류는 Docker Desktop 미실행으로 발생했고, Docker Desktop 실행 후 정상화
- 포토탭 고도화 관련 클라이언트 프롬프트 작성
  - 상단 오늘/선택 날짜 표시
  - 캡션 수정 기능
  - 사진 자체 수정 불가
  - 수정 시 `수정됨` 표시
  - 공유하기 버튼 및 공유 링크 생성 UI
  - 삭제 완료 시 기록탭과 동일하게 `삭제되었습니다` 문구 표시
- 공유 전용 UI 관련 반복 조정 요청 정리
  - 대상 파일: `components/CaptionModal.tsx`, `app/photos/[externalId].tsx`, `api/share-photo.js`
  - 공유 미리보기 모달과 실제 공유화면을 최대한 일치시키는 방향
  - `공유화면 미리보기` 타이틀 제거/조정 요청 이력 있음
  - `{반려동물이름}의 하루 한 장` 문구 추가/삭제/복원 관련 조정 이력 있음
  - 최종 방향은 공유 미리보기 모달 기준으로 실제 공유화면을 맞추는 것
  - 날짜, 이미지, 캡션, 홍보문구, 버튼 여백을 일관되게 맞춤
  - 미리보기/공유화면 모두 가능하면 스크롤 없이 한 화면에 들어오도록 압축
- 공유 UI 세부 스타일 요청 정리
  - 좌측 상단 로고 여백 조정
  - 로고는 `32px x 32px`
  - `object-fit: contain`
  - `object-position: right center`
  - 이미지가 컨테이너 안에서 기준 방향에 맞게 정렬되도록 처리
  - `app/photos/[externalId].tsx`와 `api/share-photo.js`의 로고 위치 일관성 유지
  - `수정됨` 표시는 더 작고 약하게 조정
  - `꼬리에서 공유됨` 뱃지는 더 우측으로 붙도록 조정
- Phase E 및 후속 UX 안정화 완료
- 하루 한 장 사진 S3 업로드 서버/클라이언트 연동 완료
- 기록 사진 기능 추가 완료
  - DailyLog별 최대 3장
  - S3 업로드
  - `thumbnailUrl`, `mediumUrl` 저장
  - 조회 응답에 사진 목록 포함
  - 큰 이미지 보기
  - 삭제 UX
  - 업로드 실패/재시도 UX
- 기록 사진 UI 수정
  - X 버튼 잘림 문제 해결
- 운영 인프라 기준 유지
  - 도메인: `kkori.co.kr`
  - API 도메인: `api.kkori.co.kr`
  - 운영 API URL: `https://api.kkori.co.kr`
  - 현재 운영 Public IP: `13.124.220.29`

## 클라이언트 오류 메모

`Error while reading cache, falling back to a full crawl: Unable to deserialize cloned data`는 Expo/Metro 캐시 손상 가능성이 높다.

우선 아래 명령으로 캐시를 비우고 재시작한다.

```bash
npx expo start -c
```

필요 시 `.expo`, `node_modules/.cache`, temp metro/haste-map 캐시를 삭제한다.

## S3 업로드 문제 해결 기록

사진 업로드 중 `S3Exception: The specified bucket is not valid`가 발생했다.

원인은 IAM access key 자체 문제가 아니라 Docker 컨테이너 안에 AWS/S3 환경변수가 전달되지 않던 문제였다. `docker-compose.yml`의 `api.environment`에 아래 환경변수를 전달하도록 추가해 해결했다.

```yaml
AWS_ACCESS_KEY_ID: ${AWS_ACCESS_KEY_ID}
AWS_SECRET_ACCESS_KEY: ${AWS_SECRET_ACCESS_KEY}
AWS_REGION: ${AWS_REGION}
AWS_S3_BUCKET: ${AWS_S3_BUCKET}
```

운영 `.env`에는 아래 값들이 필요하다. 민감정보는 문서에 기록하지 않는다.

```dotenv
AWS_ACCESS_KEY_ID=...
AWS_SECRET_ACCESS_KEY=...
AWS_REGION=ap-northeast-2
AWS_S3_BUCKET=버킷명만
```

`AWS_S3_BUCKET`에는 `s3://`, URL, 경로를 넣지 않고 순수 버킷명만 넣어야 한다.

## Phase 상태

- Phase A: 로컬 API 구축 완료
- Phase B: React Native 클라이언트 연동 완료
- Phase C: Lightsail 배포 + 도메인 + HTTPS 적용 완료
- Phase D: OAuth 전용 User 소유권 전환 1차 구현 완료
  - 실제 Google/Kakao OAuth 토큰 검증 구현 완료
  - JWT 인증 필터와 refresh 재발급 API 구현 완료
  - 운영 `JWT_SECRET`, `GOOGLE_CLIENT_ID`, Kakao 키 설정 확인 필요
- Phase E: S3 사진 업로드 및 후속 UX 안정화 완료
  - 하루 한 장 사진 업로드 완료
  - 기록 사진 업로드 완료
  - thumbnail/medium 표시 완료
  - 큰 이미지 보기, 삭제, 재시도 UX 완료
- 설정/프로필 UX 정리 완료
  - 캐시 비우기 안정화
  - Notion 공개 링크 기반 정책/약관/업데이트 소식 연결
  - 알림 섹션 주석 처리 및 알림 권한 위치 이동
  - 이스터에그/후원 UI 정리
- Phase F: AI 리포트 미진행

## Phase E 후속 UX 안정화

- 완료
  - 업로드 실패 처리
  - 로딩/재시도 UI
  - thumbnail/medium 표시 품질 확인
  - 기록 사진 큰 이미지 보기
  - 기록 사진 삭제
  - 기록 사진 X 버튼 잘림 수정
- 별도 인프라 확인 항목
  - 8080 외부 포트 닫기 확인

## 웹/Vercel 결정

- `kkori.co.kr` / `www.kkori.co.kr`는 Vercel 유지
- 용도: 웹 랜딩, 개인정보처리방침, 계정삭제 안내, 가족 공유/메모리얼 페이지
- `api.kkori.co.kr`만 Lightsail API 서버로 연결

## 다음 작업 후보

1. 8080 외부 포트 닫기 확인
2. Vercel에 `kkori.co.kr` / `www.kkori.co.kr` 연결
3. 개인정보처리방침/계정삭제 안내 페이지 준비
4. 실제 Google/Kakao OAuth 실기기 로그인 QA
5. 운영 `JWT_SECRET`/`GOOGLE_CLIENT_ID`/Kakao 키 설정 반영 및 배포 환경 확인
6. 포토탭 고도화와 공유 UI 최종 QA
7. 개발/운영 API 환경 분리 값을 클라이언트 문서 또는 `.env` 예시에 정리
8. 프로필탭 고도화 완료 및 API/클라이언트 QA
9. 설정/정책/권한/후원 UI 최종 QA

## 운영 주의사항

- 현재 운영 IP는 반드시 `13.124.220.29` 기준으로 본다.
- `3.38.97.234`는 이전 서버 IP이며 운영 기준으로 사용하지 않는다.
- 현재 운영 API URL은 `https://api.kkori.co.kr`이다.
- AWS secret key 등 민감정보는 문서와 Git에 기록하지 않는다.
- `JWT_SECRET`도 민감정보이므로 문서와 Git에 실제 값을 기록하지 않는다.
- local/dev/prod 예시:
  - 로컬 `.env`: `JWT_SECRET=<32자 이상 로컬 개발용 랜덤 문자열>`
  - 개발 서버 `.env`: `JWT_SECRET=<32자 이상 개발 서버용 랜덤 문자열>`
  - 운영 서버 `.env`: `JWT_SECRET=<32자 이상 운영 전용 랜덤 문자열>`
  - 선택 값: `JWT_ACCESS_TOKEN_TTL_SECONDS=3600`, `JWT_REFRESH_TOKEN_TTL_SECONDS=2592000`
- Spring Boot 8080은 외부 공개 대상이 아니다. Nginx 내부 프록시 포트로만 사용한다.

## 작업 스타일 / 프롬프트 작성 규칙

- 앞으로 client/api 프롬프트에는 "직접 화면을 열어서 실행/확인"하라는 문구를 넣지 않는다.
- 화면 확인은 사용자가 직접 진행한다.
- 프롬프트의 검증은 "검증 포인트/기대 동작" 중심으로 작성한다.
- 불필요한 리팩터링은 금지한다.
- React Native + Expo + TypeScript strict를 유지한다.
- 공유 API/조회 로직은 UI 수정 중 변경하지 않는다.
- 인증은 OAuth 전용으로 간다. 이메일/비밀번호 가입은 추가하지 않는다.
- Device는 소유 주체가 아니다. User가 데이터 소유 주체이며, Device는 설치 기기/세션/푸시 보조 정보로만 본다.
- 기존 API 호환을 위해 `X-Device-Id` 흐름은 유지한다. 권한 검증은 userId 우선, 없으면 deviceId fallback을 사용한다.
- OAuth 토큰, JWT, provider 민감정보는 로그에 남기지 않는다.
- accessToken이 있으면 JWT 필터가 user 컨텍스트를 세팅한다. 토큰이 없으면 기존 Device 흐름을 사용한다.
- 프로필탭 고도화에서는 서버가 품종 목록을 책임지지 않는다. `breed`는 자유 문자열로 저장하고, 추천/자동완성은 클라이언트 상수로 처리한다.
- MVP에서는 프로필 입력 부담 최소화를 우선한다. 알레르기/약/질환 등 상세 건강 필드는 향후 건강관리/AI 리포트 단계에서 추가한다.
- 캐시 비우기 기능은 `AsyncStorage.clear()`로 전체 저장소를 지우지 않는다. 필요한 캐시 키만 선별 삭제하고 예외는 `try/catch`로 처리한다.
- 정책/약관/업데이트 소식은 현재 Notion 공개 링크 기준으로 연결한다.

## 자주 쓰는 명령

```bash
# 서버 실행
./gradlew bootRun

# 컴파일
./gradlew compileJava

# 테스트
./gradlew test

# 빌드
./gradlew build

# Docker Compose
docker compose up -d
docker compose logs -f api
docker compose logs -f db
```
