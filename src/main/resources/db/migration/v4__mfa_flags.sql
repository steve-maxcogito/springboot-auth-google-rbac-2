-- V4__mfa_flags.sql

-- If you use gen_random_uuid() anywhere, make sure the extension exists.
-- (Required on many Postgres versions)
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- 1) Add columns with defaults to avoid NULL violations, then optionally drop defaults.
ALTER TABLE app_user
    ADD COLUMN IF NOT EXISTS mfa_required   boolean NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS mfa_enrolled   boolean NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS mfa_enforced_at timestamptz NULL;

-- If you want to force developers to set these on insert going forward,
-- you can drop the defaults after backfill:
ALTER TABLE app_user
    ALTER COLUMN mfa_required DROP DEFAULT,
    ALTER COLUMN mfa_enrolled DROP DEFAULT;

-- (Optional) If you're renaming semantics from mfa_enabled -> mfa_enrolled,
-- and you already have mfa_enabled from V3, you can backfill like this:
-- UPDATE app_user SET mfa_enrolled = mfa_enabled WHERE mfa_enabled IS NOT NULL;
