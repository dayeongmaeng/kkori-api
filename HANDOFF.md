# 인수인계 노트

세션 종료 또는 컨텍스트 전환 시 다음 세션에서 바로 이어받기 위한 최신 상태 기록.

마지막 업데이트: 2026-05-19

## 현재 운영 상태

- 루트 도메인: `kkori.co.kr`
- 운영 API 도메인: `api.kkori.co.kr`
- DNS A 레코드: `api.kkori.co.kr -> 13.124.220.29`
- 운영 API URL: `https://api.kkori.co.kr`
- 클라이언트 환경변수: `EXPO_PUBLIC_API_URL=https://api.kkori.co.kr`
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
- Phase D: JWT 인증, 회원가입 미진행
- Phase E: S3 사진 업로드 및 후속 UX 안정화 완료
  - 하루 한 장 사진 업로드 완료
  - 기록 사진 업로드 완료
  - thumbnail/medium 표시 완료
  - 큰 이미지 보기, 삭제, 재시도 UX 완료
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
4. Phase D 로그인/회원가입 설계
5. JWT 인증 및 디바이스 ID -> User 연결 전략 정리
6. 포토탭 고도화와 공유 UI 최종 QA
7. 개발/운영 API 환경 분리 값을 클라이언트 문서 또는 `.env` 예시에 정리

## 운영 주의사항

- 현재 운영 IP는 반드시 `13.124.220.29` 기준으로 본다.
- `3.38.97.234`는 이전 서버 IP이며 운영 기준으로 사용하지 않는다.
- 현재 운영 API URL은 `https://api.kkori.co.kr`이다.
- AWS secret key 등 민감정보는 문서와 Git에 기록하지 않는다.
- Spring Boot 8080은 외부 공개 대상이 아니다. Nginx 내부 프록시 포트로만 사용한다.

## 작업 스타일 / 프롬프트 작성 규칙

- 앞으로 client/api 프롬프트에는 "직접 화면을 열어서 실행/확인"하라는 문구를 넣지 않는다.
- 화면 확인은 사용자가 직접 진행한다.
- 프롬프트의 검증은 "검증 포인트/기대 동작" 중심으로 작성한다.
- 불필요한 리팩터링은 금지한다.
- React Native + Expo + TypeScript strict를 유지한다.
- 공유 API/조회 로직은 UI 수정 중 변경하지 않는다.

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
