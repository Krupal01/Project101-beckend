CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE users (
                       id BIGSERIAL PRIMARY KEY,
                       email VARCHAR(255) NOT NULL UNIQUE,
                       phone VARCHAR(20),
                       password_hash VARCHAR(255) NOT NULL,
                       status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
                       email_verified BOOLEAN NOT NULL DEFAULT FALSE,
                       created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       updated_at TIMESTAMP
);

CREATE TABLE refresh_tokens (
                                id BIGSERIAL PRIMARY KEY,
                                user_id BIGINT NOT NULL,
                                token_hash VARCHAR(500) NOT NULL,
                                expires_at TIMESTAMP NOT NULL,
                                revoked BOOLEAN NOT NULL DEFAULT FALSE,
                                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                CONSTRAINT fk_user
                                    FOREIGN KEY (user_id)
                                        REFERENCES users(id)
                                        ON DELETE CASCADE
);

CREATE INDEX idx_user_email ON users(email);
CREATE INDEX idx_refresh_token ON refresh_tokens(token_hash);