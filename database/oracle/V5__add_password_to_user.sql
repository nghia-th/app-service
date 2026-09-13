-- Adds a password column to tbl_user (BCrypt hash, never plaintext) backing the standalone JWT
-- login flow added to base.security (base.security.jwt.mode=STANDALONE) - see the design discussion
-- following the 2026-09-12 review's Critical finding #2 (no auth/authorization layer). Nullable:
-- existing rows created before this feature have no password yet and simply cannot log in via the
-- standalone flow until one is set; a deployment running base.security.jwt.mode=RESOURCE_SERVER
-- never uses this column at all (that mode's auth service owns credentials entirely). Guarded the
-- same way V2/V3/V4 guard their own DDL, since Oracle has no "ADD COLUMN IF NOT EXISTS" form.
DECLARE
    v_count NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_count FROM user_tab_columns WHERE table_name = 'TBL_USER' AND column_name = 'PASSWORD';
    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE tbl_user ADD password VARCHAR2(100)';
    END IF;
END;
/
