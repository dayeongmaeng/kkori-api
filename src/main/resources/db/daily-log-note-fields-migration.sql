-- DailyLog 메모/고양이 MVP 필드 추가
-- ddl-auto: update 환경에서는 앱 재시작 시 자동 적용됨
-- 수동 적용이 필요한 경우 아래 SQL을 사용

ALTER TABLE daily_log ADD COLUMN IF NOT EXISTS meal_note   TEXT;
ALTER TABLE daily_log ADD COLUMN IF NOT EXISTS walk_note   TEXT;
ALTER TABLE daily_log ADD COLUMN IF NOT EXISTS poo_note    TEXT;
ALTER TABLE daily_log ADD COLUMN IF NOT EXISTS urine_note  TEXT;
ALTER TABLE daily_log ADD COLUMN IF NOT EXISTS water_note  TEXT;
ALTER TABLE daily_log ADD COLUMN IF NOT EXISTS play_minutes INTEGER;
ALTER TABLE daily_log ADD COLUMN IF NOT EXISTS play_note   TEXT;
ALTER TABLE daily_log ADD COLUMN IF NOT EXISTS urine_amount VARCHAR(10);
ALTER TABLE daily_log ADD COLUMN IF NOT EXISTS vomit_count  INTEGER;
ALTER TABLE daily_log ADD COLUMN IF NOT EXISTS vomit_note  TEXT;
