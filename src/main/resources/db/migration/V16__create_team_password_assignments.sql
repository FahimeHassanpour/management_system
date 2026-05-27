-- Explicit schema for password ↔ team links (matches TeamPasswordAssignment entity).
CREATE TABLE IF NOT EXISTS team_password_assignments (
    id BIGSERIAL PRIMARY KEY,
    team_id BIGINT NOT NULL,
    password_id BIGINT NOT NULL,
    CONSTRAINT fk_tpa_team
        FOREIGN KEY (team_id) REFERENCES teams (id) ON DELETE CASCADE,
    CONSTRAINT fk_tpa_password
        FOREIGN KEY (password_id) REFERENCES password_entries (id) ON DELETE CASCADE,
    CONSTRAINT uq_team_password_assignment UNIQUE (team_id, password_id)
);
