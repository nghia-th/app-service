-- MySQL allows multiple NULLs in a UNIQUE index, so users with a NULL email are still allowed.
-- MySQL has no portable "ADD INDEX IF NOT EXISTS" across supported versions, so this relies on
-- Flyway only ever running this migration once (same assumption every other MySQL script here makes).
ALTER TABLE tbl_user ADD UNIQUE INDEX uk_tbl_user_email (email);
