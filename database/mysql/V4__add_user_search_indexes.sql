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
-- indexes (Medium finding #14). MySQL has no portable "ADD INDEX IF NOT EXISTS" across supported
-- versions (same caveat as V3), so this relies on Flyway only ever running this migration once.
ALTER TABLE tbl_user ADD COLUMN IF NOT EXISTS full_name_unaccent VARCHAR(150);

ALTER TABLE tbl_user ADD INDEX idx_tbl_user_status (status);
ALTER TABLE tbl_user ADD INDEX idx_tbl_user_full_name (full_name);
ALTER TABLE tbl_user ADD INDEX idx_tbl_user_full_name_unaccent (full_name_unaccent);
