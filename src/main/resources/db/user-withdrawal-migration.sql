-- 회원 탈퇴 기능 도입을 위한 DB 마이그레이션
-- ddl-auto=update는 NOT NULL 제약 제거 및 DEFAULT 추가를 자동 처리하지 않으므로 수동 실행 필요.

-- 1. provider / provider_user_id NOT NULL 제약 해제
--    탈퇴 시 두 컬럼을 null 처리하여 동일 계정으로 재가입을 허용한다.
--    PostgreSQL은 unique 제약에서 (NULL, NULL) 행을 각각 별개로 취급하므로
--    기존 uk_user_provider_provider_user_id 제약을 유지해도 재가입이 안전하다.
ALTER TABLE users ALTER COLUMN provider DROP NOT NULL;
ALTER TABLE users ALTER COLUMN provider_user_id DROP NOT NULL;

-- 2. status 컬럼 추가 (Hibernate ddl-auto=update가 추가하지 못할 경우 수동 실행)
ALTER TABLE users ADD COLUMN IF NOT EXISTS status VARCHAR(20);

-- 3. 기존 사용자 status 기본값 설정 및 NOT NULL 제약 적용
UPDATE users SET status = 'ACTIVE' WHERE status IS NULL;
ALTER TABLE users ALTER COLUMN status SET NOT NULL;
