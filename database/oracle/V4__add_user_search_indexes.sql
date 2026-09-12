-- Adds the unaccented full_name column backing UserEntity.fullNameUnaccent (@Unaccent(from =
-- "fullName")) for accent-insensitive smart search, matching the flagship example in
-- docs/BASE_FRAMEWORK_GUIDE.md 2.8 and docs/PROMPT_TEMPLATES.md "Mau 2.3" - the module previously
-- never actually used this feature despite the docs advertising it on this exact entity (Medium
-- finding #13, 2026-09-12 review). Existing rows get a NULL full_name_unaccent until their next
-- save() (InsertExecutor/BatchInsertExecutor only populate @Unaccent fields on insert/update, not
-- retroactively) - a one-time gap for pre-existing data, not something newly-created or updated
-- rows will hit.
--
-- Also adds indexes on status/full_name/full_name_unaccent, the columns UserService.getUsersPaged()
-- filters/searches by - previously nothing existed beyond the PK and the username/email unique
-- indexes (Medium finding #14). Guarded the same way V2/V3 guard their CREATE TABLE/INDEX, since
-- Oracle has no "ADD COLUMN/INDEX IF NOT EXISTS" form.
DECLARE
    v_count NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_count FROM user_tab_columns WHERE table_name = 'TBL_USER' AND column_name = 'FULL_NAME_UNACCENT';
    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'ALTER TABLE tbl_user ADD full_name_unaccent VARCHAR2(150)';
    END IF;
END;
/

DECLARE
    v_count NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_count FROM user_indexes WHERE index_name = 'IDX_TBL_USER_STATUS';
    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'CREATE INDEX idx_tbl_user_status ON tbl_user (status)';
    END IF;
END;
/

DECLARE
    v_count NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_count FROM user_indexes WHERE index_name = 'IDX_TBL_USER_FULL_NAME';
    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'CREATE INDEX idx_tbl_user_full_name ON tbl_user (full_name)';
    END IF;
END;
/

DECLARE
    v_count NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_count FROM user_indexes WHERE index_name = 'IDX_TBL_USER_FULL_NAME_UNACC';
    IF v_count = 0 THEN
        EXECUTE IMMEDIATE 'CREATE INDEX idx_tbl_user_full_name_unacc ON tbl_user (full_name_unaccent)';
    END IF;
END;
/
