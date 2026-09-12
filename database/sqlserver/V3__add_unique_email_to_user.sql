-- Filtered index (WHERE email IS NOT NULL) because SQL Server treats NULL as a single duplicate-able
-- value in a plain UNIQUE index/constraint - without the filter, a second NULL email would be rejected.
IF NOT EXISTS (SELECT * FROM sys.indexes WHERE name = 'uk_tbl_user_email' AND object_id = OBJECT_ID('tbl_user'))
CREATE UNIQUE NONCLUSTERED INDEX uk_tbl_user_email ON tbl_user (email) WHERE email IS NOT NULL;
