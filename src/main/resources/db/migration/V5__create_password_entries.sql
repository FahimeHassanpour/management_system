CREATE TABLE password_entries (
                                  id BIGSERIAL PRIMARY KEY,
                                  title VARCHAR(150) NOT NULL,
                                  username VARCHAR(150) NOT NULL,
                                  password VARCHAR(255) NOT NULL,
                                  description TEXT,
                                  expiry_date TIMESTAMP,
                                  category_id BIGINT,
                                  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                  CONSTRAINT fk_password_category FOREIGN KEY (category_id) REFERENCES categories(id)
);