CREATE TABLE users (
 id UUID PRIMARY KEY, name VARCHAR(180) NOT NULL, email VARCHAR(255) NOT NULL UNIQUE,
 password_hash VARCHAR(100) NOT NULL, role VARCHAR(20) NOT NULL, requested_role VARCHAR(20) NOT NULL,
 status VARCHAR(20) NOT NULL, created_at TIMESTAMPTZ NOT NULL
);
CREATE TABLE programs (
 id UUID PRIMARY KEY, name VARCHAR(180) NOT NULL, type VARCHAR(30) NOT NULL,
 description VARCHAR(2000), active BOOLEAN NOT NULL DEFAULT TRUE, created_at TIMESTAMPTZ NOT NULL
);
CREATE TABLE materials (
 id UUID PRIMARY KEY, program_id UUID NOT NULL REFERENCES programs(id), title VARCHAR(255) NOT NULL,
 body VARCHAR(4000), resource_url VARCHAR(1000), created_at TIMESTAMPTZ NOT NULL
);
CREATE TABLE hafalan_tasks (
 id UUID PRIMARY KEY, program_id UUID NOT NULL REFERENCES programs(id), member_id UUID NOT NULL REFERENCES users(id),
 surah VARCHAR(180) NOT NULL, ayat_start INTEGER NOT NULL, ayat_end INTEGER NOT NULL, duration_days INTEGER NOT NULL,
 start_date DATE NOT NULL, due_date DATE NOT NULL, note VARCHAR(1000), assigned_by UUID NOT NULL REFERENCES users(id), created_at TIMESTAMPTZ NOT NULL
);
CREATE TABLE submissions (
 id UUID PRIMARY KEY, task_id UUID NOT NULL REFERENCES hafalan_tasks(id), member_id UUID NOT NULL REFERENCES users(id),
 note VARCHAR(3000), recording_url VARCHAR(1000), status VARCHAR(30) NOT NULL, reviewed_by UUID REFERENCES users(id),
 review_note VARCHAR(2000), reviewed_at TIMESTAMPTZ, submitted_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_users_status ON users(status);
CREATE INDEX idx_tasks_member_due ON hafalan_tasks(member_id,due_date);
CREATE INDEX idx_submissions_member_date ON submissions(member_id,submitted_at DESC);
