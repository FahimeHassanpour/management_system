CREATE TABLE users (
                       id BIGSERIAL PRIMARY KEY,
                       name VARCHAR(100) NOT NULL,
                       email VARCHAR(150) NOT NULL UNIQUE,
                       password VARCHAR(255) NOT NULL,
                       role_id BIGINT,
                       full_name VARCHAR(255),
                       CONSTRAINT fk_users_role FOREIGN KEY (role_id) REFERENCES roles(id)
);