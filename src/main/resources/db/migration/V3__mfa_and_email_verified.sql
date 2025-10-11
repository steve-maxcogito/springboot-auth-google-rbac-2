-- V3__mfa_and_email_verified.sql
ALTER TABLE app_user
    ADD COLUMN IF NOT EXISTS email_verified BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS mfa_enabled BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS mfa_method VARCHAR(32);

CREATE TABLE IF NOT EXISTS mfa_challenge (
                                             id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                             user_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
                                             code_hash VARCHAR(255) NOT NULL,
                                             expires_at TIMESTAMPTZ NOT NULL,
                                             attempts INT NOT NULL DEFAULT 0,
                                             purpose VARCHAR(32) NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_mfa_challenge_user_purpose
    ON mfa_challenge(user_id, purpose);
