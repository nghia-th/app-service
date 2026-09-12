IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='translate' AND xtype='U')
CREATE TABLE translate
(
    lang_key NVARCHAR(255) NOT NULL,
    lang     NVARCHAR(20)  NOT NULL,
    value    NVARCHAR(MAX),

    CONSTRAINT PK_translate PRIMARY KEY (lang_key, lang)
);
