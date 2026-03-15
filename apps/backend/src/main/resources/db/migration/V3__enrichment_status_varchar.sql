-- Fix enrichment_status column type for H2: ensure VARCHAR(16) for JPA @Enumerated(STRING).
-- Handles DBs where the column was created as native enum by an older ddl-auto=update.

-- history_event
ALTER TABLE history_event ADD COLUMN enrichment_status_varchar VARCHAR(16) NOT NULL DEFAULT 'NONE';
UPDATE history_event SET enrichment_status_varchar = CAST(enrichment_status AS VARCHAR(16));
ALTER TABLE history_event DROP COLUMN enrichment_status;
ALTER TABLE history_event RENAME COLUMN enrichment_status_varchar TO enrichment_status;

-- history_invention
ALTER TABLE history_invention ADD COLUMN enrichment_status_varchar VARCHAR(16) NOT NULL DEFAULT 'NONE';
UPDATE history_invention SET enrichment_status_varchar = CAST(enrichment_status AS VARCHAR(16));
ALTER TABLE history_invention DROP COLUMN enrichment_status;
ALTER TABLE history_invention RENAME COLUMN enrichment_status_varchar TO enrichment_status;
