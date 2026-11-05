CREATE TABLE users(

    id BIGSERIAL PRIMARY KEY,

    name VARCHAR(100) NOT NULL,

    email VARCHAR(100) NOT NULL,

    password_hash VARCHAR(100) NOT NULL,

    role VARCHAR(10) NOT NULL DEFAULT 'USER',

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chh_user_name CHECK (char_length(name) BETWEEN 2 AND 30),

    CONSTRAINT uq_users_email UNIQUE (email),

    CONSTRAINT chk_users_role CHECK (role IN ('USER', 'ADMIN'))
);

ALTER TABLE tasks ADD COLUMN user_id BIGINT;

ALTER TABLE tasks ALTER COLUMN user_id SET NOT NULL;

ALTER TABLE tasks
    ADD CONSTRAINT fk_tasks_user
    FOREIGN KEY (user_id) REFERENCES users(id);

CREATE INDEX idx_tasks_user_id ON tasks(user_id);