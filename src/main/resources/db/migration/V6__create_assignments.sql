CREATE TABLE assignments (
                             id BIGSERIAL PRIMARY KEY,
                             user_id BIGINT NOT NULL,
                             password_entry_id BIGINT NOT NULL,
                             assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                             CONSTRAINT fk_assignment_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                             CONSTRAINT fk_assignment_password FOREIGN KEY (password_entry_id) REFERENCES password_entries(id) ON DELETE CASCADE,
                             CONSTRAINT unique_assignment UNIQUE (user_id, password_entry_id)
);