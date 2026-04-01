CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE users (
                       id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                       phone VARCHAR(15) UNIQUE NOT NULL,
                       email VARCHAR(150) UNIQUE,

                       password_hash TEXT,
                       google_uid VARCHAR(128) UNIQUE,
                       apple_uid VARCHAR(128) UNIQUE,

                       phone_verified BOOLEAN DEFAULT FALSE,
                       email_verified BOOLEAN DEFAULT FALSE,

                       kyc_status VARCHAR(20) DEFAULT 'NONE',
                       global_status VARCHAR(20) DEFAULT 'ACTIVE',
                       role VARCHAR(30) DEFAULT 'SEEKER',

                       last_login_at TIMESTAMP,

                       created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE phone_otps (
                            id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                            phone VARCHAR(15) NOT NULL,
                            otp_hash TEXT NOT NULL,

                            purpose VARCHAR(30) NOT NULL,
                            attempts INT DEFAULT 0,

                            expires_at TIMESTAMP NOT NULL,
                            used_at TIMESTAMP,

                            created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE refresh_tokens (
                                id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                                user_id UUID NOT NULL,

                                token_hash TEXT NOT NULL,
                                device_info VARCHAR(500),
                                ip_address VARCHAR(45),

                                expires_at TIMESTAMP NOT NULL,
                                is_revoked BOOLEAN DEFAULT FALSE,

                                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                CONSTRAINT fk_refresh_user
                                    FOREIGN KEY (user_id)
                                        REFERENCES users(id)
                                        ON DELETE CASCADE
);

-- users
CREATE INDEX idx_users_phone ON users(phone);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role ON users(role);

-- phone otps
CREATE INDEX idx_phone_otps_phone ON phone_otps(phone);
CREATE INDEX idx_phone_otps_expires ON phone_otps(expires_at);

-- refresh tokens
CREATE INDEX idx_refresh_tokens_user ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_token ON refresh_tokens(token_hash);