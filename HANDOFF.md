# 인수인계 노트

세션 종료 또는 컨텍스트 전환 시 다음 세션에서 바로 이어받기 위한 최신 상태 기록.

마지막 업데이트: 2026-05-18

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

- 도메인 구매/결정: `kkori.co.kr`
- API 도메인 구성: `api.kkori.co.kr`
- DNS A 레코드 구성: `api.kkori.co.kr -> 13.124.220.29`
- 기존 512MB Lightsail 서버에서 새 1GB Lightsail 서버로 이전 완료
- 새 서버 Public IP 확정: `13.124.220.29`
- Nginx + Let's Encrypt + Certbot으로 HTTPS 적용 완료
- 최종 운영 API URL 확정: `https://api.kkori.co.kr`
- Certbot 인증서 발급 완료
- 새 서버에서 HTTPS 직접 확인 완료
- Spring Boot API 컨테이너와 PostgreSQL 컨테이너 정상 구동 확인
- S3 사진 업로드 정상 동작 확인
- 클라이언트 최종 검증 완료
  - 펫 조회
  - 일일 기록 저장/조회
  - 사진 메타 생성
  - 사진 업로드
  - 앱 재실행 후 서버/캐시 데이터 확인

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
- Phase E: S3 사진 업로드 서버/클라이언트 연동 및 실기기 검증 완료
- Phase F: AI 리포트 미진행

## Phase E 후속 UX 안정화

- 업로드 실패 처리
- 로딩/재시도 UI
- thumbnail/medium 표시 품질 확인
- 8080 외부 포트 닫기 확인

## 웹/Vercel 결정

- `kkori.co.kr` / `www.kkori.co.kr`는 Vercel 유지
- 용도: 웹 랜딩, 개인정보처리방침, 계정삭제 안내, 가족 공유/메모리얼 페이지
- `api.kkori.co.kr`만 Lightsail API 서버로 연결

## 다음 작업 후보

1. 8080 외부 포트 닫기 확인
2. 업로드 실패/재시도 UX 정리
3. Vercel에 `kkori.co.kr` / `www.kkori.co.kr` 연결
4. 개인정보처리방침/계정삭제 안내 페이지 준비
5. Phase D 로그인/회원가입 설계

## 운영 주의사항

- 현재 운영 IP는 반드시 `13.124.220.29` 기준으로 본다.
- `3.38.97.234`는 이전 서버 IP이며 운영 기준으로 사용하지 않는다.
- 현재 운영 API URL은 `https://api.kkori.co.kr`이다.
- AWS secret key 등 민감정보는 문서와 Git에 기록하지 않는다.
- Spring Boot 8080은 외부 공개 대상이 아니다. Nginx 내부 프록시 포트로만 사용한다.

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
