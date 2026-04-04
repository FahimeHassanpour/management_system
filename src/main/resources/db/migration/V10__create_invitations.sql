CREATE TABLE invitations (
                             id BIGSERIAL PRIMARY KEY,
                             email VARCHAR(150) NOT NULL,
                             token VARCHAR(255) NOT NULL UNIQUE,
                             expires_at TIMESTAMP NOT NULL,
                             created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                             used_at TIMESTAMP,
                             created_by_user_id BIGINT,
                             CONSTRAINT fk_invitation_creator FOREIGN KEY (created_by_user_id) REFERENCES users (id)
);

CREATE INDEX idx_invitations_email ON invitations (email);