-- Google OAuth token 저장 테이블
-- ddl-auto: update로는 생성되지만 UNIQUE 제약은 별도 인덱스로 생성되므로 확인 필요.
-- 운영 배포 전 실행하거나 ddl-auto: update 첫 기동 후 검증한다.

CREATE TABLE IF NOT EXISTS user_oauth_token (
    id                      BIGSERIAL PRIMARY KEY,
    user_id                 BIGINT NOT NULL,
    provider                VARCHAR(20) NOT NULL,
    encrypted_access_token  TEXT,
    encrypted_refresh_token TEXT,
    access_token_expires_at TIMESTAMP,
    scope                   VARCHAR(500),
    revoked_at              TIMESTAMP,
    created_at              TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_user_oauth_token_user_provider UNIQUE (user_id, provider)
);

CREATE INDEX IF NOT EXISTS idx_user_oauth_token_user_id ON user_oauth_token (user_id);
