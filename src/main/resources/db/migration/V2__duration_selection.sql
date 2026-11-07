ALTER TABLE hafalan_tasks ADD COLUMN allowed_durations VARCHAR(255) NOT NULL DEFAULT '7,14,30';
UPDATE hafalan_tasks SET allowed_durations=duration_days::text WHERE duration_days NOT IN (7,14,30);
ALTER TABLE hafalan_tasks ALTER COLUMN duration_days DROP NOT NULL;
ALTER TABLE hafalan_tasks ALTER COLUMN due_date DROP NOT NULL;
ALTER TABLE hafalan_tasks ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE submissions ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
