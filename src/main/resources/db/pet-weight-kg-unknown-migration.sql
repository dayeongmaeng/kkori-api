-- weight_kg_unknown 컬럼 추가 (default false)
ALTER TABLE pet ADD COLUMN weight_kg_unknown boolean NOT NULL DEFAULT false;

-- 기존 weight_kg null 행을 weight_kg_unknown=true로 업데이트
UPDATE pet SET weight_kg_unknown = true WHERE weight_kg IS NULL;
