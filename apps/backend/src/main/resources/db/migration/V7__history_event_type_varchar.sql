-- Fix history_event.event_type column type for H2: ensure VARCHAR(64) for JPA @Enumerated(STRING).
-- Handles DBs where the column was created as native enum by an older ddl-auto=update.

ALTER TABLE history_event ADD COLUMN event_type_varchar VARCHAR(64) NOT NULL DEFAULT 'SIMULATION_STARTED';
UPDATE history_event SET event_type_varchar = CAST(event_type AS VARCHAR(64));
DROP INDEX IF EXISTS idx_history_event_city_type;
ALTER TABLE history_event DROP COLUMN event_type;
ALTER TABLE history_event RENAME COLUMN event_type_varchar TO event_type;

CREATE INDEX idx_history_event_city_type ON history_event(city_id, event_type);
