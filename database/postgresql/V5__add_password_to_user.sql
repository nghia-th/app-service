-- Adds a password column to tbl_user (BCrypt hash, never plaintext) backing the standalone JWT
-- login flow added to base.security (base.security.jwt.mode=STANDALONE) - see the design discussion
-- following the 2026-09-12 review's Critical finding #2 (no auth/authorization layer). Nullable:
-- existing rows created before this feature have no password yet and simply cannot log in via the
-- standalone flow until one is set; a deployment running base.security.jwt.mode=RESOURCE_SERVER
-- never uses this column at all (that mode's auth service owns credentials entirely). Sized like
-- username (VARCHAR(100)) - a BCrypt hash is a fixed 60 characters, this leaves headroom.
ALTER TABLE tbl_user ADD COLUMN IF NOT EXISTS password VARCHAR(100);
