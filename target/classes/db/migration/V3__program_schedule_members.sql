ALTER TABLE programs ADD COLUMN scheduled_at TIMESTAMPTZ;
ALTER TABLE programs ADD COLUMN location VARCHAR(255);
ALTER TABLE programs ADD COLUMN group_name VARCHAR(255);
CREATE TABLE program_members (program_id UUID NOT NULL REFERENCES programs(id), member_id UUID NOT NULL REFERENCES users(id), PRIMARY KEY(program_id,member_id));
