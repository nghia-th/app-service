-- PostgreSQL treats NULL as distinct in a unique index, so multiple users with a NULL email are still allowed.
CREATE UNIQUE INDEX IF NOT EXISTS uk_tbl_user_email ON tbl_user (email);
