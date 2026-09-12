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
-- indexes (Medium finding #14). SQL Server has no "ADD COLUMN/INDEX IF NOT EXISTS" form, so each
-- statement is guarded the same way V2/V3 guard their CREATE TABLE/INDEX.
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('tbl_user') AND name = 'full_name_unaccent')
ALTER TABLE tbl_user ADD full_name_unaccent NVARCHAR(150);

IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_tbl_user_status' AND object_id = OBJECT_ID('tbl_user'))
CREATE INDEX idx_tbl_user_status ON tbl_user (status);

IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_tbl_user_full_name' AND object_id = OBJECT_ID('tbl_user'))
CREATE INDEX idx_tbl_user_full_name ON tbl_user (full_name);

IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'idx_tbl_user_full_name_unaccent' AND object_id = OBJECT_ID('tbl_user'))
CREATE INDEX idx_tbl_user_full_name_unaccent ON tbl_user (full_name_unaccent);
