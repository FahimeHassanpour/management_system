CREATE TABLE teams (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    description VARCHAR(500)
);

CREATE TABLE user_team (
    id BIGSERIAL PRIMARY KEY,

    user_id BIGINT NOT NULL,
    team_id BIGINT NOT NULL,

    joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_user_teams_user
                       foreign key (user_id)
                       REFERENCES users(id)
                       ON DELETE CASCADE,

    CONSTRAINT fk_user_teams_team
        foreign key (team_id)
            REFERENCES teams(id)
            ON DELETE CASCADE
);