-- V5__cascade_user_deletes.sql

-- refresh_token.user_id -> app_user.id
ALTER TABLE refresh_token
    DROP CONSTRAINT IF EXISTS fk5wkt2p042y3lwltk29cvpxuh,
    ADD  CONSTRAINT refresh_token_user_fk
        FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE CASCADE;

-- user_roles.user_id -> app_user.id
ALTER TABLE user_roles
    DROP CONSTRAINT IF EXISTS fk6fql8djp64yp4q9b3qeyhr82b,
    ADD  CONSTRAINT user_roles_user_fk
        FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE CASCADE;

-- verification_token.user_id -> app_user.id
ALTER TABLE verification_token
    DROP CONSTRAINT IF EXISTS fk1tc78gv9fnf5oyv26e2uy0xy0,
    ADD  CONSTRAINT verification_token_user_fk
        FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE CASCADE;

-- password_reset_token.user_id -> app_user.id
ALTER TABLE password_reset_token
    DROP CONSTRAINT IF EXISTS fkli7wollcmb8tibymo3s94o57h,
    ADD  CONSTRAINT password_reset_token_user_fk
        FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE CASCADE;

-- mfa_challenge already has ON DELETE CASCADE; no change needed.
