-- Soft delete 컬럼 추가 (ddl-auto: update 환경에서는 앱 재시작 시 자동 적용됨)
-- 수동 적용이 필요한 경우 아래 SQL을 사용
-- 기존 데이터는 deleted_at = NULL (삭제되지 않은 상태)로 유지됨

ALTER TABLE users          ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;
ALTER TABLE pet            ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;
ALTER TABLE caregiver      ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;
ALTER TABLE daily_log      ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;
ALTER TABLE daily_log_photo ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;
ALTER TABLE daily_photo    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP;
