-- Oracle does not index fully-null keys in a B-tree unique index, so multiple NULL emails are allowed.
DECLARE
    v_count NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_count FROM user_indexes WHERE index_name = 'UK_TBL_USER_EMAIL';
    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'CREATE UNIQUE INDEX uk_tbl_user_email ON tbl_user (email)';
    END IF;
END;
/
