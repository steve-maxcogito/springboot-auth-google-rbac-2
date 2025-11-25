CREATE EXTENSION IF NOT EXISTS pgcrypto;


CREATE TABLE IF NOT EXISTS role (
                                    id   UUID PRIMARY KEY,
                                    name VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS app_user (
                                        id UUID PRIMARY KEY DEFAULT gen_random_uuid(), -- or use uuid_generate_v4() if you prefer
                                        username VARCHAR(64) NOT NULL UNIQUE,
                                        password_hash VARCHAR(255), -- adjust length as needed
                                        provider VARCHAR(16) NOT NULL, -- Enum, store as varchar
                                        first_name VARCHAR(64),
                                        middle_name VARCHAR(64),
                                        last_name VARCHAR(64),
                                        address_line1 VARCHAR(128),
                                        address_line2 VARCHAR(128),
                                        city VARCHAR(64),
                                        state VARCHAR(64),
                                        postal_code VARCHAR(32),
                                        country VARCHAR(64),
                                        last_login_at TIMESTAMPTZ,
                                        email VARCHAR(255) NOT NULL UNIQUE,
                                        email_verified BOOLEAN NOT NULL DEFAULT false,
                                        mfa_enabled BOOLEAN NOT NULL DEFAULT false,
                                        mfa_required BOOLEAN NOT NULL DEFAULT false,
                                        mfa_enrolled BOOLEAN NOT NULL DEFAULT false,
                                        mfa_enforced_at TIMESTAMPTZ,
                                        mfa_method VARCHAR(32),
                                        phone_number VARCHAR(32),
                                        google_sub VARCHAR(32)
);

-- [Already exists] CREATE EXTENSION IF NOT EXISTS pgcrypto;


-- user_roles join table, if not already present:
CREATE TABLE IF NOT EXISTS user_roles (
                                          user_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
                                          role_id UUID NOT NULL REFERENCES role(id) ON DELETE CASCADE,
                                          PRIMARY KEY (user_id, role_id)
);

CREATE TABLE IF NOT EXISTS verification_token (
                                                  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                                  user_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
                                                  token VARCHAR(128) NOT NULL UNIQUE,
                                                  expires_at TIMESTAMPTZ NOT NULL,
                                                  used BOOLEAN NOT NULL DEFAULT false
);

CREATE TABLE IF NOT EXISTS refresh_token (
                                             id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                             user_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
                                             token VARCHAR(128) NOT NULL UNIQUE,
                                             expires_at TIMESTAMPTZ NOT NULL,
                                             revoked BOOLEAN NOT NULL DEFAULT false,
                                             created_at TIMESTAMPTZ NOT NULL,
                                             revoked_at TIMESTAMPTZ,
                                             last_used_at TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS password_reset_token (
                                                    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                                    token VARCHAR(128) NOT NULL UNIQUE,
                                                    user_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
                                                    expires_at TIMESTAMPTZ NOT NULL,
                                                    consumed BOOLEAN NOT NULL DEFAULT false
);

CREATE TABLE IF NOT EXISTS mfa_challenge (
                                             id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                             user_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
                                             code_hash VARCHAR(255) NOT NULL,
                                             expires_at TIMESTAMPTZ NOT NULL,
                                             attempts INT NOT NULL,
                                             purpose VARCHAR(32) NOT NULL,
                                             created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Indexes already declared inline; extra if you want composite:
CREATE INDEX IF NOT EXISTS idx_mfa_challenge_user_purpose ON mfa_challenge(user_id, purpose);


