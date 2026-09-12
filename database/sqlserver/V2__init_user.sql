IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='tbl_user' AND xtype='U')
CREATE TABLE tbl_user
(
    id         BIGINT IDENTITY(1,1) PRIMARY KEY,
    username   NVARCHAR(100) NOT NULL UNIQUE,
    email      NVARCHAR(150),
    full_name  NVARCHAR(150),
    status     NVARCHAR(50) DEFAULT 'ACTIVE',
    role       NVARCHAR(50) DEFAULT 'USER',
    created_at DATETIME2,
    updated_at DATETIME2,
    created_by NVARCHAR(100),
    updated_by NVARCHAR(100),
    deleted    BIT DEFAULT 0
);
